package com.efx.twitterwrapper.core.update

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubReleaseTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun `parses release with null body`() {
        val payload = """
            {
              "tag_name": "v1.0.0",
              "body": null,
              "assets": [
                {"name": "app-1.0.0.apk", "browser_download_url": "https://example.com/app.apk", "size": 1234},
                {"name": "checksums.txt", "browser_download_url": "https://example.com/checksums.txt", "size": 80}
              ]
            }
        """.trimIndent()

        val release = json.decodeFromString(GitHubRelease.serializer(), payload)

        assertEquals("v1.0.0", release.tagName)
        assertEquals("", release.body.orEmpty())
        assertEquals(2, release.assets.size)
        assertEquals(1234L, release.assets[0].size)
        assertEquals("https://example.com/checksums.txt", release.assets[1].downloadUrl)
    }

    @Test
    fun `parses release without body and assets`() {
        val release = json.decodeFromString(
            GitHubRelease.serializer(),
            """{"tag_name":"v2.0.0"}""",
        )

        assertNull(release.body)
        assertTrue(release.assets.isEmpty())
    }
}
