package com.tobiso.tobisoappnative.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LatexEnvironmentsTest {

    @Test
    fun `two row pmatrix gets top and bottom brackets`() {
        assertEquals(
            "⎛ 1   0 ⎞\n⎝ 0   1 ⎠",
            latexToUnicode("\\begin{pmatrix} 1 & 0 \\\\ 0 & 1 \\end{pmatrix}")
        )
    }

    @Test
    fun `three row bmatrix has a middle section`() {
        assertEquals(
            "⎡ a ⎤\n⎢ b ⎥\n⎣ c ⎦",
            latexToUnicode("\\begin{bmatrix} a \\\\ b \\\\ c \\end{bmatrix}")
        )
    }

    @Test
    fun `single row matrix uses plain brackets`() {
        assertEquals("( 1   2   3 )", latexToUnicode("\\begin{pmatrix} 1 & 2 & 3 \\end{pmatrix}"))
    }

    @Test
    fun `cases are drawn with a brace`() {
        assertEquals(
            "f(x) = ⎧ x   x > 0\n⎩ -x   x ≤ 0".replace("f(x) = ", ""),
            latexToUnicode("\\begin{cases} x & x > 0 \\\\ -x & x \\leq 0 \\end{cases}")
        )
    }

    @Test
    fun `aligned equations lose their anchors and keep their lines`() {
        assertEquals(
            "x + 1 = 3\nx = 2",
            latexToUnicode("\\begin{aligned} x + 1 &= 3 \\\\ x &= 2 \\end{aligned}")
        )
    }

    @Test
    fun `cells keep their own latex`() {
        val out = latexToUnicode("\\begin{pmatrix} x^2 & \\frac{1}{2} \\\\ \\alpha & 0 \\end{pmatrix}")
        assertTrue(out.contains("x²"))
        assertTrue(out.contains("1/2"))
        assertTrue(out.contains("α"))
    }

    @Test
    fun `unterminated environment does not crash and keeps the content`() {
        val out = latexToUnicode("\\begin{pmatrix} 1 & 2")
        assertTrue(out.contains("1"))
        assertFalse(out.contains("begin"))
    }

    @Test
    fun `binomial coefficient is readable`() {
        assertEquals("C(n, k)", latexToUnicode("\\binom{n}{k}"))
    }

    @Test
    fun `boxed and underline keep their argument`() {
        assertEquals("x = 2", latexToUnicode("\\boxed{x = 2}"))
        assertEquals("abc", latexToUnicode("\\underline{abc}"))
    }

    @Test
    fun `math spans in chat text are converted`() {
        assertEquals("Obsah je π r² cm.", convertMathSpans("Obsah je \$\\pi r^2\$ cm."))
    }
}
