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
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.random.Random
import kotlin.math.max
import kotlin.math.min

data class QuizState(
    val status: Status = Status.Loading,
    val sourceLang: Lang? = null,
    val targetLang: Lang? = null,
    val direction: Direction = Direction.SRC_TO_TGT,

    val pageOffset: Int = 0,
    val pageSize: Int = 10, // <= 10-word chunks

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

    // TODO: put your real Spreadsheet ID here (you already did)
    private val spreadsheetId = "1HI8QRSYkGNsXvyO2Grx3o1wFe6Q9uscyfAO31Xe50QQ"
    private val repo = VocabRepository(app, spreadsheetId)

    private val _state = MutableStateFlow(QuizState())
    val state = _state.asStateFlow()

    // single-flight gate to avoid overlapping loads
    private var loading = false

    init {
        viewModelScope.launch {
            // 1) Load all data (IO thread is handled inside repo)
            repo.loadAll()

            // 2) Read saved settings with a timeout so we never block startup
            val snap = withTimeoutOrNull(1500) { settings.snapshot.first() } ?: SettingsStore.Snapshot()

            // 3) Decide starting pair (saved or defaults)
            val startSrc = when (snap.src) { "fi","es","en" -> snap.src else -> "fi" }
            val startTgt = when (snap.tgt) { "fi","es","en" -> snap.tgt else -> "es" }
            val pair = LanguagePair(startSrc, startTgt)

            // Reflect chosen pair in state (Lang enums)
            _state.value = _state.value.copy(
                sourceLang = Lang.valueOf(startSrc.uppercase()),
                targetLang = Lang.valueOf(startTgt.uppercase())
            )

            // 4) Choose offset: resume if valid, else random chunk start
            val total = repo.pairSize(pair)
            val size  = _state.value.pageSize
            val hasResume = snap.offset in 0 until total
            val maxStart = max(0, total - size)
            val startOffset = if (hasResume) {
                // align to page boundary
                (snap.offset / size) * size
            } else {
                if (total <= 0) 0 else Random.nextInt(0, maxStart + 1)
            }

            // Persist where we start (nice to have)
            settings.savePair(startSrc, startTgt)
            settings.saveOffset(startOffset)
            if (!hasResume) settings.saveIndex(0)

            // 5) Load that page and jump to saved (clamped) index
            val desiredIndex = if (hasResume) snap.index else 0
            loadChunkFor(pair, _state.value.direction, startOffset, desiredIndex)
        }
    }

    fun setLangs(src: Lang, tgt: Lang) {
        if (src == tgt) return
        val pair = LanguagePair(src.code, tgt.code)

        viewModelScope.launch {
            // randomize a fresh page whenever pair changes
            val total = repo.pairSize(pair)
            val size  = _state.value.pageSize
            val maxStart = max(0, total - size)
            val randomOffset = if (total <= 0) 0 else Random.nextInt(0, maxStart + 1)

            settings.savePair(src.code, tgt.code)
            settings.saveOffset(randomOffset)
            settings.saveIndex(0)

            _state.value = _state.value.copy(sourceLang = src, targetLang = tgt, revealed = false)
            loadChunkFor(pair, _state.value.direction, randomOffset, desiredIndex = 0)
        }
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
        val total = repo.pairSize(pair)
        if (total == 0) return

        val newOffset = s.pageOffset + s.pageSize
        if (newOffset >= total) {
            android.util.Log.d("QuizVM", "No next chunk for $pair (total=$total)")
            return
        }

        viewModelScope.launch { settings.saveOffset(newOffset); settings.saveIndex(0) }
        loadChunkFor(pair, s.direction, newOffset, desiredIndex = 0)
    }

    fun prevPage() {
        val s = _state.value
        val pair = currentPair() ?: return
        val newOffset = (s.pageOffset - s.pageSize).coerceAtLeast(0)
        if (newOffset == s.pageOffset) return

        viewModelScope.launch { settings.saveOffset(newOffset); settings.saveIndex(0) }
        loadChunkFor(pair, s.direction, newOffset, desiredIndex = 0)
    }

    fun prevCard() {
        val s = _state.value
        if (s.pool.isEmpty()) return
        val prev = (s.index - 1 + s.pool.size).mod(s.pool.size)
        setCard(prev, reveal = false)
        viewModelScope.launch { settings.saveIndex(prev) }
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

    // now accepts desiredIndex (will be clamped to chunk size)
    private fun loadChunkFor(
        pair: LanguagePair,
        direction: Direction,
        offset: Int,
        desiredIndex: Int = 0
    ) {
        if (loading) return
        loading = true
        viewModelScope.launch {
            try {
                val size = _state.value.pageSize
                val chunk = repo.getChunk(pair, offset, size)
                if (chunk.isEmpty()) {
                    android.util.Log.d("QuizVM", "Empty chunk at offset=$offset for $pair")
                    return@launch
                }
                _state.value = _state.value.copy(
                    status = QuizState.Status.Ready,
                    pageOffset = offset,
                    pool = chunk,
                    index = 0,
                    revealed = false
                )
                val idx = desiredIndex.coerceIn(0, chunk.size - 1)
                setCard(idx, reveal = false)
            } finally {
                loading = false
            }
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
