package com.paperscreen

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.widget.Button
import android.widget.TextView

/**
 * Shown instead of a blocked app: a moment to reconsider. "Back to home" is always there;
 * "use it anyway" only after a countdown, and only for a few minutes.
 */
class PauseActivity : Activity() {

    private val main = Handler(Looper.getMainLooper())
    private lateinit var settings: PaperSettings
    private lateinit var useAnyway: Button
    private var pkg = ""
    private var readyAt = 0L

    private val tick = object : Runnable {
        override fun run() {
            val left = ((readyAt - SystemClock.elapsedRealtime() + 999) / 1000).toInt()
            if (left > 0) {
                useAnyway.isEnabled = false
                useAnyway.text = getString(R.string.pause_wait, left)
                main.postDelayed(this, 250)
            } else {
                useAnyway.isEnabled = true
                useAnyway.text = resources.getQuantityString(
                    R.plurals.pause_use_anyway, settings.unlockMinutes, settings.unlockMinutes,
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pause)
        settings = PaperSettings(this)
        useAnyway = findViewById(R.id.use_anyway)
        findViewById<Button>(R.id.go_home).setOnClickListener { goHome() }
        useAnyway.setOnClickListener {
            Unlocks.unlock(pkg, SystemClock.elapsedRealtime() + settings.unlockMinutes * 60_000L)
            finish()
        }
        show(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        show(intent)
    }

    /** Each time it comes up, the countdown starts over. */
    private fun show(intent: Intent) {
        pkg = intent.getStringExtra(EXTRA_PACKAGE).orEmpty()
        val label = try {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0))
        } catch (e: PackageManager.NameNotFoundException) {
            pkg
        }
        findViewById<TextView>(R.id.pause_title).text = getString(R.string.pause_title, label)
        readyAt = SystemClock.elapsedRealtime() + COUNTDOWN_MS
        main.removeCallbacks(tick)
        tick.run()
    }

    private fun goHome() {
        startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }

    @Deprecated("Back always goes home rather than to the blocked app.")
    @Suppress("DEPRECATION")
    override fun onBackPressed() = goHome()

    override fun onDestroy() {
        main.removeCallbacks(tick)
        super.onDestroy()
    }

    companion object {
        const val EXTRA_PACKAGE = "package"
        private const val COUNTDOWN_MS = 10_000L
    }
}
