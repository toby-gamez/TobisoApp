package com.tobiso.tobisoappnative.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class BookmarkSyncPlanTest {

    @Test
    fun `first sync unions local and server`() {
        val plan = planBookmarkSync(local = setOf(1, 2), server = setOf(2, 3), pendingAdd = emptySet(), pendingRemove = emptySet(), firstSync = true)
        assertEquals(setOf(1), plan.push)
        assertEquals(emptySet<Int>(), plan.delete)
        assertEquals(setOf(1, 2, 3), plan.finalIds)
    }

    @Test
    fun `later syncs treat the server as authoritative`() {
        // 1 was removed on another device; local still has it and nothing is pending for it.
        val plan = planBookmarkSync(local = setOf(1, 2), server = setOf(2), pendingAdd = emptySet(), pendingRemove = emptySet(), firstSync = false)
        assertEquals(emptySet<Int>(), plan.push)
        assertEquals(setOf(2), plan.finalIds)
    }

    @Test
    fun `offline edits in the outbox survive`() {
        val plan = planBookmarkSync(
            local = setOf(2, 5), server = setOf(2, 3),
            pendingAdd = setOf(5), pendingRemove = setOf(3), firstSync = false
        )
        assertEquals(setOf(5), plan.push)
        assertEquals(setOf(3), plan.delete)
        assertEquals(setOf(2, 5), plan.finalIds)
    }

    @Test
    fun `pending add that is already on the server is not pushed twice`() {
        val plan = planBookmarkSync(setOf(4), setOf(4), pendingAdd = setOf(4), pendingRemove = emptySet(), firstSync = false)
        assertEquals(emptySet<Int>(), plan.push)
        assertEquals(setOf(4), plan.finalIds)
    }
}
