package com.efx.twitterwrapper.core.update

/**
 * Maps a release tag (`v1.2.3` or `1.2.3`) to a versionCode.
 *
 * `versionCode = major * 10_000 + minor * 100 + patch`, which is also computed
 * by the release workflow. Keep both implementations in sync.
 */
object SemverParser {

    const val MAX_MAJOR = 2000
    const val MAX_MINOR = 99
    const val MAX_PATCH = 99

    private val REGEX = Regex("""^[vV]?(\d{1,4})\.(\d{1,2})\.(\d{1,2})$""")

    fun parse(tag: String): Parsed? {
        val match = REGEX.find(tag.trim()) ?: return null
        val major = match.groupValues[1].toIntOrNull() ?: return null
        val minor = match.groupValues[2].toIntOrNull() ?: return null
        val patch = match.groupValues[3].toIntOrNull() ?: return null

        if (major > MAX_MAJOR || minor > MAX_MINOR || patch > MAX_PATCH) return null

        return Parsed(
            versionName = "$major.$minor.$patch",
            versionCode = versionCode(major, minor, patch),
        )
    }

    fun versionCode(major: Int, minor: Int, patch: Int): Int =
        major * 10_000 + minor * 100 + patch

    data class Parsed(val versionName: String, val versionCode: Int)
}
