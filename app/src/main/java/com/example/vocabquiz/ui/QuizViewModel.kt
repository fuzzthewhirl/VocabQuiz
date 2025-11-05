package com.example.vocabquiz.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.vocabquiz.data.SettingsStore
import com.example.vocabquiz.data.VocabRepository
import com.example.vocabquiz.model.Direction
import com.example.vocabquiz.model.Lang
import com.example.vocabquiz.model.LanguagePair
import com.example.vocabquiz.model.Vocab
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class QuizState(
    val status: Status = Status.Loading,
    val sourceLang: Lang? = null,
    val targetLang: Lang? = null,
    val direction: Direction = Direction.SRC_TO_TGT,

    val pageOffset: Int = 0,
    val pageSize: Int = 10,

    val pool: List<Vocab> = emptyList(),
    val index: Int = 0,
    val promptText: String = "",
    val answerText: String = "",
    val revealed: Boolean = false
) {
    enum class Status { Loading, Ready, Error }

}



class QuizViewModel(app: Application) : AndroidViewModel(app) {

    private val settings = SettingsStore(app)

    // TODO: put your real Spreadsheet ID here
    private val spreadsheetId = "1HI8QRSYkGNsXvyO2Grx3o1wFe6Q9uscyfAO31Xe50QQ"
    private val repo = VocabRepository(app, spreadsheetId)

    private val _state = MutableStateFlow(QuizState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            // 1) Load sheet data
            repo.loadAll()

            // 2) Read saved settings (if any)
            val snap = settings.snapshot.first()
            val savedSrc = snap.src
            val savedTgt = snap.tgt
            val savedOffset = snap.offset.coerceAtLeast(0)
            val savedIndex  = snap.index.coerceAtLeast(0)

            // 3) Decide starting pair
            val startSrc = when (savedSrc) { "fi","es","en" -> savedSrc else -> "fi" }
            val startTgt = when (savedTgt) { "fi","es","en" -> savedTgt else -> "es" }

            // 4) Load that chunk and jump to saved index
            val pair = com.example.vocabquiz.model.LanguagePair(startSrc, startTgt)
            loadChunkFor(pair, _state.value.direction, savedOffset)

            _state.value = _state.value.copy(
                // reflect chosen pair in state (Lang enums)
                sourceLang = com.example.vocabquiz.model.Lang.valueOf(startSrc.uppercase()),
                targetLang = com.example.vocabquiz.model.Lang.valueOf(startTgt.uppercase())
            )

            // Set the saved card if available
            setCard(savedIndex, reveal = false)
        }
    }

    fun setLangs(src: Lang, tgt: Lang) {
        if (src == tgt) return
        viewModelScope.launch {
            settings.savePair(src.code, tgt.code)
            settings.saveOffset(0)
            settings.saveIndex(0)
        }
        val pair = LanguagePair(src.code, tgt.code)
        loadChunkFor(pair, _state.value.direction, 0)
        _state.value = _state.value.copy(sourceLang = src, targetLang = tgt, revealed = false)
    }

    fun changeDirection(direction: Direction) {
        val s = _state.value
        val src = s.sourceLang ?: return
        val tgt = s.targetLang ?: return
        _state.value = s.copy(direction = direction, revealed = false)
        setCard(s.index, reveal = false) // recompute prompt/answer for same card
    }

    // ui/QuizViewModel.kt
    fun nextPage() {
        val s = _state.value
        val pair = currentPair() ?: return
        val total = repo.pairSize(pair)
        if (total == 0) return

        val newOffset = s.pageOffset + s.pageSize
        if (newOffset >= total) {
            // no more data — stay on current page
            // (optional) Log or trigger a UI hint
            android.util.Log.d("QuizVM", "No next chunk for $pair (total=$total)")
            return
        }

        viewModelScope.launch { settings.saveOffset(newOffset); settings.saveIndex(0) }
        loadChunkFor(pair, s.direction, newOffset)
    }

    fun prevPage() {
        val s = _state.value
        val pair = currentPair() ?: return
        val newOffset = (s.pageOffset - s.pageSize).coerceAtLeast(0)
        if (newOffset == s.pageOffset) return

        viewModelScope.launch { settings.saveOffset(newOffset); settings.saveIndex(0) }
        loadChunkFor(pair, s.direction, newOffset)
    }

    fun prevCard() {
        val s = _state.value
        if (s.pool.isEmpty()) return
        val prev = (s.index - 1 + s.pool.size).mod(s.pool.size)
        setCard(prev, reveal = false)
    }

    fun nextCard() {
        val s = _state.value
        if (s.pool.isEmpty()) return
        val next = (s.index + 1).mod(s.pool.size)
        setCard(next, reveal = false)
        viewModelScope.launch { settings.saveIndex(next) }
    }

    fun toggleReveal() {
        _state.value = _state.value.copy(revealed = !_state.value.revealed)
    }

    private fun currentPair(): LanguagePair? {
        val s = _state.value
        val src = s.sourceLang ?: return null
        val tgt = s.targetLang ?: return null
        return LanguagePair(src.code, tgt.code)
    }

    private fun loadChunkFor(pair: LanguagePair, direction: Direction, offset: Int) {
        viewModelScope.launch {
            val size = _state.value.pageSize
            val chunk = repo.getChunk(pair, offset, size)
            val status = if (chunk.isEmpty()) QuizState.Status.Error else QuizState.Status.Ready
            _state.value = _state.value.copy(
                status = status,
                pageOffset = offset,
                pool = chunk,
                index = 0,
                revealed = false
            )
            if (chunk.isNotEmpty()) setCard(0, reveal = false)
        }
    }

    private fun setCard(i: Int, reveal: Boolean) {
        val s = _state.value
        val v = s.pool.getOrNull(i) ?: return
        val (prompt, answer) = when (s.direction) {
            Direction.SRC_TO_TGT -> v.source to v.target
            Direction.TGT_TO_SRC -> v.target to v.source
        }
        _state.value = s.copy(index = i, promptText = prompt, answerText = answer, revealed = reveal)
    }
}
