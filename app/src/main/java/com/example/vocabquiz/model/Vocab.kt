package com.example.vocabquiz.model

data class Vocab(
    val source: String,
    val target: String,
    val srcLang: String? = null,
    val tgtLang: String? = null
)
