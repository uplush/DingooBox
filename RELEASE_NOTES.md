# 1.0.0

- Initial public release of DingooBox for Android.
- Set the formal application ID to `io.github.uplush.dingoobox` without a
  debug-only package suffix.
- Included the Dingoo A320 game library, original-folder execution, physical
  and virtual controller support, save-state management, shortcuts, cover
  management, localization and MiNiQ-aligned navigation.
- Retained DingooEmu author attribution, the original project address,
  permission statement and third-party license notices.

# 0.2.0-alpha26 fix12

- Restored controller navigation on the in-game save-state, load-state and
  save-state manager pages by following MiNiQ's input-mode and initial-focus
  handoff.
- Bounded directional focus to the visible page, retained a usable Back action
  on empty pages, and restored focus after deleting a state.
- Made the manager delete dialog focus `No` initially, support explicit
  Left/Right navigation, and restore page focus when it closes.

# 0.2.0-alpha26 fix11

- Added `Dingoo（丁果科技）` to the About rights statement.
- Added DingooEmu original-author attribution for `@AloysHF`, the permission
  statement, and a focusable link to `https://github.com/AloysHF/DingooEmu`.

# 0.2.0-alpha26 fix10

- Renamed the application to `丁果盒 / DingooBox` while preserving the formal
  package ID `io.github.uplush.dingoobox`.

# 0.2.0-alpha26 fix09

- Restored the complete game path in the summary page instead of truncating it.
- Prevented a controller-confirm long press in the home library from also
  launching the selected game when the long-press menu opens.

# 0.2.0-alpha26 fix08 r2

- Fixed the white screen after launching games from their original folders by
  preserving Android document access and passing the resolved game directory
  consistently to the native core.

# 0.2.0-alpha26 fix08

- Changed game-library entries to run `.app` files from their selected original
  folders instead of copying only the executable into app-private storage.
- Preserved access to sibling resource files required by some Dingoo games.

# 0.2.0-alpha26 fix07

- Renamed the Android namespace and application ID to
  `io.github.uplush.dingoobox`.

# 0.2.0-alpha26 fix06

- Paused emulation and released all active inputs while the virtual-control
  layout editor is visible.
- Blocked physical buttons, joystick axes and underlying virtual controls from
  reaching the core during editing, and removed the game-frame flash when
  returning to PauseMenu.

# 0.2.0-alpha26 fix05 r4

- Updated the A/B/X/Y virtual-button palette in both gameplay and the layout
  editor: A `#946190`, B `#7B9ACB`, X `#4B6B6C`, and Y `#6F2835`.
- Kept all r3 portrait viewport, non-overlapping system-row, symmetry and
  default-layout migration behavior unchanged.

# 0.2.0-alpha26 fix05 r3

- Moved the portrait L/R buttons below the 4:3 game viewport. Their default
  row is calculated from the viewport's actual lower edge with a 16 dp gap, so
  controls no longer overlap the game image on different portrait heights.
- Moved portrait Select/Start upward onto the same dedicated row as L/R. The
  row is ordered L, Select, Start, R and leaves the lower area for the D-pad
  and A/B/X/Y clusters.
- Made Select and Start strictly symmetric in both orientations: Select uses
  x=0.42 and Start x=0.58 with an identical y coordinate and identical size.
- Advanced the default-layout migration to version 3 so untouched fix05/r2
  layouts adopt the new portrait row, while genuinely customized layouts stay
  unchanged.

# 0.2.0-alpha26 fix05 r2

- Replaced the face-button group's percentage-based spacing with the exact
  MiNiQ geometry. A/B/X/Y now sit 57 dp from their shared center, matching the
  center offset of each 56 dp direction button inside the 170 dp D-pad.
- Fixed the layout editor retaining the orientation captured when it opened.
  It now follows the current `LocalConfiguration` and reloads the matching
  portrait or landscape preferences immediately after a rotation.
- Kept custom button coordinates untouched. Dynamic symmetric positioning is
  applied only while A/B/X/Y retain their current default coordinates.

# 0.2.0-alpha26 fix05

- Matched MiNiQ's portrait game viewport: the native 4:3 frame now fills the
  portrait width and is centered at 50% horizontally / 36% vertically instead
  of being vertically centered in the whole screen.
