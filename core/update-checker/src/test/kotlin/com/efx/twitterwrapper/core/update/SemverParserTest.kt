package com.efx.twitterwrapper.core.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SemverParserTest {

    @Test
    fun `parses plain and v-prefixed tags`() {
        assertEquals("1.2.3", SemverParser.parse("1.2.3")?.versionName)
        assertEquals("1.2.3", SemverParser.parse("v1.2.3")?.versionName)
        assertEquals("1.2.3", SemverParser.parse(" v1.2.3 ")?.versionName)
        assertEquals("1.2.3", SemverParser.parse("V1.2.3")?.versionName)
    }

    @Test
    fun `computes version code used by the release workflow`() {
        assertEquals(10_000, SemverParser.parse("1.0.0")?.versionCode)
        assertEquals(10_001, SemverParser.parse("1.0.1")?.versionCode)
        assertEquals(10_100, SemverParser.parse("1.1.0")?.versionCode)
        assertEquals(20_302, SemverParser.parse("2.3.2")?.versionCode)
    }

    @Test
    fun `rejects malformed tags`() {
        assertNull(SemverParser.parse(""))
        assertNull(SemverParser.parse("1.0"))
        assertNull(SemverParser.parse("1.0.0.0"))
        assertNull(SemverParser.parse("v1.0.0-beta"))
        assertNull(SemverParser.parse("release-1.0.0"))
        assertNull(SemverParser.parse("1.100.0"))
        assertNull(SemverParser.parse("1.0.100"))
        assertNull(SemverParser.parse("2001.0.0"))
    }

    @Test
    fun `version code is monotonic for supported ranges`() {
        var previous = -1
        for (major in 0..2) {
            for (minor in 0..99) {
                for (patch in 0..99) {
                    val code = SemverParser.versionCode(major, minor, patch)
                    require(code > previous) { "non monotonic at $major.$minor.$patch" }
                    previous = code
                }
            }
        }
    }
}
