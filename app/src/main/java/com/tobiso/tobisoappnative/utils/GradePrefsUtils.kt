package com.tobiso.tobisoappnative.utils

import android.content.Context
import com.tobiso.tobisoappnative.model.Grade
import com.tobiso.tobisoappnative.model.Post
import com.tobiso.tobisoappnative.model.PostVersion

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

fun List<PostVersion>.bestMatchForLevel(gradeLevel: Int): PostVersion? {
    val candidates = filter { (it.gradeLevel ?: 0) <= gradeLevel }
    return if (candidates.isNotEmpty()) candidates.maxByOrNull { it.gradeLevel ?: 0 }
    else maxByOrNull { it.gradeLevel ?: 0 }
}

fun Post.filterVersionForGrade(gradeId: Int, grades: List<Grade>): Post {
    val versions = versions?.takeIf { it.isNotEmpty() } ?: return this
    val level = grades.find { it.id == gradeId }?.level
        ?: return copy(versions = listOf(versions.first()))
    val best = versions.bestMatchForLevel(level) ?: return this
    return copy(versions = listOf(best))
}
