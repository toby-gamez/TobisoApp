package com.tobiso.tobisoappnative.components

import androidx.compose.ui.geometry.Offset
import com.tobiso.tobisoappnative.components.conceptmap.ConceptMapLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot

class ConceptMapLayoutTest {

    private fun dist(a: Offset, b: Offset) = hypot(a.x - b.x, a.y - b.y)

    @Test
    fun `empty input gives an empty layout`() {
        assertTrue(ConceptMapLayout.compute(emptyList(), emptyList()).isEmpty())
    }

    @Test
    fun `a single node sits at the origin`() {
        assertEquals(mapOf("a" to Offset.Zero), ConceptMapLayout.compute(listOf("a"), emptyList()))
    }

    @Test
    fun `every node gets a distinct position`() {
        val ids = listOf("root", "a", "b", "c", "a1", "a2", "b1")
        val edges = listOf("root" to "a", "root" to "b", "root" to "c", "a" to "a1", "a" to "a2", "b" to "b1")
        val layout = ConceptMapLayout.compute(ids, edges)
        assertEquals(ids.toSet(), layout.keys)
        assertEquals(ids.size, layout.values.toSet().size)
    }

    @Test
    fun `children sit at least the minimum radius from their parent`() {
        val layout = ConceptMapLayout.compute(listOf("r", "a", "b"), listOf("r" to "a", "r" to "b"))
        assertTrue(dist(layout.getValue("r"), layout.getValue("a")) >= 110f)
        assertTrue(dist(layout.getValue("r"), layout.getValue("b")) >= 110f)
    }

    @Test
    fun `a single root is placed at the origin`() {
        val layout = ConceptMapLayout.compute(listOf("r", "a"), listOf("r" to "a"))
        assertEquals(Offset.Zero, layout.getValue("r"))
    }

    @Test
    fun `a cycle does not loop forever and still places every node`() {
        val layout = ConceptMapLayout.compute(listOf("a", "b", "c"), listOf("a" to "b", "b" to "c", "c" to "a"))
        assertEquals(setOf("a", "b", "c"), layout.keys)
    }

    @Test
    fun `disconnected islands are all placed`() {
        val layout = ConceptMapLayout.compute(listOf("a", "b", "x", "y"), listOf("a" to "b", "x" to "y"))
        assertEquals(4, layout.size)
        assertTrue(dist(layout.getValue("a"), layout.getValue("x")) > 100f)
    }

    @Test
    fun `edges to unknown nodes and self loops are ignored`() {
        val layout = ConceptMapLayout.compute(listOf("a", "b"), listOf("a" to "ghost", "b" to "b", "a" to "b"))
        assertEquals(setOf("a", "b"), layout.keys)
    }

    @Test
    fun `a node reachable from two parents is placed once`() {
        val layout = ConceptMapLayout.compute(listOf("r", "a", "b", "c"), listOf("r" to "a", "r" to "b", "a" to "c", "b" to "c"))
        assertEquals(4, layout.size)
    }

    @Test
    fun `fit shrinks a large map into the viewport and centres it`() {
        val positions = listOf(Offset(-1000f, 0f), Offset(1000f, 0f))
        val (scale, offset) = ConceptMapLayout.fit(positions, 40f, 17f, 400f, 300f)
        assertTrue(scale < 0.25f)
        // the map centre (0,0) lands in the middle of the viewport
        assertEquals(200f, offset.x, 0.01f)
        assertEquals(150f, offset.y, 0.01f)
    }

    @Test
    fun `fit never zooms a tiny map beyond 1,5x`() {
        val (scale, _) = ConceptMapLayout.fit(listOf(Offset.Zero), 40f, 17f, 800f, 800f)
        assertEquals(1.5f, scale, 0.0001f)
    }

    @Test
    fun `fit handles empty input and a zero viewport`() {
        assertEquals(1f to Offset.Zero, ConceptMapLayout.fit(emptyList(), 1f, 1f, 100f, 100f))
        assertEquals(1f to Offset.Zero, ConceptMapLayout.fit(listOf(Offset.Zero), 1f, 1f, 0f, 0f))
    }
}
