package com.tobiso.tobisoappnative.di

import android.content.Context
import com.tobiso.tobisoappnative.model.ApiService
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Gives classes Hilt doesn't construct (WorkManager workers, composables outside a ViewModel) the same
 * [ApiService] the repositories use, without touching the `ApiClient` singleton directly.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ApiServiceEntryPoint {
    fun apiService(): ApiService
}

fun Context.injectedApiService(): ApiService =
    EntryPointAccessors.fromApplication(applicationContext, ApiServiceEntryPoint::class.java).apiService()
