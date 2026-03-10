package com.example.vocabquiz.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SheetSelectionTest {

    @Test
    fun startupSelectsFirstWhenNoSavedSheet() {
        val names = listOf("Sheet A", "Sheet B")
        val selection = selectSheetForStartup(null, names)

        assertEquals("Sheet A", selection.selected)
        assertFalse(selection.missing)
    }

    @Test
    fun startupKeepsSavedWhenPresent() {
        val names = listOf("Sheet A", "Sheet B")
        val selection = selectSheetForStartup("Sheet B", names)

        assertEquals("Sheet B", selection.selected)
        assertFalse(selection.missing)
    }

    @Test
    fun startupSignalsMissingWhenSavedNotFound() {
        val names = listOf("Sheet A", "Sheet B")
        val selection = selectSheetForStartup("Old Sheet", names)

        assertNull(selection.selected)
        assertTrue(selection.missing)
    }

    @Test
    fun refreshKeepsCurrentWhenPresent() {
        val names = listOf("Sheet A", "Sheet B")
        val selection = selectSheetForRefresh("Sheet A", names)

        assertEquals("Sheet A", selection.selected)
        assertFalse(selection.missing)
    }

    @Test
    fun refreshSignalsMissingWhenCurrentNotFound() {
        val names = listOf("Sheet A", "Sheet B")
        val selection = selectSheetForRefresh("Old Sheet", names)

        assertNull(selection.selected)
        assertTrue(selection.missing)
    }

    @Test
    fun refreshReturnsNullWhenNoCurrentSelection() {
        val names = listOf("Sheet A", "Sheet B")
        val selection = selectSheetForRefresh(null, names)

        assertNull(selection.selected)
        assertFalse(selection.missing)
    }
}
