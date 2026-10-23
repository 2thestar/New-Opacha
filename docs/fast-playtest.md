# Fast playtesting

The latest [original-feel revision](original-feel.md) supersedes the populated campaign openings and balance totals recorded below. It keeps the testing environment and adds `-Mode Feel` for zero starts, wave timing, shields, routes and haptic callbacks.

Use `playtest.ps1` from the project root. It builds only when source, assets or test classes change. Each build gets an immutable cache, each run gets separate saves and native-library extraction, and existing run names cannot be overwritten. This lets an old play window stay open while a new build is tested.

```powershell
.\playtest.ps1 -Mode Play -Level 28
.\playtest.ps1 -Mode Play -Level 19 -Screen Desktop
.\playtest.ps1 -Name baseline
.\playtest.ps1 -Mode Full -Name full
.\playtest.ps1 -Mode Random -Seeds 8 -Difficulty normal
.\playtest.ps1 -Mode Quick -Levels '19,21,28' -Styles 'hesitant,planner'
.\playtest.ps1 -Mode Quick -Levels 28 -SeedStart 3 -Seeds 8
.\playtest.ps1 -Mode Touch
.\playtest.ps1 -Mode Touch -Screen Desktop
.\playtest.ps1 -Mode Feel
.\playtest.ps1 -Name candidate -Compare 'build/playtest/baseline' -Open
```

`Play` opens the actual game with its native renderer and pointer controls at 390 × 844, or 480 × 800 with `-Screen Desktop`. F5 restarts the current map; brackets change maps; F6 captures the window; F8 turns the selected scripted player on or off; Space pauses. The default scripted player is Planner. Bot-assisted attempts are not human observations. Keyboard controls belong to this test launcher; the shipped Android controls stay touch based.

Maps without a briefing open paused. Press Space or Resume when ready.

`Quick` runs ten maps, seven strategies and two seeds: 140 matches. `Full` runs all 48 maps: 672 matches. `Random` runs all four modes with green/red starting-position swaps. Its difficulty flag applies to random maps; campaign maps retain their authored difficulty. `Touch` tests input, map loading, day/night renders and generated-flag travel fairness.

Open each run's `report.html`. Results, Inputs, Data checks and Rules are separate views. `matches.csv` retains every completed attempt, including defeats and timeouts. `manifest.json` records source and policy fingerprints, campaign data, seed ranges and settings. `timing.json` distinguishes wall time from game time. A stopped batch retains completed CSV rows but has no successful completion timing; do not present it as a complete batch.

Measured without the Android emulator competing for resources, 140 matches took 6.2 seconds in the Java process and 7.1 seconds for the complete warm command. All 672 campaign matches took 13.4 seconds in Java and 14.4 seconds for the command. Build time is additional when game files change. Running several graphics tests and an emulator together was slower; use serial runs when measuring performance.

## Earlier expedited revision

- Zoomed-out nodes retain a target equivalent to 44 points on a 390-point-wide phone. Overlapping targets resolve to the closest node. Saved-map import uses model coordinates separately, so touch tolerance cannot alter map connectivity.
- A drag shows the repeating route before release, with `Repeat` or `Repeat half`. Returning to the source cancels. A drag that is already moving can take longer than 900 ms. Pinching, losing the source, or aiming at an unusable blueprint cannot create the order. The existing automatic-send mechanic remains.
- The first lesson says to send more than the target's troops and shields. Equal forces remove defenders without capturing ownership, so the previous instruction to “match” them was misleading.
- Generated neutral flags minimize the relative spread of shortest travel times across factions. Routes use their actual directional multipliers; unfinished routes are excluded. Multiple starts for one faction count as one contender, using its closest start. This measures travel opportunity, not full strategic fairness: troops, production and contested routes still matter.
- Map 28, Hold Bridge, starts with a visible 20-troop economic supply node. It previously had zero wins in 14 current-version attempts. The same policies and seeds win 10/14 after this single map change. A separate 56-game check on seeds 3–10 wins 33 times, with at least two wins on every seed. Nine of those holdout matches reach the time limit, so long cleanup still needs human review.

