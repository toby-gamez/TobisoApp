package com.tobiso.tobisoappnative.db

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tobiso.tobisoappnative.di.DatabaseModule.MIGRATION_5_6
import com.tobiso.tobisoappnative.di.DatabaseModule.MIGRATION_6_7
import com.tobiso.tobisoappnative.di.DatabaseModule.MIGRATION_7_8
import com.tobiso.tobisoappnative.di.DatabaseModule.MIGRATION_8_9
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Schemas 5..9 are exported to `app/schemas` (wired into the androidTest assets in build.gradle.kts). Migrations 1→5
 * predate the exported schemas and can't be validated here.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val testDb = "migration-test"
    private val all = arrayOf(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @After
    fun cleanup() {
        InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase(testDb)
    }

    @Test
    fun migrate5to6_createsExerciseCategoryAndKeepsData() {
        helper.createDatabase(testDb, 5).apply {
            execSQL("INSERT INTO categories (id, name, slug, parentId, parentJson, childrenJson) VALUES (1, 'Math', 'math', NULL, '', '')")
            close()
        }

        val db = helper.runMigrationsAndValidate(testDb, 6, true, MIGRATION_5_6)

        db.query("SELECT name FROM categories WHERE id=1").use {
            assertTrue(it.moveToFirst()); assertEquals("Math", it.getString(0))
        }
        db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='exercise_category'").use {
            assertEquals(1, it.count)
        }
    }

    @Test
    fun migrate7to8_addsEventColumnsAsNullAndKeepsRows() {
        helper.createDatabase(testDb, 7).apply {
            execSQL("INSERT INTO events (id, title, startDate, color, isLocal) VALUES (3, 'Test z matiky', '2026-10-30T08:00:00', '#ff0000', 0)")
            close()
        }

        val db = helper.runMigrationsAndValidate(testDb, 8, true, MIGRATION_7_8)

        db.query("SELECT title, color, type, scope, ownerKey, themesJson FROM events WHERE id=3").use {
            assertTrue(it.moveToFirst())
            assertEquals("Test z matiky", it.getString(0))
            assertEquals("#ff0000", it.getString(1))
            (2..5).forEach { c -> assertTrue(it.isNull(c)) }
        }
    }

    @Test
    fun migrate8to9_queuedFeedbackSurvivesWithDefaults() {
        helper.createDatabase(testDb, 8).apply {
            execSQL("INSERT INTO pending_feedback (id, name, email, message, platform, createdAt, retryCount) VALUES (1, 'Jana', 'j@x.cz', 'Nefunguje to', 'android', 1000, 2)")
            close()
        }

        val db = helper.runMigrationsAndValidate(testDb, 9, true, MIGRATION_8_9)

        db.query("SELECT message, retryCount, title, type FROM pending_feedback WHERE id=1").use {
            assertTrue(it.moveToFirst())
            assertEquals("Nefunguje to", it.getString(0))
            assertEquals(2, it.getInt(1))
            assertEquals("", it.getString(2))
            assertEquals("Bug", it.getString(3))
        }
    }

    @Test
    fun migrateFrom5ToLatest_keepsDataAndOpensWithRoom() {
        helper.createDatabase(testDb, 5).apply {
            execSQL("INSERT INTO categories (id, name, slug, parentId, parentJson, childrenJson) VALUES (1, 'Math', 'math', NULL, '', '')")
            execSQL("INSERT INTO events (id, title, startDate, isLocal) VALUES (9, 'Svátek', '2026-11-17T00:00:00', 0)")
            close()
        }

        helper.runMigrationsAndValidate(testDb, 9, true, *all).close()

        // Open it the way the app does, so Room's own identity/schema check runs too.
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val room = Room.databaseBuilder(ctx, AppDatabase::class.java, testDb).addMigrations(*all).build()
        try {
            room.openHelper.writableDatabase.query("SELECT title, ownerKey FROM events WHERE id=9").use {
                assertTrue(it.moveToFirst())
                assertEquals("Svátek", it.getString(0))
                assertTrue(it.isNull(1))
            }
        } finally {
            room.close()
        }
    }
}
