package com.paperscreen

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent

/**
 * The e-ink page turn: when a new screen (activity) comes up, the display flashes black, then
 * white, then shows it, the way an e-ink panel clears itself on a full refresh.
 *
 * It only listens for one event type (window state changes), and only while the flash is on.
 * The flash is a plain coloured window that exists just for its few hundred milliseconds;
 * there's no capture and no per-pixel work. Paper mode's greyscale and warmth apply to it like
 * to everything else, so its black and white are the paper's ink and page.
 */
class PageTurnService : AccessibilityService() {

    private val settings by lazy { PaperSettings(this) }
    private val main = Handler(Looper.getMainLooper())
    private val windowManager by lazy { getSystemService(WindowManager::class.java) }

    private var flash: View? = null
    private var lastScreen: String? = null
    private var lastFlashAt = 0L

    /** Whether each class name seen is an activity; asked of the package manager once each. */
    private val isActivity = HashMap<String, Boolean>()

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == PaperSettings.KEY_ENABLED || key == PaperSettings.KEY_PAGE_FLASH) updateEvents()
    }

    override fun onServiceConnected() {
        settings.prefs.registerOnSharedPreferenceChangeListener(prefsListener)
        updateEvents()
    }

    /** No events at all while there's nothing to flash for. */
    private fun updateEvents() {
        val info = serviceInfo ?: return
        val wanted = if (settings.enabled && settings.pageFlash) AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED else 0
        if (info.eventTypes == wanted) return
        info.eventTypes = wanted
        serviceInfo = info
        if (wanted == 0) lastScreen = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        val cls = event.className?.toString() ?: return
        // Dialogs, menus, toasts and the keyboard also change window state; only a new screen
        // is a page turn. The notification shade and our own windows never are.
        if (pkg == packageName || pkg == SYSTEM_UI || !isActivity(pkg, cls)) return
        val screen = "$pkg/$cls"
        if (screen == lastScreen) return
        val first = lastScreen == null
        lastScreen = screen
        val now = SystemClock.uptimeMillis()
        if (first || now - lastFlashAt < MIN_GAP_MS) return
        lastFlashAt = now
        showFlash()
    }

    private fun isActivity(pkg: String, cls: String): Boolean = isActivity.getOrPut("$pkg/$cls") {
        try {
            @Suppress("DEPRECATION") // The flags overload is Android 13+.
            packageManager.getActivityInfo(ComponentName(pkg, cls), 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun showFlash() {
        if (flash != null) return
        val view = View(this).apply {
            setBackgroundColor(Color.BLACK)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        }
        val bounds = windowManager.maximumWindowMetrics.bounds
        val params = WindowManager.LayoutParams(
            bounds.width(),
            bounds.height(),
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.OPAQUE,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            fitInsetsTypes = 0
            title = "PaperScreen page turn"
        }
        try {
            windowManager.addView(view, params)
        } catch (e: RuntimeException) {
            return
        }
        flash = view
        val total = settings.flashMs.toLong()
        main.postDelayed({ view.setBackgroundColor(Color.WHITE) }, total / 2)
        main.postDelayed(::hideFlash, total)
    }

    private fun hideFlash() {
        val view = flash ?: return
        flash = null
        main.removeCallbacksAndMessages(null)
        runCatching { windowManager.removeViewImmediate(view) }
    }

    override fun onInterrupt() = hideFlash()

    override fun onDestroy() {
        settings.prefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
        hideFlash()
        super.onDestroy()
    }

    companion object {
        private const val SYSTEM_UI = "com.android.systemui"

        /** Screens that follow each other faster than this (a splash, then the app) flash once. */
        private const val MIN_GAP_MS = 400L
    }
}
