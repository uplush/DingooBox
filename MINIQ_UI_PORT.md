# MiNiQ UI source port

Source repository: `https://github.com/uplush/MiNiQ`

Pinned source commit: `596088d5c684d7990ef27d5f8e49d1897205ece6`

This Android frontend keeps DingooEmu's game model, repositories, native
session and callbacks. The UI below is copied or structurally ported from the
matching MiNiQ source file; only package names, strings, game properties and
callback types are adapted.

| Dingoo file | MiNiQ source |
| --- | --- |
| `CutoutInsets.kt` | `CutoutInsets.kt` (copied; package changed) |
| `MiniQStandardDialog.kt` | `MiniQStandardDialog.kt` (copied; package changed) |
| `HomeScreen.kt` | `HomeScreen.kt` home header, safe-area library/grid, drawer, focus, and the source-structured long-press game dialog |
| `AboutDialog.kt` | `AboutDialog.kt` |
| `SettingsScreen.kt` | `SettingsScreen.kt` header, tabs, rows and selection dialogs |
| `ControllerSettingsScreen.kt` | `ControllerSettingsScreen.kt` header, tabs, rows, profiles, virtual-control selection and `InputBindingDialog.kt` content |
| `ControlSettingsPreferences.kt` | `ControlSettingsScopePreferences.kt` and `ControlProfilePreferences.kt`, adapted to Dingoo mappings and portrait/landscape layouts |
| `VirtualControlEditorMenu.kt` | `VirtualControlEditorMenu.kt` menu, opacity dialog, visibility dialog and edit-mode switching |
| `VirtualControls.kt` | `ControlLayoutEditorScreen.kt`, `LandscapeControls.kt` and `PortraitControls.kt`, adapted to Dingoo control shapes and IDs |
| `PauseMenu.kt` | `PauseMenu.kt` full-screen page container, header, page buttons, action rows and embedded pages |
| `GameSummaryScreen.kt` | `GameSummaryScreen.kt` summary rows and rename dialog |
| `DingooCoverDownloader.kt` | `PokeMiniCoverDownloader.kt` batch state, result accounting, bounded HTTPS download and background-install contract; identity/source adapted to DingooEmu screenshots |
| `AutoSaveResumeDialog.kt` | `AutoSaveResumeDialog.kt` |
| `SaveStateScreen.kt` | `SaveStateScreen.kt` full-screen save/load slot page |
| `SaveStateManagerScreen.kt` | `SaveStateManagerScreen.kt` header, cards and delete dialog |
| `DingooApp.kt` | `MainActivity.kt` safe-drawing `Scaffold` and top `innerPadding` arrangement |

The Rust `arm64-v8a` DingooEmu core remains prebuilt. Starting with alpha17,
the small C++ JNI frontend is compiled from `libretro_frontend.cpp` so native
save-state diagnostics and lifecycle fixes cannot drift from the checked-in
source.

MiNiQ's `ControlId` contains only emulated core inputs and has no virtual
Pause/Menu entry. Dingoo follows the same boundary: its screen controls expose
the Dingoo core buttons, while PauseMenu is opened by Android Back or the
separately configurable physical Pause/Menu hotkey. Obsolete `Pause_*` layout
keys from earlier Dingoo builds are ignored when existing preferences are
loaded.

MiNiQ's `GamePerformanceOverlay` is also retained as the display contract for
FPS and emulation speed: top-right safe-area placement, a single ` | `-joined
line, white monospace label text and a 2 px black shadow without a background
panel. Dingoo uses the same format and visibility rules while calculating
speed against the Dingoo core's 60 FPS target instead of PokeMini's 72 FPS.

## UI runtime contract

The dialog position is determined by more than its composable tree. To keep
MiNiQ's activity and dialog coordinate systems identical, this port also uses
the following MiNiQ runtime inputs:

| Runtime input | Ported value/behavior |
| --- | --- |
| Compose BOM | `2026.02.01` |
| `activity-compose` | `1.8.0` |
| Android platform | `compileSdk { version = release(37) }`, `targetSdk 37` |
| Android Gradle Plugin | `9.3.1`, including built-in Kotlin support |
| Kotlin Compose plugin | `2.2.10` |
| Gradle wrapper | `9.5.0` |
| Native Android theme | `android:Theme.Material.Light.NoActionBar` |
| Activity window | edge-to-edge, `decorFitsSystemWindows=false`, short-edge display cutout |
| System bars | MiNiQ background color, stable/full-screen/hide-navigation layout flags |
| Material theme | MiNiQ light/dark color schemes and `Typography` |

The build stack now matches MiNiQ as well. AGP 9.3.1's new SDK DSL resolves the
Android 17 platform directory named `android-37.0`; older AGP versions instead
look for the legacy `android-37` directory and cannot build against this SDK.

## Home long-press menu adaptation