- Matched MiNiQ's virtual-control sizing. The D-pad uses a 170 dp layout box
  with four 56 dp direction buttons, equal to the default 56 dp A/B/X/Y size,
  in both gameplay and the layout editor.
- Reworked the portrait and landscape defaults so the A/B/X/Y cluster is a
  horizontal mirror of the D-pad cluster. The two groups now share the same
  center height and equivalent outer bounds on common 20:9 displays.
- Added a non-destructive default-layout migration. Saved layouts that still
  exactly match the previous defaults adopt the new MiNiQ-aligned positions;
  genuinely customized positions, visibility, opacity and per-control sizes
  remain unchanged. Legacy default positions in named profiles are migrated
  when those profiles are loaded.

# 0.2.0-alpha26 fix04

- Rebuilt the adaptive and Android 13 monochrome launcher foreground from the
  corrected five-layer PSD. The black base, white face/screen and colored
  button layers are now composited in their real Photoshop order instead of
  treating an intermediate layer as the complete handheld artwork.
- Matched MiNiQ's pinned-shortcut launch contract: shortcut intents now use a
  stable game URI together with `CLEAR_TOP`, `SINGLE_TOP` and read-grant flags,
  and are consumed once so activity recreation cannot replay a game launch.
- Preserved existing alpha26 shortcuts through the legacy path extra while new
  shortcuts resolve by the stable library game ID. A removed game now produces
  a localized Toast instead of silently doing nothing.
- Prevented a shortcut tap from replacing an already running game or an open
  auto-save launch dialog. The current session remains untouched and MiNiQ's
  `A game is already running` Toast is shown.
- Completed shortcut activity ownership: dismissing the auto-save prompt or
  leaving a game launched from the home screen shortcut now closes that
  shortcut activity, while ordinary library launches still return home.
- Switched shortcut creation to AndroidX's MiNiQ-compatible builder and system
  pinning prompt, removed the duplicate request Toast, and updated the fallback
  shortcut icon to the new mipmap launcher resource.

# 0.2.0-alpha26 fix03

- Replaced the temporary vector application icon with the supplied DingooEmu
  artwork while preserving the PSD's gray gradient and centered handheld
  design.
- Added legacy launcher bitmaps for mdpi through xxxhdpi, a circular fallback,
  and layered adaptive icons so round, squircle and vendor launcher masks keep
  the handheld inside Android's safe area.
- Added an Android 13 monochrome layer for themed icons and moved the manifest
  to the standard mipmap launcher resources, including `roundIcon`.

# 0.2.0-alpha26 fix02

- Completed runtime localization for the home library, game summary, About,
  launch-failure surface, common dialog actions, cover descriptions and touch
  control labels. English mode no longer falls back to hard-coded Chinese
  strings, and play-time/date values follow the selected app locale.
- Removed the visible and persisted original-sound setting because DingooEmu
  has no equivalent to MiNiQ's Pokemon Mini piezo filter. Settings schema 22
  removes the obsolete keys during a non-destructive upgrade.
- Matched MiNiQ's input-scope conflict rules. A game-specific button mapping
  can no longer clear a global hotkey, and editing a global hotkey from an
  in-game settings page cannot erase that game's independent button mapping.

# 0.2.0-alpha26 fix01

- Fixed pinned game shortcuts always using DingooEmu's application icon even
  when the selected game already had a downloaded or manually chosen cover.
- Matched MiNiQ's shortcut icon path: decode the current cover into an Android
  bitmap and pass it to the platform shortcut builder; retain the application
  icon only as the missing/invalid-cover fallback.
- Kept shortcut IDs, labels and game launch intents unchanged. Launchers cache
  pinned icons, so shortcuts created by older builds must be removed and
  recreated to display their covers.

# 0.2.0-alpha26

- Ported MiNiQ's missing-cover batch workflow, including its in-progress drawer
  label, duplicate-tap guard, background execution, library refresh and
  downloaded/existing/not-found/failed Toast summary.
- Replaced MiNiQ's Pokemon Mini-specific Libretro/No-Intro lookup with a Dingoo
  adapter backed by the verified screenshots in DingooEmu's own `docs/images`
  catalog. Strict English, Chinese and legacy filename aliases cover the 39
  compatibility-suite `.app` builds without cross-platform fuzzy matching.
- Added Android Internet permission and a bounded HTTPS downloader with 15/20
  second timeouts, an 8 MiB response ceiling and concise `DingooCover` logs.
