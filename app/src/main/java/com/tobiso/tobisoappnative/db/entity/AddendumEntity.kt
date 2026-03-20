package com.tobiso.tobisoappnative.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.tobiso.tobisoappnative.model.Addendum

@Entity(tableName = "addendums")
data class AddendumEntity(
    @PrimaryKey val id: Int,
    val name: String?,
    val content: String?,
    val updatedAt: String? = null
)

fun AddendumEntity.toDomain(): Addendum = Addendum(
    id = id, name = name, content = content, updatedAt = updatedAt
)

fun Addendum.toEntity(): AddendumEntity = AddendumEntity(
    id = id, name = name, content = content, updatedAt = updatedAt
)
