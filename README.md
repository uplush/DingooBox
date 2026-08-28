# 丁果盒 / DingooBox

丁果盒（DingooBox）是一款面向 Android 的 Dingoo A320 游戏前端，基于
[AloysHF/DingooEmu](https://github.com/AloysHF/DingooEmu) libretro 核心构建。
界面使用 Jetpack Compose 和 Material 3，原生桥接层通过 JNI 驱动模拟核心。

> 当前版本：`1.0.0`
>
> Android 包名：`io.github.uplush.dingoobox`

## 主要功能

- 通过系统文件选择器批量添加 Dingoo `.app` 游戏，并直接在原文件夹运行，
  兼容依赖同目录资源文件的游戏
- MiNiQ 游戏库主页、固定抽屉、搜索、列表/网格切换和四种排序
- 游戏封面、基于 DingooEmu 验证截图库的批量封面下载、游戏摘要、最近游玩和累计时长数据库
- 长按游戏菜单与 MiNiQ 一致：开始游戏、读取存档、游戏摘要、选择封面图片、创建快捷方式
- 常规、显示、音频、高级四页应用设置
- MiNiQ 文件化设置仓库、简体中文/英文运行时切换和旧版设置迁移
- 与 MiNiQ 统一的设置、触摸控制、映射、快捷键四页控制设置
- 实体手柄按键/摇杆轴捕获、全局/按游戏控制方案及命名方案保存与载入
- MiNiQ 式横屏和竖屏虚拟按键编辑器（网格吸附位置、单键大小、透明度、可见性）
- 320×240 RGB565 画面和 22,050 Hz 立体声音频
- 屏幕方向键、A/B/X/Y、L/R、开始和选择键
- Android 实体手柄按键支持
- MiNiQ 全屏四页 PauseMenu：暂停菜单、游戏信息、应用设置、控制设置
- MiNiQ 全屏保存/读取页、快速存档、五个手动槽、自动续玩提示及统一存档管理器
- 快进、截图、重启、沉浸式全屏、比例/过滤、FPS/速度叠加
- 仅包含 `arm64-v8a`，原生库按 16 KB 页面大小对齐

## 支持环境

- Android 7.0（API 24）或更高版本
- 64 位 ARM Android 设备
- 构建：Android Studio 自带 JBR 21、Android SDK Platform 37.0、Gradle 9.5.0
- alpha17 起构建 JNI：NDK 28.2.13676358、CMake 3.22.1
- 重新编译模拟器核心：Rust stable、`aarch64-linux-android` target、`cargo-ndk`

## 构建

仓库包含供 `arm64-v8a` 使用的预编译 `libdingooemu.so` 和
`libc++_shared.so`。Gradle 会使用 NDK/CMake 编译轻量 JNI 前端，常规构建
不需要重新编译 Rust 模拟器核心：

```bash
./gradlew assembleDebug
```

生成文件位于 `app/build/outputs/apk/debug/app-debug.apk`。Debug 与 Release
构建使用相同的正式包名，不再追加 `.debug`。

存档失败诊断：清空日志、复现一次失败，再导出两个原生标签：

```powershell
adb logcat -c
# 在游戏内尝试保存一次
adb logcat -d -s DingooState:I DingooCore:E *:S
```

## 重新构建 DingooEmu 核心

Linux/macOS：

```bash
rustup target add aarch64-linux-android
cargo install cargo-ndk
ANDROID_NDK_HOME=/path/to/ndk/28.2.13676358 ./scripts/build-core.sh
```

Windows PowerShell：

```powershell
rustup target add aarch64-linux-android
cargo install cargo-ndk
$env:ANDROID_NDK_HOME = "C:\path\to\ndk\28.2.13676358"
.\scripts\build-core.ps1
```

然后运行 `./gradlew assembleDebug`（Windows 使用 `.\gradlew.bat assembleDebug`）。

## 已知限制

- 在线封面下载仅匹配 DingooEmu 上游验证截图目录中的 `.app` 游戏；目录外游戏仍可从长按菜单选择本地图片。
- 语言切换、显示滤镜和声音柔化已经连接运行时；DingooEmu 没有等价
  核心接口的 Pokémon Mini 液晶模式、原机音效和屏幕震动设置不会显示。
- 本仓库不提供签名密钥。自行构建的 Debug APK 使用 Android 开发调试证书；
  正式分发 APK 时请使用自己的 Release 签名。

## 来源与授权

- DingooEmu 原作者：[@AloysHF](https://github.com/AloysHF)
- DingooEmu 原项目：[AloysHF/DingooEmu](https://github.com/AloysHF/DingooEmu)
- 原作者已明确表示欢迎二创，并要求保留署名和原项目地址。
- Dingoo（丁果科技）、Dingoo A320 及相关名称和商标归各自权利人所有。

界面与交互架构基于 [uplush/MiNiQ](https://github.com/uplush/MiNiQ) 源码适配，
锁定提交 `596088d5c684d7990ef27d5f8e49d1897205ece6`。逐文件来源与 Dingoo 适配点
见 [`MINIQ_UI_PORT.md`](MINIQ_UI_PORT.md)。

## 许可证

Android 前端使用 [BSD 3-Clause License](LICENSE)。内置 DingooEmu 源码及其
第三方组件保留各自许可证，详见 [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md)、
[`native/DingooEmu/LICENSE`](native/DingooEmu/LICENSE) 和
[`native/DingooEmu/THIRD_PARTY_LICENSES.md`](native/DingooEmu/THIRD_PARTY_LICENSES.md)。