- Added decoded-image validation and staged replacement for downloaded files.
  Existing local/manual covers are never overwritten by the batch action, and
  successful files remain in the existing private cover directory.

# 0.2.0-alpha25 fix05

- Replaced the home drawer's hard-coded Chinese labels with the existing MiNiQ
  string resources, preserving correct Simplified Chinese and English text.
  Labels now match MiNiQ, including `按标题排序`, `初始化全部设置` and `存档管理`.
- Matched MiNiQ's drawer icons and grouped order: library/sort; open game;
  application/controller settings; reset/scan/save-state manager/cover action;
  and About, with the same divider positions.
- Replaced Dingoo's `添加时间` sort option with MiNiQ's `游戏大小`. The four
  options are now title, recently played, play time and game size; size sorting
  is descending with title as the stable tie-breaker.
- Persisted `home_sort/sort_mode` through the same file-backed preference used
  for list/grid mode, so the selected ordering survives a process restart.

# 0.2.0-alpha25 fix04

- Fixed the home library returning to list view after the app process was
  restarted. Dingoo's Compose state had always initialized the grid flag to
  `false` even though the MiNiQ-compatible settings repository already
  supported the preference.
- Matched MiNiQ's `HomeScreen` contract: initialize the saveable UI state from
  `home_sort/grid_view_enabled`, then immediately persist every list/grid
  toggle through the file-backed preference editor.
- Localized the layout-toggle accessibility description so it announces the
  action that will be performed: switch to list view or switch to grid view.

# 0.2.0-alpha25 fix03

- Replaced Dingoo's boxed, three-line performance panel with MiNiQ's in-game
  information format: a single top-right line using white monospace label text
  with a 2 px black shadow and no background surface.
- Matched MiNiQ's content and ordering. The overlay now renders
  `FPS: 60.0 | 100%` when both options are enabled, or only the selected value;
  the extra `Dingoo A320 · 320×240` line and localized `速度` prefix are removed.
- Matched MiNiQ's portrait/landscape safe-area offsets and visibility gate. No
  overlay is shown until a valid FPS sample exists, when the master information
  switch is off, or when both detail switches are off.
- Kept Dingoo's native 60 FPS speed baseline while changing its measurement to
  floating-point precision, so the one-decimal FPS presentation is meaningful.

# 0.2.0-alpha25 fix02

- Removed Dingoo's extra on-screen Pause/Menu control from the virtual-control
  model, default portrait/landscape layouts, runtime renderer, layout editor
  and visibility dialogs. This matches MiNiQ, whose virtual controls contain
  only core input buttons.
- Kept PauseMenu access through Android Back and the configurable physical
  controller Pause/Menu hotkey. Removing the screen button does not change
  gamepad or keyboard pause behavior.
- Existing layouts and saved controller profiles remain compatible. Obsolete
  `Pause_*` preference keys may stay on disk but are no longer enumerated,
  rendered or written by the current control model.

# 0.2.0-alpha25 fix01

- Fixed controller focus jumping to the top of the mapping page after a key or
  axis was captured. The Dingoo adapter had wrapped the complete controller
  page in `key(localRevision, ...)`, which destroyed the focused row whenever
  its binding changed.
- Matched MiNiQ's binding refresh model: the controller and hotkey page trees
  now remain stable, while each displayed binding is reread with a local
  revision key. Closing the capture dialog therefore restores focus to the row
  that opened it and keeps the existing scroll position.
- Applied the same stable-row behavior to key capture, joystick-axis capture,
  clearing a binding and duplicate-binding reassignment.

# 0.2.0-alpha25

- Kept MiNiQ's PauseMenu entry contract: gamepad/keyboard input mode is
  requested after the opening transition and initial focus lands on the close
  button, while inactive pages remain out of composition.
- Added explicit left/right neighbours for the four page buttons and close
  button. Focus can no longer leave the visible header at either horizontal
  edge or jump diagonally into a different page's content.
- Unified the content focus boundary for the pause actions, game summary,
  embedded app settings and embedded controller settings. The first tab/row
  returns upward to its matching PauseMenu page button; full-width rows do not
  leak horizontally, while paired percentage controls retain internal
  left/right navigation.
- Returning from save/load or the virtual-control editor recreates PauseMenu
  through the same MiNiQ entry path, so focus is restored predictably instead
  of retaining a removed content target.

