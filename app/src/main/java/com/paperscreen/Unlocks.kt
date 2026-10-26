package com.paperscreen

/**
 * Blocked apps let through for a while from the pause screen, until an
 * [android.os.SystemClock.elapsedRealtime]. Kept in memory: if Android ends the process, the
 * apps are simply blocked again.
 */
object Unlocks {
    private val until = HashMap<String, Long>()

    fun until(pkg: String): Long? = until[pkg]

    fun unlock(pkg: String, untilElapsed: Long) {
        until[pkg] = untilElapsed
    }
}
