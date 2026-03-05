package com.example.vocabquiz.model

data class LanguagePair(val src: String, val tgt: String) {
    override fun toString(): String {
        val srcCode = LanguageCatalog.toTranslateCode(src) ?: src
        val tgtCode = LanguageCatalog.toTranslateCode(tgt) ?: tgt
        return "$srcCode→$tgtCode"
    }
}
