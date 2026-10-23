package com.tobiso.tobisoappnative.model

import com.tobiso.tobisoappnative.db.entity.toDomain
import com.tobiso.tobisoappnative.db.entity.toEntity
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EventModelTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `old payload without the new fields still parses`() {
        val e = json.decodeFromString<Event>("""{"id":1,"title":"Volno","startDate":"2026-10-23T00:00:00","color":"#007bff"}""")
        assertNull(e.type)
        assertTrue(e.themes.isEmpty())
        assertEquals("#007bff", e.getColorSafe())
    }

    @Test
    fun `null color falls back to the type color`() {
        val e = json.decodeFromString<Event>("""{"id":2,"title":"Test z mluvnice","color":null,"type":"Test","scope":"Class"}""")
        assertEquals("#d9534f", e.getColorSafe())
        assertEquals(EventType.Test, e.eventType)
        assertEquals(EventScope.Class, e.eventScope)
    }

    @Test
    fun `enums sent as numbers are understood too`() {
        val e = json.decodeFromString<Event>("""{"id":3,"type":2,"scope":3}""")
        assertEquals(EventType.Homework, e.eventType)
        assertEquals(EventScope.Personal, e.eventScope)
    }

    @Test
    fun `unknown enum values are tolerated`() {
        val e = json.decodeFromString<Event>("""{"id":4,"type":"Excursion","scope":"Planet"}""")
        assertNull(e.eventType)
        assertNull(e.eventScope)
        assertEquals("#33d17a", e.getColorSafe())
    }

    @Test
    fun `student created events and themes are decoded`() {
        val e = json.decodeFromString<Event>(
            """{"id":5,"type":"Test","scope":"Group","groupId":7,"groupName":"A","createdByUserId":9,"createdByName":"Jana",
               "themes":[{"postId":12,"name":"Slovesa","postFilePath":"cestina/slovesa"},{"categoryId":3,"name":"Mluvnice"}]}"""
        )
        assertTrue(e.isStudentCreated)
        assertEquals(2, e.themes.size)
        assertEquals(12, e.themes[0].postId)
        assertEquals(3, e.themes[1].categoryId)
    }

    @Test
    fun `entity round trip keeps the new fields`() {
        val e = Event(
            id = 6, title = "T", type = "Homework", scope = "Personal", classId = 1, className = "7.A",
            createdByUserId = 4, createdByName = "Já", customColor = "#112233",
            themes = listOf(EventTheme(postId = 1, name = "P"))
        )
        val back = e.toEntity(ownerKey = "a@b.cz").also { assertEquals("a@b.cz", it.ownerKey) }.toDomain()
        assertEquals(e.type, back.type)
        assertEquals(e.scope, back.scope)
        assertEquals(e.className, back.className)
        assertEquals(e.createdByName, back.createdByName)
        assertEquals(e.themes, back.themes)
        assertFalse(back.themes.isEmpty())
    }

    @Test
    fun `my class decodes status and the no-pending case`() {
        val m = json.decodeFromString<MyClass>(
            """{"classId":1,"className":"7.A","status":"Approved","groups":[{"id":1,"name":"Skupina 1"}],"availableGroups":[{"id":1,"name":"Skupina 1"},{"id":2,"name":"Skupina 2"}]}"""
        )
        assertEquals(ClassMembershipStatus.Approved, m.membershipStatus)
        assertFalse(m.groupChangePending)
        assertNull(m.pendingGroup)
    }
}
