package com.paperscreen

/**
 * What the focus service does about the app in front. Pure, so it's unit-tested.
 *
 * Two kinds of blocking:
 * - **Whole apps** the user picked (and, with short videos blocked, apps that are nothing but
 *   short videos): a pause screen comes up instead, which can let the app through for a few
 *   minutes after a countdown.
 * - **Short-video players inside apps** that are otherwise useful (YouTube Shorts, Instagram
 *   Reels): the player is closed with Back, the rest of the app keeps working. They're found
 *   by the view ids of their full-screen players, which the apps may rename in an update.
 */
object FocusRules {

    /** Apps that are only short videos: blocked as a whole when short videos are. */
    val SHORT_VIDEO_APPS = setOf(
        "com.zhiliaoapp.musically", // TikTok
        "com.ss.android.ugc.trill", // TikTok (some regions)
        "com.zhiliaoapp.musically.go", // TikTok Lite
        "com.kwai.video", // Kwai
    )

    /** View ids of the full-screen short-video players, per app. */
    val SHORT_VIDEO_VIEWS = mapOf(
        "com.google.android.youtube" to listOf(
            "com.google.android.youtube:id/reel_recycler",
            "com.google.android.youtube:id/reel_player_page_container",
            "com.google.android.youtube:id/reel_watch_player",
        ),
        "com.instagram.android" to listOf(
            "com.instagram.android:id/clips_viewer_view_pager",
            "com.instagram.android:id/clips_viewer_container",
            "com.instagram.android:id/root_clips_layout",
        ),
    )

    enum class Action { NONE, PAUSE_SCREEN, CHECK_FOR_SHORTS }

    fun actionFor(
        pkg: String,
        blockedApps: Set<String>,
        blockShorts: Boolean,
        unlockedUntil: Long?,
        now: Long,
    ): Action {
        val wholeApp = pkg in blockedApps || (blockShorts && pkg in SHORT_VIDEO_APPS)
        return when {
            wholeApp && (unlockedUntil == null || now >= unlockedUntil) -> Action.PAUSE_SCREEN
            wholeApp -> Action.NONE
            blockShorts && pkg in SHORT_VIDEO_VIEWS -> Action.CHECK_FOR_SHORTS
            else -> Action.NONE
        }
    }

    /** The packages the service needs events from; empty means it isn't needed at all. */
    fun watchedPackages(blockedApps: Set<String>, blockShorts: Boolean): Set<String> =
        if (blockShorts) blockedApps + SHORT_VIDEO_APPS + SHORT_VIDEO_VIEWS.keys else blockedApps
}
