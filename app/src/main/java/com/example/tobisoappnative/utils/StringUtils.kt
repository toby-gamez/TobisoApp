package com.example.tobisoappnative.utils

import java.text.Normalizer

// Normalize text for comparisons/search: remove diacritics and lowercase
fun normalizeText(input: String): String {
    val normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
    return normalized.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "").lowercase()
}
