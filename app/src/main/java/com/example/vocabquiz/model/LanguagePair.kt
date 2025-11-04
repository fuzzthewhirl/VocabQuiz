package com.example.vocabquiz.model

data class LanguagePair(val src: String, val tgt: String) {
    override fun toString() = "$src→$tgt"
}
