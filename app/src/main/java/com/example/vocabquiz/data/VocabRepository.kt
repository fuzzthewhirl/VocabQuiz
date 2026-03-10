package com.example.vocabquiz.data

import DriveServiceFactory
import SheetsServiceFactory
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.vocabquiz.model.LanguagePair
import com.example.vocabquiz.model.Vocab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class VocabRepository(
    private val context: Context
) {
    private var all: List<Vocab> = emptyList()
    private var byPair: Map<LanguagePair, List<Vocab>> = emptyMap()

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun loadAll(spreadsheetId: String, sheetTab: String): Result<LoadOutcome> =
        withContext(Dispatchers.IO) {

        if (!isNetworkAvailable()) {
            Log.e("VocabRepo", "No internet connection available.")
            clearAndLog("No internet connection available.")
            return@withContext Result.failure(IOException("No internet connection"))
        }
        else{
            Log.i("VocabRepo", "Internet connection OK.")
        }
        val service = SheetsServiceFactory.create(context)
            ?: run {
                clearAndLog("No Sheets service (not signed in?)")
                return@withContext Result.failure(IllegalStateException("No Sheets service"))
            }

        val resolvedSheet = resolveSheetName(service, spreadsheetId, sheetTab)
        if (resolvedSheet == null) {
            clearAndLog("Sheet tab not found: '$sheetTab'")
            return@withContext Result.failure(IllegalStateException("Sheet tab not found: '$sheetTab'"))
        }
        val effectiveSheet = resolvedSheet
        if (resolvedSheet != sheetTab) {
            Log.w("VocabRepo", "Using resolved sheet name: $resolvedSheet")
        }

        // Fixed range and fixed column order (A:D)
        val rangeA1 = "${formatSheetName(effectiveSheet)}!A:D"
        val resp = runCatching {
            service.spreadsheets().values().get(spreadsheetId, rangeA1).execute()
        }.getOrElse { t ->
            clearAndLog("Sheets get failed for range=$rangeA1", t)
            return@withContext Result.failure(t)
        }

        val rows = resp.getValues() ?: emptyList()
        val parsed = SheetParser.parse(rows)
        all = parsed.valid
        byPair = all.groupBy { LanguagePair(it.srcLang!!, it.tgtLang!!) }
        byPair.forEach { (pair, list) -> Log.d("VocabRepo", "Pair $pair -> ${list.size} rows") }

        if (rows.isEmpty()) {
            clearAndLog("No rows returned (range=$rangeA1)")
        }

        Log.d(
            "VocabRepo",
            "Parsed ${parsed.report.totalRows} rows, kept ${parsed.report.validRows} " +
                "(skipped ${parsed.report.skippedRows}) (tab=$sheetTab)"
        )

        Result.success(
            LoadOutcome(
                sizes = byPair.mapValues { it.value.size },
                report = parsed.report
            )
        )
    }

    suspend fun getSheetNames(spreadsheetId: String): Result<List<String>> = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable()) {
            Log.e("VocabRepo", "No internet connection available.")
            return@withContext Result.failure(IOException("No internet connection"))
        }
        val service = SheetsServiceFactory.create(context)
            ?: run {
                Log.e("VocabRepo", "No Sheets service (not signed in?)")
                return@withContext Result.failure(IllegalStateException("No Sheets service"))
            }

        val resp = runCatching {
            service.spreadsheets().get(spreadsheetId).setFields("sheets.properties.title").execute()
        }.getOrElse { t ->
            Log.e("VocabRepo", "Sheets metadata fetch failed", t)
            return@withContext Result.failure(t)
        }

        val names = resp.sheets?.mapNotNull { it.properties?.title } ?: emptyList()
        Result.success(names)
    }


    fun pairSize(pair: LanguagePair): Int = byPair[pair]?.size ?: 0

    fun availablePairs(): List<LanguagePair> = byPair.keys.sortedBy { it.toString() }

    suspend fun getSpreadsheetsInFolder(folderName: String): Result<List<SpreadsheetInfo>> =
        withContext(Dispatchers.IO) {
            if (!isNetworkAvailable()) {
                Log.e("VocabRepo", "No internet connection available.")
                return@withContext Result.failure(IOException("No internet connection"))
            }
            val service = DriveServiceFactory.create(context)
                ?: run {
                    Log.e("VocabRepo", "No Drive service (not signed in?)")
                    return@withContext Result.failure(IllegalStateException("No Drive service"))
                }

            val folderResp = runCatching {
                service.files().list()
                    .setQ(
                        "mimeType='application/vnd.google-apps.folder' and name='$folderName' " +
                            "and 'root' in parents and trashed=false"
                    )
                    .setFields("files(id,name)")
                    .execute()
            }.getOrElse { t ->
                Log.e("VocabRepo", "Drive folder lookup failed", t)
                return@withContext Result.failure(t)
            }

            val folderId = folderResp.files?.firstOrNull()?.id
            if (folderId.isNullOrBlank()) {
                return@withContext Result.failure(FolderNotFoundException(folderName))
            }

            val filesResp = runCatching {
                service.files().list()
                    .setQ(
                        "mimeType='application/vnd.google-apps.spreadsheet' and '$folderId' in parents " +
                            "and trashed=false"
                    )
                    .setFields("files(id,name)")
                    .execute()
            }.getOrElse { t ->
                Log.e("VocabRepo", "Drive spreadsheet list failed", t)
                return@withContext Result.failure(t)
            }

            val spreadsheets = filesResp.files
                ?.mapNotNull { file ->
                    val id = file.id
                    val name = file.name
                    if (id.isNullOrBlank() || name.isNullOrBlank()) null else SpreadsheetInfo(id, name)
                }
                .orEmpty()
                .sortedBy { it.name.lowercase() }

            Result.success(spreadsheets)
        }

    fun getChunk(pair: LanguagePair, offset: Int, size: Int): List<Vocab> {
        val list = byPair[pair].orEmpty()
        if (list.isEmpty() || offset >= list.size) return emptyList()
        val to = minOf(offset + size, list.size)
        return list.subList(offset, to)
    }

    private fun clearAndLog(msg: String, t: Throwable? = null) {
        all = emptyList(); byPair = emptyMap()
        if (t != null) Log.e("VocabRepo", msg, t) else Log.e("VocabRepo", msg)
    }

}

internal fun formatSheetName(sheetTab: String): String {
    val escaped = sheetTab.replace("'", "''")
    return "'$escaped'"
}

private fun resolveSheetName(
    service: com.google.api.services.sheets.v4.Sheets,
    spreadsheetId: String,
    desired: String
): String? {
    val names = runCatching {
        service.spreadsheets().get(spreadsheetId)
            .setFields("sheets.properties.title")
            .execute()
            .sheets
            ?.mapNotNull { it.properties?.title }
            .orEmpty()
    }.getOrElse { return null }

    Log.d("VocabRepo", "Available sheets: ${names.joinToString()}")

    if (names.contains(desired)) return desired

    val normalizedDesired = normalizeSheetName(desired)
    val resolved = names.firstOrNull { normalizeSheetName(it) == normalizedDesired }
    if (resolved == null) {
        Log.w(
            "VocabRepo",
            "Sheet tab not found. desired='$desired' normalized='${normalizedDesired}'"
        )
    }
    return resolved
}

private fun normalizeSheetName(name: String): String {
    return name
        .replace("\u00A0", " ")
        .trim()
        .replace(Regex("\\s+"), " ")
        .lowercase()
}

data class SpreadsheetInfo(
    val id: String,
    val name: String
)

class FolderNotFoundException(folderName: String) : Exception("Folder not found: $folderName")
