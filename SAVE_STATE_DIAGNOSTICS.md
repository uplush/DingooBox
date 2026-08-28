# Save-state diagnostics

Alpha18 compiles the JNI frontend from source and forwards DingooEmu's exact
libretro serializer errors to Android Logcat. It also raises the decoded-state
limit from 64 MiB to 128 MiB, so the Rust core must be rebuilt once.

## Required SDK tools

- Android NDK 28.2.13676358
- CMake 3.22.1
- Rust stable with the `aarch64-linux-android` target
- `cargo-ndk`

Install both in Android Studio under **Settings > Languages & Frameworks >
Android SDK > SDK Tools**. Enable **Show Package Details** to select the exact
versions.

## Rebuild the core, app, and install (PowerShell)

```powershell
rustup target add aarch64-linux-android
cargo install cargo-ndk
$env:ANDROID_NDK_HOME = "$env:LOCALAPPDATA\Android\Sdk\ndk\28.2.13676358"
.\scripts\build-core.ps1
.\gradlew.bat clean assembleDebug --no-configuration-cache
adb install -r ".\app\build\outputs\apk\debug\app-debug.apk"
```

The installed version must report `1.0.0` or later.

## Capture one failed save

```powershell
adb logcat -c
```

Start a game, open PauseMenu, open instant save, and try one slot. Then run:

```powershell
adb logcat -d -s DingooState:I DingooCore:E *:S
```

`DingooState` identifies the JNI stage: initialization, serialization, opening
the destination, or writing/flushing it. `DingooCore` contains the underlying
Rust serializer error, including decoded-size and fixed-capacity failures.

## Verify files

```powershell
adb shell run-as io.github.uplush.dingoobox ls -lR files/states
```

A successful state created by the bundled core is exactly 50,331,648 bytes.
Alpha18 updates a preview image only after the matching state succeeds.
