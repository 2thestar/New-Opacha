# Organic battlefield details

Boris requested this change on 30 October 2026:

> Factories should be visibly wavy pulsating nodes; bombs become just a troop
> bubble with a sparking fuse; the cannon is more like a tentacle with a soft
> opening that releases a little black ball.

This replaces the factory Conveyor marks, the rigid cannon barrel and the
old bomb glyph. The existing candy material, overhead viewpoint, flat troop
bodies, routes, tower/shield system, typography and faction colors remain
the surrounding visual authority.

- Factory motion belongs to the contour. Broad soft lobes travel around the
  existing candy face with a gentle breath. Centered troop numbers do not
  distort or oscillate. Factories retain a wavy silhouette in reduced motion.
- Bombs use the same matte oval and fine contour as ordinary troops, with
  a curved fuse and tiny warm sparks. Do not invent a troop count for the
  converted bomb: its model has no surviving troop population. Legacy
  couriers retain their actual two-person crew count. Owner-only visibility
  and the weapon work's model/drawn/touch position contract must be preserved.
- The cannon grows from the node rim as a rounded, tapered, gently curved
  tube. Its opening dilates to release a small dark ball, then settles. The
  existing target burst remains. Its center count stays clear. The weapon
  work's thicker armed state remains supported.
- Motion uses the simulation clock, so tactical pause holds the factory,
  fuse and shooting state. Reduced motion keeps recognizable silhouettes,
  a static fuse ember and existing brief impact feedback.
- Reuse the existing candy atlas and shared geometry texture. Shared mesh
  corners and batched geometry retain the performance work from 0.6.2.

Native evidence is captured by `OrganicVisualTest` using the real game
renderer and actual bomb-conversion and firing actions. Factory pixel
checks cover pause and reduced motion. `FramePerformanceTest` measures the
same idle, contact and crowded fixtures before and after the visual change;
media export is excluded from performance measurements. Device type and
unmeasured physical-phone limits must accompany any release claim.
