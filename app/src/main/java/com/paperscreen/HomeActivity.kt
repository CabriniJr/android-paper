package com.paperscreen

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextClock
import android.widget.TextView
import java.util.Locale

/**
 * A minimal home screen, for a phone that's a tool rather than a feed: the time, the date and
 * up to ten favourite apps as plain words, and a searchable list of every app behind them.
 * No icons, badges, widgets or wallpaper to pull you in.
 *
 * Long-press a name to rearrange favourites, or to block an app (it then gets the pause
 * screen; see [FocusService]).
 */
class HomeActivity : Activity() {

    private lateinit var mode: PaperMode
    private val settings get() = mode.settings

    private lateinit var home: View
    private lateinit var drawer: View
    private lateinit var search: EditText
    private lateinit var favouritesView: LinearLayout
    private val adapter = AppAdapter()

    private var apps: List<AppEntry> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        mode = PaperMode(this)
        home = findViewById(R.id.home)
        drawer = findViewById(R.id.drawer)
        search = findViewById(R.id.search)
        favouritesView = findViewById(R.id.favourites)

        val pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEEEdMMMM")
        findViewById<TextClock>(R.id.date).apply {
            format12Hour = pattern
            format24Hour = pattern
        }
        findViewById<View>(R.id.open_drawer).setOnClickListener { openDrawer() }

        val list = findViewById<ListView>(R.id.app_list)
        list.adapter = adapter
        list.setOnItemClickListener { _, _, position, _ -> launch(adapter.getItem(position).component) }
        list.setOnItemLongClickListener { _, _, position, _ ->
            appMenu(adapter.getItem(position))
            true
        }
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) = adapter.filter(s?.toString().orEmpty())
        })
        search.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO && adapter.count > 0) {
                launch(adapter.getItem(0).component)
                true
            } else {
                false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Apps may have been installed or removed meanwhile; reading the list is quick.
        apps = AppCatalog.load(this)
        adapter.filter(search.text.toString())
        renderFavourites()
    }

    /** Pressing Home while already home closes the app list. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        closeDrawer()
    }

    @Deprecated("The home screen has nowhere to go back to; Back only closes the app list.")
    @Suppress("DEPRECATION")
    override fun onBackPressed() = closeDrawer()

    override fun onStop() {
        // Leaving for an app: come back to the home page, not a half-typed search.
        closeDrawer()
        super.onStop()
    }

    private fun renderFavourites() {
        favouritesView.removeAllViews()
        val byComponent = apps.associateBy { it.component.flattenToString() }
        val favourites = settings.favourites.mapNotNull { byComponent[it] }
        if (favourites.isEmpty()) {
            favouritesView.addView(row(getString(R.string.no_favourites)).apply { alpha = 0.5f; textSize = 16f })
            return
        }
        for (app in favourites) {
            favouritesView.addView(
                row(label(app)).apply {
                    setOnClickListener { launch(app.component) }
                    setOnLongClickListener {
                        favouriteMenu(app)
                        true
                    }
                },
            )
        }
    }

    private fun row(text: String): TextView =
        (LayoutInflater.from(this).inflate(R.layout.item_home_app, favouritesView, false) as TextView)
            .apply { this.text = text }

    private fun label(app: AppEntry): String =
        if (app.pkg in settings.blockedApps) getString(R.string.blocked_label, app.label) else app.label

    private fun openDrawer() {
        home.visibility = View.GONE
        drawer.visibility = View.VISIBLE
        search.requestFocus()
        getSystemService(InputMethodManager::class.java).showSoftInput(search, 0)
    }

    private fun closeDrawer() {
        if (drawer.visibility != View.VISIBLE) return
        getSystemService(InputMethodManager::class.java).hideSoftInputFromWindow(search.windowToken, 0)
        search.text.clear()
        drawer.visibility = View.GONE
        home.visibility = View.VISIBLE
    }

    private fun launch(component: ComponentName) {
        try {
            AppCatalog.launch(this, component)
        } catch (e: RuntimeException) {
            // Uninstalled or disabled since the list was read.
            onResume()
        }
    }

    private fun favouriteMenu(app: AppEntry) {
        val key = app.component.flattenToString()
        val actions = listOf<Pair<Int, () -> Unit>>(
            R.string.move_up to { settings.favourites = Favourites.move(settings.favourites, key, -1) },
            R.string.move_down to { settings.favourites = Favourites.move(settings.favourites, key, 1) },
            R.string.remove_favourite to { settings.favourites = Favourites.remove(settings.favourites, key) },
        )
        menu(app.label, actions)
    }

    private fun appMenu(app: AppEntry) {
        val key = app.component.flattenToString()
        val favourites = settings.favourites
        val blocked = app.pkg in settings.blockedApps
        val actions = buildList<Pair<Int, () -> Unit>> {
            when {
                key in favourites -> add(R.string.remove_favourite to { settings.favourites = Favourites.remove(favourites, key) })
                favourites.size < Favourites.MAX -> add(R.string.add_favourite to { settings.favourites = Favourites.add(favourites, key) })
            }
            add((if (blocked) R.string.unblock_app else R.string.block_app) to { setBlocked(app.pkg, !blocked) })
            add(R.string.app_info to { openAppInfo(app.pkg) })
        }
        menu(app.label, actions)
    }

    private fun menu(title: String, actions: List<Pair<Int, () -> Unit>>) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setItems(actions.map { getString(it.first) }.toTypedArray()) { _, which ->
                actions[which].second()
                adapter.notifyDataSetChanged()
                renderFavourites()
            }
            .show()
    }

    private fun setBlocked(pkg: String, block: Boolean) {
        settings.blockedApps = if (block) settings.blockedApps + pkg else settings.blockedApps - pkg
        if (!mode.sync()) {
            // Without the one-time permission the focus service can't be switched on for us.
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun openAppInfo(pkg: String) {
        try {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg")))
        } catch (e: ActivityNotFoundException) {
            // Nothing to show.
        }
    }

    /** Every app, or those matching the search: names starting with it first. */
    private inner class AppAdapter : BaseAdapter() {
        private var shown: List<AppEntry> = emptyList()

        fun filter(query: String) {
            val q = AppEntry.fold(query.trim())
            shown = if (q.isEmpty()) {
                apps
            } else {
                apps.filter { q in it.key }.sortedBy { if (it.key.startsWith(q)) 0 else 1 }
            }
            notifyDataSetChanged()
        }

        override fun getCount() = shown.size
        override fun getItem(position: Int) = shown[position]
        override fun getItemId(position: Int) = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = (convertView ?: LayoutInflater.from(parent.context).inflate(R.layout.item_home_app, parent, false)) as TextView
            view.textSize = 20f
            view.text = label(shown[position])
            return view
        }
    }
}
