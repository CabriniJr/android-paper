package com.paperscreen

import android.content.Context
import android.content.SharedPreferences

/** The user's options, shared by the settings screen, the tile and the page-turn service. */
class PaperSettings(context: Context) {

    val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Paper mode on or off. */
    var enabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    /** 0 keeps neutral grey (Night Light untouched); 1–100 tints from barely warm to amber. */
    var warmth: Int
        get() = prefs.getInt(KEY_WARMTH, DEFAULT_WARMTH).coerceIn(0, 100)
        set(value) = prefs.edit().putInt(KEY_WARMTH, value.coerceIn(0, 100)).apply()

    var dim: Boolean
        get() = prefs.getBoolean(KEY_DIM, false)
        set(value) = prefs.edit().putBoolean(KEY_DIM, value).apply()

    var dimLevel: Int
        get() = prefs.getInt(KEY_DIM_LEVEL, DEFAULT_DIM_LEVEL).coerceIn(MIN_DIM_LEVEL, MAX_DIM_LEVEL)
        set(value) = prefs.edit().putInt(KEY_DIM_LEVEL, value.coerceIn(MIN_DIM_LEVEL, MAX_DIM_LEVEL)).apply()

    var crispText: Boolean
        get() = prefs.getBoolean(KEY_CRISP_TEXT, false)
        set(value) = prefs.edit().putBoolean(KEY_CRISP_TEXT, value).apply()

    var noAnimations: Boolean
        get() = prefs.getBoolean(KEY_NO_ANIMATIONS, true)
        set(value) = prefs.edit().putBoolean(KEY_NO_ANIMATIONS, value).apply()

    /** Black-then-white flash when the screen changes to another app or page. */
    var pageFlash: Boolean
        get() = prefs.getBoolean(KEY_PAGE_FLASH, false)
        set(value) = prefs.edit().putBoolean(KEY_PAGE_FLASH, value).apply()

    var flashMs: Int
        get() = prefs.getInt(KEY_FLASH_MS, DEFAULT_FLASH_MS).coerceIn(MIN_FLASH_MS, MAX_FLASH_MS)
        set(value) = prefs.edit().putInt(KEY_FLASH_MS, value.coerceIn(MIN_FLASH_MS, MAX_FLASH_MS)).apply()

    /** Close YouTube Shorts and Instagram Reels, and block short-video-only apps. */
    var blockShorts: Boolean
        get() = prefs.getBoolean(KEY_BLOCK_SHORTS, false)
        set(value) = prefs.edit().putBoolean(KEY_BLOCK_SHORTS, value).apply()

    /** Package names of apps that get the pause screen instead of opening. */
    var blockedApps: Set<String>
        get() = prefs.getStringSet(KEY_BLOCKED_APPS, null)?.toSet() ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_BLOCKED_APPS, value).apply()

    /** How long "use it anyway" lets a blocked app through. */
    var unlockMinutes: Int
        get() = prefs.getInt(KEY_UNLOCK_MINUTES, DEFAULT_UNLOCK_MINUTES).coerceIn(MIN_UNLOCK_MINUTES, MAX_UNLOCK_MINUTES)
        set(value) = prefs.edit().putInt(KEY_UNLOCK_MINUTES, value.coerceIn(MIN_UNLOCK_MINUTES, MAX_UNLOCK_MINUTES)).apply()

    /**
     * The original system values paper mode took over, as [SystemTweaks.encode]d. Written
     * synchronously: losing them would leave the phone greyscale with nothing to restore.
     */
    var originals: String?
        get() = prefs.getString(KEY_ORIGINALS, null)
        set(value) {
            prefs.edit().putString(KEY_ORIGINALS, value).commit()
        }

    companion object {
        private const val PREFS_NAME = "paper_screen"

        const val KEY_ENABLED = "enabled"
        const val KEY_WARMTH = "warmth"
        const val KEY_DIM = "dim"
        const val KEY_DIM_LEVEL = "dim_level"
        const val KEY_CRISP_TEXT = "crisp_text"
        const val KEY_NO_ANIMATIONS = "no_animations"
        const val KEY_PAGE_FLASH = "page_flash"
        const val KEY_FLASH_MS = "flash_ms"
        private const val KEY_ORIGINALS = "originals"
        const val KEY_BLOCK_SHORTS = "block_shorts"
        const val KEY_BLOCKED_APPS = "blocked_apps"
        const val KEY_UNLOCK_MINUTES = "unlock_minutes"

        const val DEFAULT_WARMTH = 40

        const val MIN_DIM_LEVEL = 10
        const val MAX_DIM_LEVEL = 80
        const val DEFAULT_DIM_LEVEL = 30

        const val MIN_UNLOCK_MINUTES = 1
        const val MAX_UNLOCK_MINUTES = 15
        const val DEFAULT_UNLOCK_MINUTES = 5

        const val MIN_FLASH_MS = 100
        const val MAX_FLASH_MS = 600
        const val FLASH_STEP_MS = 25
        const val DEFAULT_FLASH_MS = 250
    }
}
