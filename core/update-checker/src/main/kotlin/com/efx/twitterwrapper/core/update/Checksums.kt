package com.efx.twitterwrapper.core.update

/** Pure helpers around `checksums.txt`, kept free of Android APIs so they are unit-testable. */
object Checksums {

    /**
     * Finds the SHA-256 of [fileName] in a `sha256sum` style file:
     *
     * ```
     * 9f86d0...  app-1.0.1.apk
     * ```
     *
     * Accepts `hash  name`, `hash *name` and paths (`dist/app.apk`).
     */
    fun findHash(checksumsFile: String, fileName: String): String? {
        for (rawLine in checksumsFile.lineSequence()) {
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#")) continue

            val parts = line.split(WHITESPACE, limit = 2)
            if (parts.size != 2) continue

            val hash = parts[0].lowercase()
            if (hash.length != 64 || hash.any { it !in HEX_CHARS }) continue

            val name = parts[1]
                .trim()
                .removePrefix("*")
                .trim()
                .substringAfterLast('/')
                .substringAfterLast('\\')

            if (name.equals(fileName, ignoreCase = true)) return hash
        }
        return null
    }

    /** Constant-time comparison of two hex digests (case-insensitive). */
    fun constantTimeEquals(expected: String, actual: String): Boolean {
        if (expected.length != actual.length) return false
        var diff = 0
        for (index in expected.indices) {
            diff = diff or (expected[index].lowercaseChar().code xor actual[index].lowercaseChar().code)
        }
        return diff == 0
    }

    private val WHITESPACE = Regex("\\s+")
    private val HEX_CHARS = ('0'..'9') + ('a'..'f')
}
