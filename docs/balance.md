# Playability and balance

The campaign is built around a useful capture every few decisions: take a foothold, choose a front, then commit or hold a reserve. The 48 fixed maps have twelve layouts per mode, with changing objectives, directions, starting forces and opponents. They reuse tactical motifs across chapters so a familiar board asks a new question.

## What changed

- The opening lessons introduce one concept at a time and pause for their briefing. Grow, Defend, half sends and weapons cannot appear before their lesson. Hints are available on demand; every map is also available directly in Maps.
- Early lessons use slower opponents. Later conquest and neutral-flag puzzles add fronts and tactical choices without switching to the fastest AI cadence. The last starting-flag and alliance maps keep hard opponents.
- Flag homes are separated by intervening nodes and begin with shields. Previously, opposing AI could decide some starting-flag maps before the player reached the front. Selected later maps have a visible economic supply node to give a slower opening room to recover.
- Neutral flags retain 50 troops. Their positions consider every starting faction; directional approaches point toward the objective. This does not guarantee equal weighted travel times.
- Artillery avoids empty ordinary targets and preserves its own incoming defence. Large idle gun garrisons can advance toward a foothold. AI preserves economic nodes when choosing gun sites.
- AI alliances require a credible common rival and allow one simultaneous partner. This prevents an automatic chain of acceptances from ending a map immediately. Betrayal remains possible and permanent for that pair.
- Captures have a short gold pulse and optional native haptic feedback. They retain the existing capture sound. No capture effect blocks orders or changes game speed.

Your numerical rules remain unchanged: 2×/0.3× routes, 80 diggers per typical length, 20-price bombs plus two crew and 12 damage, 20-price guns and shots with 10 damage, five-second reload, three-length range, and 70% allied arrivals.

## How the modes play

| Mode | Tactical choices | Map treatment |
| --- | --- | --- |
| Conquest | Expand first, grow a protected rear, then combine arrivals at a junction. Constant small attacks can feed the defender. | Starts with one front; later forks, loops and three factions require choosing where to concentrate. |
| Neutral flag | Save enough for 50 defenders, take the fast approach, and avoid spreading the final attack across several weak waves. Digging can bypass the race. | Early races give time to prepare; later layouts add rival approaches, supply and an unfinished crossing. |
| Starting flags | Keep a home reserve, watch incoming fleets, then attack a flag when the opponent commits elsewhere. A territorial lead is not required. | Shielded, separated starts prevent a trivial first-wave finish. Late maps combine several approaches and directional commitments. |
| Alliances | Offer cooperation while a common rival is credible, support the shared front, then decide whether to keep the pact. Supplies lose 30% when gifted. | Three factions from the start; the strongest rival can refuse. The final puzzle rewards an early pact with yellow. |

The unfinished crossing is a concentration puzzle: its remaining 80 work and 50 defenders require combining forces or saving before sending. The artillery introduction has a funded forward position and a weak upgraded target across a gap, so disabling an upgrade has a visible purpose.

## Evidence and limits

The [generated results](balance-metrics.md) retain every final campaign and random-skirmish outcome. The [test plan](playtest-plan.md) defines the questions. CSVs in [playtests](playtests/) are reproducible inputs, not player telemetry.

Seven scripted players approximate expansion, investment, rushing, siege, diplomacy, hesitant input and pause-assisted planning. They use visible state, delayed decisions, half/all troop orders and seeded mistakes. Planner can queue two orders, rally troops at a visible flag blueprint and wait briefly to fund it. They do not learn, understand the tutorial or reproduce the full range of human tactics. A poor proxy opening can lose a map that a human would solve.

The final campaign sweep ran 672 games. The first five lessons were won by all seven policies on both seeds. Every authored map had a winning line; the three narrow puzzles (19, 21 and 48) also had a win on each of four additional error seeds. Median first capture was five game seconds in each campaign chapter. Thirteen games reached the 300-second limit, including some long detours and repeated recaptures; the maps have other tested finishes, but slow cleanup remains a playtest concern.

The separate 224-game hard random-map sweep was much harsher: only five conquest wins and five alliance wins out of 56 attempts per mode. Normal remains the skirmish default. Authored maps are the stronger starting experience; hard tiny random openings still need wider seed coverage before calling the generator consistently fair.

