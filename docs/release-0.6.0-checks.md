# 0.6.0 APK checks

Built 22 October 2026. Installable files:

- `build/downloads/New-Opacha-0.6.0.apk`
- `build/downloads/New-Opacha-0.6.0-install.zip`

Package `io.github.twothestar.newopacha`, version code 6, Android 8.0+,
ARM64 and x86_64. The APK uses the existing development signing certificate.
The ZIP contains the identical APK.

APK SHA-256: `6CC6304E833EB1169329906CB3950F963921C0E927099CA65345644C7DDB162A`.

## Verification

- Native battlefield checks passed at 390×844 and 480×800: Day, Night,
  segmented shields, fluid departure/approach/merge, enemy contact, cannon,
  working and zero-crew rigs, colorblind colors, and reduced motion.
- Tactics: 119 assertions and mode simulations passed. Campaign: 3,871
  assertions across 48 maps passed. Phone speed slider checks passed.
- Final APK installed over the existing app in the Android 15 Pixel 8a
  emulator. Day and Night map screens were captured; AndroidRuntime reported
  no errors. Signature, package metadata, and ZIP contents were verified.
- Finish review scored all three listed corrections resolved: crossing
  counts, colorblind troop identification, and design documentation.

Physical-device performance and haptic feel were not checked in this release.
The software-rendered emulator does not establish phone frame rate.
No public release was published by this build task.

## Project workflow notes

Autoskill outcome: project note. These build and capture details are specific
to this repository; no new skill was installed or published.

- Use `opacha.ps1` with `--offline --no-daemon` for the bundled toolchain.
  A fresh single-use Gradle process resolved this session's cached-JAR access
  failure; changing cache permissions was unnecessary.
- Capture Android PNGs as binary data with `adb exec-out`, or screencap on
  the device followed by `adb pull`. Do not redirect binary PNG output through
  PowerShell text encoding.
- Wait for a known menu or board state before capturing. Whole-image hash
  stability cannot detect readiness while gameplay or the economy rim moves.
- Inspect native pixel crops when the chat's resized image preview appears
  to omit a small label. The Night speed label remained present in the PNG.
- Keep continuous digging pose separate from integer resource ticks. Count
  clearance belongs in presentation; do not alter troop travel or digging rules.

Build logs and detailed verification are local under `.tools/qa/`.
