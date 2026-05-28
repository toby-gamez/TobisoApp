package com.tobiso.tobisoappnative

import android.content.Context
import org.json.JSONObject

data class QuestionStats(val correct: Int, val total: Int) {
    val percentage: Float get() = if (total == 0) 1f else correct.toFloat() / total
}

class QuestionProgressManager private constructor(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun recordResults(results: Map<Int, Boolean>) {
        if (results.isEmpty()) return
        val stats = loadRaw()
        results.forEach { (questionId, isCorrect) ->
            val key = questionId.toString()
            val current = stats.optJSONArray(key)
            val correct = (current?.optInt(0) ?: 0) + if (isCorrect) 1 else 0
            val total = (current?.optInt(1) ?: 0) + 1
            stats.put(key, org.json.JSONArray().apply { put(correct); put(total) })
        }
        prefs.edit().putString(KEY_STATS, stats.toString()).apply()
    }

    fun getStats(): Map<Int, QuestionStats> {
        val json = loadRaw()
        val result = mutableMapOf<Int, QuestionStats>()
        json.keys().forEach { key ->
            val arr = json.optJSONArray(key) ?: return@forEach
            result[key.toIntOrNull() ?: return@forEach] = QuestionStats(arr.optInt(0), arr.optInt(1))
        }
        return result
    }

    fun getProgressForQuestions(questionIds: Set<Int>): Float {
        if (questionIds.isEmpty()) return 0f
        val stats = getStats()
        val attempted = questionIds.mapNotNull { stats[it] }
        if (attempted.isEmpty()) return -1f
        return attempted.sumOf { it.correct }.toFloat() / attempted.sumOf { it.total }.coerceAtLeast(1)
    }

    fun hasAnyAttemptForQuestions(questionIds: Set<Int>): Boolean {
        val stats = getStats()
        return questionIds.any { stats.containsKey(it) }
    }

    fun resetAll() {
        prefs.edit().remove(KEY_STATS).apply()
    }

    private fun loadRaw(): JSONObject {
        val raw = prefs.getString(KEY_STATS, null) ?: return JSONObject()
        return runCatching { JSONObject(raw) }.getOrDefault(JSONObject())
    }

    companion object {
        private const val PREFS_NAME = "question_progress_prefs"
        private const val KEY_STATS = "question_stats_json"

        lateinit var instance: QuestionProgressManager
            private set

        fun initialize(context: Context) {
            instance = QuestionProgressManager(context.applicationContext)
        }
    }
}
