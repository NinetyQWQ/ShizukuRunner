# ShizukuRunner
An android app to run any commands via Shizuku.
# ShizukuRunner

<img src="app/src/main/res/drawable/icon.png" alt="ShizukuRunner Cat Icon" width="96">

ShizukuRunner is a compact Android app that executes shell commands through the Shizuku service and displays live stdout/stderr. It provides editable command slots, quick one-shot execution, copy-to-clipboard for commands, and lightweight UI animations for a smooth experience.

- Minimum Android SDK: 23
- Uses: Rikka Shizuku API (dev.rikka.shizuku)
- Language: Java (Android)

---

## Features

- Run arbitrary shell commands through Shizuku (real-time stdout/stderr streaming).
- Save named command slots (tap to run, long-press to copy).
- One-shot command entry mode (tap the cat icon).
- Option to "drop root to shell" when invoking certain commands (implemented in adapter).
- Lightweight animations:
  - Flip animation on the cat icon (ObjectAnimator.rotationY).
  - List entrance animations (TranslateAnimation + LayoutAnimationController).
- No network features and no analytics (app does not collect or transmit your data).

---

## Screenshots

Use your local build to view UI. Example icon (repo-relative):

<img src="app/src/main/res/drawable/icon.png" alt="Cat Icon" width="160">

---

## Requirements

- Android device (or emulator) running Android 6.0+ (API 23+).
- Shizuku Manager installed and running (https://github.com/RikkaApps/Shizuku).
  - You can start Shizuku via ADB or via root depending on your device.
- Android build toolchain (Android Studio recommended) or the Gradle wrapper included.

---

## Install & Build

### With Android Studio
1. Open Android Studio.
2. Choose "Open an existing Android Studio project" and select this repository.
3. Allow Gradle to sync (it uses the included Gradle wrapper).
4. Run on a device (remember to install and authorize Shizuku).

### From command line (Linux / macOS / Windows with WSL)
1. Ensure Java JDK 8+ and Android SDK/NDK available (the project uses ndkVersion in the module file).
2. In the repository root:

   - Build debug APK:
     ```
     ./gradlew :app:assembleDebug
     ```
   - Install on connected device:
     ```
     ./gradlew :app:installDebug
     ```

3. Open the app on the device.

Notes:
- The project uses Gradle wrapper (gradle-7.5-rc-1) — using the wrapper (`./gradlew`) ensures reproducible builds.
- If your environment differs from the project's ndkPath, remove or adjust ndkPath/ndkVersion lines in app/build.gradle to use your installed NDK.

---

## Usage

1. Start or authorize Shizuku:
   - Start Shizuku Manager on your device and enable it.
   - If needed, start via ADB (when supported) or root.
2. Launch ShizukuRunner.
3. The two top buttons show Shizuku status:
   - "Shizuku Running" / "Not Running"
   - "Shizuku Authorized" / "Not Authorized"
   - Tap to refresh.
4. To run saved commands:
   - Tap a slot’s run icon to execute the saved command.
   - Long-press a slot to copy the command to clipboard.
   - Tap the slot itself to edit the command and its name.
5. One-shot execution:
   - Single-tap the cat icon to reveal the one-shot command entry field. Enter a command and press the run button (or keyboard Done).
6. Execution output:
   - Exec activity streams stdout and stderr live. stderr lines are colored red.
   - Return value and elapsed time are shown when execution completes.

Example: entering `ls -la /data` in the one-shot field will execute it via Shizuku and show results.

Security note: If Shizuku is started via root, commands may run with root privileges. Use the "Drop root to Shell" option when editing a command if you want to avoid running with root-level privileges (the app supports downgrade logic via a native helper invocation).

---

## Troubleshooting

- Shizuku not running / Not authorized
  - Make sure Shizuku Manager is started on your device and you have authorized this app in the Shizuku Manager UI.
  - If Shizuku is started via ADB or root, authorize following Shizuku Manager instructions.
- App crashes while executing commands
  - Check logcat for exceptions. Exec activity uses background threads to stream output — unhandled exceptions are swallowed in some places; check device logs.
- Gradle/NDK mismatch
  - If the project references an NDK path not present on your machine, remove the ndkPath line in app/build.gradle or install the referenced NDK version, or set local sdk locations in Android Studio.

---

## Privacy & Security

- This app does not use network connections or collect user data.
- Commands executed through Shizuku run with the privileges Shizuku provides. If Shizuku runs under root, commands will have root privileges. Use the "Drop root to Shell" option when editing a slot if you want the app to attempt to lower privileges for that command.
- Be careful when running commands — they can affect system files and settings.
