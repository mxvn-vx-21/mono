# MONO — Today · Done · Memo

A native Android app (Kotlin + Jetpack Compose + Room). Fully offline: no accounts, no network, **no permissions**.

## 1. Replace the mascot (`logo.png`)
Location: `app/src/main/res/drawable-nodpi/logo.png`

The file currently there is a **provisional cut-out of the mascot from your showcase image** (transparent background, eyes preserved) so the project compiles and runs immediately. Replace it with your own clean PNG, **keeping the exact filename `logo.png`** (square, transparent, ideally 512–1024 px). No code changes needed. It's used in the centre nav button, the Today header, empty states, Settings and as the launcher icon.

The display font is `app/src/main/res/font/mono_font.ttf` (your `font.ttf`).

## 2. Open & build
1. Install **Android Studio** (Koala or newer) with **JDK 17** (bundled "jbr" is fine) and Android SDK **34**.
2. *File → Open* the `MONO` folder. Let Gradle sync (needs internet once to download dependencies).
3. **Gradle wrapper note:** `gradle/wrapper/gradle-wrapper.properties` pins Gradle 8.7, but `gradlew`, `gradlew.bat` and `gradle-wrapper.jar` could not be generated offline. Android Studio syncs fine without them. To create them once, in the Android Studio terminal run `gradle wrapper --gradle-version 8.7` (or copy the three files from any other Android project).

Debug APK (once the wrapper exists):

    ./gradlew assembleDebug          # macOS / Linux
    gradlew.bat assembleDebug        # Windows

Or in Android Studio: *Build → Build Bundle(s)/APK(s) → Build APK(s)*.

Output: `app/build/outputs/apk/debug/app-debug.apk` (no paid developer account needed).

## 3. Install on your phone
- **USB:** enable Developer options → USB debugging, then `adb install -r app/build/outputs/apk/debug/app-debug.apk` (or press Run in Android Studio).
- **File copy:** send the APK to the phone, open it, allow "Install unknown apps" for your file manager when asked.

Rebuild after any change by re-running `assembleDebug` and reinstalling with `-r` (keeps your data).

## 4. Tests
    ./gradlew testDebugUnitTest            # JVM: sorting, progress, search, rich-content round trip, dates
    ./gradlew connectedDebugAndroidTest    # device/emulator: Room persistence, folders, nav bar UI

## 5. Design notes & decisions
- **Dates** are stored as `LocalDate` epoch days (+ optional minutes-after-midnight for task time): no timezone shifts.
- **Notes** are saved as versioned JSON (`RichContent`): blocks of paragraph / bullet / numbered / checklist with bold & italic flags; checklist state persists. Malformed content is recovered as plain text, never a crash.
- **Formatting is per line** (bold/italic apply to the whole line), a deliberate simplification for a reliable editor.
- **Autosave:** notes save ~0.6 s after you stop typing, on leaving the app, and on back. Back only exits once the save succeeded.
- **Deleting a folder keeps its notes** (they become folder-less).
- Schema is version 1 with exported schemas (`app/schemas`). Future changes must add a Room `Migration`; destructive migration is never used.
- Greeting uses the name `$uf1` (`util/UserProfile.kt`).

## 6. Deviations from the mock-up
Controls that would have no working purpose in v1 were left out: the bell, header search/⋯ buttons, Feedback, Rate, About-chevron and the Add-Task FAB (the centre mascot button is the Quick Add). Search lives as a field in Memo. The mountain illustration is replaced by the mascot.
