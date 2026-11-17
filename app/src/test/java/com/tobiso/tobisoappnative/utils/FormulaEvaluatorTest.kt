package com.tobiso.tobisoappnative.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FormulaEvaluatorTest {

    private fun eval(text: String, vararg vars: Pair<String, Double>): Double {
        val compiled = FormulaEvaluator.compile(text, vars.map { it.first })
        assertTrue("expected a compiled formula but got $compiled", compiled is FormulaEvaluator.Compiled.Ok)
        return (compiled as FormulaEvaluator.Compiled.Ok).evaluate(vars.toMap())
    }

    private fun error(text: String, vararg names: String): String {
        val compiled = FormulaEvaluator.compile(text, names.toList())
        assertTrue(compiled is FormulaEvaluator.Compiled.Error)
        return (compiled as FormulaEvaluator.Compiled.Error).message
    }

    @Test fun `product of two variables`() = assertEquals(6.0, eval("m * a", "m" to 2.0, "a" to 3.0), 1e-9)

    @Test fun `implicit multiplication of variables`() = assertEquals(6.0, eval("ma", "m" to 2.0, "a" to 3.0), 1e-9)

    @Test fun `kinetic energy with a power and a decimal`() =
        assertEquals(25.0, eval("0.5 m v^2", "m" to 2.0, "v" to 5.0), 1e-9)

    @Test fun `decimal comma is accepted`() = assertEquals(25.0, eval("0,5 * m * v^2", "m" to 2.0, "v" to 5.0), 1e-9)

    @Test fun `result prefix is dropped`() = assertEquals(6.0, eval("F = m * a", "m" to 2.0, "a" to 3.0), 1e-9)

    @Test fun `fractions and brackets`() =
        assertEquals(2.5, eval("(a + b) / 2", "a" to 2.0, "b" to 3.0), 1e-9)

    @Test fun `functions and constants`() {
        assertEquals(Math.PI * 4, eval("pi r^2", "r" to 2.0), 1e-9)
        assertEquals(5.0, eval("sqrt(a^2 + b^2)", "a" to 3.0, "b" to 4.0), 1e-9)
    }

    @Test fun `unicode operators and squares`() =
        assertEquals(9.0, eval("a² × b ÷ 2 − 0", "a" to 3.0, "b" to 2.0), 1e-9)

    @Test fun `longest variable name wins`() =
        assertEquals(12.0, eval("vt", "v" to 3.0, "vt" to 12.0), 1e-9)

    @Test fun `variables are case sensitive`() =
        assertEquals(5.0, eval("M + m", "M" to 2.0, "m" to 3.0), 1e-9)

    @Test fun `unary minus binds looser than power`() = assertEquals(-9.0, eval("-a^2", "a" to 3.0), 1e-9)

    @Test fun `division by zero gives infinity not a crash`() =
        assertTrue(eval("a / b", "a" to 1.0, "b" to 0.0).isInfinite())

    @Test fun `unknown name is reported`() = assertTrue(error("m * q", "m").contains("neznámý"))

    @Test fun `missing bracket is reported`() = assertTrue(error("(a + b", "a", "b").contains("závorka"))

    @Test fun `blank formula is reported`() = assertTrue(error("   ").contains("chybí"))

    @Test fun `trailing garbage is reported`() = assertTrue(error("a + )", "a").isNotEmpty())
}

class FormatFormulaValueTest {
    @Test fun `whole numbers have no decimals`() = assertEquals("25", formatFormulaValue(25.0))
    @Test fun `decimals use a comma and drop trailing zeros`() = assertEquals("2,5", formatFormulaValue(2.5))
    @Test fun `long decimals are rounded to four places`() = assertEquals("0,3333", formatFormulaValue(1.0 / 3))
    @Test fun `negative values keep their sign`() = assertEquals("-1,5", formatFormulaValue(-1.5))
    @Test fun `infinity and NaN are readable`() {
        assertEquals("∞", formatFormulaValue(Double.POSITIVE_INFINITY))
        assertEquals("–", formatFormulaValue(Double.NaN))
    }
}
