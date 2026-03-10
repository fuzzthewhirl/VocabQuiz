package com.example.vocabquiz.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SheetRangeFormatTest {
    @Test
    fun quotesSheetNamesWithSpaces() {
        val result = formatSheetName("Saved translations")
        assertEquals("'Saved translations'", result)
    }

    @Test
    fun escapesApostrophesInSheetNames() {
        val result = formatSheetName("Bob's Sheet")
        assertEquals("'Bob''s Sheet'", result)
    }
}
