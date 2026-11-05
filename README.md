# Notification Log

A small Android app that keeps a searchable history of every notification your phone receives, even the ones you swipe away. Everything stays on the device. The app has no internet permission.

I built it because my phone (Infinix, XOS 13 / Android 13) has no notification history in Settings, and the hidden Android one only keeps dismissed notifications for about a day.

## What it does

- Saves notifications from every app as they arrive, including silent, ongoing and group ones
- Treats chat apps properly: each message is stored on its own, with the sender, instead of the whole thread being saved again every time a new message comes in
- Separates incoming messages from the ones you send (replies you type from the notification show up as "You")
- Remembers what happened to each notification afterwards: opened, dismissed, cleared, or removed by the app
- Tap a notification to open the app. If the original is still in the shade, it opens the exact chat or screen
- Search by app, title, message or sender, and filter by app with chips
- Swipe to delete with undo, clear everything, and auto-delete after 7, 30 or 90 days
- Optional toggle to hide ongoing and system noise from the list without deleting it
- Material 3 design with Material You dynamic color, a slightly deeper dark theme, and single-tone themed icons that follow your wallpaper

## Privacy

- No `INTERNET` permission, so nothing can leave your phone
- No accounts, analytics, ads or third-party services
- Data lives in a local Room database and is removed when you uninstall the app or clear it from Settings

Keep in mind that the log stores the text of your notifications, which can include private messages and one-time codes. Anyone who can open your phone can open the app.

## Install

1. Download `NotificationLog.apk` from the **Releases** page (the release named "Latest build").
2. Open it and allow "Install unknown apps" for your browser or file manager when asked.
3. Open the app and finish the setup screen.

### First-time setup

1. **Allow notification access.** Tap the button on the setup screen and turn on Notification Log.
2. **If you see "Restricted setting"** (Android 13 and newer, for apps installed outside the Play Store): go to Settings > Apps > Notification Log, tap the ⋮ menu, choose **Allow restricted settings**, then go back and enable notification access.
3. **Exclude the app from battery optimization** and lock it in the recent apps list. Some phones, XOS included, stop background apps aggressively, and notifications are missed while the service is stopped.

## Tech

- Kotlin and Jetpack Compose, Material 3 with dynamic color
- `NotificationListenerService` for capture
- Room for storage, Paging 3 for the list, DataStore for settings, WorkManager for auto-delete
- No Hilt, Retrofit, Coil or other image/network libraries, to keep memory use low
- R8 minification and resource shrinking on for release builds

On my phone the app sits at around 14 MB of RAM while idle.

Rough layout:

```
app/src/main/java/.../
  data/     Room database, DAO, repository, notification capture service
  ui/       home screen, settings, theme, shared components
  util/     small helpers
```

## Build it yourself

### With Android Studio

1. Clone the repo and open it in Android Studio.
2. Let Gradle sync.
3. Connect your phone with USB debugging on and press Run, or use **Build > Build APK(s)**.

### With GitHub Actions (no computer needed)

The workflow in `.github/workflows/build.yml` builds on every push to `main`:

1. Pushing to `main` starts the build (a few minutes).
2. It produces a signed release APK and publishes it to a GitHub Release called "Latest build".
3. Download the APK from the Releases page on your phone and install it.

The build signs every version with the same key stored in `app/debug.keystore`, so new versions install over the old one and your saved notifications are kept. That key is a throwaway debug-style key, but keep the repository private anyway.

## Known limits

- It can only record notifications that arrive while it has access. Notifications from before you enabled access are not available, apart from the ones still sitting in the shade.
- Replies are only recorded as outgoing when the app includes them in the notification. Messages typed inside the app itself are not visible.
- If the phone's battery manager kills the service, notifications during that time are missed. The battery and recents-lock steps above help a lot.
- Some apps hide or shorten their text. The app saves whatever the notification actually contains and never fills in missing text.
- Full-color app icons are intentionally not shown. Apps with a themed icon appear in a single tint, and the rest get a simple category symbol.

## Roadmap ideas

- Export and backup of the history
- A "noisiest apps" stats screen
- A home-screen widget with the latest notifications
