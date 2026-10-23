package com.tobiso.tobisoappnative.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EventThemeOptionsTest {
    private val options = buildThemeOptions(
        categories = listOf(Category(id = 1, name = "Matematika"), Category(id = 2, name = "Dějepis")),
        posts = listOf(
            PostSummaryResponse(id = 1, title = "Zlomky", filePath = "mat/zlomky.md"),
            PostSummaryResponse(id = 2, title = "Matematická logika", filePath = "mat/logika.md")
        )
    )

    @Test fun categoriesComeFirstAndKeysDoNotCollide() {
        assertEquals(listOf("c2", "c1", "p2", "p1").size, options.map { it.key }.distinct().size)
        assertTrue(options.take(2).all { it.categoryId != null })
        assertEquals("Dějepis", options.first().name)
    }

    @Test fun blankQueryReturnsNothing() = assertTrue(filterThemeOptions(options, "  ", emptySet()).isEmpty())

    @Test fun filtersCaseInsensitivelyAndSkipsSelected() {
        val all = filterThemeOptions(options, "matemat", emptySet())
        assertEquals(2, all.size)
        assertEquals(1, filterThemeOptions(options, "matemat", setOf("c1")).size)
    }

    @Test fun limitIsRespected() = assertEquals(1, filterThemeOptions(options, "a", emptySet(), limit = 1).size)
}
