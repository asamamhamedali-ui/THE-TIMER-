# THE STUDY TIMER

Android-native, Galaxy-inspired clock and study alarm starter project.

## Implemented in this source release
- Create alarms with time, label and optional repeat days.
- Enable/disable individual alarms and delete them.
- Persist alarms locally.
- Schedule alarms with Android AlarmManager; exact-alarm permission is checked and an inexact fallback is used.
- Reschedule enabled alarms after reboot, time changes, timezone changes and app replacement.
- High-priority alarm notification with full-screen ringing activity where Android permits it.
- Countdown timer scheduling through AlarmManager.
- Stopwatch UI.
- Dark Galaxy-inspired Material 3 interface.
- Unit tests for next-alarm calculation.

## Important current limitations
- The selected tone picker is currently a first-alarm shortcut, not yet a per-alarm editor.
- The countdown timer's remaining-time display is not yet a live countdown; Android schedules its completion notification.
- The ringing activity currently provides Dismiss UI, but full ringtone playback, snooze controls, volume ramp, and persistent foreground playback are not implemented yet.
- World clock, alarm history, wallpaper/theme editor, backup/restore and sleep analytics are planned, not implemented in this initial source milestone.
- Android 13+ notification permission and Android 12+ exact alarm access are subject to user/system settings. Android may restrict full-screen launch.
- Exact alarm delivery cannot be guaranteed if the user force-stops the app, revokes permissions, or the device applies manufacturer-specific restrictions.

## Build
Requirements: Android Studio with JDK 17 and Android SDK Platform 35.

1. Open this folder in Android Studio.
2. Allow Gradle sync.
3. Run unit tests: `./gradlew test` (after adding a Gradle wrapper) or use Android Studio's Gradle test task.
4. Build APK: `./gradlew assembleDebug`.
5. APK output: `app/build/outputs/apk/debug/app-debug.apk`.

The build environment used to generate this source package did not include Android SDK, Gradle, or adb, so an APK could not be built or device-tested here. Do not treat this source archive as a verified APK.
