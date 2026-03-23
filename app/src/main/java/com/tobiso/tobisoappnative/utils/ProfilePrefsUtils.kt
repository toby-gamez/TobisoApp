package com.tobiso.tobisoappnative.utils

import android.content.Context

private const val PREFS_PROFILE = "ProfilePrefs"
private const val KEY_PROFILE_NAME = "profile_name"
private const val KEY_PROFILE_IMAGE_URI = "profile_image_uri"
private const val DEFAULT_PROFILE_NAME = "Chytrá věc"

fun getProfileName(context: Context): String {
    val prefs = context.getSharedPreferences(PREFS_PROFILE, Context.MODE_PRIVATE)
    return prefs.getString(KEY_PROFILE_NAME, DEFAULT_PROFILE_NAME) ?: DEFAULT_PROFILE_NAME
}

fun saveProfileName(context: Context, name: String) {
    val prefs = context.getSharedPreferences(PREFS_PROFILE, Context.MODE_PRIVATE)
    prefs.edit().putString(KEY_PROFILE_NAME, name).apply()
}

fun getProfileImageUri(context: Context): String? {
    val prefs = context.getSharedPreferences(PREFS_PROFILE, Context.MODE_PRIVATE)
    return prefs.getString(KEY_PROFILE_IMAGE_URI, null)
}

fun saveProfileImageUri(context: Context, uri: String?) {
    val prefs = context.getSharedPreferences(PREFS_PROFILE, Context.MODE_PRIVATE)
    prefs.edit().putString(KEY_PROFILE_IMAGE_URI, uri).apply()
}
