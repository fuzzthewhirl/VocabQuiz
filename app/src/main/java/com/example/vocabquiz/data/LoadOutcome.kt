package com.example.vocabquiz.data

import com.example.vocabquiz.model.LanguagePair

data class LoadOutcome(
    val sizes: Map<LanguagePair, Int>,
    val report: LoadReport
)
