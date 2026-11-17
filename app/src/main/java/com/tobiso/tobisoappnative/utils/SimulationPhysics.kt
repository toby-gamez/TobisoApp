package com.tobiso.tobisoappnative.utils

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Pure physics for the "simulation" exercise type; a port of web `simulations.js`. */
object SimulationPhysics {
    const val TAU = 2 * PI
    const val DEG = PI / 180

    // ── Kepler ──

    /** Solves Kepler's equation E - e·sin(E) = M (Newton); E in [0, 2π). */
    fun keplerE(meanAnomaly: Double, e: Double): Double {
        val m = ((meanAnomaly % TAU) + TAU) % TAU
        var big = if (e < 0.8) m else PI
        repeat(40) {
            val d = (big - e * sin(big) - m) / (1 - e * cos(big))
            big -= d
            if (abs(d) < 1e-12) return big
        }
        return big
    }

    data class Sector(val e0: Double, val e1: Double, val area: Double, val arc: Double)

    /** Splits one orbit into n equal-TIME sectors (area = Kepler II swept area, arc = orbit length). */
    fun keplerSectors(a: Double, e: Double, n: Int): List<Sector> {
        val b = a * sqrt(1 - e * e)
        return (0 until n).map { k ->
            val e0 = if (k == 0) 0.0 else keplerE(k * TAU / n, e)
            val e1 = if (k == n - 1) TAU else keplerE((k + 1) * TAU / n, e)
            val area = 0.5 * a * b * ((e1 - e0) - e * (sin(e1) - sin(e0)))
            val steps = 48
            var arc = 0.0
            for (i in 0 until steps) {
                val big = e0 + (i + 0.5) / steps * (e1 - e0)
                arc += sqrt(a * a * sin(big).pow(2) + b * b * cos(big).pow(2))
            }
            Sector(e0, e1, area, arc * (e1 - e0) / steps)
        }
    }

    // ── Integration ──

    fun rk4(s: DoubleArray, dt: Double, derivs: (DoubleArray) -> DoubleArray): DoubleArray {
        fun add(a: DoubleArray, b: DoubleArray, k: Double) = DoubleArray(a.size) { a[it] + b[it] * k }
        val k1 = derivs(s)
        val k2 = derivs(add(s, k1, dt / 2))
        val k3 = derivs(add(s, k2, dt / 2))
        val k4 = derivs(add(s, k3, dt))
        return DoubleArray(s.size) { s[it] + dt / 6 * (k1[it] + 2 * k2[it] + 2 * k3[it] + k4[it]) }
    }

    /** Central force F = mu / r^n per unit mass; state = [x, y, vx, vy]. */
    fun centralDerivs(mu: Double, n: Double): (DoubleArray) -> DoubleArray = { s ->
        val r = hypot(s[0], s[1])
        val f = mu / r.pow(n + 1)
        doubleArrayOf(s[2], s[3], -s[0] * f, -s[1] * f)
    }

    data class TrajPoint(val t: Double, val x: Double, val y: Double, val vx: Double, val vy: Double)
    enum class End { CRASH, ESCAPE, TIME }
    class Trajectory(val pts: List<TrajPoint>, val end: End)

    fun integrateCentral(
        start: DoubleArray, mu: Double, n: Double, rMin: Double, rMax: Double, maxTime: Double
    ): Trajectory {
        val f = centralDerivs(mu, n)
        var s = start.copyOf()
        var t = 0.0
        val pts = mutableListOf(TrajPoint(t, s[0], s[1], s[2], s[3]))
        repeat(6000) {
            val r = hypot(s[0], s[1])
            val v = hypot(s[2], s[3])
            val dt = min(0.01 * r / max(v, 1e-9), 0.02 * sqrt(r.pow(n + 1) / mu))
            s = rk4(s, dt, f)
            t += dt
            val r2 = hypot(s[0], s[1])
            if (r2 < rMin) {
                val k = (r - rMin) / max(r - r2, 1e-12)
                val p = pts.last()
                pts += TrajPoint(p.t + (t - p.t) * k, p.x + (s[0] - p.x) * k, p.y + (s[1] - p.y) * k, s[2], s[3])
                return Trajectory(pts, End.CRASH)
            }
            pts += TrajPoint(t, s[0], s[1], s[2], s[3])
            if (r2 > rMax) return Trajectory(pts, End.ESCAPE)
            if (t >= maxTime) return Trajectory(pts, End.TIME)
        }
        return Trajectory(pts, End.TIME)
    }

    // ── Orbit classification ──

    enum class OrbitKind { CIRCLE, ORBIT, SUBORBITAL, PARABOLA, HYPERBOLA }
    data class OrbitInfo(val kind: OrbitKind, val e: Double, val period: Double?)

    fun classifyOrbit(r: Double, v: Double, angle: Double, mu: Double, rBody: Double): OrbitInfo {
        val eps = v * v / 2 - mu / r
        val h = r * v * cos(angle)
        val e = sqrt(max(0.0, 1 + 2 * eps * h * h / (mu * mu)))
        if (abs(eps) < 1e-4 * mu / r) return OrbitInfo(OrbitKind.PARABOLA, 1.0, null)
        if (eps > 0) return OrbitInfo(OrbitKind.HYPERBOLA, e, null)
        val a = -mu / (2 * eps)
        val rp = a * (1 - e)
        val period = TAU * sqrt(a * a * a / mu)
        val kind = when {
            e < 0.005 && rp > rBody -> OrbitKind.CIRCLE
            rp > rBody -> OrbitKind.ORBIT
            else -> OrbitKind.SUBORBITAL
        }
        return OrbitInfo(kind, e, period)
    }

    // ── Flat throw ──

    data class FlatThrow(
        val vx: Double, val vy: Double, val tFlight: Double,
        val range: Double, val hMax: Double, val vImpact: Double
    )

    fun flatThrow(v0: Double, angleDeg: Double, h: Double, g: Double): FlatThrow {
        val vx = v0 * cos(angleDeg * DEG)
        val vy = v0 * sin(angleDeg * DEG)
        val tFlight = (vy + sqrt(vy * vy + 2 * g * h)) / g
        val hMax = if (vy > 0) h + vy * vy / (2 * g) else h
        return FlatThrow(vx, vy, tFlight, vx * tFlight, hMax, hypot(vx, vy - g * tFlight))
    }

    fun niceStep(span: Double, target: Double = 6.0): Double {
        val raw = span / target
        val pow = 10.0.pow(floor(log10(raw)))
        val m = raw / pow
        return (if (m < 1.5) 1.0 else if (m < 3.5) 2.0 else if (m < 7.5) 5.0 else 10.0) * pow
    }
}
