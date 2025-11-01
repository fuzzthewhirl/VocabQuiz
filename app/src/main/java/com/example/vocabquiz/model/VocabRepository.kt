package com.example.vocabquiz.data

import android.content.Context
import com.example.vocabquiz.model.Vocab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VocabRepository(
    private val context: Context,
    private val spreadsheetId: String,
    private val rangeA1: String = "Sheet1!A:E" // adjust to your tab & columns
) {
    suspend fun fetch(): List<Vocab> = withContext(Dispatchers.IO) {
        val service = SheetsServiceFactory.create(context) ?: return@withContext emptyList()
        val resp = service.spreadsheets().values().get(spreadsheetId, rangeA1).execute()
        val rows = resp.getValues() ?: return@withContext emptyList()
        rows.drop(1).mapNotNull { r ->
            val src = r.getOrNull(1)?.toString()?.trim().orEmpty()
            val tgt = r.getOrNull(2)?.toString()?.trim().orEmpty()
            if (src.isEmpty() || tgt.isEmpty()) null
            else Vocab(
                source = src,
                target = tgt,
                srcLang = r.getOrNull(3)?.toString(),
                tgtLang = r.getOrNull(4)?.toString()
            )
        }
    }
}
