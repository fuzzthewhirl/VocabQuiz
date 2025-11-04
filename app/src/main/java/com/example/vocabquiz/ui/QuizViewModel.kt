package com.example.vocabquiz.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.vocabquiz.data.VocabRepository
import com.example.vocabquiz.model.Direction
import com.example.vocabquiz.model.Lang
import com.example.vocabquiz.model.LanguagePair
import com.example.vocabquiz.model.Vocab
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class QuizState(
    val status: Status = Status.Loading,
    val sourceLang: Lang? = null,
    val targetLang: Lang? = null,
    val direction: Direction = Direction.SRC_TO_TGT,

    val pageOffset: Int = 0,
    val pageSize: Int = 100,

    val pool: List<Vocab> = emptyList(),
    val index: Int = 0,
    val promptText: String = "",
    val answerText: String = "",
    val revealed: Boolean = false
) {
    enum class Status { Loading, Ready, Error }
}

class QuizViewModel(app: Application) : AndroidViewModel(app) {

    // TODO: put your real Spreadsheet ID here
    private val spreadsheetId = "1HI8QRSYkGNsXvyO2Grx3o1wFe6Q9uscyfAO31Xe50QQ"
    private val repo = VocabRepository(app, spreadsheetId)

    private val _state = MutableStateFlow(QuizState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repo.loadAll()
            // default to fi -> es if available
            val src = Lang.FI
            val tgt = Lang.ES
            _state.value = _state.value.copy(status = QuizState.Status.Ready, sourceLang = src, targetLang = tgt)
            setLangs(src, tgt)
        }
    }

    fun setLangs(src: Lang, tgt: Lang) {
        if (src == tgt) return
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

    fun nextPage() {
        val s = _state.value
        val pair = currentPair() ?: return
        loadChunkFor(pair, s.direction, s.pageOffset + s.pageSize)
    }

    fun prevPage() {
        val s = _state.value
        val pair = currentPair() ?: return
        loadChunkFor(pair, s.direction, (s.pageOffset - s.pageSize).coerceAtLeast(0))
    }

    fun nextCard() {
        val s = _state.value
        if (s.pool.isEmpty()) return
        val next = (s.index + 1).mod(s.pool.size)
        setCard(next, reveal = false)
    }

    fun prevCard() {
        val s = _state.value
        if (s.pool.isEmpty()) return
        val prev = (s.index - 1 + s.pool.size).mod(s.pool.size)
        setCard(prev, reveal = false)
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
