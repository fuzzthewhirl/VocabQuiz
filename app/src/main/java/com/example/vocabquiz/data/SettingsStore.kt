package com.example.vocabquiz.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "vocab_settings")

class SettingsStore(private val context: Context) {

    object Keys {
        val LAST_SRC  = stringPreferencesKey("last_src")   // "fi" | "es" | "en"
        val LAST_TGT  = stringPreferencesKey("last_tgt")
        val LAST_OFF  = intPreferencesKey("last_offset")   // chunk offset
        val LAST_IDX  = intPreferencesKey("last_index")    // index within chunk
    }

    data class Snapshot(
        val src: String? = null,
        val tgt: String? = null,
        val offset: Int = 0,
        val index: Int = 0
    )

    val snapshot: Flow<Snapshot> = context.dataStore.data.map { p ->
        Snapshot(
            src = p[Keys.LAST_SRC],
            tgt = p[Keys.LAST_TGT],
            offset = p[Keys.LAST_OFF] ?: 0,
            index  = p[Keys.LAST_IDX] ?: 0
        )
    }

    suspend fun savePair(src: String, tgt: String) {
        context.dataStore.edit { it[Keys.LAST_SRC] = src; it[Keys.LAST_TGT] = tgt }
    }
    suspend fun saveOffset(offset: Int) {
        context.dataStore.edit { it[Keys.LAST_OFF] = offset }
    }
    suspend fun saveIndex(index: Int) {
        context.dataStore.edit { it[Keys.LAST_IDX] = index }
    }
}
