package com.tobiso.tobisoappnative.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalloutAndMathTest {

    private fun parse(md: String) = parseContentToElements(md, isOffline = false, posts = emptyList())

    // ── Callouts ──────────────────────────────────────────────────────────

    @Test
    fun `tip callout wraps its content`() {
        val els = parse("Uvod\n\n!!!tip Pouzij carku pred ze. !!!\n\nKonec")
        assertEquals(3, els.size)
        val callout = els[1] as ContentElement.Callout
        assertEquals(CalloutKind.Tip, callout.kind)
        val inner = callout.children.single() as ContentElement.Paragraph
        assertEquals("Pouzij carku pred ze.", inlinePartsToText(inner.parts))
    }

    @Test
    fun `sidefact callout can span several paragraphs and has its own kind`() {
        val els = parse("!!!sidefact Prvni odstavec.\n\nDruhy odstavec s **tucnym**. !!!")
        val callout = els.single() as ContentElement.Callout
        assertEquals(CalloutKind.SideFact, callout.kind)
        assertEquals(2, callout.children.size)
    }

    @Test
    fun `callout keywords are case insensitive and text around is kept in order`() {
        val els = parse("Pred\n\n!!!TIP Rada !!!\n\nPo")
        assertTrue(els[0] is ContentElement.Paragraph)
        assertTrue(els[1] is ContentElement.Callout)
        assertTrue(els[2] is ContentElement.Paragraph)
    }

    @Test
    fun `unterminated callout stays ordinary text`() {
        val els = parse("!!!tip Bez konce")
        assertTrue(els.none { it is ContentElement.Callout })
    }

    @Test
    fun `callout inside content does not trigger intra twice`() {
        val els = parse("Uvod ... intro ... zbytek\n\n!!!tip Rada ... a ... dalsi !!!")
        assertEquals(1, els.count { it is ContentElement.Intra })
        val callout = els.filterIsInstance<ContentElement.Callout>().single()
        assertTrue(callout.children.none { it is ContentElement.Intra })
    }

    // ── Math ──────────────────────────────────────────────────────────────

    @Test
    fun `exponents and indices become unicode`() {
        assertEquals("x² + y²", latexToUnicode("x^2 + y^2"))
        assertEquals("a₁ + a₂", latexToUnicode("a_1 + a_2"))
        assertEquals("2¹⁰", latexToUnicode("2^{10}"))
        assertEquals("eˣ⁺¹", latexToUnicode("e^{x+1}"))
    }

    @Test
    fun `unmappable scripts fall back to readable parentheses`() {
        assertEquals("x^(q)", latexToUnicode("x^{q}"))
    }

    @Test
    fun `fractions and roots`() {
        assertEquals("1/2", latexToUnicode("\\frac{1}{2}"))
        assertEquals("(a+b)/c", latexToUnicode("\\frac{a+b}{c}"))
        assertEquals("√9", latexToUnicode("\\sqrt{9}"))
        assertEquals("√(x+1)", latexToUnicode("\\sqrt{x+1}"))
        assertEquals("∛8", latexToUnicode("\\sqrt[3]{8}"))
    }

    @Test
    fun `symbols and greek letters`() {
        assertEquals("v = s/t", latexToUnicode("v = s/t"))
        assertEquals("α ≤ π · 2", latexToUnicode("\\alpha \\leq \\pi \\cdot 2"))
        assertEquals("5 × 3 ≠ 16", latexToUnicode("5 \\times 3 \\neq 16"))
    }

    @Test
    fun `text commands keep their content and left right are dropped`() {
        assertEquals("v = 5 m/s", latexToUnicode("v = 5\\,\\text{m/s}").replace(" ", " "))
        assertEquals("(x)", latexToUnicode("\\left(x\\right)"))
    }

    @Test
    fun `inline math spans are converted in paragraphs`() {
        val p = parse("Pythagoras: \$a^2 + b^2 = c^2\$ platí.").single() as ContentElement.Paragraph
        assertEquals("Pythagoras: a² + b² = c² platí.", inlinePartsToText(p.parts))
    }

    @Test
    fun `display math is converted too`() {
        val p = parse("\$\$x^2 + y^2 = r^2\$\$").single() as ContentElement.Paragraph
        assertEquals("x² + y² = r²", inlinePartsToText(p.parts))
    }

    @Test
    fun `a lone dollar sign is not math`() {
        assertEquals("Stojí to 5 \$ a 6 \$", convertMathSpans("Stojí to 5 \$ a 6 \$"))
    }
}
