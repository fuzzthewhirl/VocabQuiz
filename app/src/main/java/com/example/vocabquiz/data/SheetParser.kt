package com.example.vocabquiz.data

import com.example.vocabquiz.model.LanguageCatalog
import com.example.vocabquiz.model.Vocab

data class RowIssue(
    val rowIndex: Int,
    val reason: SkipReason,
    val detail: String? = null
)

enum class SkipReason {
    MissingColumns,
    BlankSource,
    BlankTarget,
    UnsupportedLanguage,
    SameLanguagePair
}

data class LoadReport(
    val totalRows: Int,
    val validRows: Int,
    val skippedRows: Int,
    val skippedByReason: Map<SkipReason, Int>
)

data class ParseResult(
    val valid: List<Vocab>,
    val issues: List<RowIssue>,
    val report: LoadReport
)

enum class LoadIssue {
    NoValidRows,
    NoPairs
}

object LoadIssueResolver {
    fun resolve(report: LoadReport): LoadIssue? {
        if (report.validRows > 0) return null
        if (report.skippedRows == 0) return LoadIssue.NoValidRows
        val reasons = report.skippedByReason.keys
        val pairOnly = reasons.all { it == SkipReason.UnsupportedLanguage || it == SkipReason.SameLanguagePair }
        return if (pairOnly) LoadIssue.NoPairs else LoadIssue.NoValidRows
    }
}

object SheetParser {
    fun parse(rows: List<List<Any?>>): ParseResult {
        val issues = mutableListOf<RowIssue>()
        val valid = mutableListOf<Vocab>()

        fun norm(s: Any?) = s?.toString()?.trim().orEmpty()

        rows.forEachIndexed { index, row ->
            val rowIndex = index + 1
            if (row.size < 4) {
                issues += RowIssue(rowIndex, SkipReason.MissingColumns)
                return@forEachIndexed
            }

            val srcLangRaw = norm(row.getOrNull(0))
            val tgtLangRaw = norm(row.getOrNull(1))
            val source = norm(row.getOrNull(2))
            val target = norm(row.getOrNull(3))

            if (source.isBlank()) {
                issues += RowIssue(rowIndex, SkipReason.BlankSource)
                return@forEachIndexed
            }
            if (target.isBlank()) {
                issues += RowIssue(rowIndex, SkipReason.BlankTarget)
                return@forEachIndexed
            }

            val srcLang = LanguageCatalog.normalize(srcLangRaw)
            val tgtLang = LanguageCatalog.normalize(tgtLangRaw)
            if (srcLang == null || tgtLang == null) {
                issues += RowIssue(rowIndex, SkipReason.UnsupportedLanguage)
                return@forEachIndexed
            }

            if (srcLang == tgtLang) {
                issues += RowIssue(rowIndex, SkipReason.SameLanguagePair)
                return@forEachIndexed
            }

            valid += Vocab(
                source = source,
                target = target,
                srcLang = srcLang,
                tgtLang = tgtLang
            )
        }

        val skippedByReason = issues.groupingBy { it.reason }.eachCount()
        val report = LoadReport(
            totalRows = rows.size,
            validRows = valid.size,
            skippedRows = issues.size,
            skippedByReason = skippedByReason
        )

        return ParseResult(valid = valid, issues = issues, report = report)
    }
}
