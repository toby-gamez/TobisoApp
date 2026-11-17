package com.tobiso.tobisoappnative.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownTableTest {

    @Test
    fun `parses header and rows and drops the separator`() {
        val parts = parseMarkdownTable(
            """
            | Vlastnost | Lev | Tygr |
            |---|---|---|
            | Váha | 190 kg | 220 kg |
            | Žije | Afrika | Asie |
            """.trimIndent()
        )
        assertEquals(
            listOf(
                listOf("Vlastnost", "Lev", "Tygr"),
                listOf("Váha", "190 kg", "220 kg"),
                listOf("Žije", "Afrika", "Asie")
            ),
            parts.rows
        )
        assertEquals("", parts.outside)
    }

    @Test
    fun `separator with alignment colons is dropped`() {
        val parts = parseMarkdownTable("| A | B |\n| :--- | ---: |\n| 1 | 2 |")
        assertEquals(listOf(listOf("A", "B"), listOf("1", "2")), parts.rows)
    }

    @Test
    fun `text around the table is kept as outside`() {
        val parts = parseMarkdownTable("Srovnání:\n\n| A | B |\n|---|---|\n| 1 | 2 |\n\nZávěr.")
        assertEquals(2, parts.rows.size)
        assertEquals("Srovnání:\n\n\nZávěr.", parts.outside)
    }

    @Test
    fun `plain text without a table has no rows`() {
        val parts = parseMarkdownTable("Žádná tabulka.")
        assertTrue(parts.rows.isEmpty())
        assertEquals("Žádná tabulka.", parts.outside)
    }

    @Test
    fun `windows line endings are handled`() {
        val parts = parseMarkdownTable("| A | B |\r\n|---|---|\r\n| 1 | 2 |")
        assertEquals(listOf(listOf("A", "B"), listOf("1", "2")), parts.rows)
    }

    @Test
    fun `rows with fewer cells are kept as is`() {
        val parts = parseMarkdownTable("| A | B | C |\n|---|---|---|\n| 1 | 2 |")
        assertEquals(listOf("1", "2"), parts.rows[1])
    }
}
