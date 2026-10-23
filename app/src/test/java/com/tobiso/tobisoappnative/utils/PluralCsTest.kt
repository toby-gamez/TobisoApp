package com.tobiso.tobisoappnative.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class PluralCsTest {
    @Test
    fun `picks czech plural form`() {
        assertEquals("hlas", pluralCs(1, "hlas", "hlasy", "hlasů"))
        assertEquals("hlasy", pluralCs(2, "hlas", "hlasy", "hlasů"))
        assertEquals("hlasy", pluralCs(4, "hlas", "hlasy", "hlasů"))
        assertEquals("hlasů", pluralCs(5, "hlas", "hlasy", "hlasů"))
        assertEquals("hlasů", pluralCs(0, "hlas", "hlasy", "hlasů"))
    }
}
