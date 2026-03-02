package com.example.vocabquiz.model

data class LanguageSpec(
    val canonical: String,
    val aliases: List<String>
)

object LanguageCatalog {
    private val specs: List<LanguageSpec> = listOf(
        LanguageSpec("English", listOf("en", "english")),
        LanguageSpec("Spanish", listOf("es", "spanish", "espanol", "castellano")),
        LanguageSpec("French", listOf("fr", "french", "francais")),
        LanguageSpec("German", listOf("de", "german", "deutsch")),
        LanguageSpec("Italian", listOf("it", "italian", "italiano")),
        LanguageSpec("Portuguese", listOf("pt", "portuguese", "portugues")),
        LanguageSpec("Dutch", listOf("nl", "dutch", "nederlands")),
        LanguageSpec("Swedish", listOf("sv", "swedish", "svenska")),
        LanguageSpec("Norwegian", listOf("no", "norwegian", "norsk", "nb", "nn")),
        LanguageSpec("Danish", listOf("da", "danish", "dansk")),
        LanguageSpec("Finnish", listOf("fi", "finnish", "suomi")),
        LanguageSpec("Icelandic", listOf("is", "icelandic", "islenska")),
        LanguageSpec("Irish", listOf("ga", "irish", "gaeilge")),
        LanguageSpec("Welsh", listOf("cy", "welsh", "cymraeg")),
        LanguageSpec("Catalan", listOf("ca", "catalan", "catala")),
        LanguageSpec("Basque", listOf("eu", "basque", "euskara")),
        LanguageSpec("Galician", listOf("gl", "galician", "galego")),
        LanguageSpec("Polish", listOf("pl", "polish", "polski")),
        LanguageSpec("Czech", listOf("cs", "czech", "cesky")),
        LanguageSpec("Slovak", listOf("sk", "slovak", "slovencina")),
        LanguageSpec("Hungarian", listOf("hu", "hungarian", "magyar")),
        LanguageSpec("Romanian", listOf("ro", "romanian", "romana")),
        LanguageSpec("Greek", listOf("el", "greek", "ellinika"))
    )

    private val aliasToCanonical: Map<String, String> = specs
        .flatMap { spec ->
            val canonicalKey = spec.canonical.lowercase()
            val aliases = spec.aliases.map { it.lowercase() }
            (aliases + canonicalKey).map { alias -> alias to spec.canonical }
        }
        .toMap()

    fun normalize(raw: String?): String? {
        val s = raw?.trim()?.lowercase() ?: return null
        if (s.isBlank()) return null
        return aliasToCanonical[s]
    }

    fun supportedLanguageNames(): List<String> = specs.map { it.canonical }
}
