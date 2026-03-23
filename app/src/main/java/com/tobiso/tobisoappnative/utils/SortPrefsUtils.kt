package com.tobiso.tobisoappnative.utils

import android.content.Context

enum class SortMode {
    SUBJECTS,
    NEWEST
}

private const val HOME_PREFS = "home_prefs"
private const val KEY_SORT_MODE = "home_sort_mode"

fun loadSortMode(context: Context): SortMode {
    val prefs = context.getSharedPreferences(HOME_PREFS, Context.MODE_PRIVATE)
    val saved = prefs.getString(KEY_SORT_MODE, null)
    return when (saved) {
        SortMode.NEWEST.name -> SortMode.NEWEST
        SortMode.SUBJECTS.name -> SortMode.SUBJECTS
        else -> SortMode.SUBJECTS
    }
}

fun saveSortMode(context: Context, mode: SortMode) {
    val prefs = context.getSharedPreferences(HOME_PREFS, Context.MODE_PRIVATE)
    prefs.edit().putString(KEY_SORT_MODE, mode.name).apply()
}
