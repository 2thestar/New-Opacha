# 0.6.1 visual correction

Boris rejected the 0.6.0 APK screenshot because the shipped battlefield did
not reproduce the agreed design. The saved colorblind setting overlaid old
opaque node discs, and the new renderer had also simplified the approved
material and equipment. Removing that overlay alone was insufficient.

## What changed

- The original approved Canvas material and seven-spike contour now produce
  the node atlas. Gradients, bevel, shine, contour and contact shadow come
  from the executable reference rather than a separate Java approximation.
- Colorblind mode retains the candy material. Violet replaces yellow, with
  transparent rim marks; opaque inset discs and legacy rings are removed.
- The fifteen light conveyor marks, layered cyan shield segments, matte
  troop contours, rounded tank/rig hulls and soft enemy vapor follow the
  approved studies. Counts remain upright and on their paths.
- Rig presentation eases across real resource intervals using a 0.12-second
  simulation-time constant. Zero crew freezes its last displayed position
  and drill phase. Integer costs, work ticks and arrival rules are unchanged.
- Incoming digger centers use uninterrupted ordinary travel. Only their
  shells taper at the intake; their counts transfer on actual model contact.

## Evidence

Current native fixtures cover thirteen states at each of 390×844, 480×800
and 576×1280. The last size matches Boris's screenshot. Installed Android 15
APK captures cover normal/colorblind × Day/Night at 576×1280.

Two eight-second native simulation recordings cover actual friendly/enemy
contacts and digging, including working, idle zero, incoming and restart.
Their ten-column CSVs record newly rendered poses separately from the integer
model front. Both passed checks for continued positive-crew movement,
continued incoming movement, exact zero front/phase hold and real arrivals.
The existing engine can present cached frames under load; these tests do
not establish physical-device frame rate or haptic feel.

The approved source and PNG pixel-data hashes are in
`assets/game/battlefield/provenance.json`; all four PNGs embed their origins.
The APK contains those exact source PNG files. Its signature uses the
existing certificate, and the install ZIP contains the identical APK.

APK SHA-256: `1EFFC0D3AC599A4473479691723206A8454C52E0380693D991C03F4B3197FC4A`.

Local build, verification and motion measurements are under `.tools/qa/`.
The historical quality-bar card, build spec and measured comparison reports
were not available for the original release; this correction does not invent
those earlier records. No public release was published by this task.

The final reviewer scored all four listed corrections resolved: continuous
rig motion, uninterrupted intake, motion verification and documentation.
The scoped verdict was `ship`, with no regressions observed in the recaptured
states. Missing historical artifacts remain the limitation described above.

## Project workflow correction

Autoskill outcome: project note. Preserve the executable approved material
as the production source, and test saved accessibility choices as well as
defaults. Never let old accessibility artwork replace the approved visual
system. Check actual PNG dimensions rather than requested window dimensions:
Windows clipped the first tall native capture until its window constraints
were corrected. A scene's disappearing flag also does not prove its last
animation frame is gone; wait for its visible factors to reach zero.

Native recording compresses PNGs off the rendering thread. Motion checks
inspect newly rendered positions and intake movement, not just nondecreasing
model progress. This catches the repeated work-tick pauses and early intake
stop that the earlier checks missed. These details belong to this project's
capture and renderer workflow; no new skill was installed or published.
