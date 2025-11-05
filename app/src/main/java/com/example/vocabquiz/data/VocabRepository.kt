package com.example.vocabquiz.data

import android.content.Context
import android.util.Log
import com.example.vocabquiz.model.Lang
import com.example.vocabquiz.model.LanguagePair
import com.example.vocabquiz.model.Vocab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VocabRepository(
    private val context: Context,
    private val spreadsheetId: String,
    // ✅ put your actual tab name here (e.g., "Sheet1" or "Saved")
    private val sheetTab: String = "Saved translations"
) {
    private var all: List<Vocab> = emptyList()
    private var byPair: Map<LanguagePair, List<Vocab>> = emptyMap()

    suspend fun loadAll(): Map<LanguagePair, Int> = withContext(Dispatchers.IO) {
        val service = SheetsServiceFactory.create(context)
            ?: run { clearAndLog("No Sheets service (not signed in?)"); return@withContext emptyMap() }

        // Fixed range and fixed column order (A:D)
        val rangeA1 = "$sheetTab!A:D"
        val resp = runCatching {
            service.spreadsheets().values().get(spreadsheetId, rangeA1).execute()
        }.getOrElse { t ->
            clearAndLog("Sheets get failed for range=$rangeA1", t)
            return@withContext emptyMap()
        }

        val rows = resp.getValues() ?: emptyList()
        if (rows.isEmpty()) {
            clearAndLog("No rows returned (range=$rangeA1)")
            return@withContext emptyMap()
        }

        // A:srcLangName, B:tgtLangName, C:sourceWord, D:targetWord
        fun norm(s: Any?) = s?.toString()?.trim().orEmpty()
        fun normLangName(s: Any?): String? = Lang.Companion.normalize(norm(s))

        all = rows.mapNotNull { r ->
            val srcLang = normLangName(r.getOrNull(0))
            val tgtLang = normLangName(r.getOrNull(1))
            val source = norm(r.getOrNull(2))
            val target = norm(r.getOrNull(3))

            if (source.isEmpty() || target.isEmpty()) return@mapNotNull null
            if (srcLang == null || tgtLang == null) return@mapNotNull null

            Vocab(
                source = source,
                target = target,
                srcLang = srcLang,
                tgtLang = tgtLang
            )
        }

        // Keep only fi/es/en and exclude same-language pairs
        val allowed = setOf("fi", "es", "en")
        val before = all.size
        all =
            all.filter { it.srcLang in allowed && it.tgtLang in allowed && it.srcLang != it.tgtLang }
        Log.d(
            "VocabRepo",
            "Parsed $before rows, kept ${all.size} after lang filter (tab=$sheetTab)"
        )

        byPair = all.groupBy { LanguagePair(it.srcLang!!, it.tgtLang!!) }
        byPair.forEach { (pair, list) -> Log.d("VocabRepo", "Pair $pair -> ${list.size} rows") }

        byPair.mapValues { it.value.size }
    }

    fun availablePairs(): List<LanguagePair> = byPair.keys.sortedBy { it.toString() }

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