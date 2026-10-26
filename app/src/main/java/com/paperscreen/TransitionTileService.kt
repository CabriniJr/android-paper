package com.paperscreen

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/**
 * Quick Settings tile that turns e-ink transitions on and off, so the effect can be switched
 * from the notification shade without opening the settings. The running service picks the
 * change up through its preferences listener.
 */
class TransitionTileService : TileService() {

    private val settings by lazy { PaperSettings(this) }

    override fun onStartListening() {
        render()
    }

    override fun onClick() {
        settings.transitionFlash = !settings.transitionFlash
        render()
    }

    private fun render() {
        val tile = qsTile ?: return
        tile.state = if (settings.transitionFlash) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
    }
}
