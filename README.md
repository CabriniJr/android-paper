# PaperScreen

Turns an Android phone into a calm, good "dumb phone": it looks like e-ink and keeps short
videos and the apps you choose out of reach. Pair it with a minimal launcher of your choice
(e.g. Niagara). Two independent parts:

- **Paper mode** – the e-ink look (below).
- **Focus** – blocks short videos and chosen apps.

## Paper mode

Makes an OLED Android phone look and feel like an e-ink reader, at practically zero cost: no
screen capture, no per-pixel work, nothing running in the background. Paper mode is built
entirely from Android's own display pipeline, which SurfaceFlinger applies to every frame in
the same pass that composes the screen.

### What it does

| Effect | How | Option |
|---|---|---|
| Greyscale | Colour correction, monochromacy mode (what Bedtime mode uses) | always on |
| Paper warmth | Night Light at a chosen temperature; greyscale is applied first, so grey turns warm | Neutral–100, default 40 |
| Dim whites | Extra dim, so white reads as paper instead of a backlight | off; 10–80% |
| Crisp text | High-contrast text: solid black type | off |
| No animations | Window, transition and animator scales set to 0: screens change in one step | on |
| Page-turn flash | A black-then-white flash when a new screen (activity) opens, like an e-ink full refresh | off; 100–600 ms |

Before taking over a system setting, PaperScreen saves its value; turning paper mode (or one
option) off puts back exactly what was there, including a Night Light schedule. The look is
held by the system, so it survives reboots without the app running.

The page-turn flash is the only part with any code running: a tiny accessibility service
that listens for window-state changes only (never window content), asks the package manager
once per class whether it's an activity, and shows a plain coloured window for the length of
the flash. It turns its own event delivery off while the flash isn't wanted, and PaperScreen
switches the service itself on and off with the option.

## Focus

`FocusService` (an accessibility service PaperScreen switches on only while something is
blocked) does two things:

- **Block short videos:** closes YouTube Shorts and Instagram Reels with Back as soon as their
  full-screen player shows, so search, messages and normal videos keep working, and treats
  short-video-only apps (TikTok, TikTok Lite, Kwai) as blocked apps. The players are found by
  view id (`FocusRules.SHORT_VIDEO_VIEWS`); if an app update renames them, update that list.
- **Blocked apps** (picked in settings): opening one
  brings up a pause screen instead – "What did you open it for?" – with *Back to home*, and
  *Use it for N minutes* (1–15, default 5) only after a 10-second wait. When the time runs
  out, the pause screen returns at the app's next change.

It stays light: the system only delivers events from the watched apps, coalesced to one per
type every 300 ms, and window content is only queried inside YouTube and Instagram. It's
friction, not a lock: everything can be undone from the settings page.

## Toggling paper mode

- **Quick Settings tile** "Paper mode": tap to toggle, long-press for the settings. On Android
  13+, the app's *Add Quick Settings tile* button adds it for you.
- **App shortcut:** long-press the app icon → *Toggle paper mode*; it can be pinned to the home
  screen. It runs `ToggleActivity` (no UI), which anything that can launch an activity can use,
  e.g. the `com.paperscreen.TOGGLE` intent from a routine/automation app.
- **The app:** master switch plus the options above. English and Brazilian Portuguese.

## One-time setup

Writing these settings needs `WRITE_SECURE_SETTINGS`, which Android only lets adb grant:

```sh
adb install PaperScreen.apk
adb shell pm grant com.paperscreen android.permission.WRITE_SECURE_SETTINGS
```

Without a computer, Wireless debugging plus Shizuku or LADB on the phone can run the same
`pm grant`. The grant survives reboots and app updates (not an uninstall). Turn paper mode
off before uninstalling; otherwise switch off greyscale under Settings → Accessibility →
Colour correction.

## Building

The APK is built by GitHub Actions on every push (`.github/workflows/apk.yml`); download it
from the run's *Artifacts*. Locally (JDK 17 and an Android SDK with platform 34):

```sh
./gradlew assembleRelease      # app/build/outputs/apk/release/app-release.apk (minified, ~tens of KB)
./gradlew testDebugUnitTest    # SystemTweaks, FocusRules
```

The release build is signed with the debug key so it installs directly; use your own key
before distributing it. No libraries: framework APIs and the Kotlin stdlib only. minSdk 31
(Android 12, for Extra dim), target 34.

## Code

| File | Role |
|---|---|
| `SystemTweaks.kt` | The settings each option needs, and the save/apply/restore logic (pure, unit-tested) |
| `PaperMode.kt` | Android glue: reads/writes the settings, Night Light range, the page-turn service switch |
| `PaperSettings.kt` | The user's options (SharedPreferences) |
| `SettingsActivity.kt` | The single settings page, in the system's own theme |
| `PaperTileService.kt` | Quick Settings tile |
| `ToggleActivity.kt` | Invisible toggle for the launcher shortcut |
| `PageTurnService.kt` | Optional page-turn flash |
| `FocusRules.kt` | What to block and how (pure, unit-tested) |
| `FocusService.kt` | Focus blocking |
| `PauseActivity.kt` / `Unlocks.kt` | The pause screen, and apps let through for a while |
| `AppCatalog.kt` | The launchable apps, for picking which to block |

## Limitations

- No posterization or custom contrast curve: Android only exposes fixed colour transforms
  to apps. (The earlier capture-based version did that per pixel, at a real CPU and battery
  cost; it's in the git history.)
- Night Light is shared with the system: while paper mode is on with a warmth set, Night
  Light's own schedule is paused and its temperature is PaperScreen's.
- On some phones, Extra dim or high-contrast text may be missing or named differently; the
  options then do nothing.
