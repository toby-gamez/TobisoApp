package com.tobiso.tobisoappnative.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class PostOrderTest {

    private data class P(val id: Int, val order: Int?)

    private val posts = listOf(P(1, null), P(2, 3), P(3, 1), P(4, 2), P(5, null))

    @Test
    fun `sorted category orders by position with unplaced last`() {
        val result = posts.orderedByManualOrder(sorted = true) { it.order }
        assertEquals(listOf(3, 4, 2, 1, 5), result.map { it.id })
    }

    @Test
    fun `unsorted category keeps the server order even if positions exist`() {
        val result = posts.orderedByManualOrder(sorted = false) { it.order }
        assertEquals(listOf(1, 2, 3, 4, 5), result.map { it.id })
    }

    @Test
    fun `unplaced posts keep their relative order`() {
        val result = listOf(P(9, null), P(8, null)).orderedByManualOrder(sorted = true) { it.order }
        assertEquals(listOf(9, 8), result.map { it.id })
    }

    @Test
    fun `empty list stays empty`() {
        assertEquals(emptyList<P>(), emptyList<P>().orderedByManualOrder(sorted = true) { it.order })
    }
}
