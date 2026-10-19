# Version 0.4.0 checks

## Gameplay

- 672 campaign games across seven delayed, mistake-prone policies; every map had a winning line. All first five lessons were won on both seeds by every policy.
- 224 hard random games, with four map seeds and starting-position swaps.
- 84 additional games on the three narrowest puzzles. Each map had a winning policy on every additional error seed.
- 3,367 campaign data assertions covering 48 imports, export/import round trips, connectivity, touch spacing, objectives, lesson restrictions, editor routes, haptics and progression.
- 79 deterministic tactical assertions, including regenerating one-/two-gun sieges, economy preservation, empty-target avoidance, gun defence and pact acceptance limits; eight complete mixed-AI games across four modes.

The [results](balance-metrics.md) include losses and time limits. These scripted policies do not establish human enjoyment.

## Native interface

Checked at 480 × 800 and 390 × 844:

- Keyboard and touch navigation, tactical pause, half sends, upgrades and context restoration.
- Day/Night settings, backgrounds and chapter boundaries.
- AI selection, markers and 30,044 AI assertions at each size.
- Speed stops, touch dragging, event throttling and saved speed.
- Buying/placing bombs, drawing/funding routes, artillery targeting, pause/reload, pacts and victory controls.
- Maps → details → editor; exact garrisons, mode/route controls and bomb stock.
- All new-concept briefings, optional hint, first capture, saved victory, Next, defeat without unlocking the next lesson, and final campaign completion.
- Rendered all 48 boards at both sizes. Small-board framing keeps nodes below the HUD; a native assertion checks this for every board.

General regression suites passed before the final campaign-navigation change. The complete campaign tour and data checks were then repeated against the final frozen source and packaged binaries. Production files outside that campaign navigation change remained identical. The final 672-game sweep reproduced its CSV byte for byte.

## Android

Android 15 emulator with the Pixel 8a display profile, 1080 × 2400 at 420 dpi:

- Updated the existing 0.3.0 installation to versionCode 4 / 0.4.0; prior settings and speed matched before first launch.
- Used actual Android touch input for the first briefing, troop order and capture, Back-to-pause, hint, Maps, map details, editor mode/route controls and tool unlock.
- Saved Haptics off through Settings Back; the choice survived Home/resume and a process restart. Only one launcher activity remained.
- Checked runtime logs for application exceptions.
- APK signature verified; both ARM64 and x86-64 libraries have load segments aligned to at least 16 KiB. ZIP-entry alignment passed.
- Install ZIP contains exactly the tested APK. It can be extracted with Files before installation.
- Lint: zero errors, six build/manifest warnings concerning the pinned target/AGP, portrait orientation, API-specific Back attribute and backup configuration.

Physical Pixel vibration strength and blind-player comprehension still require a phone playtest. They cannot be confirmed by the emulator.

## Download integrity

| File | Bytes | SHA256 |
| --- | ---: | --- |
| New-Opacha-0.4.0.apk | 7,759,353 | `f233b426dd3bfc54833ac977828af170e42db3a3b3c8e624ed1f4d625717990f` |
| New-Opacha-0.4.0-install.zip | 1,072,119 | `bc43888c7f28841fc7162970a31ba7fb23ff92b75f450dd8f2e9674e430119e6` |

`scripts/verify-apk.py` checks campaign counts, native load-segment alignment and install ZIP equality. Android SDK `apksigner verify` and `zipalign -c -P 16 -v 4` provide the signing and ZIP-entry checks separately.