The full campaign comparison contains 672 paired games: 10 gained wins, zero lost wins, and 47 unchanged maps. Every campaign map has a tested winning approach. There are 439 wins and 16 unfinished games in the final sweep. The first five lessons are won by all seven policies on both seeds. These are scripted results, not human win rates.

On 64 generated maps at 390 × 844, travel spread improves on 48 and worsens on none. Median spread falls from 0.656 to 0.506, where spread is `(latest arrival − earliest arrival) / latest arrival`. At 480 × 800, 49 improve and none worsen; map relaxation and rounding depend slightly on viewport dimensions. A substantial residual spread remains on some maps.

The hard random sweep wins 69/448 and has no unfinished games. It remains harsh for slower strategies. It uses small four-faction maps, not introductory campaign conditions. Weapon prices and damage are unchanged. The existing bomb and artillery hypotheses in [balance.md](balance.md) still require focused tests before numerical tuning.

Raw evidence is in [playtests/expedited](playtests/expedited/). The old campaign report describes an earlier build; the new before/after CSVs are the matched current-version comparison. Repeating the selected cases alone produces the same outcomes and timings as those cases inside the full sweep.

## Human playtest, about ten minutes

Give a new player maps 1, 2 and 4, then a flag race and map 28. Give no coaching. Observe the first attempted order, missed taps, unintended repeating orders, use of half sends and pause, moments where they stop making choices, and whether they choose another map. Record the map, first confusing action, best capture, most frustrating loss and their reason to continue or stop. Keep observation separate from the player's explanation.

After each map ask: “What changed your plan?”, “What was unclear?”, and “Would you choose another map now?” A win without understanding is an onboarding failure. Repeated tiny attacks against shields can indicate an unreadable combat rule. Replaying by choice is stronger evidence of replay appeal than a bot win or an extra particle effect. Physical thumb reach, fatigue and haptic feel still require a phone in a person's hands.

Change one cause at a time, keep policies and seeds fixed, rerun the affected cases, then use fresh seeds and the full sweep. Do not revise a bot and a map together and call the result a controlled improvement. Do not tune for 100% wins: reserve choices, recoverable mistakes and different winning approaches should remain.

## Research used

- [Hunicke, LeBlanc and Zubek: MDA](https://www.cs.northwestern.edu/~hunicke/MDA.pdf). Start with the desired experience, trace it to decisions and feedback, then change the smallest relevant rule. Here the intended experience is choosing a commitment, seeing its capture land, and using the new foothold.
- [Przybylski, Rigby and Ryan: A Motivational Model of Video Game Engagement](https://selfdeterminationtheory.org/SDT/documents/2010_PrzybylskiRigbyRyan_ROGP.pdf). Competence and meaningful choice are useful design hypotheses. Applied here: readable controls, evidence that a choice worked, optional tactical pause and varied approaches. This research does not establish that the revised game is enjoyable.
- [Game Accessibility Guidelines: large, spaced interactive elements](https://gameaccessibilityguidelines.com/ensure-interactive-elements-virtual-controls-are-large-and-well-spaced-particularly-on-small-or-touch-screens/). Finger accuracy and comfortable movement constrain interface design. The 44-point minimum is this project's target, not a measured physical-device result.
- [Politowski et al.: Assessing Video Game Balance using Autonomous Agents](https://arxiv.org/abs/2304.08699). Automated players can supplement balance testing. They cannot replace observation of learning, emotion or willingness to play again.

## Reusable procedure

Keep the numerical simulation on the production model. Keep compiled JARs immutable while a native window is open; texture restoration can reopen them. Give concurrent JVMs separate native extraction paths, as documented by [LWJGL configuration](https://javadoc.lwjgl.org/org/lwjgl/system/Configuration.html). Keep screen-space picking out of import and export. Test the same case both alone and inside a sweep to detect leaked state. Retain current baselines because earlier reports can refer to changed maps, policies or rules.

Autoskill outcome: project note. These project-specific commands and corrected failure cases belong here; game-design and playtest-protocol-designer already cover the general method. No skill was installed or published.
