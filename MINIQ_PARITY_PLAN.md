# MiNiQ one-to-one parity plan

Source baseline: `uplush/MiNiQ` commit
`596088d5c684d7990ef27d5f8e49d1897205ece6`.

The verified DingooEmu JNI/video/audio/input/save path remains the protected
runtime baseline. Shared UI and behavior are copied from MiNiQ; `.app` files,
Dingoo controls, 320x240 video and core capabilities are handled by explicit
platform adapters.

| Milestone | Scope | Status |
| --- | --- | --- |
| alpha14 | PauseMenu geometry, invocation and dark-theme content colors | Complete |
| alpha15 | Languages, resources, theme/orientation models and JSON settings foundation | Complete |
| alpha16 | Save/load slot screen, save manager and auto-save resume flow | Complete |
| alpha17 | Native save-state diagnostics and reliable auto-preview commit | Complete |
| alpha18 | Core decoded-state limit repair and exact serializer sizing errors | Complete |
| alpha19 | Full source SettingsScreen layout, descriptions, dependencies and dialogs | Complete |
| alpha20 | Input bindings, axes, hotkeys, focus and control profiles | Complete |
| alpha21 | Virtual controls, selection and layout editors | Complete |
| alpha22 | Home, drawer and game-library gamepad focus boundaries | Complete |
| alpha23 | Auto-save launch gate, in-game orchestration and lifecycle | Ready for device verification |
| alpha24 | MiNiQ Toast format, launch rendering and Android audio output | Complete |
| alpha25 | PauseMenu header, embedded-page and directional focus boundaries | Ready for device verification |
| alpha26 | Dingoo screenshot cover download, progress state, safe cache install and library refresh | Ready for device verification |
| beta01 | Upgrade migration, golden-layout checks and full device regression | Planned |

## Parity gates

- No user-visible text hard-coded in newly ported UI.
- Common layout values, animation specifications and focus transitions match
  the pinned MiNiQ source.
- Every visible setting persists and has a verified runtime effect.
- Upgrading from alpha14 preserves the game library, covers, play metadata,
  save states, bindings and virtual-control layouts.
- Each milestone produces a buildable source package and a rollback point.
