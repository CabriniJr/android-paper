package com.paperscreen

import android.app.Activity
import android.app.AlertDialog
import android.app.StatusBarManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

/** The only screen: the master switch, the one-time setup, and the options. */
class SettingsActivity : Activity() {

    private lateinit var mode: PaperMode
    private val settings get() = mode.settings

    private lateinit var masterSwitch: Switch
    private lateinit var status: TextView
    private lateinit var setup: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        mode = PaperMode(this)

        masterSwitch = findViewById(R.id.master_switch)
        status = findViewById(R.id.status)
        setup = findViewById(R.id.setup)

        masterSwitch.setOnCheckedChangeListener { _, checked ->
            if (checked != mode.isOn && !mode.setOn(checked)) {
                Toast.makeText(this, R.string.needs_setup, Toast.LENGTH_SHORT).show()
            }
            render()
        }

        findViewById<TextView>(R.id.grant_command).text = PaperMode.GRANT_COMMAND
        findViewById<Button>(R.id.copy_command).setOnClickListener {
            getSystemService(ClipboardManager::class.java)
                .setPrimaryClip(ClipData.newPlainText(getString(R.string.app_name), PaperMode.GRANT_COMMAND))
        }

        bindSlider(R.id.warmth_seek, R.id.warmth_value, 0, 100, 1, settings.warmth, { settings.warmth = it }) {
            if (it == 0) getString(R.string.warmth_off) else getString(R.string.percent, it)
        }
        bindOption(R.id.dim_switch, R.id.dim_options, settings.dim) { settings.dim = it }
        bindSlider(
            R.id.dim_seek, R.id.dim_value,
            PaperSettings.MIN_DIM_LEVEL, PaperSettings.MAX_DIM_LEVEL, 1,
            settings.dimLevel, { settings.dimLevel = it },
        ) { getString(R.string.percent, it) }
        bindOption(R.id.crisp_switch, null, settings.crispText) { settings.crispText = it }
        bindOption(R.id.animations_switch, null, settings.noAnimations) { settings.noAnimations = it }
        bindOption(R.id.flash_switch, R.id.flash_options, settings.pageFlash) { settings.pageFlash = it }
        bindSlider(
            R.id.flash_seek, R.id.flash_value,
            PaperSettings.MIN_FLASH_MS, PaperSettings.MAX_FLASH_MS, PaperSettings.FLASH_STEP_MS,
            settings.flashMs, { settings.flashMs = it },
        ) { getString(R.string.milliseconds, it) }

        bindOption(R.id.shorts_switch, null, settings.blockShorts) { settings.blockShorts = it }
        findViewById<Button>(R.id.choose_blocked).setOnClickListener { chooseBlockedApps() }
        bindSlider(
            R.id.unlock_seek, R.id.unlock_value,
            PaperSettings.MIN_UNLOCK_MINUTES, PaperSettings.MAX_UNLOCK_MINUTES, 1,
            settings.unlockMinutes, { settings.unlockMinutes = it },
        ) { resources.getQuantityString(R.plurals.minutes, it, it) }

        val addTile = findViewById<Button>(R.id.add_tile)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            addTile.setOnClickListener { requestTile() }
        } else {
            addTile.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        // The permission, the tile or a shortcut may have changed things meanwhile.
        render()
    }

    private fun render() {
        val ready = mode.hasPermission()
        setup.visibility = if (ready) View.GONE else View.VISIBLE
        masterSwitch.isEnabled = ready
        masterSwitch.isChecked = ready && mode.isOn
        status.setText(
            when {
                !ready -> R.string.status_setup
                mode.isOn -> R.string.status_on
                else -> R.string.status_off
            },
        )
        renderBlocked()
    }

    private fun renderBlocked() {
        val blocked = settings.blockedApps
        val names = AppCatalog.load(this).filter { it.pkg in blocked }.map { it.label }
        findViewById<TextView>(R.id.blocked_summary).text =
            if (names.isEmpty()) getString(R.string.no_blocked_apps) else names.joinToString(", ")
    }

    /** Pick the apps that get the pause screen. */
    private fun chooseBlockedApps() {
        val apps = AppCatalog.load(this).distinctBy { it.pkg }
        val chosen = settings.blockedApps.toMutableSet()
        val checked = apps.map { it.pkg in chosen }.toBooleanArray()
        AlertDialog.Builder(this)
            .setTitle(R.string.choose_blocked)
            .setMultiChoiceItems(apps.map { it.label }.toTypedArray(), checked) { _, which, isChecked ->
                if (isChecked) chosen += apps[which].pkg else chosen -= apps[which].pkg
            }
            .setPositiveButton(android.R.string.ok) { _, _ ->
                settings.blockedApps = chosen
                if (!mode.sync()) Toast.makeText(this, R.string.needs_setup, Toast.LENGTH_SHORT).show()
                renderBlocked()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    /** A switch for an option, showing its sub-settings only while on. */
    private fun bindOption(switchId: Int, optionsId: Int?, initial: Boolean, save: (Boolean) -> Unit) {
        val toggle = findViewById<Switch>(switchId)
        val options = optionsId?.let { findViewById<View>(it) }
        toggle.isChecked = initial
        options?.visibility = if (initial) View.VISIBLE else View.GONE
        toggle.setOnCheckedChangeListener { _, checked ->
            save(checked)
            options?.visibility = if (checked) View.VISIBLE else View.GONE
            mode.sync()
        }
    }

    /** A slider from [min] to [max] in [step]s; the system is updated once it's let go. */
    private fun bindSlider(
        seekId: Int,
        valueId: Int,
        min: Int,
        max: Int,
        step: Int,
        initial: Int,
        save: (Int) -> Unit,
        label: (Int) -> String,
    ) {
        val seek = findViewById<SeekBar>(seekId)
        val value = findViewById<TextView>(valueId)
        seek.min = min / step
        seek.max = max / step
        seek.progress = initial / step
        value.text = label(initial)
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar, progress: Int, fromUser: Boolean) {
                value.text = label(progress * step)
                if (fromUser) save(progress * step)
            }

            override fun onStartTrackingTouch(bar: SeekBar) = Unit

            override fun onStopTrackingTouch(bar: SeekBar) {
                mode.sync()
            }
        })
    }

    private fun requestTile() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        getSystemService(StatusBarManager::class.java).requestAddTileService(
            ComponentName(this, PaperTileService::class.java),
            getString(R.string.paper_mode),
            Icon.createWithResource(this, R.drawable.ic_paper),
            mainExecutor,
        ) { result ->
            if (result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED) {
                Toast.makeText(this, R.string.tile_already_added, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
