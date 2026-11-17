package com.tobiso.tobisoappnative.components

import android.content.Context
import com.tobiso.tobisoappnative.fake.FakeSharedPreferences
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ReadingPrefsTest {

    private lateinit var prefs: FakeSharedPreferences
    private lateinit var context: Context

    @Before
    fun setUp() {
        prefs = FakeSharedPreferences()
        context = mockk()
        every { context.getSharedPreferences("ReadingPrefs", Context.MODE_PRIVATE) } returns prefs
    }

    @Test
    fun `defaults when nothing is stored`() {
        assertEquals(ReadingPrefs(), loadReadingPrefs(context))
    }

    @Test
    fun `saved prefs round trip`() {
        val value = ReadingPrefs(ReadingFont.SERIF, ReadingSize.LARGE, ReadingWidth.NARROW)
        saveReadingPrefs(context, value)
        assertEquals(value, loadReadingPrefs(context))
    }

    @Test
    fun `unknown stored values fall back to defaults`() {
        prefs.edit().putString("font", "COMIC").putString("size", "HUGE").putString("width", "").apply()
        assertEquals(ReadingPrefs(), loadReadingPrefs(context))
    }

    @Test
    fun `one bad value does not reset the others`() {
        prefs.edit().putString("font", "MONO").putString("size", "HUGE").apply()
        val loaded = loadReadingPrefs(context)
        assertEquals(ReadingFont.MONO, loaded.font)
        assertEquals(ReadingSize.MEDIUM, loaded.size)
    }

    @Test
    fun `size scales are ordered small to large`() {
        assert(ReadingSize.SMALL.scale < ReadingSize.MEDIUM.scale && ReadingSize.MEDIUM.scale < ReadingSize.LARGE.scale)
    }
}
