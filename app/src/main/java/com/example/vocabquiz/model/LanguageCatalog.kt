package com.example.vocabquiz.model

data class LanguageSpec(
    val canonical: String,
    val code: String,
    val aliases: List<String>
)

object LanguageCatalog {
    private val specs: List<LanguageSpec> = listOf(
        LanguageSpec("English", "en", listOf("english")),
        LanguageSpec("Spanish", "es", listOf("spanish", "espanol", "castellano")),
        LanguageSpec("French", "fr", listOf("french", "francais")),
        LanguageSpec("German", "de", listOf("german", "deutsch")),
        LanguageSpec("Italian", "it", listOf("italian", "italiano")),
        LanguageSpec("Portuguese", "pt", listOf("portuguese", "portugues")),
        LanguageSpec("Dutch", "nl", listOf("dutch", "nederlands")),
        LanguageSpec("Swedish", "sv", listOf("swedish", "svenska")),
        LanguageSpec("Norwegian", "no", listOf("norwegian", "norsk", "nb", "nn")),
        LanguageSpec("Danish", "da", listOf("danish", "dansk")),
        LanguageSpec("Finnish", "fi", listOf("finnish", "suomi")),
        LanguageSpec("Icelandic", "is", listOf("icelandic", "islenska")),
        LanguageSpec("Irish", "ga", listOf("irish", "gaeilge")),
        LanguageSpec("Welsh", "cy", listOf("welsh", "cymraeg")),
        LanguageSpec("Catalan", "ca", listOf("catalan", "catala")),
        LanguageSpec("Basque", "eu", listOf("basque", "euskara")),
        LanguageSpec("Galician", "gl", listOf("galician", "galego")),
        LanguageSpec("Polish", "pl", listOf("polish", "polski")),
        LanguageSpec("Czech", "cs", listOf("czech", "cesky")),
        LanguageSpec("Slovak", "sk", listOf("slovak", "slovencina")),
        LanguageSpec("Hungarian", "hu", listOf("hungarian", "magyar")),
        LanguageSpec("Romanian", "ro", listOf("romanian", "romana")),
        LanguageSpec("Greek", "el", listOf("greek", "ellinika"))
    )

    private val aliasToCanonical: Map<String, String> = specs
        .flatMap { spec ->
            val canonicalKey = spec.canonical.lowercase()
            val aliases = spec.aliases.map { it.lowercase() }
            val codeKey = spec.code.lowercase()
            (aliases + canonicalKey + codeKey).map { alias -> alias to spec.canonical }
        }
        .toMap()

    private val canonicalToCode: Map<String, String> = specs
        .associate { it.canonical.lowercase() to it.code }

    fun normalize(raw: String?): String? {
        val s = raw?.trim()?.lowercase() ?: return null
        if (s.isBlank()) return null
        return aliasToCanonical[s]
    }

    fun supportedLanguageNames(): List<String> = specs.map { it.canonical }

    fun toTranslateCode(canonical: String?): String? {
        val key = canonical?.trim()?.lowercase() ?: return null
        return canonicalToCode[key]
    }
}
