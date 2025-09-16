package com.example.tobisoappnative.model

data class Question(
    val id: Int,
    val questionText: String, // Odpovídá JSON struktuře
    val postId: Int,
    val answers: List<Answer>,
    val explanations: List<Explanation> = emptyList()
) {
    // Helper properties pro kompatibilitu s UI kódem
    val text: String get() = questionText
    val options: List<String> get() = answers.map { it.answerText }
    val correctAnswer: Int get() = answers.indexOfFirst { it.correct == 1 }
    
    // Helper property pro získání vysvětlení jako jednoho textu
    val explanation: String? get() = explanations.firstOrNull()?.text
}