# 0.6.1 performance handoff

Prepared 23 October 2026. Rendering edits, builds, publication and emulator
starts are on hold in the UI chat while the development chat profiles the
game. Boris's reported slowdown and graphical glitches remain unresolved.

## Exact retained artifacts

Verified copies are frozen at `.tools/release-0.6.1-tested-artifacts/`.
Its `manifest.json` lists every copied file, size and SHA-256. No signing key
or credentials were copied.

| Artifact | SHA-256 |
| --- | --- |
| APK, 8,817,460 bytes | `1EFFC0D3AC599A4473479691723206A8454C52E0380693D991C03F4B3197FC4A` |
| Install ZIP, 7,327,040 bytes | `4427DD19350ECA9D475EEF4383EC2ACEA72139FF76FB00106F0CF6AE15B37578` |
| core-0.6.1.jar | `DB4E957C7EBAD51BE05194322CEB1D42807893E5FD97090FEAADF39DCC6E0ED8` |
| lwjgl3-0.6.1.jar | `270A168BAE71936096EC0A07CAE5FA95CD33514E76818141599DE86172B9C380` |

Original downloads remain in `build/downloads/`. Package ID is
`io.github.twothestar.newopacha`, version code 7, version name 0.6.1,
minSdk 26, targetSdk 35, ARM64 and x86_64. Signing certificate SHA-256 is
`270631fbbc296fba0e6156cd6a0bda2377b5eb7761665c61b4c714c5b35c461a`.
The ZIP contains the identical APK; the APK contains the four verified
Canvas-derived PNG assets.

No 0.6.1 commit, tag, push or public release was created by the UI chat.
Delivery used local APK/ZIP links. Remote release state was not rechecked
for this handoff.

## Source provenance gap

There is no immutable tested 0.6.1 source snapshot or source commit. The
release was built from the shared dirty checkout, whose base HEAD was
`2d829e1a2b29d0f2b359da08c2c0a671f6dce20e`. That commit alone does not
reproduce 0.6.1. Existing campaign, AI and gameplay changes were preserved.
The frozen APK and compiled JARs identify the retained binary baseline;
the current checkout must not be described as an immutable tested source.

Do not use `.tools/qa/snapshot-release.ps1` for a shareable source archive
without adapting it: it explicitly copies the local signing keystore.

## What was measured

The frozen correctness evidence includes
`graphics-061-verification.json`, `graphics-061-motion-measurements.json`
and the Day/Night `simulation.csv` files. Original traces are under
`lwjgl3/build/battlefield-motion/{Day,Night}/simulation.csv`.

Each theme has 160 newly rendered pose samples across an eight-second
simulation fixture. Minimum positive working-front step was 0.00141148;
minimum incoming route step was 1.51064 world units. Idle front and phase
drift were both exactly zero. These are animation correctness measurements,
not wall-clock FPS or CPU/GPU frame-time benchmarks.

The harness deliberately records newly rendered poses by render index:
the engine can present an old framebuffer between renders. Frame indices
and simulation-time gaps are preserved. PNG compression runs on a worker
thread. The 20 FPS media export and filtered samples do not establish smooth
60 FPS playback, frame pacing, or whole-game performance under load.

Native stills cover thirteen states at 390×844, 480×800 and 576×1280;
four installed Android 15 emulator stills cover normal/colorblind ×
Day/Night at 576×1280. Installation, signature, asset inclusion and no
AndroidRuntime errors were verified. No physical-device frame-rate,
latency, memory, batching or haptic benchmark was recorded.

Final logs: `.tools/qa/graphics-061-rendered-pose-check.log` (Day),
`.tools/qa/graphics-061-release.log` (Night and APK).
`graphics-061-motion-fix.log` is a failed intermediate attempt and must not
be mistaken for the final result.

## Source paths worth profiling

These are code observations, not measured bottleneck rankings:

- `YioGdxGame.render()` repeats a cached framebuffer when delta time is
  at least 25 ms, rendering internals only after two skipped iterations.
  Simulation still moves before that branch. Measure frame pacing and
  simulation time independently.
- `BattlefieldArt.node()` uses a 20×20 deformation grid for an attached
  fluid node: 400 submitted quads and 1,600 evaluated corners. Each corner
  evaluates powers/exponentials. This is **not** evidence of 400 GPU draw
  calls; SpriteBatch can batch quads using one texture.
- `BattlefieldArt.ovalOutline()` emits 32 rounded line segments per troop.
  `UiStyle.line()` uses a pixel from the shape texture and two discs from
  a separate circle texture, so adjacent segments can force repeated
  texture changes and SpriteBatch flushes. Measure actual render calls.
  Shield arcs, tread details and other rounded lines use the same helper.
- `RenderPlanets.node()` scans the unit list for each visible node and
  recalculates troop poses while looking for fluid contacts. Measure this
  node-by-unit work in crowded maps.
- `RenderTactics.updateRigPocket()` scores candidates against other links
  for each blueprint. Counts also perform GlyphLayout/font-scale work.
- Quiet legacy particles were suppressed visually while retaining their
  simulation/RNG behavior. Check whether invisible particle work matters
  before changing it, since removing it can change deterministic behavior.

## Preserve while optimizing

The approved visual authority is the executable study set and DECISIONS.md
under `.impeccable/mocks/battlefield/2026-10-21/`. Production nodes use the
Canvas-derived atlas at `assets/game/battlefield/`, with embedded origins
and `provenance.json`. Preserve that material and the whole-surface
colorblind treatment; the rejected legacy inset discs must not return.

The last visual review's ship verdict covered the four scored visual/motion
corrections. It did not certify playability, device frame rate or the
slowdown now reported by Boris. The combined performance release needs its
own measured load and device checks.
