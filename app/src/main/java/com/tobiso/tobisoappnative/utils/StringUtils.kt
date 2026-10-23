package com.tobiso.tobisoappnative.utils

import java.text.Normalizer

// Normalize text for comparisons/search: remove diacritics and lowercase
fun normalizeText(input: String): String {
    val normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
    return normalized.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "").lowercase()
}

/** Czech plural: 1 → one, 2–4 → few, otherwise many (e.g. 1 hlas / 3 hlasy / 5 hlasů). */
fun pluralCs(n: Int, one: String, few: String, many: String): String = when (n) {
    1 -> one
    in 2..4 -> few
    else -> many
}
