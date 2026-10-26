package com.paperscreen

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.SharedPreferences
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

/**
 * Blocks apps and short-video players ([FocusRules]).
 *
 * Kept light: the system only delivers events from the apps it watches (the blocked ones and
 * the short-video apps), coalesced to at most one per type every 300 ms, and the window
 * content is only looked at inside YouTube and Instagram, by view id. PaperScreen turns the
 * whole service off while nothing is blocked.
 */
class FocusService : AccessibilityService() {

    private val settings by lazy { PaperSettings(this) }
    private var lastShortsClose = 0L
    private var lastPause = 0L

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == PaperSettings.KEY_BLOCKED_APPS || key == PaperSettings.KEY_BLOCK_SHORTS) updateWatched()
    }

    override fun onServiceConnected() {
        settings.prefs.registerOnSharedPreferenceChangeListener(prefsListener)
        updateWatched()
    }

    /** Events from the watched apps only. */
    private fun updateWatched() {
        val info = serviceInfo ?: return
        val watched = FocusRules.watchedPackages(settings.blockedApps, settings.blockShorts)
        // An empty list would mean every app; with nothing watched, PaperScreen turns us off.
        info.packageNames = watched.ifEmpty { setOf(packageName) }.toTypedArray()
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString() ?: return
        val action = FocusRules.actionFor(
            pkg,
            settings.blockedApps,
            settings.blockShorts,
            Unlocks.until(pkg),
            SystemClock.elapsedRealtime(),
        )
        when (action) {
            FocusRules.Action.PAUSE_SCREEN ->
                // A content change can come from an app that isn't in front; only act on it if it is.
                if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
                    rootInActiveWindow?.packageName?.toString() == pkg
                ) {
                    showPause(pkg)
                }
            FocusRules.Action.CHECK_FOR_SHORTS -> closeShortsIfOpen(pkg)
            FocusRules.Action.NONE -> Unit
        }
    }

    /**
     * Brings up the pause screen. Content changes count too, so a "use it anyway" that runs out
     * while the app is open ends it, at the app's next change.
     */
    private fun showPause(pkg: String) {
        val now = SystemClock.uptimeMillis()
        if (now - lastPause < PAUSE_COOLDOWN_MS) return
        lastPause = now
        startActivity(
            Intent(this, PauseActivity::class.java)
                .putExtra(PauseActivity.EXTRA_PACKAGE, pkg)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION),
        )
    }

    /** If a full-screen short-video player is showing, backs out of it. */
    private fun closeShortsIfOpen(pkg: String) {
        val now = SystemClock.uptimeMillis()
        // Give Back a moment to take effect before looking again.
        if (now - lastShortsClose < SHORTS_COOLDOWN_MS) return
        val root = rootInActiveWindow ?: return
        if (root.packageName?.toString() != pkg) return
        val ids = FocusRules.SHORT_VIDEO_VIEWS[pkg] ?: return
        val open = ids.any { id -> root.findAccessibilityNodeInfosByViewId(id).any { it.isVisibleToUser } }
        if (!open) return
        lastShortsClose = now
        performGlobalAction(GLOBAL_ACTION_BACK)
        Toast.makeText(this, R.string.shorts_blocked, Toast.LENGTH_SHORT).show()
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        settings.prefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
        super.onDestroy()
    }

    companion object {
        private const val SHORTS_COOLDOWN_MS = 1000L
        private const val PAUSE_COOLDOWN_MS = 1500L
    }
}
