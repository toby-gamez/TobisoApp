package com.tobiso.tobisoappnative.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.tobiso.tobisoappnative.db.dao.AddendumDao
import com.tobiso.tobisoappnative.db.dao.AiChatDao
import com.tobiso.tobisoappnative.db.dao.CategoryDao
import com.tobiso.tobisoappnative.db.dao.EventDao
import com.tobiso.tobisoappnative.db.dao.ExerciseDao
import com.tobiso.tobisoappnative.db.dao.ExercisePostDao
import com.tobiso.tobisoappnative.db.dao.FeedbackDao
import com.tobiso.tobisoappnative.db.dao.PostDao
import com.tobiso.tobisoappnative.db.dao.QuestionDao
import com.tobiso.tobisoappnative.db.dao.QuestionPostDao
import com.tobiso.tobisoappnative.db.dao.RelatedPostDao
import com.tobiso.tobisoappnative.db.entity.AddendumEntity
import com.tobiso.tobisoappnative.db.entity.AiChatMessageEntity
import com.tobiso.tobisoappnative.db.entity.AiChatSessionEntity
import com.tobiso.tobisoappnative.db.entity.CategoryEntity
import com.tobiso.tobisoappnative.db.entity.EventEntity
import com.tobiso.tobisoappnative.db.entity.ExerciseEntity
import com.tobiso.tobisoappnative.db.entity.ExercisePostEntity
import com.tobiso.tobisoappnative.db.entity.FeedbackEntity
import com.tobiso.tobisoappnative.db.entity.PostEntity
import com.tobiso.tobisoappnative.db.entity.QuestionEntity
import com.tobiso.tobisoappnative.db.entity.QuestionPostEntity
import com.tobiso.tobisoappnative.db.entity.RelatedPostEntity

@Database(
    entities = [
        CategoryEntity::class,
        PostEntity::class,
        QuestionPostEntity::class,
        QuestionEntity::class,
        EventEntity::class,
        AddendumEntity::class,
        RelatedPostEntity::class,
        ExerciseEntity::class,
        ExercisePostEntity::class,
        AiChatSessionEntity::class,
        AiChatMessageEntity::class,
        FeedbackEntity::class
    ],
    version = 5,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun postDao(): PostDao
    abstract fun questionPostDao(): QuestionPostDao
    abstract fun questionDao(): QuestionDao
    abstract fun eventDao(): EventDao
    abstract fun addendumDao(): AddendumDao
    abstract fun relatedPostDao(): RelatedPostDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun exercisePostDao(): ExercisePostDao
    abstract fun aiChatDao(): AiChatDao
    abstract fun feedbackDao(): FeedbackDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "tobiso_offline.db")
                    .addMigrations(
                        com.tobiso.tobisoappnative.di.DatabaseModule.MIGRATION_1_2,
                        com.tobiso.tobisoappnative.di.DatabaseModule.MIGRATION_2_3,
                        com.tobiso.tobisoappnative.di.DatabaseModule.MIGRATION_3_4,
                        com.tobiso.tobisoappnative.di.DatabaseModule.MIGRATION_4_5
                    )
                    .build().also { INSTANCE = it }
            }
        }
    }
}
