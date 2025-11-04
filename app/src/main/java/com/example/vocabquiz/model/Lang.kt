package com.example.vocabquiz.model

enum class Lang(val code: String) {
    FI("fi"), ES("es"), EN("en");

    companion object {
        private val aliases: Map<String, String> = mapOf(
            // Finnish
            "finnish" to "fi", "suomi" to "fi",
            // Spanish
            "spanish" to "es", "español" to "es", "castellano" to "es",
            // English
            "english" to "en",
            // Accept codes too
            "fi" to "fi", "es" to "es", "en" to "en"
        )
        fun normalize(raw: String?): String? {
            val s = raw?.trim()?.lowercase() ?: return null
            return aliases[s] ?: s // leaves unknowns as-is; they’ll be filtered out
        }
    }
}
