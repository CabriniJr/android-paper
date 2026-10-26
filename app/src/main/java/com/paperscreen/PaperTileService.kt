package com.paperscreen

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/**
 * Quick Settings tile: paper mode on and off from the notification shade. Long-pressing it
 * opens the settings. Until the one-time permission is granted, a tap opens the setup instead.
 */
class PaperTileService : TileService() {

    private val mode by lazy { PaperMode(this) }

    override fun onStartListening() = render()

    override fun onClick() {
        if (!mode.hasPermission()) {
            openSettings()
            return
        }
        mode.toggle()
        render()
    }

    private fun render() {
        val tile = qsTile ?: return
        val ready = mode.hasPermission()
        tile.state = if (ready && mode.isOn) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.subtitle = getString(
            when {
                !ready -> R.string.tile_setup
                mode.isOn -> R.string.tile_on
                else -> R.string.tile_off
            },
        )
        tile.updateTile()
    }

    private fun openSettings() {
        val intent = Intent(this, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE))
        } else {
            @Suppress("DEPRECATION", "StartActivityAndCollapseDeprecated")
            startActivityAndCollapse(intent)
        }
    }
}
