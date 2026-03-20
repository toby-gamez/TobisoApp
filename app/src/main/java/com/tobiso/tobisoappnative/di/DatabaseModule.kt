package com.tobiso.tobisoappnative.di

import android.content.Context
import androidx.room.Room
import com.tobiso.tobisoappnative.db.AppDatabase
import com.tobiso.tobisoappnative.db.dao.AddendumDao
import com.tobiso.tobisoappnative.db.dao.CategoryDao
import com.tobiso.tobisoappnative.db.dao.EventDao
import com.tobiso.tobisoappnative.db.dao.ExerciseDao
import com.tobiso.tobisoappnative.db.dao.PostDao
import com.tobiso.tobisoappnative.db.dao.QuestionDao
import com.tobiso.tobisoappnative.db.dao.QuestionPostDao
import com.tobiso.tobisoappnative.db.dao.RelatedPostDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "tobiso_offline.db")
            .fallbackToDestructiveMigration()
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
}
