package com.tobiso.tobisoappnative.utils

import com.tobiso.tobisoappnative.utils.SimulationPhysics.End
import com.tobiso.tobisoappnative.utils.SimulationPhysics.OrbitKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.sqrt

class SimulationPhysicsTest {

    @Test
    fun `kepler equation is satisfied`() {
        val e = 0.6
        val m = 1.3
        val big = SimulationPhysics.keplerE(m, e)
        assertEquals(m, big - e * sin(big), 1e-9)
    }

    @Test
    fun `sectors have equal total area summing to the ellipse`() {
        val a = 1.5
        val e = 0.5
        val sectors = SimulationPhysics.keplerSectors(a, e, 8)
        val b = a * sqrt(1 - e * e)
        assertEquals(PI * a * b, sectors.sumOf { it.area }, 1e-6)
        val first = sectors.first().area
        sectors.forEach { assertEquals(first, it.area, 1e-6) }
    }

    @Test
    fun `flat throw at 45 degrees from ground has v0 squared over g range`() {
        val t = SimulationPhysics.flatThrow(20.0, 45.0, 0.0, 9.81)
        assertEquals(400 / 9.81, t.range, 1e-6)
    }

    @Test
    fun `circular speed gives a circle and escape speed escapes`() {
        val mu = 398600.0
        val r = 6671.0
        val circ = SimulationPhysics.classifyOrbit(r, sqrt(mu / r), 0.0, mu, 6371.0)
        assertEquals(OrbitKind.CIRCLE, circ.kind)
        val esc = SimulationPhysics.classifyOrbit(r, sqrt(2 * mu / r) * 1.01, 0.0, mu, 6371.0)
        assertEquals(OrbitKind.HYPERBOLA, esc.kind)
    }

    @Test
    fun `slow shot crashes and fast shot escapes`() {
        val mu = 398600.0
        val r = 6671.0
        val slow = SimulationPhysics.integrateCentral(doubleArrayOf(0.0, r, 3.0, 0.0), mu, 2.0, 6371.0, 14 * 6371.0, 20000.0)
        assertEquals(End.CRASH, slow.end)
        val fast = SimulationPhysics.integrateCentral(doubleArrayOf(0.0, r, sqrt(2 * mu / r) * 1.2, 0.0), mu, 2.0, 6371.0, 14 * 6371.0, 100000.0)
        assertEquals(End.ESCAPE, fast.end)
        assertTrue(fast.pts.size > 2)
    }
}
