package com.tobiso.tobisoappnative.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tobiso.tobisoappnative.model.Answer
import com.tobiso.tobisoappnative.model.Explanation
import com.tobiso.tobisoappnative.model.Question
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

@Entity(
    tableName = "questions",
    indices = [Index(value = ["postId"])]
)
data class QuestionEntity(
    @PrimaryKey val id: Int,
    val questionText: String,
    val postId: Int,
    val answersJson: String,         // kotlinx.serialization-serialized List<Answer>
    val explanationsJson: String     // kotlinx.serialization-serialized List<Explanation>
)

fun QuestionEntity.toDomain(): Question = Question(
    id = id,
    questionText = questionText,
    postId = postId,
    answers = try { json.decodeFromString<List<Answer>>(answersJson) } catch (e: Exception) { emptyList() },
    explanations = try { json.decodeFromString<List<Explanation>>(explanationsJson) } catch (e: Exception) { emptyList() }
)

fun Question.toEntity(): QuestionEntity = QuestionEntity(
    id = id,
    questionText = questionText,
    postId = postId,
    answersJson = json.encodeToString(kotlinx.serialization.builtins.ListSerializer(Answer.serializer()), answers),
    explanationsJson = json.encodeToString(kotlinx.serialization.builtins.ListSerializer(Explanation.serializer()), explanations)
)
