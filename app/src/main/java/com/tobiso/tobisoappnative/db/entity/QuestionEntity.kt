package com.tobiso.tobisoappnative.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.Gson
import com.tobiso.tobisoappnative.model.Answer
import com.tobiso.tobisoappnative.model.Explanation
import com.tobiso.tobisoappnative.model.Question

private val gson = Gson()

@Entity(
    tableName = "questions",
    indices = [Index(value = ["postId"])]
)
data class QuestionEntity(
    @PrimaryKey val id: Int,
    val questionText: String,
    val postId: Int,
    val answersJson: String,         // Gson-serialized List<Answer>
    val explanationsJson: String     // Gson-serialized List<Explanation>
)

fun QuestionEntity.toDomain(): Question = Question(
    id = id,
    questionText = questionText,
    postId = postId,
    answers = gson.fromJson(answersJson, Array<Answer>::class.java)?.toList() ?: emptyList(),
    explanations = gson.fromJson(explanationsJson, Array<Explanation>::class.java)?.toList() ?: emptyList()
)

fun Question.toEntity(): QuestionEntity = QuestionEntity(
    id = id,
    questionText = questionText,
    postId = postId,
    answersJson = gson.toJson(answers),
    explanationsJson = gson.toJson(explanations)
)
