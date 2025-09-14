package com.example.tobisoappnative.model

data class Question(
    val id: Int,
    val questionText: String, // Odpovídá JSON struktuře
    val postId: Int,
    val answers: List<Answer>
) {
    // Helper properties pro kompatibilitu s UI kódem
    val text: String get() = questionText
    val options: List<String> get() = answers.map { it.answerText }
    val correctAnswer: Int get() = answers.indexOfFirst { it.correct == 1 }
}