The dialog geometry and scrolling chain are copied from MiNiQ's
`HomeScreen.kt`: `Dialog(usePlatformDefaultWidth=false,
decorFitsSystemWindows=false)`, a centered full-size safe-area box, 84%/78%
portrait/landscape width, a 28 dp surface, and a
`weight(fill=false).verticalScroll(...)` action column. Its five actions and
order also match MiNiQ. `读取存档` now opens the same source-derived save-state
manager as the drawer, filtered to the selected game, and starts the core with
the selected state already loaded.

The list/grid selector follows the same MiNiQ persistence contract. Its
saveable Compose state is initialized from `home_sort/grid_view_enabled`, and
every toggle is written immediately to the file-backed preference repository.
The last selected library layout therefore survives page navigation and a full
application process restart.

The drawer also uses MiNiQ's localized labels, icons, group order and divider
positions. Its active sort row displays the source labels (`按标题排序`, `按最近游玩排序`,
`按游玩时间排序` or `按游戏大小排序`), and the selection is stored under
`home_sort/sort_mode`. Dingoo maps MiNiQ's size option to the imported `.app`
file size and retains title ordering as the tie-breaker.

## Alpha26 cover download adapter

MiNiQ can identify Pokemon Mini ROMs through CRC32, cartridge headers and the
Libretro No-Intro/Named_Boxarts catalogs. Dingoo `.app` executables do not have
an equivalent public thumbnail database, and broad title matching would map
common names to unrelated systems. Dingoo therefore preserves MiNiQ's batch
orchestration while replacing only the platform identity/source layer.

`DingooCoverDownloader` uses the filenames, English/Chinese names and verified
screenshots listed by DingooEmu's `docs/Game-Compatibility.md`. Exact build
suffixes select variant screenshots first; conservative aliases cover legacy
pack names. Missing entries remain not-found and continue to support the local
cover picker. Existing covers are skipped. Downloaded bytes are bounded,
decoded and staged before `GameRepository` installs them, and the home drawer
retains MiNiQ's progress label and four-count completion Toast.

## PauseMenu invocation contract

MiNiQ's PauseMenu geometry depends on code in both `PauseMenu.kt` and its
`MainActivity.kt` call site. The Dingoo port therefore also applies the source
call-site behavior: a separate status-bar-height 92% surface scrim,
`statusBarsPadding()` on the menu, a 4 dp blur on the underlying game, and
150/120 ms linear fade transitions. System bars are restored while paused so
the status-bar inset remains available when gameplay uses immersive mode.

The Dingoo invocation is outside MiNiQ's original parent `Scaffold`, so the
PauseMenu root surface explicitly provides `onSurface` as its content color.
The game title and action labels retain MiNiQ's source-level explicit
`onSurface` colors; this keeps all four PauseMenu pages legible in dark mode.

## Alpha15 settings foundation

The following MiNiQ infrastructure files are now source-copied with only the
package name and Dingoo naming adapted:

- `AppLanguage.kt`
- `AppThemeMode.kt`
- `ScreenOrientationMode.kt`
- `PokeMiniUserDirectory.kt` -> `DingooUserDirectory.kt`
- `PokeMiniFilePreferences.kt` -> `DingooFilePreferences.kt`
- `PokeMiniPreferenceRepository.kt` -> `DingooPreferenceRepository.kt`

Both source string tables are copied in full. Platform-specific occurrences of
Pokémon Mini, PokeMini, `.min`, and the MiNiQ application name are replaced by
DingooEmu/Dingoo A320/`.app` wording. `AppPreferences` provides a compatibility
facade for the current runtime and migrates alpha14 keys into the source-style
atomic JSON store without deleting the rollback XML.

## Alpha16 save-state surfaces

MiNiQ's `SaveStateScreen.kt`, `SaveStateManagerScreen.kt` and
`AutoSaveResumeDialog.kt` layouts are ported as separate source-shaped files.
The in-game save and load actions share the full-screen slot list, while the
home game action and drawer manager share the managed-state list with an
optional game filter. Dingoo's native file callbacks remain the adapter layer.

The slot model now matches MiNiQ: auto-save, quick-save, and five manual slots.
Only `quick.state` is new; the existing `auto.state` and `slot1.state` through
`slot5.state` paths are unchanged, so no state-file migration is required.

## Alpha17 save-state diagnostics

MiNiQ supplies the UI and state-manager structure, but DingooEmu supplies a
different libretro serializer. The frontend now implements the core log
interface and forwards its exact errors to Android's `DingooCore` Logcat tag.
The surrounding JNI records each save stage under `DingooState`. Auto-save
previews are committed only after the corresponding native state succeeds.

## Alpha19 settings surface

`SettingsScreen.kt` now follows the pinned MiNiQ source structure rather than
the earlier compact approximation: the general, display, audio and advanced
tabs use the same rows, descriptions, dividers, percentage controls, disabled
states and scrollable `MiniQStandardDialog` option layout. Preference values
for aspect ratio, image filter and fast-forward use the
same numeric encodings as MiNiQ; schema-19 migration preserves the visual
meaning of values written by alpha15-alpha18.

