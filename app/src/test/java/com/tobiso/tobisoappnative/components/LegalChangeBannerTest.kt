package com.tobiso.tobisoappnative.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LegalChangeBannerTest {

    @Test
    fun `formats an ISO timestamp in Czech`() {
        assertEquals("1. listopadu 2026", formatLegalChangeDate("2026-11-01T08:00:00Z"))
    }

    @Test
    fun `formats a date only value`() {
        assertEquals("24. prosince 2025", formatLegalChangeDate("2025-12-24"))
    }

    @Test
    fun `garbage gives null so the text simply omits the date`() {
        assertNull(formatLegalChangeDate("not a date"))
        assertNull(formatLegalChangeDate(""))
    }
}
