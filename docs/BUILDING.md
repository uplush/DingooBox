# 构建丁果盒

普通 Android 构建使用仓库里的预编译核心。只有更新 DingooEmu 时，才需要额外准备 Rust 工具链。

## 环境

- Android Studio 自带的 JDK，或 JDK 21（GitHub Actions 使用 21）
- Android SDK Platform 37
- NDK 28.2.13676358
- CMake 3.22.1
- Gradle 使用仓库自带的 wrapper，不必另装

Windows 上请把工程放在类似 `C:\dev\DingooBox` 的短路径下。含中文等非 ASCII 字符的路径会触发 AGP/NDK 的路径检查。

## 构建 APK

```bash
git clone --recurse-submodules https://github.com/uplush/DingooBox.git
cd DingooBox
./gradlew testDebugUnitTest assembleDebug
```

已经克隆过仓库的话，先补齐子模块：

```bash
git submodule update --init --recursive
```

Windows PowerShell 中使用：

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

生成的调试包位于 `app/build/outputs/apk/debug/app-debug.apk`，只包含 `arm64-v8a`。Debug 和 Release 的包名都是 `io.github.uplush.dingoobox`。

调试包使用本机的 Android 调试证书，不等于 Releases 中安装包的签名。正式分发需要自行配置 Release 签名；仓库不包含签名密钥。不要为了安装不同签名的包直接卸载正在使用的版本，先备份数据。

## 重新编译核心

核心子模块位于 `native/DingooEmu`，来源是官方 [AloysHF/DingooEmu](https://github.com/AloysHF/DingooEmu)。当前使用的提交记录在 [`native/UPSTREAM_COMMIT`](../native/UPSTREAM_COMMIT)，预编译库的 SHA-256 记录在 [`native/core-build.sha256`](../native/core-build.sha256)。构建脚本会检查提交和源码状态，不会自动拉取最新的 `master`。

需要 Rust stable、`aarch64-linux-android` target 和 `cargo-ndk`。

Linux/macOS：

```bash
git submodule update --init --recursive
rustup target add aarch64-linux-android
cargo install cargo-ndk --version 4.1.2 --locked
ANDROID_NDK_HOME=/path/to/ndk/28.2.13676358 ./scripts/build-core.sh
ANDROID_NDK_HOME=/path/to/ndk/28.2.13676358 ./scripts/verify-core.sh
./gradlew testDebugUnitTest assembleDebug
```

Windows PowerShell（验证脚本使用 Git for Windows 的 Bash）：

```powershell
git submodule update --init --recursive
rustup target add aarch64-linux-android
cargo install cargo-ndk --version 4.1.2 --locked
$env:ANDROID_NDK_HOME = "$env:LOCALAPPDATA\Android\Sdk\ndk\28.2.13676358"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\build-core.ps1
& 'C:\Program Files\Git\bin\bash.exe' ./scripts/verify-core.sh
.\gradlew.bat testDebugUnitTest assembleDebug
```

脚本会生成 `app/src/main/jniLibs/arm64-v8a/libdingooemu.so` 并更新哈希文件。更新核心时，应一起提交子模块指针、`UPSTREAM_COMMIT`、预编译库和哈希文件，再构建 APK。原生库需要保持 16 KB 页面大小对齐。

核心测试可以单独运行：

```bash
cargo test --manifest-path native/DingooEmu/Cargo.toml --locked -p dingooemu-core -p dingooemu-libretro
```

部分上游测试需要额外的游戏样本，缺少样本时可能跳过实际检查。构建和单元测试通过不代表所有游戏都已经在 Android 真机上验证。

## 存档与日志

1.1.0 的即时存档目录为：

```text
Android/data/io.github.uplush.dingoobox/files/savestates/dingooemu-v4-libretro-v1/<ROM SHA-256>/
```

其中 `auto.state` 是自动存档，`quick.state` 是快速存档，`slot1.state` 至 `slot5.state` 是手动槽。旧核心的存档保留原位，不导入这个目录。游戏内保存的进度仍放在原来的 `saves` 目录。

排查存档失败时，清空日志、复现一次问题，再导出相关标签：

```bash
adb logcat -c
# 在游戏内尝试保存一次
adb logcat -d -s DingooState:I DingooCore:E '*:S'
```

更多细节见 [存档诊断](../SAVE_STATE_DIAGNOSTICS.md)。
