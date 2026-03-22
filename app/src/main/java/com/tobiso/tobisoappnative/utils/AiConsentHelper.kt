package com.tobiso.tobisoappnative.utils

import android.content.Context

private const val PREFS_AI_CONSENT = "ai_consent"
private const val KEY_CONSENT_GIVEN = "consent_given"

fun hasAiConsent(context: Context): Boolean {
    val prefs = context.getSharedPreferences(PREFS_AI_CONSENT, Context.MODE_PRIVATE)
    return prefs.getBoolean(KEY_CONSENT_GIVEN, false)
}

fun saveAiConsent(context: Context) {
    val prefs = context.getSharedPreferences(PREFS_AI_CONSENT, Context.MODE_PRIVATE)
    prefs.edit().putBoolean(KEY_CONSENT_GIVEN, true).apply()
}