# 0.2.0-alpha24 fix02

- Hardened the remaining Android audio-output path after fix01 restored the
  first frame but device playback stayed silent. `AudioTrack` now uses the same
  media usage as MiNiQ's audio-focus request, verifies playback before writes,
  and recreates a dead output track before retrying the current PCM buffer.
- Added concise `DingooAudio` diagnostics for focus grant, configured output
  volume, track creation/playback, the first core PCM buffer and the first
  successful Android write. A 180-frame zero-sample warning distinguishes a
  silent core from a platform output failure without per-frame log spam.

# 0.2.0-alpha24 fix01

- Fixed the first game frame remaining black after either auto-save choice.
  The prepared session now publishes a Compose `sessionStarted` state as soon
  as its runner starts, so the viewport, controls and overlays enter
  composition without waiting for PauseMenu to trigger an unrelated recompose.
- Ported MiNiQ's `GameAudioFocus` request/abandon lifecycle. Audio focus is
  requested before initial start and resume, abandoned while the activity is
  paused or the session is disposed, and reacquired on foreground resume.
- Made `AudioTrack` start and resume explicitly clear the runner's paused flag,
  restoring sound for auto-save load, clean boot and direct launch paths.

# 0.2.0-alpha24

- Unified ordinary transient feedback with MiNiQ's platform
  `Toast.makeText(..., Toast.LENGTH_SHORT)` format across the home library,
  save-state flows, auto-save launch dialog, controller profiles and gameplay.
- Removed Dingoo's competing bottom Snackbar and top Compose status card, so
  position, app icon, typography and duration now follow the same Android
  system Toast presentation used by the pinned MiNiQ source.
- Ported MiNiQ's screenshot-only exception: a 90% inverse-surface message with
  a camera icon, 14 dp corners, 150/120 ms fades and an 1.8 second lifetime.
  It sits 20 dp above the landscape bottom edge and 96 dp above the portrait
  bottom edge.
- Localized the remaining library feedback for game import, cover updates,
  scanning and shortcut creation instead of emitting hard-coded Chinese text.

# 0.2.0-alpha23

- Reordered auto-save startup to match MiNiQ's application-layer launch gate.
  When an auto-save exists, the game library remains rendered behind the
  resume dialog; the black emulation surface is not entered until the user
  chooses clean boot or load auto-save.
- Native initialization, the emulation thread, audio, viewport and play timer
  now start only after that choice. A selected auto-save is still loaded before
  the first emulated frame.
- Dismissing the auto-save dialog now cancels launch and returns to the library
  instead of implicitly starting a clean game. A cancelled launch does not
  update recently played/play time or overwrite the existing auto-save.
- Loading a valid auto-save applies it before the first emulated frame. If the
  load fails, the core resets and starts clean with MiNiQ's failure message.
- Lifecycle pause and teardown save only sessions that actually started, so
  backgrounding or cancelling the pending dialog cannot replace the old state.
- Kept the game viewport, overlays, virtual controls and immersive system bars
  out of composition behind the pending auto-save dialog.

# 0.2.0-alpha22

- Removed the closed navigation drawer from gamepad focus traversal. Its items
  remain focusable throughout opening and closing animations, then become
  unavailable as soon as the drawer is fully hidden.
- Added explicit focus boundaries for the single-column game library and the
  first/last drawer items, preventing directional navigation from escaping to
  hidden or unrelated controls.
- Ported MiNiQ's home-menu left-edge guard, drawer Back handling, search-field
  gamepad cancel keys and search-button focus restoration.
- Audited the remaining settings, controller, save-state, PauseMenu and dialog
  surfaces. They use conditional page composition or separate Dialog focus
  windows and do not retain a permanently hidden drawer-like focus subtree.

# 0.2.0-alpha21

- Replaced the standalone Dingoo virtual-control editor toolbar and bottom
  sliders with MiNiQ's in-canvas edit menu and standard dialogs.
- Ported MiNiQ's two editing modes: long-press and drag positions on an 8 dp
  grid, or horizontally drag an individual control to resize it in 10% steps
  from 70% to 140%.
- The editor now renders the real Dingoo D-pad, A/B/X/Y, L/R, Start, Select and
  Pause control shapes instead of generic circular placeholders, and constrains
  position dragging using each control's actual width and height.
