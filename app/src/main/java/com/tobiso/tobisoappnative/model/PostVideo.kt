package com.tobiso.tobisoappnative.model

import kotlinx.serialization.Serializable

/** "Video k článku" – the featured YouTube video of a post. */
@Serializable
data class PostVideo(
    val youtubeUrl: String = "",
    val timestamp: Int = 0,
    val label: String = ""
) {
    /** 11-char YouTube id, or null when [youtubeUrl] is not a YouTube link. */
    val youtubeId: String?
        get() = YOUTUBE_ID_REGEX.find(youtubeUrl)?.groupValues?.get(1)

    private companion object {
        val YOUTUBE_ID_REGEX =
            Regex("""(?:youtube\.com/(?:watch\?(?:.*&)?v=|embed/|shorts/|live/)|youtu\.be/)([A-Za-z0-9_-]{11})""")
    }
}
