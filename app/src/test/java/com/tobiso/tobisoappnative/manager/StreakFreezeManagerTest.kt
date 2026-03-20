package com.tobiso.tobisoappnative.manager

import com.tobiso.tobisoappnative.fake.FakeStreakFreezeManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class StreakFreezeManagerTest {

    private lateinit var manager: FakeStreakFreezeManager

    @Before
    fun setUp() {
        manager = FakeStreakFreezeManager(initialFreezes = 0)
    }

    // --- addStreakFreeze ---

    @Test
    fun `addStreakFreeze returns true and increments count`() {
        val result = manager.addStreakFreeze()
        assertTrue(result)
        assertEquals(1, manager.getAvailableFreezes())
    }

    @Test
    fun `addStreakFreeze can add up to MAX_FREEZES`() {
        repeat(FakeStreakFreezeManager.MAX_FREEZES) {
            assertTrue(manager.addStreakFreeze())
        }
        assertEquals(FakeStreakFreezeManager.MAX_FREEZES, manager.getAvailableFreezes())
    }

    @Test
    fun `addStreakFreeze returns false when at max`() {
        repeat(FakeStreakFreezeManager.MAX_FREEZES) { manager.addStreakFreeze() }
        val result = manager.addStreakFreeze()
        assertFalse(result)
        assertEquals(FakeStreakFreezeManager.MAX_FREEZES, manager.getAvailableFreezes())
    }

    // --- useFreeze ---

    @Test
    fun `useFreeze returns true and decrements count`() {
        manager.addStreakFreeze()
        val result = manager.useFreeze("2026-03-15")
        assertTrue(result)
        assertEquals(0, manager.getAvailableFreezes())
    }

    @Test
    fun `useFreeze returns false when no freezes available`() {
        val result = manager.useFreeze("2026-03-15")
        assertFalse(result)
    }

    @Test
    fun `useFreeze returns false when same date is already used`() {
        manager.addStreakFreeze()
        manager.addStreakFreeze()
        manager.useFreeze("2026-03-15")
        val result = manager.useFreeze("2026-03-15")
        assertFalse(result)
        assertEquals(1, manager.getAvailableFreezes()) // second freeze still available
    }

    @Test
    fun `useFreeze allows different dates`() {
        manager.addStreakFreeze()
        manager.addStreakFreeze()
        assertTrue(manager.useFreeze("2026-03-14"))
        assertTrue(manager.useFreeze("2026-03-15"))
        assertEquals(0, manager.getAvailableFreezes())
    }

    // --- isFreezeActive ---

    @Test
    fun `isFreezeActive returns false before freeze used`() {
        assertFalse(manager.isFreezeActive("2026-03-15"))
    }

    @Test
    fun `isFreezeActive returns true after freeze used`() {
        manager.addStreakFreeze()
        manager.useFreeze("2026-03-15")
        assertTrue(manager.isFreezeActive("2026-03-15"))
    }

    @Test
    fun `isFreezeActive returns false for date that was not frozen`() {
        manager.addStreakFreeze()
        manager.useFreeze("2026-03-15")
        assertFalse(manager.isFreezeActive("2026-03-16"))
    }

    // --- getUsedFreezes ---

    @Test
    fun `getUsedFreezes returns all used dates`() {
        manager.addStreakFreeze()
        manager.addStreakFreeze()
        manager.useFreeze("2026-03-10")
        manager.useFreeze("2026-03-11")
        assertEquals(setOf("2026-03-10", "2026-03-11"), manager.getUsedFreezes())
    }

    // --- resetFreezes ---

    @Test
    fun `resetFreezes clears count and used dates`() {
        manager.addStreakFreeze()
        manager.useFreeze("2026-03-15")
        manager.resetFreezes()
        assertEquals(0, manager.getAvailableFreezes())
        assertTrue(manager.getUsedFreezes().isEmpty())
    }

    @Test
    fun `availableFreezes flow reflects current state`() {
        assertEquals(0, manager.availableFreezes.value)
        manager.addStreakFreeze()
        assertEquals(1, manager.availableFreezes.value)
        manager.useFreeze("2026-03-15")
        assertEquals(0, manager.availableFreezes.value)
    }
}
