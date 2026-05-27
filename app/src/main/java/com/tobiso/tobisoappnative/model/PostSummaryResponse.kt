package com.tobiso.tobisoappnative.model

import kotlinx.serialization.Serializable

@Serializable
data class PostSummaryResponse(
    val id: Int,
    val title: String,
    val categoryId: Int? = null,
    val filePath: String,
    val lastEdit: String? = null,
    val lastFix: String? = null,
    val availableGradeNames: List<String> = emptyList()
)
