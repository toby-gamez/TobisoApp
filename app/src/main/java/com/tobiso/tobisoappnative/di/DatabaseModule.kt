package com.tobiso.tobisoappnative.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.tobiso.tobisoappnative.db.AppDatabase
import com.tobiso.tobisoappnative.db.dao.AddendumDao
import com.tobiso.tobisoappnative.db.dao.AiChatDao
import com.tobiso.tobisoappnative.db.dao.CategoryDao
import com.tobiso.tobisoappnative.db.dao.EventDao
import com.tobiso.tobisoappnative.db.dao.ExerciseDao
import com.tobiso.tobisoappnative.db.dao.ExercisePostDao
import com.tobiso.tobisoappnative.db.dao.PostDao
import com.tobiso.tobisoappnative.db.dao.QuestionDao
import com.tobiso.tobisoappnative.db.dao.QuestionPostDao
import com.tobiso.tobisoappnative.db.dao.FeedbackDao
import com.tobiso.tobisoappnative.db.dao.RelatedPostDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import timber.log.Timber

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """CREATE TABLE IF NOT EXISTS ai_chat_sessions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    postId INTEGER NOT NULL,
                    postTitle TEXT NOT NULL,
                    startedAt INTEGER NOT NULL,
                    lastMessageAt INTEGER NOT NULL,
                    lastMessagePreview TEXT NOT NULL DEFAULT '',
                    messageCount INTEGER NOT NULL DEFAULT 0
                )"""
            )
            database.execSQL(
                """CREATE TABLE IF NOT EXISTS ai_chat_messages (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    sessionId INTEGER NOT NULL,
                    role TEXT NOT NULL,
                    content TEXT NOT NULL,
                    timestamp INTEGER NOT NULL,
                    FOREIGN KEY(sessionId) REFERENCES ai_chat_sessions(id) ON DELETE CASCADE
                )"""
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_ai_chat_messages_sessionId ON ai_chat_messages(sessionId)"
            )
            Timber.d("Applying migration 1->2: created ai_chat_sessions and ai_chat_messages tables")
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE posts ADD COLUMN versionsJson TEXT")
            database.execSQL("ALTER TABLE questions_posts ADD COLUMN versionsJson TEXT")
            Timber.d("Applying migration 2->3: added versionsJson to posts and questions_posts")
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """CREATE TABLE IF NOT EXISTS exercise_post (
                    exerciseId INTEGER NOT NULL,
                    postId INTEGER NOT NULL,
                    PRIMARY KEY(exerciseId, postId)
                )"""
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_exercise_post_postId ON exercise_post(postId)"
            )
            Timber.d("Applying migration 3->4: added exercise_post join table")
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """CREATE TABLE IF NOT EXISTS pending_feedback (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL,
                    email TEXT NOT NULL,
                    message TEXT NOT NULL,
                    platform TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    retryCount INTEGER NOT NULL DEFAULT 0
                )"""
            )
            Timber.d("Applying migration 4->5: added pending_feedback table")
        }
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "tobiso_offline.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .build()

    @Provides
    fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun providePostDao(db: AppDatabase): PostDao = db.postDao()

    @Provides
    fun provideQuestionPostDao(db: AppDatabase): QuestionPostDao = db.questionPostDao()

    @Provides
    fun provideQuestionDao(db: AppDatabase): QuestionDao = db.questionDao()

    @Provides
    fun provideEventDao(db: AppDatabase): EventDao = db.eventDao()

    @Provides
    fun provideAddendumDao(db: AppDatabase): AddendumDao = db.addendumDao()

    @Provides
    fun provideRelatedPostDao(db: AppDatabase): RelatedPostDao = db.relatedPostDao()

    @Provides
    fun provideExerciseDao(db: AppDatabase): ExerciseDao = db.exerciseDao()

    @Provides
    fun provideExercisePostDao(db: AppDatabase): ExercisePostDao = db.exercisePostDao()

    @Provides
    fun provideAiChatDao(db: AppDatabase): AiChatDao = db.aiChatDao()

    @Provides
    fun provideFeedbackDao(db: AppDatabase): FeedbackDao = db.feedbackDao()
}
