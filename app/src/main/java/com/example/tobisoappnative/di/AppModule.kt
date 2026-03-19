package com.example.tobisoappnative.di

import android.app.Application
import android.content.Context
import com.example.tobisoappnative.domain.usecase.GetAllQuestionsUseCase
import com.example.tobisoappnative.domain.usecase.GetExerciseUseCase
import com.example.tobisoappnative.domain.usecase.ValidateExerciseUseCase
import com.example.tobisoappnative.model.OfflineDataManager
import com.example.tobisoappnative.repository.ExerciseRepository
import com.example.tobisoappnative.repository.ExerciseRepositoryImpl
import com.example.tobisoappnative.repository.FavoritesRepositoryImpl
import com.example.tobisoappnative.repository.OfflineRepositoryImpl
import com.example.tobisoappnative.repository.PostDetailRepository
import com.example.tobisoappnative.repository.PostDetailRepositoryImpl
import com.example.tobisoappnative.repository.PostsRepository
import com.example.tobisoappnative.repository.PostsRepositoryImpl
import com.example.tobisoappnative.repository.QuestionsRepository
import com.example.tobisoappnative.repository.QuestionsRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideOfflineDataManager(@ApplicationContext context: Context): OfflineDataManager =
        OfflineDataManager(context)

    @Provides
    @Singleton
    fun providePostsRepository(
        @ApplicationContext context: Context,
        offlineDataManager: OfflineDataManager
    ): PostsRepository = PostsRepositoryImpl(context, offlineDataManager)

    @Provides
    @Singleton
    fun providePostDetailRepository(
        @ApplicationContext context: Context,
        offlineDataManager: OfflineDataManager
    ): PostDetailRepository = PostDetailRepositoryImpl(context, offlineDataManager)

    @Provides
    @Singleton
    fun provideExerciseRepository(
        @ApplicationContext context: Context,
        offlineDataManager: OfflineDataManager
    ): ExerciseRepository = ExerciseRepositoryImpl(context, offlineDataManager)

    @Provides
    @Singleton
    fun provideQuestionsRepository(
        @ApplicationContext context: Context,
        offlineDataManager: OfflineDataManager
    ): QuestionsRepository = QuestionsRepositoryImpl(context, offlineDataManager)

    @Provides
    @Singleton
    fun provideOfflineRepository(
        @ApplicationContext context: Context,
        offlineDataManager: OfflineDataManager
    ): OfflineRepositoryImpl = OfflineRepositoryImpl(context, offlineDataManager)

    @Provides
    @Singleton
    fun provideFavoritesRepository(application: Application): FavoritesRepositoryImpl =
        FavoritesRepositoryImpl(application)

    @Provides
    @Singleton
    fun provideGetExerciseUseCase(repository: ExerciseRepository): GetExerciseUseCase =
        GetExerciseUseCase(repository)

    @Provides
    @Singleton
    fun provideValidateExerciseUseCase(repository: ExerciseRepository): ValidateExerciseUseCase =
        ValidateExerciseUseCase(repository)

    @Provides
    @Singleton
    fun provideGetAllQuestionsUseCase(repository: QuestionsRepository): GetAllQuestionsUseCase =
        GetAllQuestionsUseCase(repository)
}
