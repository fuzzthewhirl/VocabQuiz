package com.example.vocabquiz.model

data class Vocab(
    val source: String,
    val target: String,
    val srcLang: String?, // canonical language name (e.g. "English")
    val tgtLang: String?
)
