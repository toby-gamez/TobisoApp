package com.tobiso.tobisoappnative.utils

import android.content.Context

private const val GRADE_PREFS = "grade_prefs"
private const val KEY_GRADE_ID = "preferred_grade_id"

fun loadGradeId(context: Context): Int? {
    val prefs = context.getSharedPreferences(GRADE_PREFS, Context.MODE_PRIVATE)
    val value = prefs.getInt(KEY_GRADE_ID, -1)
    return if (value == -1) null else value
}

fun saveGradeId(context: Context, gradeId: Int?) {
    val prefs = context.getSharedPreferences(GRADE_PREFS, Context.MODE_PRIVATE)
    if (gradeId == null) prefs.edit().remove(KEY_GRADE_ID).apply()
    else prefs.edit().putInt(KEY_GRADE_ID, gradeId).apply()
}
