package com.tobiso.tobisoappnative.di

import android.app.Application
import android.content.Context
import com.tobiso.tobisoappnative.db.dao.AddendumDao
import com.tobiso.tobisoappnative.db.dao.CategoryDao
import com.tobiso.tobisoappnative.db.dao.EventDao
import com.tobiso.tobisoappnative.db.dao.ExerciseDao
import com.tobiso.tobisoappnative.db.dao.ExerciseCategoryDao
import com.tobiso.tobisoappnative.db.dao.ExercisePostDao
import com.tobiso.tobisoappnative.db.dao.PostDao
import com.tobiso.tobisoappnative.db.dao.QuestionDao
import com.tobiso.tobisoappnative.db.dao.QuestionPostDao
import com.tobiso.tobisoappnative.db.dao.RelatedPostDao
import com.tobiso.tobisoappnative.domain.usecase.GetAllQuestionsUseCase
import com.tobiso.tobisoappnative.domain.usecase.GetExerciseUseCase
import com.tobiso.tobisoappnative.domain.usecase.ValidateExerciseUseCase
import com.tobiso.tobisoappnative.model.ApiClient
import com.tobiso.tobisoappnative.model.ApiService
import com.tobiso.tobisoappnative.model.OfflineDataManager
import com.tobiso.tobisoappnative.repository.AuthRepository
import com.tobiso.tobisoappnative.repository.AuthRepositoryImpl
import com.tobiso.tobisoappnative.repository.ExerciseRepository
import com.tobiso.tobisoappnative.repository.ExerciseRepositoryImpl
import com.tobiso.tobisoappnative.repository.FavoritesRepositoryImpl
import com.tobiso.tobisoappnative.repository.OfflineRepositoryImpl
import com.tobiso.tobisoappnative.repository.PostsRepository
import com.tobiso.tobisoappnative.repository.PostsRepositoryImpl
import com.tobiso.tobisoappnative.repository.QuestionsRepository
import com.tobiso.tobisoappnative.repository.QuestionsRepositoryImpl
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
    fun provideApiService(): ApiService = ApiClient.apiService

    @Provides
    @Singleton
    fun provideOfflineDataManager(
        @ApplicationContext context: Context,
        categoryDao: CategoryDao,
        postDao: PostDao,
        questionPostDao: QuestionPostDao,
        questionDao: QuestionDao,
        eventDao: EventDao,
        addendumDao: AddendumDao,
        relatedPostDao: RelatedPostDao,
        exerciseDao: ExerciseDao,
        exercisePostDao: ExercisePostDao,
        exerciseCategoryDao: ExerciseCategoryDao,
        db: com.tobiso.tobisoappnative.db.AppDatabase
    ): OfflineDataManager = OfflineDataManager(
        context, categoryDao, postDao, questionPostDao,
        questionDao, eventDao, addendumDao, relatedPostDao,
        exerciseDao, exercisePostDao, exerciseCategoryDao, db
    )

    @Provides
    @Singleton
    fun providePostsRepository(
        @ApplicationContext context: Context,
        offlineDataManager: OfflineDataManager,
        api: ApiService
    ): PostsRepository = PostsRepositoryImpl(context, offlineDataManager, api)

    @Provides
    @Singleton
    fun provideExerciseRepository(
        @ApplicationContext context: Context,
        offlineDataManager: OfflineDataManager,
        api: ApiService
    ): ExerciseRepository = ExerciseRepositoryImpl(context, offlineDataManager, api)

    @Provides
    @Singleton
    fun provideQuestionsRepository(
        @ApplicationContext context: Context,
        offlineDataManager: OfflineDataManager,
        api: ApiService
    ): QuestionsRepository = QuestionsRepositoryImpl(context, offlineDataManager, api)

    @Provides
    @Singleton
    fun provideOfflineRepository(
        @ApplicationContext context: Context,
        offlineDataManager: OfflineDataManager,
        api: ApiService
    ): OfflineRepositoryImpl = OfflineRepositoryImpl(context, offlineDataManager, api)

    @Provides
    @Singleton
    fun provideFavoritesRepository(application: Application, api: ApiService): FavoritesRepositoryImpl =
        FavoritesRepositoryImpl(application, api)

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

    @Provides
    @Singleton
    fun provideAuthRepository(api: ApiService): AuthRepository = AuthRepositoryImpl(api)
}