- Added MiNiQ-style opacity, add/remove, reset-layout and exit-editing flows.
  Back and exit save the active orientation as one layout transaction.
- Extended global, per-game and named control profiles with independent scale
  values for every virtual control. Existing alpha20 layouts keep their prior
  overall scale as the non-destructive fallback for each control.
- Entering the in-game editor now closes the PauseMenu while keeping emulation
  paused, then restores the controller-settings PauseMenu after saving.
- Removed the extra square from the center of the D-pad in both gameplay and
  editor previews, restoring MiNiQ's four independent circular direction keys.

# 0.2.0-alpha20

- Replaced the abbreviated controller settings page with MiNiQ's complete
  four-tab structure, localized descriptions, dividers, disabled states and
  standard dialogs while retaining Dingoo's A/B/X/Y, L/R, Start and Select
  actions.
- Added named control-profile save, overwrite and load flows. Profiles include
  active button mappings, portrait/landscape touch layouts, visibility and
  vibration values while keeping system hotkeys global.
- Added the MiNiQ per-game control scope workflow. Enabling a game-specific
  profile first copies the current global mappings and both touch layouts;
  disabling it removes only that game's independent control data.
- Added in-game add/remove virtual-control selection and made layout editing
  target the current screen orientation, matching MiNiQ's availability rules.
- Upgraded physical input bindings from key-only values to key-or-axis records,
  including stick/hat capture, localized binding names, conflict resolution and
  live Dingoo runtime axis dispatch. Existing alpha19 key mappings are read
  without modification and migrate when edited.

# 0.2.0-alpha19

- Removed the Pokemon Mini-specific LCD appearance and screen-shake selectors,
  their saved preferences and the Dingoo frontend grayscale adapter. Theme
  switching is unchanged and still supports system, light and dark appearances.
- Replaced the abbreviated Dingoo settings UI with MiNiQ's complete
  `SettingsScreen` structure: four source tabs, descriptions, dividers,
  percentage controls and scrollable standard selection dialogs.
- Restored MiNiQ's source dependencies: aspect ratio is editable only from the
  landscape in-game PauseMenu, auto-save remains visible but disabled outside
  a game, information-dependent overlays are disabled with their parent, and
  sound softening uses the source 5%-95% range.
- Restored the source option sets for display filters and
  2x/3x/4x/unlimited fast-forward. Added Dingoo
  frontend adapters for display effects, fill/stretch geometry, audio
  softening and unlimited execution.
- Kept MiNiQ's original-sound control and persistence, while documenting that
  DingooEmu has no equivalent for PokeMini's piezo-filter interface yet.
- Added schema-19 preference migration so alpha15-alpha18 aspect-ratio and
  filter values keep their previous visual meaning after adopting MiNiQ's
  numeric encodings.
- Fixed the in-game save/load screen header and back icon inheriting a light
  content color in dark theme while keeping MiNiQ's original geometry.

# 0.2.0-alpha18

- Confirmed from alpha17 device logs that `retro_serialize()` fails because the
  decoded Dingoo snapshot exceeds the core's original 64 MiB ceiling; storage,
  permissions, slot paths and the PauseMenu callback are not the cause.
- Raised the decoded-state safety limit to 128 MiB without changing the version
  3 state schema or the fixed 48 MiB libretro buffer. Existing 48 MiB state
  files remain compatible.
- Added exact decoded and compressed byte counts to future core errors. If a
  particular game also exceeds the compressed 48 MiB capacity, the next log
  identifies that independently instead of reporting a generic failure.
- Suppressed multi-thousand-line JIT information dumps from Android Logcat while
  retaining core warnings/errors and the concise `DingooState` operation log.
- This fix changes the Rust core and therefore requires running
  `scripts/build-core.ps1` before the Android Gradle build.

# 0.2.0-alpha17

- Confirmed from device timestamps that the current native save-state operation
  fails before replacing either manual or auto state files; the previous exit
  path could still replace the preview image and create a false success signal.
- Fixed exit/lifecycle auto-save so its preview is updated only when the native
  state file was written successfully.
- Connected the DingooEmu libretro log interface to Android Logcat. Core errors
  such as an oversized decoded state or fixed-buffer overflow now appear under
  `DingooCore`, while JNI stage, size and destination diagnostics appear under
  `DingooState`.
