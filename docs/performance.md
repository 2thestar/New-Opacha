# Performance correction, 0.6.2

Boris reported slow play and graphical glitches in 0.6.1. This revision changes
rendering, timing and the starting camera. The approved battlefield PNGs and
game prices, damage and victory rules are retained.

## Causes and changes

- Every rounded line switched between two textures. Fleet outlines, shield
  arcs and route arrows turned this into thousands of GPU drawing calls.
  The rectangle, pixel and circle now share one managed texture. Fleet counts
  are drawn together instead of switching to a font after every body.
- When a frame took at least 25 ms, the engine could show an old framebuffer
  for two further frames. It now renders the current world on every display
  frame, without the full-screen offscreen render and copy. Menu transitions
  still use their separate snapshot.
- Stretching a node evaluated 1,600 corners and submitted 400 small meshes.
  Corners are shared and the whole mesh is submitted together. The grid uses
  12, 16 or 20 cells per side according to the node's size. The original atlas
  supplies its color, lighting and contour. Unrelated fleets are rejected
  before their poses are calculated for a node.
- Simulation formerly depended on display-frame updates and a saved refresh
  rate guess. It now runs fixed 60 Hz ticks. Presentation uses elapsed frame
  time, and repeating timers retain fractional time. Pause clears pending
  time; a long stall catches up at most six ticks to keep input responsive.
  Frames slower than 100 ms therefore intentionally lose excess game time.
- Starting-camera fitting previously covered tiny maps only. It now checks
  every map size and fits any overlapping board below the controls.

## Measurement method

`FramePerformanceTest` uses the real OpenGL desktop renderer at 390×844.
It fixes the world state, disables simulation and audio, lets entry animations
settle for at least one real second, then measures 120 frames per scene.
There is no screenshot readback or PNG compression in measured frames.
GLProfiler records drawing calls, texture bindings and submitted vertices.
The fresh-frame counter detects repeated worlds independently of simulation.

The workloads are twelve nodes without moving fleets, twelve nodes with 48
fleets at departure/arrival/travel positions, and 48 nodes with 240 fleets.
These are rendering stress fixtures. They do not measure human input latency
or a complete match's AI cost. Separate rule checks and an active Android AI
match exercise simulation.

Frame time here is CPU execution and GL submission time around `render()`.
Uncapped rendering excludes the wait for vertical synchronization. It is not
a physical Pixel GPU time or a claim of Android 60 FPS. The old renderer can
do less work by showing old frames; compare its fresh-frame count too.

The final comparison runs the same compiled benchmark against the retained
0.6.1 core/launcher JARs and the new JARs. Both use the same dependencies,
assets, host, viewport and settling rule, with the Android emulator stopped.
The retained 0.6.1 core SHA-256 is
`DB4E957C7EBAD51BE05194322CEB1D42807893E5FD97090FEAADF39DCC6E0ED8`.
There is no immutable 0.6.1 source snapshot; see
[the prior handoff](performance-handoff-0.6.1.md). The preserved binary is
the baseline authority.

Raw results and the final comparison are in [playtests/performance](playtests/performance/).

| Scene | 0.6.1 p95 ms | 0.6.2 p95 ms | Drawing calls, old → new | Fresh frames, old → new |
| --- | ---: | ---: | ---: | ---: |
| 12 nodes, no fleets | 1.976 | 0.419 | 1,485 → 43 | 120 → 120 |
| 12 nodes, 48 fleets | 15.126 | 0.917 | 4,463 → 80 | 120 → 120 |
| 48 nodes, 240 fleets | 64.051 | 5.531 | 10,382.5 → 316 | 60 → 120 |

The crowded scene's mean falls from 28.408 to 4.501 ms, about 6.3× less
render cost, with 97.0% fewer drawing calls per displayed frame. A separate
forced 30 FPS run also has 120/120 fresh frames in every scene; its crowded
scene p95 is 5.992 ms. Earlier diagnostic runs used a shorter settling rule
and are superseded by this frozen-binary comparison.

