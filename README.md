# 丁果盒 / DingooBox

在 Android 上玩丁果 A320 的原生 `.app` 游戏。

模拟核心来自 [DingooEmu](https://github.com/AloysHF/DingooEmu)。丁果盒在它的基础上做了 Android 界面，负责游戏库、触屏按键、手柄和存档这些日常使用的部分。

## 下载和使用

到 [Releases](https://github.com/uplush/DingooBox/releases/latest) 下载 APK，安装后选择「添加游戏目录」，找到存放 `.app` 游戏的文件夹。也可以单独打开一个游戏文件。

需要 **Android 7.0 或更高版本，以及 64 位 ARM 设备**。游戏文件需要自行准备；这里支持的是 A320 原生游戏，不是 Android 的 `.apk` 安装包。

## 玩起来有哪些功能

- 游戏库可以搜索、排序，在列表和网格之间切换。长按游戏可以换封面、查看游玩记录或创建桌面快捷方式。
- 可以用屏幕按键，也可以接手柄。触屏布局能调整位置、大小和透明度，手柄按键可以重新映射，并为不同游戏保存配置。
- 除了游戏本身的存档，还有五个手动即时存档槽、快速存档和退出时自动存档。
- 游戏中支持快进、截图和重启，也能调整画面比例、过滤方式，或显示 FPS。
- 界面有简体中文和英文。封面可以手动选择，也可以批量下载；自动下载只覆盖 DingooEmu 截图库里能匹配到的游戏。

目前前端仍按 A320 的 320×240 横向画面运行，还没有接入上游的 A330、核心画面旋转和异步音频功能。不同游戏的兼容性和速度可能有差异，遇到问题欢迎在 [Issues](https://github.com/uplush/DingooBox/issues) 留下游戏名、手机型号、系统版本和复现步骤。

## 从旧版升级

仓库当前源码版本为 **1.1.0**，已发布的安装包以 Releases 页面为准。

这一版改用官方 DingooEmu 核心，不再附加之前分支的补丁，因此也移除了旧版的 Extreme Drift 帧率增强开关。核心固定在一个经过构建测试的提交，不会在每次编译时自动追踪最新版本；具体提交见 [UPSTREAM_COMMIT](native/UPSTREAM_COMMIT)。

**旧版的即时存档不能在新核心中继续读取。** 手动、快速和自动存档都会留在原处，新版另用一个目录保存，不会覆盖旧文件。检测到旧存档时，应用会弹出说明。这和游戏菜单里的「保存进度」不是一回事：后者仍使用原来的 `saves` 目录。

如果还需要用旧核心读档，请先保留旧版安装包并备份应用数据。不要为了降级直接卸载应用，以免丢失数据。完整改动见 [更新记录](RELEASE_NOTES.md)。

## 和天马 G 等前端一起用

建议先在丁果盒里添加游戏目录，让应用取得目录访问权限，再从外部前端启动游戏。

支持 `VIEW` URI，例如：

```text
am start -n io.github.uplush.dingoobox/.MainActivity -a android.intent.action.VIEW -d "file://{file.path}"
```

也支持天马 G 常用的 `ROM` 参数：

```text
am start -n io.github.uplush.dingoobox/.MainActivity -e ROM "{file.path}"
```

`{file.path}` 由前端替换成游戏路径，文件扩展名设为 `app`。此外也接受 `rom`、`path`、`game_path` 参数和 `content://` URI。由外部前端启动时，退出游戏会返回调用它的前端。

## 自己编译

仓库带有预编译的 ARM64 核心，普通 APK 构建不需要先安装 Rust。准备好 Android Studio 的 JDK、Android SDK 37、NDK 28.2.13676358 和 CMake 3.22.1 后：

```bash
git clone --recurse-submodules https://github.com/uplush/DingooBox.git
cd DingooBox
./gradlew assembleDebug
```

Windows 下最后一条命令换成 `.\gradlew.bat assembleDebug`，工程请放在不含中文等非 ASCII 字符的短路径中。APK 会生成在 `app/build/outputs/apk/debug/app-debug.apk`。

重新编译核心、运行测试和签名的说明放在 [构建文档](docs/BUILDING.md)。遇到存档失败，可以参考 [存档诊断](SAVE_STATE_DIAGNOSTICS.md) 导出日志。

## 感谢与许可

感谢 [AloysHF](https://github.com/AloysHF) 开发 [DingooEmu](https://github.com/AloysHF/DingooEmu)，丁果盒的模拟能力来自这个项目。Android 界面和交互从 [MiNiQ](https://github.com/uplush/MiNiQ) 适配而来，具体来源记录在 [MINIQ_UI_PORT.md](MINIQ_UI_PORT.md)。

Android 前端使用 [BSD 3-Clause License](LICENSE)。DingooEmu 及其依赖保留各自的许可证，详见 [第三方声明](THIRD_PARTY_NOTICES.md)、[核心许可证](native/DingooEmu/LICENSE) 和 [核心第三方许可](native/DingooEmu/THIRD_PARTY_LICENSES.md)。Dingoo、丁果 A320 等名称及商标归各自权利人所有。
