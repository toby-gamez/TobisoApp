package com.tobiso.tobisoappnative.repository

/** What a bookmark sync has to do against the server, and where local favorites end up. */
data class BookmarkSyncPlan(
    val push: Set<Int>,
    val delete: Set<Int>,
    val finalIds: Set<Int>
)

/**
 * Reconciles local favorites with the server's bookmarks.
 *
 * - First sync for an account: union — every local favorite is pushed, nothing local is lost.
 * - Afterwards the server is authoritative, except for changes still waiting in the outbox
 *   ([pendingAdd] / [pendingRemove]) which are pushed first so offline edits survive.
 */
fun planBookmarkSync(
    local: Set<Int>,
    server: Set<Int>,
    pendingAdd: Set<Int>,
    pendingRemove: Set<Int>,
    firstSync: Boolean
): BookmarkSyncPlan {
    val push = (if (firstSync) local else pendingAdd.intersect(local)) - server
    val delete = pendingRemove.intersect(server) - push
    return BookmarkSyncPlan(push = push, delete = delete, finalIds = (server - delete) + push)
}
