package com.efx.twitterwrapper.core.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChecksumsTest {

    private val hash = "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08"
    private val other = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"

    @Test
    fun `finds sha256sum output with two spaces`() {
        val body = """
            # release checksums
            $hash  app-1.0.1.apk
            $other  app-1.0.0.apk
        """.trimIndent()

        assertEquals(hash, Checksums.findHash(body, "app-1.0.1.apk"))
    }

    @Test
    fun `handles binary marker and paths`() {
        val body = """
            $hash *app-1.0.1.apk
            $other  dist/app-1.0.2.apk
        """.trimIndent()

        assertEquals(hash, Checksums.findHash(body, "app-1.0.1.apk"))
        assertEquals(other, Checksums.findHash(body, "app-1.0.2.apk"))
    }

    @Test
    fun `is case insensitive for the file name`() {
        val body = "$hash  App-1.0.1.APK"
        assertEquals(hash, Checksums.findHash(body, "app-1.0.1.apk"))
    }

    @Test
    fun `returns null when there is no entry`() {
        val body = "$hash  app-1.0.1.apk"
        assertNull(Checksums.findHash(body, "app-9.9.9.apk"))
        assertNull(Checksums.findHash("", "app-1.0.1.apk"))
    }

    @Test
    fun `ignores malformed lines`() {
        val body = """
            not-a-hash  app-1.0.1.apk
            ${hash.take(63)}  app-1.0.1.apk
            $hash
        """.trimIndent()

        assertNull(Checksums.findHash(body, "app-1.0.1.apk"))
    }

    @Test
    fun `constant time comparison`() {
        assertTrue(Checksums.constantTimeEquals(hash, hash))
        assertTrue(Checksums.constantTimeEquals(hash.uppercase(), hash))
        assertFalse(Checksums.constantTimeEquals(hash, other))
        assertFalse(Checksums.constantTimeEquals(hash, hash.dropLast(1)))
    }
}
