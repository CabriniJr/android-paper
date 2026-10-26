package com.paperscreen

import com.paperscreen.FocusRules.Action
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusRulesTest {

    private val instagram = "com.instagram.android"
    private val youtube = "com.google.android.youtube"
    private val tiktok = "com.zhiliaoapp.musically"

    @Test
    fun blockedAppGetsThePauseScreenUntilUnlocked() {
        val blocked = setOf(instagram)
        assertEquals(Action.PAUSE_SCREEN, FocusRules.actionFor(instagram, blocked, false, null, 1000))
        assertEquals(Action.NONE, FocusRules.actionFor(instagram, blocked, false, 2000, 1000))
        assertEquals(Action.PAUSE_SCREEN, FocusRules.actionFor(instagram, blocked, false, 2000, 2000))
    }

    @Test
    fun shortsModeChecksPlayersInsideUsefulApps() {
        assertEquals(Action.CHECK_FOR_SHORTS, FocusRules.actionFor(youtube, emptySet(), true, null, 0))
        assertEquals(Action.CHECK_FOR_SHORTS, FocusRules.actionFor(instagram, emptySet(), true, null, 0))
        assertEquals(Action.NONE, FocusRules.actionFor(youtube, emptySet(), false, null, 0))
    }

    @Test
    fun shortVideoOnlyAppsAreBlockedWhole() {
        assertEquals(Action.PAUSE_SCREEN, FocusRules.actionFor(tiktok, emptySet(), true, null, 0))
        assertEquals(Action.NONE, FocusRules.actionFor(tiktok, emptySet(), false, null, 0))
    }

    @Test
    fun anUnlockedAppIsntCheckedForShortsEither() {
        assertEquals(Action.NONE, FocusRules.actionFor(instagram, setOf(instagram), true, 5000, 0))
    }

    @Test
    fun otherAppsAreLeftAlone() {
        assertEquals(Action.NONE, FocusRules.actionFor("com.whatsapp", setOf(instagram), true, null, 0))
    }

    @Test
    fun watchesOnlyWhatItNeeds() {
        assertTrue(FocusRules.watchedPackages(emptySet(), false).isEmpty())
        assertEquals(setOf(instagram), FocusRules.watchedPackages(setOf(instagram), false))
        assertTrue(youtube in FocusRules.watchedPackages(emptySet(), true))
        assertTrue(tiktok in FocusRules.watchedPackages(emptySet(), true))
    }
}
