import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LocaleStringsTest {
    @Test
    fun allLocaleStringsIncludeBaseKeys() {
        val resDir = resolveResDir()
        val baseFile = File(resDir, "values/strings.xml")
        require(baseFile.exists()) { "Base strings.xml not found at ${baseFile.path}" }

        val baseKeys = extractStringKeys(baseFile)
        require(baseKeys.isNotEmpty()) { "Base strings.xml has no string entries" }

        val localeDirs = resDir.listFiles()
            ?.filter { it.isDirectory && it.name.startsWith("values-") }
            .orEmpty()

        val missingByLocale = mutableMapOf<String, List<String>>()

        localeDirs.forEach { dir ->
            val localeFile = File(dir, "strings.xml")
            if (!localeFile.exists()) {
                missingByLocale[dir.name] = baseKeys.toList()
                return@forEach
            }
            val localeKeys = extractStringKeys(localeFile)
            val missing = baseKeys.filterNot { localeKeys.contains(it) }
            if (missing.isNotEmpty()) {
                missingByLocale[dir.name] = missing
            }
        }

        assertTrue(
            "Missing localized keys: ${missingByLocale.entries.joinToString { "${it.key} -> ${it.value}" }}",
            missingByLocale.isEmpty()
        )
    }

    private fun extractStringKeys(file: File): Set<String> {
        val regex = Regex("""<string\s+name="([^"]+)"""")
        return regex.findAll(file.readText())
            .map { it.groupValues[1] }
            .toSet()
    }

    private fun resolveResDir(): File {
        val userDirPath = requireNotNull(System.getProperty("user.dir")) { "user.dir is not set" }
        val userDir = File(userDirPath)
        val candidates = listOf(
            File(userDir, "src/main/res"),
            File(userDir, "app/src/main/res")
        )
        return candidates.firstOrNull { it.exists() }
            ?: error("Could not locate src/main/res under ${userDir.path}")
    }
}
