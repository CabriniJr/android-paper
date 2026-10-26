package com.paperscreen

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.os.Process
import java.text.Collator

/** A launchable app: its name and the activity that opens it. */
class AppEntry(val label: String, val component: ComponentName) {
    val pkg: String get() = component.packageName
}

/** Every launchable app (this user's), by name in the phone's language. */
object AppCatalog {
    fun load(context: Context): List<AppEntry> {
        val launcherApps = context.getSystemService(LauncherApps::class.java)
        val collator = Collator.getInstance()
        return launcherApps.getActivityList(null, Process.myUserHandle())
            .filter { it.componentName.packageName != context.packageName }
            .map { AppEntry(it.label.toString(), it.componentName) }
            .sortedWith { a, b -> collator.compare(a.label, b.label) }
    }
}
