package com.tobiso.tobisoappnative.db

import android.database.sqlite.SQLiteException
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tobiso.tobisoappnative.di.DatabaseModule.MIGRATION_5_6
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val TEST_DB = "migration-test"
    private val DB_PATH = "com.tobiso.tobisoappnative.db.AppDatabase"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        listOf()
    )

    @After
    @Throws(IOException::class)
    fun cleanup() {
        InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase(TEST_DB)
    }

    @Test
    @Throws(IOException::class)
    fun migrate5to6_createsExerciseCategoryTable() {
        var db: SupportSQLiteDatabase = helper.createDatabase(TEST_DB, 5)

        db.execSQL("INSERT INTO categories (id, name, slug, parentId, parentJson, childrenJson) VALUES (1, 'Math', 'math', NULL, '', '')")
        db.execSQL("INSERT INTO exercises (id, postId, type, title, instructions, content, correctAnswer, optionsJson, points, difficulty, position, createdAt, active) VALUES (10, 1, 'quiz', 'Test', '', '{}', 'A', '[]', 10, 1, 1, '2024-01-01', 1)")
        db.close()

        db = helper.runMigrationsAndValidate(TEST_DB, 6, true, MIGRATION_5_6)

        val cursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='exercise_category'")
        assertTrue(cursor.count > 0)
        cursor.close()

        db.execSQL("INSERT INTO exercise_category (exerciseId, categoryId) VALUES (10, 1)")
        val verifyCursor = db.query("SELECT * FROM exercise_category WHERE exerciseId=10 AND categoryId=1")
        assertTrue(verifyCursor.count > 0)
        verifyCursor.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate5to6_preservesExistingData() {
        var db: SupportSQLiteDatabase = helper.createDatabase(TEST_DB, 5)

        db.execSQL("INSERT INTO categories (id, name, slug, parentId, parentJson, childrenJson) VALUES (2, 'Physics', 'physics', NULL, '', '')")
        db.close()

        db = helper.runMigrationsAndValidate(TEST_DB, 6, true, MIGRATION_5_6)

        val cursor = db.query("SELECT name FROM categories WHERE id=2")
        assertTrue(cursor.count > 0)
        cursor.moveToFirst()
        assertTrue(cursor.getString(cursor.getColumnIndexOrThrow("name")) == "Physics")
        cursor.close()
    }
}
