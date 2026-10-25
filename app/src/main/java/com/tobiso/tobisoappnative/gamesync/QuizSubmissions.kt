package com.tobiso.tobisoappnative.gamesync

import com.tobiso.tobisoappnative.model.Question
import com.tobiso.tobisoappnative.model.QuizAnswerSubmission

/**
 * What the user answered, in the form the server grades (`user/points/earn`). [selected] and [multi] are indices
 * into [Question.answers]; free-text questions send [text].
 */
fun Question.toSubmission(selected: Int?, multi: Set<Int>?, text: String?): QuizAnswerSubmission = when {
    isTextQuestion -> QuizAnswerSubmission(id, text = text?.trim())
    isMultiSelectQuestion -> QuizAnswerSubmission(id, answerIds = multi.orEmpty().mapNotNull { answers.getOrNull(it)?.id })
    else -> QuizAnswerSubmission(id, answerIds = listOfNotNull(selected?.let { answers.getOrNull(it)?.id }))
}
