package com.paperscreen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemTweaksTest {

    private class MapStore(val values: MutableMap<SysKey, String?> = mutableMapOf()) : SettingsStore {
        val writes = mutableListOf<SysKey>()
        override fun read(key: SysKey) = values[key]
        override fun write(key: SysKey, value: String?) {
            writes += key
            values[key] = value
        }
    }

    private val look = PaperLook(warmthKelvin = 3000, dimLevel = 30, crispText = true, noAnimations = true)

    @Test
    fun onThenOffPutsEverythingBack() {
        val before = mapOf<SysKey, String?>(
            SystemTweaks.NIGHT_ACTIVATED to "0",
            SystemTweaks.NIGHT_AUTO_MODE to "1",
            SystemTweaks.NIGHT_TEMPERATURE to "3500",
            SystemTweaks.WINDOW_ANIMATION to "1.0",
        )
        val store = MapStore(before.toMutableMap())
        val saved = SystemTweaks.apply(store, SystemTweaks.desired(look), emptyMap())
        assertEquals("1", store.values[SystemTweaks.DALTONIZER_ENABLED])
        assertEquals("0", store.values[SystemTweaks.DALTONIZER])
        assertEquals("3000", store.values[SystemTweaks.NIGHT_TEMPERATURE])
        assertEquals("0", store.values[SystemTweaks.WINDOW_ANIMATION])

        val left = SystemTweaks.apply(store, emptyMap(), saved)
        assertTrue(left.isEmpty())
        for (key in SystemTweaks.ALL) assertEquals(key.name, before[key], store.values[key])
    }

    @Test
    fun droppingAnOptionRestoresOnlyItsKeys() {
        val store = MapStore()
        val saved = SystemTweaks.apply(store, SystemTweaks.desired(look), emptyMap())
        val neutral = look.copy(warmthKelvin = null)
        val after = SystemTweaks.apply(store, SystemTweaks.desired(neutral), saved)
        assertEquals(null, store.values[SystemTweaks.NIGHT_ACTIVATED])
        assertEquals("1", store.values[SystemTweaks.DALTONIZER_ENABLED])
        assertTrue(SystemTweaks.NIGHT_ACTIVATED !in after)
        assertTrue(SystemTweaks.DALTONIZER_ENABLED in after)
    }

    @Test
    fun reapplyingKeepsTheFirstOriginals() {
        val store = MapStore(mutableMapOf(SystemTweaks.DIM_LEVEL to "50"))
        val first = SystemTweaks.apply(store, SystemTweaks.desired(look), emptyMap())
        val second = SystemTweaks.apply(store, SystemTweaks.desired(look.copy(dimLevel = 60)), first)
        assertEquals("50", second[SystemTweaks.DIM_LEVEL])
        assertEquals("60", store.values[SystemTweaks.DIM_LEVEL])
    }

    @Test
    fun unchangedValuesAreNotRewritten() {
        val store = MapStore()
        val saved = SystemTweaks.apply(store, SystemTweaks.desired(look), emptyMap())
        store.writes.clear()
        SystemTweaks.apply(store, SystemTweaks.desired(look), saved)
        assertTrue(store.writes.isEmpty())
    }

    @Test
    fun nightLightIsUnscheduledBeforeItIsActivated() {
        val store = MapStore()
        SystemTweaks.apply(store, SystemTweaks.desired(look), emptyMap())
        assertTrue(store.writes.indexOf(SystemTweaks.NIGHT_AUTO_MODE) < store.writes.indexOf(SystemTweaks.NIGHT_ACTIVATED))
    }

    @Test
    fun encodeRoundTripsUnsetValues() {
        val originals = mapOf(SystemTweaks.DALTONIZER to null, SystemTweaks.ANIMATOR_DURATION to "0.5")
        assertEquals(originals, SystemTweaks.decode(SystemTweaks.encode(originals)))
        assertEquals(emptyMap<SysKey, String?>(), SystemTweaks.decode(null))
    }

    @Test
    fun warmthSpansTheNightLightRange() {
        assertEquals(4068, SystemTweaks.kelvinFor(1, warmest = 2596, coolest = 4082))
        assertEquals(2596, SystemTweaks.kelvinFor(100, warmest = 2596, coolest = 4082))
    }
}
