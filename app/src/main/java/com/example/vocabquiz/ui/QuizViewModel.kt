package com.example.vocabquiz.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.vocabquiz.data.VocabRepository
import com.example.vocabquiz.model.Vocab
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

data class QuizState(
    val ready: Boolean = false,
    val current: Vocab? = null,
    val options: List<String> = emptyList(),
    val correctIndex: Int = -1,
    val score: Int = 0,
    val total: Int = 0
)

class QuizViewModel(app: Application) : AndroidViewModel(app) {
    private val spreadsheetId = "1HI8QRSYkGNsXvyO2Grx3o1wFe6Q9uscyfAO31Xe50QQ"
    private val repo = VocabRepository(app, spreadsheetId)

    private val _state = MutableStateFlow(QuizState())
    val state = _state.asStateFlow()

    private var data: List<Vocab> = emptyList()

    init {
        viewModelScope.launch {
            data = repo.fetch()
            _state.value = _state.value.copy(ready = data.isNotEmpty())
            nextQuestion()
        }
    }

    fun nextQuestion() {
        if (data.size < 4) return
        val idx = Random.nextInt(data.size)
        val correct = data[idx]
        val wrongs = (data.indices - idx).shuffled().take(3).map { data[it].target }
        val all = (wrongs + correct.target).shuffled()
        _state.value = _state.value.copy(
            current = correct,
            options = all,
            correctIndex = all.indexOf(correct.target),
            total = _state.value.total + 1
        )
    }

    fun answer(selectedIndex: Int) {
        val isCorrect = selectedIndex == _state.value.correctIndex
        _state.value = _state.value.copy(score = _state.value.score + if (isCorrect) 1 else 0)
        nextQuestion()
    }
}
