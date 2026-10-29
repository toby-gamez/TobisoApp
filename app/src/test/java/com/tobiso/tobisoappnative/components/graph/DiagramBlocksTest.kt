package com.tobiso.tobisoappnative.components.graph

import com.tobiso.tobisoappnative.components.ContentElement
import com.tobiso.tobisoappnative.components.parseContentToElements
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagramBlocksTest {

    private val evenOdd = """
        title: Je číslo sudé?
        start(id: s): Začátek
        decision(id: d): Zbytek po dělení 2 je 0?
        process(id: a): Číslo je sudé
        process(id: b): Číslo je liché
        end(id: e): Konec
        s -> d
        d -> a (ano)
        d -> b (ne) RED
        a -> e
        b -> e
    """.trimIndent()

    // ── Flowchart ─────────────────────────────────────────────────────────

    @Test
    fun `flowchart parses nodes, labelled and coloured edges`() {
        val f = FlowchartParser.parse(evenOdd)
        assertEquals("Je číslo sudé?", f.title)
        assertEquals(5, f.nodes.size)
        assertEquals(FcKind.Decision, f.nodes[1].kind)
        assertEquals(5, f.edges.size)
        assertEquals("ano", f.edges[1].label)
        assertEquals("#d1495b", f.edges[2].color)
        assertTrue(f.warnings.isEmpty())
    }

    @Test
    fun `flowchart layers follow the longest path and the branches split sideways`() {
        val f = FlowchartParser.parse(evenOdd)
        val layout = FlowchartLayouter.layout(f)
        assertEquals(listOf(0, 1, 2, 2, 3), f.nodes.map { it.layer })
        assertTrue(f.nodes[2].x < f.nodes[3].x)
        assertTrue(f.nodes[0].y < f.nodes[1].y && f.nodes[1].y < f.nodes[4].y)
        assertEquals(f.edges.size, layout.routes.size)
        assertTrue(layout.width > 0 && layout.height > 0)
    }

    @Test
    fun `flowchart loops are back edges routed around the diagram`() {
        val f = FlowchartParser.parse("start(id: s): Start\nprocess(id: a): Krok\ndecision(id: d): Hotovo?\ns -> a\na -> d\nd -> a (ne)")
        val layout = FlowchartLayouter.layout(f)
        assertTrue(f.edges[2].back)
        assertTrue(layout.routes[2].size >= 4)
    }

    @Test
    fun `flowchart warns about unknown nodes, duplicate ids and missing ids`() {
        val f = FlowchartParser.parse("start(id: s): A\nprocess(id: s): B\nprocess: C\ns -> x")
        assertEquals(5, f.warnings.size) // duplicate id, missing id, unknown arrow target, two unconnected nodes
        assertEquals(2, f.nodes.size)
    }

    @Test
    fun `flowchart horizontal direction lays layers out left to right`() {
        val f = FlowchartParser.parse("direction: doprava\nstart(id: s): A\nend(id: e): B\ns -> e")
        FlowchartLayouter.layout(f)
        assertTrue(f.nodes[0].x < f.nodes[1].x)
    }

    // ── Venn ──────────────────────────────────────────────────────────────

    @Test
    fun `venn puts shared elements into the intersection`() {
        val v = VennParser.parse("set(name: D12, color: BLUE): 1; 2; 3; 4; 6; 12\nset(name: D18, color: ORANGE): 1; 2; 3; 6; 9; 18\noutside: 5; 7")
        assertEquals(2, v.sets.size)
        assertEquals("#2f6fdf", v.sets[0].color)
        assertEquals(listOf("1", "2", "3", "6"), v.regions[3])
        assertEquals(listOf("4", "12"), v.regions[1])
        assertEquals(listOf("9", "18"), v.regions[2])
        assertEquals(listOf("5", "7"), v.outside)
    }

    @Test
    fun `venn explicit region lines and whitespace lists with a trailing colour`() {
        val v = VennParser.parse("set(A): a b RED\nset(B): c d\nboth(A, B): x\nonly(B): y")
        assertEquals(listOf("a", "b"), v.sets[0].elements)
        assertEquals("#d1495b", v.sets[0].color)
        assertEquals(listOf("x"), v.regions[3])
        assertTrue(v.regions[2]!!.containsAll(listOf("c", "d", "y")))
    }

    @Test
    fun `venn needs two sets and caps at three`() {
        assertTrue(VennParser.parse("set(A): 1").let { it.sets.size < 2 && it.warnings.isNotEmpty() })
        val v = VennParser.parse("set(A): 1\nset(B): 2\nset(C): 3\nset(D): 4")
        assertEquals(3, v.sets.size)
        assertEquals(1, v.warnings.size)
    }

    @Test
    fun `venn terms cover the right regions and show picks the starting term`() {
        val v = VennParser.parse("set(A): 1; 2\nset(B): 2; 3\nshow: prunik A B\nplayground: rozdil")
        assertEquals(listOf("prunik:0,1"), v.showKeys)
        val ops = VennParser.visibleOps(v)
        assertEquals(setOf(3), ops.first { it.key == "prunik:0,1" }.masks)
        assertEquals(setOf(1), ops.first { it.key == "rozdil:0,1" }.masks)
        assertTrue(ops.first { it.key == "prunik:0,1" }.description.endsWith("Prvky: {2}."))
    }

    @Test
    fun `venn complement includes the area outside all sets`() {
        val v = VennParser.parse("set(A): 1\nset(B): 2\noutside: 9")
        val op = VennParser.buildOps(v).first { it.key == "doplnek:0" }
        assertEquals(setOf(0, 2), op.masks)
        assertTrue(op.description.contains("9"))
    }

    @Test
    fun `venn fitting shrinks the font for long element lists`() {
        val slot = VennSlot(0.0, 0.0, 66.0, 80.0)
        assertTrue(fitVennText(listOf("1", "2"), slot).fits)
        assertTrue(!fitVennText((1..60).map { "prvek$it" }, slot).fits)
    }

    // ── Content integration ───────────────────────────────────────────────

    @Test
    fun `blocks become Flowchart and Venn elements`() {
        val md = "Úvod\n\n[vyvoj1]\nstart(id: s): A\n[/vyvoj1]\n\n[venn1]\nset(A): 1\nset(B): 2\n[/venn1]\n\nKonec"
        val els = parseContentToElements(md, isOffline = false, posts = emptyList())
        assertEquals(4, els.size)
        assertTrue((els[1] as ContentElement.Flowchart).source.contains("start(id: s)"))
        assertTrue((els[2] as ContentElement.Venn).source.contains("set(B)"))
    }

    @Test
    fun `arrows and dots inside a flowchart survive and fenced blocks stay literal`() {
        val els = parseContentToElements("[vyvoj1]\nnote: a ... b -> c\nstart(id: s): A\n[/vyvoj1]", isOffline = false, posts = emptyList())
        assertTrue((els.single() as ContentElement.Flowchart).source.contains("a ... b -> c"))

        val fenced = parseContentToElements("```\n[venn1]\nset(A): 1\n[/venn1]\n```", isOffline = false, posts = emptyList())
        assertTrue(fenced.none { it is ContentElement.Venn })
        assertNull(fenced.firstOrNull { it is ContentElement.Flowchart })
    }
}