## Checks

- Equal-world timing at 15/30/60/90/120/144 FPS and 1×/3×: production, shields,
  digging, forward/reverse travel, bomb delivery and artillery reload.
- Tactical pause preserves population and game time, clears partial ticks,
  and bounds catch-up after long stalls. Camera checks place edge nodes
  below the HUD at all five map sizes.
- AI: 30,044 assertions plus twelve regression scenarios. Tactics: 119
  assertions and matches across all four modes. Campaign: 3,871 assertions
  over 48 authored maps.
- Thirteen battlefield states at phone and desktop sizes: Day/Night,
  colorblind, reduced motion, shields, split/merge, enemy impacts, artillery
  and zero-crew rigs. Native motion checks require a fresh world on every
  frame, forward movement, exact idle hold and actual arrivals.
- Native phone touch/menu and speed-slider checks; Android lint; APK
  signature, 16 KiB native alignment, four unchanged battlefield PNGs and
  an install ZIP containing the identical APK.

A separate full campaign sweep completes 672 scripted matches across all
48 maps: 400 wins, 242 losses, 19 eliminations and eleven timeouts. These
results exercise rules and strategies; they do not measure human enjoyment
or establish a performance improvement against an earlier balance report.
Maps 21 and 36 have no wins in this fourteen-attempt-per-map sweep and need
a separate focused balance review; this performance revision does not tune
their troops or rules to change that result.
The original-feel checks also pass zero openings, repeating-wave timing,
shield states, route orders and haptic callbacks.

## Android evidence and limits

The owned Pixel 8a Android 15 emulator uses x86-64 and SwiftShader software
graphics. The active large four-faction match was captured at its native
1080×2400 resolution. Installed Day/Night × normal/colorblind captures use
576×1280. An in-place update preserved the settings file byte for byte.
Background/resume and tactical pause are checked on the installed app.
The final native-resolution large-map capture has its upper nodes below the
HUD. The paused battlefield is pixel-identical before and after Android
background/resume. Check the actual emulator resolution after boot: a loaded
snapshot can restore a previous `wm size` override. Wait for the main-menu
Play button, since a static splash can also produce identical screenshots.

One preliminary large-match SurfaceView trace measured 18.260 average FPS
over 853 submitted frames in 0.6.2. The old large-match trace measured 5.382.
The generated maps and elapsed progression differ, so these are diagnostic
observations, not a controlled speedup ratio. The software emulator remains
slow; its 60 Hz display setting does not establish 60 FPS game rendering.
Physical Pixel frame rate, touch latency, heat and battery use remain
unmeasured. APK signatures and successful emulator play cannot establish
those properties.

## Repeating the checks

```powershell
.\opacha.ps1 -GradleArguments @(':lwjgl3:installDist', ':lwjgl3:smokeTestClasses')
.\scripts\benchmark-frames.ps1 -Name current -CheckBudgets
.\scripts\benchmark-frames.ps1 -Name limited -Fps 30 -CheckBudgets
.\opacha.ps1 -GradleArguments @(':lwjgl3:timingRules', ':lwjgl3:aiRules', ':lwjgl3:tacticsRules', ':lwjgl3:campaignRules')
```

The benchmark accepts `-CoreJar` and `-LauncherJar` to test frozen binaries.
It refuses to overwrite a run and records binary/benchmark hashes. Keep JARs
unchanged while a native process uses them, and stop the emulator before
comparing desktop timing. A changed benchmark or workload requires both
baseline and candidate to be measured again.

Autoskill outcome: project note and a repeatable benchmark script. A moving
model or a GIF export is not proof of fresh displayed frames. Retain binary
provenance, let real-time entry animations settle, measure rendering without
capture overhead, and test world timing separately from rendering cost.
These corrections belong to this project's renderer and release procedure;
no skill was installed or published.
