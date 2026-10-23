package com.tobiso.tobisoappnative.model

import com.tobiso.tobisoappnative.screens.EventFilter
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EventFilterTest {
    private val test = Event(id = 1, type = "Test", scope = "Class")
    private val homework = Event(id = 2, type = "Homework", scope = "Personal")
    private val holiday = Event(id = 3, type = "DayOff", scope = "Global")
    private val local = Event(id = -1, isLocal = true)

    @Test
    fun `type filters`() {
        assertTrue(EventFilter.TESTS.matches(test))
        assertFalse(EventFilter.TESTS.matches(homework))
        assertTrue(EventFilter.HOMEWORK.matches(homework))
        assertTrue(EventFilter.DAY_OFF.matches(holiday))
    }

    @Test
    fun `scope filters`() {
        assertTrue(EventFilter.CLASS_AND_GROUP.matches(test))
        assertFalse(EventFilter.CLASS_AND_GROUP.matches(holiday))
        assertTrue(EventFilter.PERSONAL.matches(homework))
        assertTrue(EventFilter.PERSONAL.matches(local))
    }

    @Test
    fun `local and remote are complements`() {
        assertTrue(EventFilter.LOCAL.matches(local))
        assertTrue(EventFilter.REMOTE.matches(test))
        assertFalse(EventFilter.REMOTE.matches(local))
        assertTrue(EventFilter.ALL.matches(holiday))
    }
}