Dingoo adapters apply fill/stretch geometry, dot-matrix/scanline/smoothing
filters, audio softening and unlimited fast-forward at runtime. The
Pokemon Mini-specific LCD appearance selector is intentionally omitted because
DingooEmu has no equivalent display mode.
MiNiQ's piezo-filter call is PokeMini-specific and has no DingooEmu equivalent
yet, so the original-sound control is intentionally omitted instead of exposing
a setting without a runtime effect. The PokeMini-specific screen-shake control
is intentionally omitted.

## Alpha22 home focus contract

The home screen now keeps MiNiQ's drawer focus hand-off and adds an explicit
visibility boundary around every drawer item. While the drawer is closed, its
off-screen content cannot participate in Compose spatial focus search. The
single-column library also cancels horizontal focus escape, the drawer cancels
movement above its first item and below its last item, and closing search
returns focus to the search button. Other screens were checked for the same
failure pattern; their inactive pages are conditionally removed or hosted in a
separate Android Dialog focus window.

## Alpha23 auto-save launch gate

MiNiQ loads the selected ROM before presenting its auto-save prompt so state
data can be applied, but it does not start `EmulationRunner` until the user
chooses a launch mode. Its `showingGame` state remains false and the dialog is
hosted by `MainActivity`, so the game library stays visible behind the prompt
instead of the black game surface.

Dingoo keeps the selected game pending in `DingooApp` for the same visible-state
contract. It initializes the native session only after clean boot or load is
chosen, then applies a selected auto-save before starting the first frame.
Dismiss clears the pending request and leaves the library active. Consequently
no frame, audio, viewport, virtual control, play-time accounting or lifecycle
auto-save begins while the prompt is visible.

## Alpha24 transient feedback contract

MiNiQ sends ordinary operation feedback through Android's platform short Toast
without custom gravity or a custom view. Dingoo now routes home-library,
save-state, controller-profile, auto-save and emulation feedback through the
same single call. The previous home `SnackbarHost` and game-screen status card
are removed, preventing different positions and durations between screens.

MiNiQ treats screenshot feedback separately from ordinary Toasts. Its
`GameScreenshotMessage` is ported with the source inverse colors, camera icon,
14 dp shape, 150/120 ms fade timings and 1.8 second lifetime, including the
source portrait and landscape bottom offsets.

### Alpha24 launch rendering and audio correction

The first alpha24 overlay exposed a Compose boundary that MiNiQ avoids through
its observable `showingGame` state: Dingoo's native `running` field changed
after composition but did not schedule a viewport recompose. Dingoo now mirrors
that observable boundary with `sessionStarted`, updated immediately after the
runner starts. The first game frame is therefore composed for load-auto-save,
clean-boot and direct-launch paths without a PauseMenu round trip.

MiNiQ's `GameAudioFocus.kt` is also package-adapted. The Dingoo session requests
focus before start/resume, abandons it on activity pause and teardown, and
reacquires it on resume. The `AudioTrack` and native runner remain Dingoo's
adapter layer.

Alpha24 fix02 aligns that adapter's `AudioAttributes.USAGE_MEDIA` with MiNiQ's
focus request and adds dead-track recovery. The `DingooAudio` tag reports only
state transitions and the first buffer/write, keeping normal Logcat output
quiet while separating core PCM generation from Android playback failures.

## Alpha25 PauseMenu focus contract

MiNiQ requests keyboard/gamepad input mode after the PauseMenu opening delay,
focuses the close button and conditionally composes only the selected page.
Dingoo keeps that source entry contract and supplies an explicit focus graph
for the controls that share the full-screen overlay.

The four page buttons and close button form one bounded header row. The top
edge of each embedded page returns to its own header button; the pause actions,
game-summary title row and full-width setting rows cancel horizontal escape.
App/controller sub-tabs keep normal left/right navigation but stop at their
first and last visible tab. Percentage decrement/increment buttons keep their
pair navigation. These guards are provided only inside PauseMenu, so the
standalone settings and controller screens retain their existing traversal.

Save/load replaces PauseMenu in composition, matching MiNiQ's screen routing.
On return, the menu is created again and the standard delayed close-button
focus request runs; no requester belonging to the removed slot page survives.

### Alpha25 fix01 binding-dialog focus restoration

MiNiQ does not use its binding revision as a Compose `key` for the complete
controller-settings subtree. It keeps every mapping row alive and uses
`remember(bindingRefreshRevision, localBindingRevision, action)` only to reread
the displayed binding value. The separate binding Dialog can then return focus
to the unchanged row that opened it.

Dingoo now follows the same structure for button mappings and hotkeys. A local
revision refreshes binding descriptions, including other rows cleared by
duplicate-binding resolution, without replacing the scroll container, tabs or
focus targets.

Input conflict resolution also follows MiNiQ's scope boundary. Global button
mappings and global hotkeys still resolve duplicate physical inputs, while a
game-specific mapping may modify only that game's independent profile and may
never remove a global hotkey.
