package com.paperscreen

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.provider.Settings
import android.service.quicksettings.TileService
import android.util.Log

/**
 * Turns paper mode on and off by writing Android's own display settings ([SystemTweaks]).
 * Nothing keeps running afterwards: the system holds the look, even across reboots.
 *
 * Writing those settings needs WRITE_SECURE_SETTINGS, which only adb can grant, once:
 * `adb shell pm grant com.paperscreen android.permission.WRITE_SECURE_SETTINGS`.
 */
class PaperMode(context: Context) {

    private val app = context.applicationContext
    val settings = PaperSettings(app)

    private val store = object : SettingsStore {
        private val resolver = app.contentResolver

        override fun read(key: SysKey): String? = when (key.table) {
            Table.SECURE -> Settings.Secure.getString(resolver, key.name)
            Table.GLOBAL -> Settings.Global.getString(resolver, key.name)
        }

        override fun write(key: SysKey, value: String?) {
            when (key.table) {
                Table.SECURE -> Settings.Secure.putString(resolver, key.name, value)
                Table.GLOBAL -> Settings.Global.putString(resolver, key.name, value)
            }
        }
    }

    fun hasPermission(): Boolean =
        app.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED

    val isOn: Boolean get() = settings.enabled

    /** Turns paper mode on or off. False if it couldn't (no permission); nothing changes then. */
    fun setOn(on: Boolean): Boolean {
        if (!hasPermission()) return false
        settings.enabled = on
        return sync()
    }

    fun toggle(): Boolean = setOn(!isOn)

    /** Brings the system settings in line with the options; call after changing one. */
    fun sync(): Boolean {
        if (!hasPermission()) return false
        val want = if (settings.enabled) SystemTweaks.desired(look()) else emptyMap()
        return try {
            val originals = SystemTweaks.apply(store, want, SystemTweaks.decode(settings.originals))
            settings.originals = SystemTweaks.encode(originals)
            syncPageTurnService()
            true
        } catch (e: SecurityException) {
            Log.w(TAG, "couldn't write the display settings", e)
            false
        } finally {
            TileService.requestListeningState(app, ComponentName(app, PaperTileService::class.java))
        }
    }

    private fun look() = PaperLook(
        warmthKelvin = settings.warmth.takeIf { it > 0 }?.let { warmth ->
            val (warmest, coolest) = nightLightRange()
            SystemTweaks.kelvinFor(warmth, warmest, coolest)
        },
        dimLevel = settings.dimLevel.takeIf { settings.dim },
        crispText = settings.crispText,
        noAnimations = settings.noAnimations,
    )

    /** The device's Night Light range (warmest, coolest), as its own slider offers it. */
    private fun nightLightRange(): Pair<Int, Int> {
        val res = Resources.getSystem()
        fun config(name: String, fallback: Int): Int {
            val id = res.getIdentifier(name, "integer", "android")
            return if (id != 0) res.getInteger(id) else fallback
        }
        return config("config_nightDisplayColorTemperatureMin", 2596) to
            config("config_nightDisplayColorTemperatureMax", 4082)
    }

    /**
     * The page-turn flash needs PaperScreen's accessibility service. With the permission, it's
     * switched on and off with the option, so there's no trip to Accessibility settings, and
     * nothing at all runs while the flash isn't wanted.
     */
    private fun syncPageTurnService() {
        val resolver = app.contentResolver
        val ours = ComponentName(app, PageTurnService::class.java).flattenToString()
        val current = Settings.Secure.getString(resolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            .orEmpty().split(':').filter { it.isNotEmpty() }
        val wanted = settings.enabled && settings.pageFlash
        val updated = if (wanted) (current + ours).distinct() else current - ours
        if (updated == current) return
        Settings.Secure.putString(resolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, updated.joinToString(":"))
        if (updated.isNotEmpty()) Settings.Secure.putString(resolver, Settings.Secure.ACCESSIBILITY_ENABLED, "1")
    }

    fun isPageTurnServiceOn(): Boolean {
        val ours = ComponentName(app, PageTurnService::class.java).flattenToString()
        return Settings.Secure.getString(app.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            .orEmpty().split(':').contains(ours)
    }

    companion object {
        const val TAG = "PaperScreen"
        const val GRANT_COMMAND = "adb shell pm grant com.paperscreen android.permission.WRITE_SECURE_SETTINGS"
    }
}
