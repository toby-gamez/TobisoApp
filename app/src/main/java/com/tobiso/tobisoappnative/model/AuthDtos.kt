package com.tobiso.tobisoappnative.model

import kotlinx.serialization.Serializable

/** `username` carries the account's email (web `LoginRequest(Username, Password)`). */
@Serializable
data class LoginRequest(val username: String, val password: String)

@Serializable
data class StudentLoginResponse(val token: String, val displayName: String, val credits: Int = 0)

@Serializable
data class RegisterRequest(
    val email: String,
    val displayName: String?,
    val password: String,
    val acceptedTerms: Boolean
)

@Serializable
data class StudentProfile(
    val displayName: String,
    val email: String = "",
    val credits: Int = 0,
    val role: String = "student",
    val preferredGradeId: Int? = null
)

/**
 * `GET user/legal-notice` / body of `POST user/legal-notice/dismiss`. [changedAt] is the server's timestamp kept as the
 * raw string so that dismissing echoes exactly what was received; null means nothing is pending.
 */
@Serializable
data class LegalNoticeDto(val changedAt: String? = null)

/** `GET practice/stats`: the account's answered / mastered counts across all questions. */
@Serializable
data class PracticeStats(
    val totalQuestions: Int = 0,
    val attemptedCount: Int = 0,
    val masteredCount: Int = 0,
    val accuracyPercent: Double = 0.0
)

/** Body of `POST practice/attempt`: one flashcard answer ("Umím" = [correct]). */
@Serializable
data class PracticeAttemptRequest(val questionId: Int, val correct: Boolean)

/** Body of `DELETE user`; the server refuses the deletion unless [confirm] is true. */
@Serializable
data class DeleteAccountRequest(val confirm: Boolean)

/** Body of `PUT user/preferred-grade`; a null [gradeId] clears the account's grade. */
@Serializable
data class PreferredGradeRequest(val gradeId: Int?)

@Serializable
data class ReadProgressRequest(val postId: Int, val scrollPercent: Int)

@Serializable
data class ContinueReadingDto(
    val postId: Int,
    val title: String,
    val scrollPercent: Int,
    val categoryPath: String? = null,
    val filePath: String? = null
)

/** Body of `POST auth/google-token`; [acceptedTerms] is only needed when the Google account has no Tobiso account yet. */
@Serializable
data class GoogleTokenRequest(val idToken: String, val acceptedTerms: Boolean = false)

/** `GET user/stats`: reading streak, article count and per-subject badges. */
@Serializable
data class UserStats(
    val streakDays: Int = 0,
    val totalRead: Int = 0,
    val perSubject: List<SubjectRead> = emptyList(),
    val badges: List<SubjectBadge> = emptyList(),
    val maxStreak: Int = 0,
    val freezes: Int = 0,
    val todayActive: Boolean = false,
    /** Active days (incl. freeze days) of the last ~400 days, `yyyy-MM-dd`. */
    val days: List<String> = emptyList(),
    val freezeDays: List<String> = emptyList()
)

@Serializable
data class SubjectRead(val subjectName: String = "", val count: Int = 0)

@Serializable
data class SubjectBadge(val subjectName: String = "", @Serializable(with = LenientStringSerializer::class) val level: String = "")

/** `POST auth/daily-bonus` */
@Serializable
data class DailyBonusResponse(val credits: Int = 0)
