import com.example.vocabquiz.data.LoadIssue
import com.example.vocabquiz.data.LoadIssueResolver
import com.example.vocabquiz.data.SheetParser
import com.example.vocabquiz.data.SkipReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SheetParserTest {
    @Test
    fun parsesAllValidRows() {
        val rows = listOf(
            listOf("English", "Finnish", "cat", "kissa"),
            listOf("Spanish", "English", "hola", "hello")
        )

        val result = SheetParser.parse(rows)

        assertEquals(2, result.valid.size)
        assertEquals(2, result.report.validRows)
        assertEquals(0, result.report.skippedRows)
    }

    @Test
    fun skipsRowsWithMissingColumns() {
        val rows = listOf(
            listOf("English", "Finnish", "cat")
        )

        val result = SheetParser.parse(rows)

        assertEquals(0, result.valid.size)
        assertEquals(1, result.report.skippedRows)
        assertEquals(1, result.report.skippedByReason[SkipReason.MissingColumns])
    }

    @Test
    fun skipsRowsWithBlankWords() {
        val rows = listOf(
            listOf("English", "Finnish", "", "kissa"),
            listOf("English", "Finnish", "cat", " ")
        )

        val result = SheetParser.parse(rows)

        assertEquals(0, result.valid.size)
        assertEquals(2, result.report.skippedRows)
        assertEquals(1, result.report.skippedByReason[SkipReason.BlankSource])
        assertEquals(1, result.report.skippedByReason[SkipReason.BlankTarget])
    }

    @Test
    fun skipsRowsWithUnsupportedLanguages() {
        val rows = listOf(
            listOf("Klingon", "Finnish", "qapla", "..."),
            listOf("English", "Elvish", "ring", "...")
        )

        val result = SheetParser.parse(rows)

        assertEquals(0, result.valid.size)
        assertEquals(2, result.report.skippedRows)
        assertEquals(2, result.report.skippedByReason[SkipReason.UnsupportedLanguage])
    }

    @Test
    fun skipsSameLanguagePairs() {
        val rows = listOf(
            listOf("English", "English", "dog", "dog"),
            listOf("Finnish", "Finnish", "koira", "koira")
        )

        val result = SheetParser.parse(rows)

        assertEquals(0, result.valid.size)
        assertEquals(2, result.report.skippedRows)
        assertEquals(2, result.report.skippedByReason[SkipReason.SameLanguagePair])
    }

    @Test
    fun reportsPartialLoadSummary() {
        val rows = listOf(
            listOf("English", "Finnish", "cat", "kissa"),
            listOf("English", "English", "dog", "dog"),
            listOf("English", "Finnish", "", "auto"),
            listOf("Klingon", "Finnish", "qapla", "...")
        )

        val result = SheetParser.parse(rows)

        assertEquals(1, result.valid.size)
        assertEquals(4, result.report.totalRows)
        assertEquals(3, result.report.skippedRows)
        assertTrue(result.report.skippedByReason.isNotEmpty())
    }

    @Test
    fun resolvesNoValidRows() {
        val rows = listOf(
            listOf("English", "Finnish", "", "kissa"),
            listOf("English", "Finnish", "cat", " ")
        )

        val report = SheetParser.parse(rows).report
        val issue = LoadIssueResolver.resolve(report)

        assertEquals(LoadIssue.NoValidRows, issue)
    }

    @Test
    fun resolvesNoPairsWhenOnlyUnsupportedOrSameLanguage() {
        val rows = listOf(
            listOf("English", "English", "cat", "cat"),
            listOf("Klingon", "Finnish", "qapla", "...")
        )

        val report = SheetParser.parse(rows).report
        val issue = LoadIssueResolver.resolve(report)

        assertEquals(LoadIssue.NoPairs, issue)
    }

    @Test
    fun resolvesNoValidRowsWhenMixedIssues() {
        val rows = listOf(
            listOf("English", "Finnish", "cat"),
            listOf("Klingon", "Finnish", "qapla", "...")
        )

        val report = SheetParser.parse(rows).report
        val issue = LoadIssueResolver.resolve(report)

        assertEquals(LoadIssue.NoValidRows, issue)
    }

    @Test
    fun resolvesNoIssueWhenValidRowsExist() {
        val rows = listOf(
            listOf("English", "Finnish", "cat", "kissa"),
            listOf("English", "English", "dog", "dog")
        )

        val report = SheetParser.parse(rows).report
        val issue = LoadIssueResolver.resolve(report)

        assertNull(issue)
    }
}
