package com.tobiso.tobisoappnative.data

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

/** Single shared DataStore instance for saved/favourite posts. */
val Context.savedPostsDataStore by preferencesDataStore(name = "saved_posts")
