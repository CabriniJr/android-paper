package com.paperscreen

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast

/**
 * No UI: toggles paper mode and closes. It's what the launcher shortcut (long-press the app
 * icon, or pin it to the home screen) and anything else that can start an app, like a Moto
 * gesture or a routine, run.
 */
class ToggleActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mode = PaperMode(this)
        if (mode.toggle()) {
            Toast.makeText(this, if (mode.isOn) R.string.toast_on else R.string.toast_off, Toast.LENGTH_SHORT).show()
        } else {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        finish()
    }
}
