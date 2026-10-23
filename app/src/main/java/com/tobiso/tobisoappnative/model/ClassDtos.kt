package com.tobiso.tobisoappnative.model

import kotlinx.serialization.Serializable

@Serializable
data class ClassGroup(val id: Int, val name: String)

/** `GET classes`. Approved/pending counts are zeroed for students and ignored here. */
@Serializable
data class SchoolClass(
    val id: Int,
    val name: String,
    val gradeId: Int? = null,
    val gradeName: String? = null,
    val groups: List<ClassGroup> = emptyList()
)

enum class ClassMembershipStatus {
    Pending, Approved, Rejected;

    companion object {
        fun parse(raw: String?): ClassMembershipStatus? =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }
                ?: raw?.toIntOrNull()?.let { entries.getOrNull(it) }
    }
}

/** `GET classes/mine` (204 when the student hasn't asked to join a class). */
@Serializable
data class MyClass(
    val classId: Int,
    val className: String,
    @Serializable(with = LenientStringSerializer::class) val status: String,
    val groups: List<ClassGroup> = emptyList(),
    val availableGroups: List<ClassGroup> = emptyList(),
    val groupChangePending: Boolean = false,
    val pendingGroup: ClassGroup? = null
) {
    val membershipStatus: ClassMembershipStatus? get() = ClassMembershipStatus.parse(status)
}

@Serializable
data class JoinClassRequest(val classId: Int, val groupIds: List<Int>? = null)

@Serializable
data class UpdateMyGroupsRequest(val groupIds: List<Int>)

/** Body for creating/updating a student's own event. [scope] may be Personal, Class or Group — never Global. */
@Serializable
data class StudentEventRequest(
    val title: String,
    val description: String? = null,
    val startDate: String,
    val endDate: String? = null,
    val type: String = "Test",
    val scope: String = "Personal",
    val groupId: Int? = null,
    val themes: List<EventTheme> = emptyList()
)
