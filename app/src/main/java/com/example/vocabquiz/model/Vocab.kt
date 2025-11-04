package com.example.vocabquiz.model

data class Vocab(
    val source: String,
    val target: String,
    val srcLang: String?, // normalized code like "fi", "es", "en"
    val tgtLang: String?
)
