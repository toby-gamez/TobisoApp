package com.tobiso.tobisoappnative.utils

/**
 * Applies the web's manual post order: only when the category is [sorted] (it has a sort label) are items ordered by
 * their position, with unplaced ones last; otherwise the server order is kept.
 */
fun <T> List<T>.orderedByManualOrder(sorted: Boolean, position: (T) -> Int?): List<T> =
    if (sorted) sortedBy { position(it) ?: Int.MAX_VALUE } else this