- Enabled source compilation of the JNI frontend with NDK 28.2.13676358 and
  CMake 3.22.1. The live frontend uses the distinct `dingoo_jni_live` library
  name so an alpha17 archive can safely be extracted over an older project
  without accidentally loading the historical prebuilt JNI binary.

# 0.2.0-alpha16

- Replaced the compact Dingoo save/load picker with MiNiQ's full-screen
  `SaveStateScreen`, including the source card geometry, previews, timestamps,
  empty-slot states, save/load modes, delete actions and safe-area header.
- Replaced both home-library load-state navigation and drawer save management
  with MiNiQ's shared `SaveStateManagerScreen`; the former filters by game while
  the latter shows every manual state.
- Added MiNiQ's dedicated `quick.state` slot and routed physical quick-save and
  quick-load hotkeys to it. Existing `auto.state` and `slot1` through `slot5`
  files remain unchanged and immediately visible.
- Restored MiNiQ's auto-save resume resources, locale-aware timestamp, preview
  layout and initial focus on the load action.
- Converted PauseMenu save/load labels and save-state feedback to the shared
  bilingual resources while preserving the verified native state callbacks.

# 0.2.0-alpha15

- Added MiNiQ's source-level runtime language system, theme/orientation models,
  user-data directory, atomic JSON preferences and preference repository.
- Imported the complete 370-entry English and Simplified Chinese resource sets,
  with Dingoo-specific platform, file-type and legal wording substitutions.
- Added a one-time, non-destructive migration from alpha14 `app_settings` XML
  keys to MiNiQ-compatible JSON keys. The legacy XML remains available for
  rollback while alpha15 reads and writes the new settings file.
- Converted the current application-settings tabs and options to the shared
  bilingual resources; language, theme and orientation changes now recompose
  immediately, including MiNiQ's sensor auto-rotate option.
- Kept the verified DingooEmu JNI, game library, save states, PauseMenu and
  controller runtime unchanged for this infrastructure milestone.

# 0.2.0-alpha14

- Fixed PauseMenu text and inherited icon colors in dark theme by restoring
  MiNiQ's explicit `onSurface` colors for the game title and action labels.
- Propagated the PauseMenu surface content color to the close button and all
  three embedded pages, matching the color context MiNiQ receives from its
  original parent scaffold.

# 0.2.0-alpha13

- Ported MiNiQ's PauseMenu invocation layer: status-bar padding and scrim,
  4 dp background-game blur, and 150/120 ms fade transitions.
- Aligned PauseMenu page titles and all six action labels with MiNiQ's Chinese
  resources while preserving Dingoo callbacks.
- Restored system bars while PauseMenu is open so its safe-area measurement
  matches MiNiQ even when immersive gameplay is enabled.

# 0.2.0-alpha12

- Fixed the MiNiQ system-bar port for Activity 1.8.0 by retaining the concrete
  `ComponentActivity` receiver required by `enableEdgeToEdge()`.

# 0.2.0-alpha11

- Completed the build-layer migration to MiNiQ's AGP 9.3.1, Kotlin Compose
  plugin 2.2.10 and Gradle 9.5.0.
- Switched to AGP 9's `compileSdk { version = release(37) }` DSL so Android 17
  SDK Platform 37.0 is resolved from the installed `android-37.0` directory.
- Removed the obsolete `org.jetbrains.kotlin.android` plugin and migrated to
  AGP 9's built-in Kotlin support.

# 0.2.0-alpha10

- Migrated the complete MiNiQ UI runtime contract instead of only copying
  composables: Compose BOM 2026.02.01, activity-compose 1.8.0, API 37 target,
  the MiNiQ color scheme, typography, platform theme and system-bar styling.
- Kept `MiniQStandardDialog.kt` source-identical except for the package name.
- Replaced the Dingoo-specific long-press actions with MiNiQ's exact five-item
  order and labels, including a functional `读取存档` path into the Dingoo state
  picker.
- Kept Dingoo game data/native callbacks while using MiNiQ's dialog measurement
  and `weight(fill=false) + verticalScroll` behavior from the matching runtime
  stack.

# 0.2.0-alpha09

- Completed the source-faithful MiNiQ window port: short-edge cutout mode,
  edge-to-edge decor fitting, and stable/fullscreen/navigation layout flags are
  applied at the Activity/theme level.
- Removed the non-MiNiQ forced Dialog `MATCH_PARENT`/origin compensation that
  displaced the portrait dialog by one status-bar height.
