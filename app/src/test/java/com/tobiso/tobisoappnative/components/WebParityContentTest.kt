package com.tobiso.tobisoappnative.components

import com.tobiso.tobisoappnative.components.graph.ExpressionResult
import com.tobiso.tobisoappnative.components.graph.GraphExpression
import com.tobiso.tobisoappnative.components.graph.GraphMath
import com.tobiso.tobisoappnative.components.graph.GraphParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WebParityContentTest {

    private fun parse(md: String) = parseContentToElements(md, isOffline = false, posts = emptyList())

    private fun eval(expr: String, x: Double): Double =
        (GraphExpression.parse(expr) as ExpressionResult.Ok).f(x)

    // ── Expressions ───────────────────────────────────────────────────────

    @Test
    fun `expression precedence follows the web rules`() {
        assertEquals(-4.0, eval("-x^2", 2.0), 1e-9)
        assertEquals(0.5, eval("2^-1", 0.0), 1e-9)
        assertEquals(512.0, eval("2^x^2", 3.0), 1e-9)
        assertEquals(1.0, eval("sin(x)^2 + cos(x)^2", 0.7), 1e-9)
        assertEquals(0.5 * 4, eval("1/2x", 4.0), 1e-9)
        assertEquals(8.0, eval("y = 2x + 0,5*4 - 2 + 2", 3.0), 1e-9)
        assertEquals(8.0, eval("2|x - 1|", 5.0), 1e-9)
        assertEquals(3.0, eval("(x+1)(x-1)", 2.0), 1e-9)
    }

    @Test
    fun `unknown names and brackets produce errors`() {
        assertTrue(GraphExpression.parse("foo(x)") is ExpressionResult.Error)
        assertTrue(GraphExpression.parse("(x+1") is ExpressionResult.Error)
        assertTrue(GraphExpression.parse("  ") is ExpressionResult.Error)
    }

    // ── Graph parser / layout ─────────────────────────────────────────────

    private val sample = """
        title: Pohyb robotů
        x(t [s]): 0-10
        y(v [m/s]): 0-50 (10)
        curve1(name: robot1): [0;0] [6;7] [8;7] RED
        curve2(name: robot2): [0;5] [4;30] [10;45] smooth BLUE dashed
        point(name: A): [5;8] LIGHT_BLUE
        function(name: parabola, od: 0, do: 5): y = x^2 RED dashed
        note: Poznámka // komentář
    """.trimIndent()

    @Test
    fun `graph block is parsed into axes, series, points, functions and notes`() {
        val g = GraphParser.parse(sample)
        assertEquals("Pohyb robotů", g.title)
        assertEquals("t [s]", g.x.label)
        assertEquals(10.0, g.y.step!!, 0.0)
        assertEquals(2, g.series.size)
        assertEquals("#d1495b", g.series[0].color)
        assertTrue(g.series[1].smooth && g.series[1].dashed)
        assertEquals("A", g.markers.single().name)
        val f = g.functions.single()
        assertEquals("parabola", f.name)
        assertEquals(0.0, f.from!!, 0.0)
        assertEquals(5.0, f.to!!, 0.0)
        assertEquals(listOf("Poznámka"), g.notes)
        assertTrue(g.errors.isEmpty())
    }

    @Test
    fun `layout snaps ranges to the step and sorts negative ranges`() {
        val g = GraphParser.parse("x: 0-45 (10)\ny: 30..10\ncurve: [1;1]")
        val l = GraphMath.layout(g)
        assertEquals(50.0, l.xMax, 0.0)
        assertEquals(10.0, l.yMin, 0.0)
        assertEquals(30.0, l.yMax, 0.0)
        assertEquals("−2,5", GraphMath.formatTick(-2.5, 0.5))
    }

    @Test
    fun `bad function lands in errors and empty block is detected`() {
        assertEquals(1, GraphParser.parse("x: 0-1\nfunction: y = foo(x)").errors.size)
        assertTrue(GraphParser.parse("nic").isEmpty)
    }

    @Test
    fun `graph block becomes a Graph element and survives dots and pipes in its source`() {
        val md = "Úvod\n\n[graf1]\nx: 0-10\nnote: a ... b | c\ncurve: [0;0] [1;1]\n[/graf1]\n\nKonec"
        val els = parse(md)
        assertEquals(3, els.size)
        val graph = els[1] as ContentElement.Graph
        assertTrue(graph.source.contains("note: a ... b | c"))
    }

    @Test
    fun `graph inside a code fence stays literal`() {
        val els = parse("```\n[graf1]\nx: 0-1\n[/graf1]\n```")
        assertTrue(els.none { it is ContentElement.Graph })
        assertTrue(els.single() is ContentElement.CodeBlock)
    }

    // ── Media, arrows, inline code ────────────────────────────────────────

    @Test
    fun `video and document embeds resolve against the files host`() {
        val els = parse("*[Ukázka](videos/x.mp4)\n\n_[Pracovní list](documents/a.docx)")
        assertEquals("https://files.tobiso.com/videos/x.mp4", (els[0] as ContentElement.VideoPlayer).videoUrl)
        val doc = els[1] as ContentElement.Document
        assertEquals("Pracovní list", doc.label)
        assertEquals("https://files.tobiso.com/documents/a.docx", doc.url)
    }

    @Test
    fun `arrow and inline code are parsed`() {
        val p = parse("krok 1 -> krok 2 a `kód` konec").single() as ContentElement.Paragraph
        assertEquals("krok 1 → krok 2 a kód konec", inlinePartsToText(p.parts))
        assertTrue(p.parts.any { it == InlinePart.Code("kód") })
    }

    // ── Lists ─────────────────────────────────────────────────────────────

    @Test
    fun `star and plus bullets and indentation levels`() {
        val els = parse("* a\n* b\n  + c\n1) x\n2) y")
        val first = els[0] as ContentElement.BulletList
        assertEquals(1, first.level); assertEquals(2, first.items.size)
        assertEquals(2, (els[1] as ContentElement.BulletList).level)
        assertEquals(listOf("x", "y"), (els[2] as ContentElement.NumberedList).items)
    }

    // ── Box diagrams ──────────────────────────────────────────────────────

    @Test
    fun `uniform box rows form a stack`() {
        val d = parse("| Programy |\n| jádro :: řídí hardware |\n|, GUI, |").single() as ContentElement.BoxDiagram
        assertTrue(!d.isTree)
        assertEquals(listOf("Programy", "jádro", "GUI"), d.roots.map { it.title })
        assertEquals("řídí hardware", d.roots[1].description)
    }

    @Test
    fun `indented box rows form a tree`() {
        val md = "| Počítač |\n  | Hardware |\n    | CPU |\n    | RAM |\n  | Software |"
        val d = parse(md).single() as ContentElement.BoxDiagram
        assertTrue(d.isTree)
        val root = d.roots.single()
        assertEquals(listOf("Hardware", "Software"), root.children.map { it.title })
        assertEquals(listOf("CPU", "RAM"), root.children[0].children.map { it.title })
    }

    @Test
    fun `real tables are not mistaken for box diagrams`() {
        val els = parse("| a | b |\n|---|---|\n| 1 | 2 |")
        assertNotNull(els.single() as ContentElement.Table)
    }

    @Test
    fun `invisible filler characters are removed and do not block fractions`() {
        val p = parse("Ahoj\u3164 svete\u200B!").single() as ContentElement.Paragraph
        assertEquals("Ahoj svete!", inlinePartsToText(p.parts))
        val f = parse("1\u3164/\u31642").single() as ContentElement.Paragraph
        assertTrue(f.parts.single() is InlinePart.Fraction)
    }

    @Test
    fun `fractions inside inline code are parsed and stay code`() {
        val p = parse("Spočti `1/2 + {a+b}/{c}` hned").single() as ContentElement.Paragraph
        val fractions = p.parts.filterIsInstance<InlinePart.Fraction>()
        assertEquals(2, fractions.size)
        assertEquals(listOf<InlinePart>(InlinePart.Code("1")), fractions[0].numerator)
        assertEquals("a+b", inlinePartsToText(fractions[1].numerator))
        // paths / products without fractions remain a single code part
        val q = parse("`a*b/c` a `x/y/z`").single() as ContentElement.Paragraph
        assertTrue(q.parts.contains(InlinePart.Code("a*b/c")))
    }
}
