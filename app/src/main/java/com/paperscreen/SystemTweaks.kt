package com.paperscreen

/** Which settings table a system setting lives in. */
enum class Table { SECURE, GLOBAL }

/** One Android system setting PaperScreen may take over. */
data class SysKey(val table: Table, val name: String)

/** Reads and writes system settings: Android's in the app, a map in tests. */
interface SettingsStore {
    fun read(key: SysKey): String?
    fun write(key: SysKey, value: String?)
}

/** What paper mode asks of the display, from the user's options. */
data class PaperLook(
    /** Night Light colour temperature for the paper tint; null leaves Night Light alone. */
    val warmthKelvin: Int?,
    /** Extra dim intensity (0–100) to take the glare off white; null leaves it alone. */
    val dimLevel: Int?,
    val crispText: Boolean,
    val noAnimations: Boolean,
)

/**
 * Paper mode is nothing but Android's own display settings, so the look costs no CPU at all:
 * SurfaceFlinger applies the colour transforms in the same pass that composes the screen.
 *
 * - **Greyscale:** the colour-correction "monochromacy" mode (the one Bedtime mode uses).
 * - **Warmth:** Night Light at a chosen temperature. The system applies greyscale first and
 *   Night Light's tint after it, so grey becomes warm paper grey.
 * - **Dim whites:** Extra dim, so white reads as paper rather than a backlight.
 * - **Crisp text:** high-contrast text, drawn solid black or white like e-ink type.
 * - **No animations:** the three animation scales at 0, so screens change in one step.
 *
 * Every value taken over is remembered first and put back when it's no longer needed.
 */
object SystemTweaks {
    val NIGHT_AUTO_MODE = SysKey(Table.SECURE, "night_display_auto_mode")
    val NIGHT_TEMPERATURE = SysKey(Table.SECURE, "night_display_color_temperature")
    val NIGHT_ACTIVATED = SysKey(Table.SECURE, "night_display_activated")
    val DALTONIZER = SysKey(Table.SECURE, "accessibility_display_daltonizer")
    val DALTONIZER_ENABLED = SysKey(Table.SECURE, "accessibility_display_daltonizer_enabled")
    val DIM_LEVEL = SysKey(Table.SECURE, "reduce_bright_colors_level")
    val DIM_ACTIVATED = SysKey(Table.SECURE, "reduce_bright_colors_activated")
    val HIGH_TEXT_CONTRAST = SysKey(Table.SECURE, "high_text_contrast_enabled")
    val WINDOW_ANIMATION = SysKey(Table.GLOBAL, "window_animation_scale")
    val TRANSITION_ANIMATION = SysKey(Table.GLOBAL, "transition_animation_scale")
    val ANIMATOR_DURATION = SysKey(Table.GLOBAL, "animator_duration_scale")

    /**
     * Every key, in the order they're written. Night Light's schedule is switched off before
     * it's activated, so a schedule can't turn it straight off again, and comes back first on
     * restore. Values come before the switches that use them.
     */
    val ALL = listOf(
        NIGHT_AUTO_MODE, NIGHT_TEMPERATURE, NIGHT_ACTIVATED,
        DALTONIZER, DALTONIZER_ENABLED,
        DIM_LEVEL, DIM_ACTIVATED,
        HIGH_TEXT_CONTRAST,
        WINDOW_ANIMATION, TRANSITION_ANIMATION, ANIMATOR_DURATION,
    )

    /** Colour-correction mode for monochromacy (greyscale). */
    private const val MONOCHROMACY = "0"

    /** The settings paper mode needs for [look]. */
    fun desired(look: PaperLook): Map<SysKey, String> = buildMap {
        put(DALTONIZER, MONOCHROMACY)
        put(DALTONIZER_ENABLED, "1")
        look.warmthKelvin?.let {
            put(NIGHT_AUTO_MODE, "0")
            put(NIGHT_TEMPERATURE, it.toString())
            put(NIGHT_ACTIVATED, "1")
        }
        look.dimLevel?.let {
            put(DIM_LEVEL, it.toString())
            put(DIM_ACTIVATED, "1")
        }
        if (look.crispText) put(HIGH_TEXT_CONTRAST, "1")
        if (look.noAnimations) {
            put(WINDOW_ANIMATION, "0")
            put(TRANSITION_ANIMATION, "0")
            put(ANIMATOR_DURATION, "0")
        }
    }

    /**
     * Brings the system to [want]. [saved] holds the original value of every key already taken
     * over; keys no longer wanted get theirs back, and keys newly wanted are remembered before
     * they're changed. Returns the updated originals, to keep for next time.
     */
    fun apply(store: SettingsStore, want: Map<SysKey, String>, saved: Map<SysKey, String?>): Map<SysKey, String?> {
        val originals = saved.toMutableMap()
        for (key in ALL) {
            val value = want[key]
            if (value == null) {
                if (key in originals) {
                    store.write(key, originals[key])
                    originals.remove(key)
                }
            } else {
                if (key !in originals) originals[key] = store.read(key)
                if (store.read(key) != value) store.write(key, value)
            }
        }
        return originals
    }

    /**
     * Night Light temperature for a warmth of 1–100, from barely warm ([coolest]) to amber
     * ([warmest]), the device's own Night Light range.
     */
    fun kelvinFor(warmth: Int, warmest: Int, coolest: Int): Int {
        val w = warmth.coerceIn(1, 100)
        return coolest - (coolest - warmest) * w / 100
    }

    /** One line per key: table, name and, unless it was unset, the value, tab-separated. */
    fun encode(originals: Map<SysKey, String?>): String =
        originals.entries.joinToString("\n") { (key, value) ->
            listOfNotNull(key.table.name, key.name, value).joinToString("\t")
        }

    fun decode(text: String?): Map<SysKey, String?> {
        if (text.isNullOrEmpty()) return emptyMap()
        return text.lines().mapNotNull { line ->
            val parts = line.split('\t')
            val table = Table.entries.firstOrNull { it.name == parts[0] } ?: return@mapNotNull null
            if (parts.size < 2) return@mapNotNull null
            SysKey(table, parts[1]) to parts.getOrNull(2)
        }.toMap()
    }
}
