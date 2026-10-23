package com.tobiso.tobisoappnative.components

import com.tobiso.tobisoappnative.model.EventScope
import com.tobiso.tobisoappnative.model.EventType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

class StudentEventRequestTest {
    private fun day(d: Int) = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, d, 15, 30, 0) }.time

    @Test
    fun `single day events send only a start at midnight`() {
        val r = buildStudentEventRequest("  Test z mluvnice ", " ", day(10), day(10), EventType.Test, EventScope.Personal, 3)
        assertEquals("Test z mluvnice", r.title)
        assertNull(r.description)
        assertEquals("2026-11-10T00:00:00", r.startDate)
        assertNull(r.endDate)
        assertNull(r.groupId) // only sent for Group scope
        assertEquals("Test", r.type)
        assertEquals("Personal", r.scope)
    }

    @Test
    fun `multi day events end at the end of the last day`() {
        val r = buildStudentEventRequest("Projekt", "popis", day(10), day(12), EventType.Homework, EventScope.Group, 3)
        assertEquals("2026-11-12T23:59:59", r.endDate)
        assertEquals(3, r.groupId)
        assertEquals("Homework", r.type)
        assertEquals("Group", r.scope)
    }
}