- Restored MiNiQ's original `weight(fill = false) + verticalScroll` action list.

# 0.2.0-alpha08

- Ported MiNiQ's missing activity cutout/edge-to-edge window configuration.
- Normalized every Compose Dialog window to MATCH_PARENT, centered at `(0, 0)`,
  and enabled short-edge cutout layout. This removes the landscape right/down
  offset and gives scrollable dialog content the correct safe-area viewport.

# 0.2.0-alpha07

- Fixed the home-library long-press action list being clipped in short
  landscape windows. The fixed header stays in place while the action area is
  capped to the remaining safe height and scrolls independently.

# 0.2.0-alpha06

- Replaced the home-library long-press game dialog with MiNiQ's source layout:
  safe-area centering, 78%/84% landscape/portrait width, 28 dp surface,
  64 dp cover header, 46/52 dp action rows, and rounded action icons.
- Kept Dingoo-specific game actions and callbacks unchanged.

# 0.2.0-alpha05

Source-level MiNiQ UI port based on commit
`596088d5c684d7990ef27d5f8e49d1897205ece6`.

- Copied MiNiQ's safe-area and standard-dialog implementations into the
  Dingoo package; Dingoo-specific dialog content now lives in adapters.
- Replaced the nested home `Scaffold` with MiNiQ's outer safe-drawing
  `Scaffold + innerPadding` arrangement, fixing clipped 64 dp headers.
- Ported MiNiQ's symmetric cutout layout for the home library, drawer and
  full-screen PauseMenu, including drawer focus restoration.
- Ported custom About, auto-save resume, input binding, virtual-control
  selection, game summary and save-state layouts instead of Material 3
  default dialogs/list rows.
- Preserved the verified Dingoo JNI, video/audio, virtual/physical input,
  save/load and return-to-library paths.

# 0.2.0-alpha04

MiNiQ layout calibration based on device screenshots.

- Replaced Material 3 `TopAppBar` usage with MiNiQ's custom 64 dp page headers,
  fixing clipped titles in portrait and landscape.
- Added MiNiQ's standard dialog geometry: 84% portrait width, 78% landscape
  width, 28 dp corners, symmetric safe-area centering, internal scrolling, and
  outside-tap dismissal.
- Applied the custom dialog format to game actions, sorting, reset/delete
  confirmations, settings selection, key capture, save states, auto-resume,
  control visibility, and About.
- Replaced default settings/controller rows with MiNiQ-style spacing, text
  hierarchy, trailing values, switches, and percentage controls.
- Integrated search into the home title row and completed the MiNiQ drawer
  order, including reset settings.

# 0.2.0-alpha03

Device-test hotfix based on MiNiQ main commit
`596088d5c684d7990ef27d5f8e49d1897205ece6`.

- Restored the missing ARM64 JNI wrapper and its C++ runtime dependency, so
  selecting a game can enter the emulator instead of failing at library load.
- Matched MiNiQ's home layout: compact list default, 160 dp adaptive grid,
  fixed 110 dp cover height, library/count header, 20 dp page margins, and no
  oversized floating add button.
- Kept every bundled native library 16 KB page aligned.

This remains an alpha build intended for device testing.

# 0.2.0-alpha02

MiNiQ full-interface initial port.

- Rebuilt the home game database, fixed-order navigation drawer, search, sort,
  grid/list library, cover picker, game summary, shortcuts, and play history.
- Added four-tab app settings and four-tab controller settings.
- Added physical key/hotkey capture and persistent per-game control profiles.
- Added draggable portrait/landscape virtual-control editors.
- Replaced the basic dialog with MiNiQ's full-screen four-page pause menu.
- Added five manual state slots, auto-save/resume, state manager, screenshots,
  fast-forward, display filters, volume, FPS, and speed overlays.

This is still an alpha build intended for device testing.

# 0.1.0-alpha01

Initial Android preview build.

- Added a standalone Jetpack Compose game library and settings UI.
- Added Android document-picker import for Dingoo `.app` files.
- Added JNI/libretro video, audio, input, reset, and save-state integration.
- Added touch controls, physical gamepad keys, and a pause menu.
- Added arm64-only packaging with 16 KB aligned native libraries.

This build is intended for early device testing. Games that load assets from a
companion directory are not yet supported by the importer.
