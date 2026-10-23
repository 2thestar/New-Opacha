# Original Opacha feel

This revision follows Boris's requested direction: relaxed decisions, short matches and a puzzle worth solving. It restores the information needed to make those decisions before adding more effects.

Native previews: [Day](playtests/original-feel/phone/01-day-shields-orders-routes.png), [damaged shields at night](playtests/original-feel/phone/03-three-shield-points.png), [vibration settings](playtests/original-feel/phone/09-settings-vibration.png).

## Reference observations

I inspected the opening and middle of [Yiotro's February 3 calendar solution](https://www.youtube.com/watch?v=VpANFE2FxjY), uploaded April 28, 2024, using frames sampled every five seconds through the first 95 seconds. The player builds reserves, upgrades, routes small packets from rear nodes and changes the front as opportunities appear. The moving troops are circles; the triangles around nodes show persistent repeating orders. They are different from route arrows.

[The developer's discussion of waiting tactics](https://www.reddit.com/r/yiotro_games/comments/rr8o27/opachamda_tactic_2/) supports saving an army while rivals fight. Together these suggest a useful loop: read the front, make a commitment, watch the arrival, then decide again. Constant tapping and vibration on every automatic packet would work against that pace. These are design interpretations, not findings from recruited players.

The [public original source](https://github.com/yiotro/Opacha-mda/tree/7e95e1f81281f693a190dcf663a6822106cf59ce) provides a separate mechanics check. Its old generator uses small fixed initial populations; the inspected footage also contains early spending and accumulated populations. Neither establishes that every original campaign starts literally at zero. Zero starts here implement Boris's pacing requirement directly.

## Implemented

- Every owned node, for every faction, starts with **zero troops on all 48 campaign maps**. Ordinary neutral defenders remain 3; the neutral flag remains 50. Starting shields and node upgrades retain their authored meaning. Artillery and digging lessons now describe saving and combining production instead of spending a supplied army.
- Repeating orders send every **1.8 game seconds**. The explicit clock replaces nested frame counters. Tests cover 60, 90, 120, 144 and 240 Hz at 0.5×, 1× and 2×. Redirecting preserves the phase; a new order starts its clock; an intentional immediate dispatch retains the existing control behavior.
- Repeating-order triangles sit outside the node with a contrasting outline and faction fill. They pulse slightly when a packet leaves. The former white texture vanished on the light board.
- Ordinary routes are solid. Directional routes have clean chevrons toward their faster direction. They retain the actual 2× forward and 0.3× reverse rule. Dashed lines are reserved for unfinished construction. Routes, arrows and shield segments share the node-outline stroke scale.
- Shields show **one solid segment per whole protection point**: 7 maximum normally, 14 with Defend. Missing protection is faint; the ring loses and regains segments with the actual model state. Neutral shields are visible too. Grow has no shields. The old white shield texture had the same light-theme visibility problem as the order triangles.
- Android feedback now covers selection, accepted manual/repeating orders, Grow/Defend upgrades, captures and speed changes. Automatic packets stay silent. Independent event limits prevent continuous vibration. Settings includes **Test vibration**, plus feedback when the game switch, Android touch-feedback setting or vibration motor prevents a test. Scrolled settings content is clipped below the fixed navigation header.

Android uses predefined tick, click and heavy-click effects with a View fallback on older supported versions. It respects both app and system settings. The adapter follows [Android's haptics guidance](https://developer.android.com/develop/ui/views/haptics/haptic-feedback); it adds the normal VIBRATE permission and checks the actual game view's focus. Earlier feedback covered only speed changes and captures, so most ordinary interactions gave no tactile confirmation.

## Balance evidence

The policies and their fingerprints are unchanged across these comparisons. These are delayed, fallible scripted players using the production model, not human win rates.

| Batch | Wins / games | Unfinished | Purpose |
| --- | ---: | ---: | --- |
| Before | 439 / 672 | 16 | Previous populated openings and timer |
| First zero-start sweep | 398 / 672 | 18 | Expose problems caused by the new opening pace |
| Final main sweep | 410 / 672 | 18 | Retain zero starts and revise map 8 |
| Fresh seeds, maps 8 and 33 | 45 / 112 | 0 | Check the two maps without an initial winning line |

Map 8 had no wins in its first 14 zero-start attempts. Moving yellow's start alone also produced no wins; that experiment was discarded. A visible green economic supply node, also starting at zero, gives the arrow lesson time to develop: 12/14 wins on the fixed cases and 39/56 on fresh seeds. No enemy cadence, price or combat rule changed.

Map 33 remains difficult: 0/14 in the main sweep and 6/56 on fresh seeds. Winning fresh cases include Rush on seed 5 and five policies on seed 8. Every map therefore has a tested winning approach across the combined evidence, but map 33 is narrow. The final main sweep retains all 18 timeouts; six occur on map 45. Early maps win 56/70 attempts. Median first capture across the campaign is 18.52 game seconds, versus 5.02 with the old stocked openings. That slower buildup is deliberate; whether it feels too slow requires human play.

Raw matches, manifests, timings and checksums are in [playtests/original-feel](playtests/original-feel/). The main before/after comparison changes both starting populations and the repeating timer, so it cannot isolate either cause. The focused map-8 comparison holds the new timer and zero-start rule fixed.

## Verification and next play

Native renders cover Day, Night, damaged and full shields, Grow, colorblind mode, ordinary/directional/unfinished routes, zero openings, briefings and scrolled vibration settings at 390 × 844 and 480 × 800. Automated checks cover wave timing, all zero openings, control feedback, app mute and silence during automatic sends. The campaign importer/editor checks pass 3,871 assertions; tactics pass 119. Native touch and interface-flow tests pass at both sizes. Campaign UI and speed-slider tests pass on the phone viewport. Android assembly and lint pass with zero errors and seven existing warnings. APK signature and 16 KB ZIP alignment checks pass.

Android emulator checks confirm update installation and delivery of predefined effects through the OS vibrator service. This verifies software delivery, not physical vibration strength. The included Test vibration control is the quick physical-device check.

With the emulator stopped and the build cached, the final Quick check ran 140 matches in 6.13 seconds inside Java and 7.0 seconds for the complete command. It retains 82 wins and five timeouts. This separate performance check is saved under `playtests/original-feel/speed-check`; it is not added to the 784 main and fresh-seed campaign attempts above.

```powershell
.\playtest.ps1 -Mode Play -Level 8
.\playtest.ps1 -Mode Feel
.\playtest.ps1 -Mode Feel -Screen Desktop
.\playtest.ps1 -Mode Full -Name my-zero-start-run
.\playtest.ps1 -Mode Quick -Levels '8,33' -SeedStart 3 -Seeds 8
```

Try maps 1, 2, 7, 8 and 33 without coaching. Observe whether a player saves a useful force, understands shield loss, distinguishes route arrows from standing orders, and redirects supplies without fighting the controls. Record the first confusing action, the most satisfying capture and whether they choose another map. A stronger bot score does not establish relaxation, learning or replay appeal. The broader [playtest guide](fast-playtest.md) retains the theory references and ten-minute observation protocol.

## Procedure retained

Autoskill outcome: project note. Compare source with actual footage before identifying a missing visual: travelling circles, standing-order triangles and route chevrons carry different information. Check feedback contrast in both themes, then verify platform delivery separately from event callbacks. Preserve zero-start balance experiments and failed candidates, and use new seeds after tuning. This belongs with this game's evidence; existing game-design and playtest skills cover the general workflow. No skill was installed or published.
