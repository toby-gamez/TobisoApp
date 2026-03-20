package com.tobiso.tobisoappnative.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.tobiso.tobisoappnative.db.dao.AddendumDao
import com.tobiso.tobisoappnative.db.dao.CategoryDao
import com.tobiso.tobisoappnative.db.dao.EventDao
import com.tobiso.tobisoappnative.db.dao.ExerciseDao
import com.tobiso.tobisoappnative.db.dao.PostDao
import com.tobiso.tobisoappnative.db.dao.QuestionDao
import com.tobiso.tobisoappnative.db.dao.QuestionPostDao
import com.tobiso.tobisoappnative.db.dao.RelatedPostDao
import com.tobiso.tobisoappnative.db.entity.AddendumEntity
import com.tobiso.tobisoappnative.db.entity.CategoryEntity
import com.tobiso.tobisoappnative.db.entity.EventEntity
import com.tobiso.tobisoappnative.db.entity.ExerciseEntity
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
        ExerciseEntity::class
    ],
    version = 1,
    exportSchema = false
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
}