Every map must have a tested winning line. Narrow cases receive extra error seeds. Capture sampling is every half game second; very quick recaptures can be missed. A 300-second cap is reported as unfinished rather than removed from the denominator. Random stress games use hard difficulty and starting-position swaps; they are deliberately different from teaching maps.

The early baseline used six policies; later tests added Planner and improved the policies. It is retained for diagnosis, but its win rates must not be treated as a controlled before/after improvement. Iterative tuning is evidence of corrected failure cases, not proof that one isolated change caused the overall result.

## Balance changes worth trying next

1. **Make bombs easier to justify.** Buying and placing consumes 22 troops to remove at most 12. Their advantage is relocating damage from rear production to a threatened route, so that ratio alone does not prove they are useless. Test price 12 with damage 12, or keep price 20 and test damage 20. Change one value at a time. Compare successful interceptions and resulting captures, not purchase counts alone.
2. **Choose artillery's intended role.** At 1×, a defensive node restores about 2.57 shield per second; a cannon deals 2 damage per second over repeated shots. One gun cannot penetrate a fully recovering shield by itself. The deterministic regenerating-siege check compares one gun with a synchronized pair. Keep this counter if guns are supporting weapons; test shot price 12 to make coordinated fire less costly. If a single gun should eventually breach a fort, separately test a three-second shield-recovery delay after impact. That changes the mechanic and is not included in this build.
3. **Keep digging situational.** Eighty troops is a serious investment in a short match. The crossing puzzle demonstrates when that investment opens a winning route. Before reducing cost globally, compare a finished shortcut with the troops and journey time saved by avoiding the detour; otherwise cheap construction can erase map puzzles.
4. **Use travel time when generating flag races.** Hop counts ignore the 6.67× difference between fast and reverse travel. Future random flag placement should compare weighted travel times from all homes. Authored races already avoid directing one approach away from the flag, but a fair race needs this stronger check.

The regenerating-siege fixture started the defender at 20 troops and 14 shields. After 20 game seconds it had 37.2 troops against one gun, versus 9.9 against two synchronized guns, with four shots per gun. This includes actual troop and shield regeneration rather than an isolated damage calculation. It supports coordinated bombardment and shows why a lone cannon can feel ineffective.

## Phone playtest still needed

Bots cannot establish enjoyment, tutorial comprehension or physical vibration strength. Give a new player lessons 1–4, then one flag race and an alliance map without coaching. Record the first confusing action, most satisfying capture, most frustrating loss, and whether they choose another map. Observe whether they use pause and whether a new tool changes their plan. If a lesson needs explanation aloud, fix that lesson before increasing its difficulty.

## Reusable testing lessons

Keep policy revisions separate from map revisions when measuring an effect. A headless model pass cannot catch overlapping brief text, hidden controls or an editor slider silently rounding a garrison. Use real native touch tests and inspect both phone and desktop renders, then install the same signed APK as an update in the Pixel profile. Freeze sources and packaged JARs during native tests; texture restoration can reopen the JAR later.

Opening every map does not test a campaign. Exercise first win → Next → defeat, check which lessons stay unlocked, then complete the last map and reach the completion screen. Wait for menu and game-view transitions to settle before clicking result controls; scene creation alone does not mean a button is ready.

## Version 0.5.0 bomb delivery sweep

A separate 112-game regression sweep covers maps 10, 12, 22, 24, 34, 36, 46 and 48 with seven delayed policies and two seeds. The production AI and siege policy use local bomb supplies and actual courier travel. All eight maps retain a winning approach in this sample. There are 68 human-policy wins and four games unfinished at the 300-second stop.

| Mode | Wins / games |
| --- | ---: |
| Conquest | 14 / 28 |
| Neutral flag | 21 / 28 |
| Starting flags | 20 / 28 |
| Alliances | 13 / 28 |

Only nine runs used any tool, so these results give little evidence about bomb balance. Focused scenarios check delivery, concealment, hostile and allied crossings, three-bomb capacity, capture casualties and flag wins. The price remains 20, the crew two and the blast 12, as requested. The earlier optional lower-price experiment remains a proposal. Real players should try leaving traps on an exposed node and deciding whether to reinforce or bait a capture.

This is not a controlled before/after comparison: the siege policy changed with the rules. It does not establish human enjoyment. [Raw version 0.5.0 results](playtests/bombs-0.5.0.csv) are separate from the version 0.4.0 campaign sweep.
