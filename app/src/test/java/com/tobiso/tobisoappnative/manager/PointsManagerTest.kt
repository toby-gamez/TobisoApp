package com.tobiso.tobisoappnative.manager

import com.tobiso.tobisoappnative.fake.FakePointsManager
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PointsManagerTest {

    private lateinit var manager: FakePointsManager

    @Before
    fun setUp() {
        manager = FakePointsManager()
    }

    @Test
    fun `addPoints increases total points`() {
        manager.addPoints(50)
        assertEquals(50, manager.getPoints())
    }

    @Test
    fun `addPoints accumulates correctly across multiple calls`() {
        manager.addPoints(30)
        manager.addPoints(20)
        manager.addPoints(10)
        assertEquals(60, manager.getPoints())
    }

    @Test
    fun `addPoints also increments totalEarnedPoints`() {
        manager.addPoints(100)
        assertEquals(100, manager.getTotalEarnedPoints())
    }

    @Test
    fun `subtractPoints returns true and reduces balance when sufficient`() {
        manager.addPoints(100)
        val result = manager.subtractPoints(40)
        assertTrue(result)
        assertEquals(60, manager.getPoints())
    }

    @Test
    fun `subtractPoints returns false when balance is insufficient`() {
        manager.addPoints(20)
        val result = manager.subtractPoints(50)
        assertFalse(result)
        assertEquals(20, manager.getPoints())
    }

    @Test
    fun `subtractPoints does not change balance on failure`() {
        manager.addPoints(10)
        manager.subtractPoints(100)
        assertEquals(10, manager.getPoints())
    }

    @Test
    fun `addPoints with active multiplier scales amount correctly`() {
        manager.activateMultiplier(2.0f, 60)
        manager.addPoints(50)
        assertEquals(100, manager.getPoints())
    }

    @Test
    fun `addPointsForMilestone sets lastMilestone flow`() = runTest {
        manager.lastMilestone.test {
            awaitItem() // initial null
            manager.addPointsForMilestone(200, 30)
            assertEquals(30, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `addPointsForAchievement sets lastAchievement flow`() = runTest {
        manager.lastAchievement.test {
            awaitItem() // initial null
            manager.addPointsForAchievement(150, 500)
            assertEquals(500, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `resetLastAddedPoints clears milestone and achievement`() = runTest {
        manager.addPointsForMilestone(100, 7)
        manager.addPointsForAchievement(50, 100)
        manager.resetLastAddedPoints()

        manager.lastMilestone.test {
            assertEquals(null, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        manager.lastAchievement.test {
            assertEquals(null, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `totalPoints flow emits updated value after addPoints`() = runTest {
        manager.totalPoints.test {
            awaitItem() // initial 0
            manager.addPoints(75)
            assertEquals(75, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
