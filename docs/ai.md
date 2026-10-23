# AI players

AI type belongs to a player (colour). Every node it owns shows that player's symbol. A captured node immediately uses its new owner's symbol. Human and neutral nodes have no symbol. In AI-only matches, green also has an AI type.

Skirmish has an **AI opponents** selector: Mixed, Simple, Savvy, Silly. Mixed shuffles the three types using the map seed, then assigns them without repetition until all three have been used. A four-colour game with a human has one of each. With fewer opponents, it uses a subset. Difficulty remains a separate setting.

## Simple: triangular flag

`AiCustomEntity` uses the same visible-state planning and safety checks as Savvy. It prefers the largest safe send: 100%, then 50%, then 25%. Savvy tries those shares in the opposite order. Difficulty controls reaction time separately from personality.

- It waits until its army can beat the target's expected troops and shields on arrival, including visible interceptions and reinforcements that arrive in time.
- It leaves reserves against visible incoming attacks and avoids sending a second army to an already secured target.
- At a starting flag it can send half or a quarter while keeping its reserve, so the full-army preference does not prevent expansion.
- Supplies use owned links that lead toward a rival or the flag objective. A neutral dead end does not attract supplies merely because it borders uncaptured territory.
- Normal and hard invest in safe Grow nodes. They reserve an upgrade budget only when Grow is available. Hard can combine two attacks or fortify exposed fronts.
- Easy, normal and hard evaluate every 400, 180 and 20 normalized game ticks. All personalities share two commands per game second; successful orders and upgrades consume commands. Both clocks follow game speed and refresh rate. Campaign adaptive difficulty can lengthen the evaluation interval.
- Explicit orders replace the old automatic supply routes, so a standing order cannot drain a reserve or repeatedly spend commands without changing anything.

## Savvy: crown

Savvy chooses orders from the visible map. It uses fixed rules and does not learn between matches or read another player's future orders.

- **Economical captures:** it tries 25%, then 50%, then 100% of a node's whole troops. It chooses the smallest share that can take the target with a small margin.
- **Travel matters:** the calculation includes target production and shield regeneration during travel, visible friendly waves, timely defender reinforcements, and enemies travelling toward it on the same link. Growth uses the actual refresh-adjusted regeneration interval and directional link speed. This is an estimate, since other players can change their orders during flight.
- **Reserves:** visible waves are considered in arrival order. A friendly reinforcement arriving after an attack cannot justify emptying the threatened node. Possible attacks from adjacent enemies lower an order's score without blocking every cheap expansion.
- In starting-flag games, Savvy also keeps at least eight troops at its original flag. Visible incoming threats can require a larger reserve.
- **Two-node attacks:** on hard, if two neighbours can take a target together, it evaluates their 25/50/100% combinations. Both orders are issued together, cost two commands, and require similar journey lengths to limit isolated arrivals.
- **Reinforcement:** help must arrive before the threatened node falls. Useful partial contributions are allowed. Spare troops move toward a rival or flag using travel distance, including directional routes. A winning flag capture takes priority over ordinary expansion.
- **Upgrades:** it protects the ten-troop budget for safe economic upgrades before supplying the front. It can fortify a node facing several enemies. Upgrade costs are unchanged; easy disables upgrades.
- It shares Simple's game-time evaluation cadence and command allowance, including both parts of a combined attack.

## Silly: circle

Silly uses Simple's planning. For each planned command that would change the game, it independently rolls a 20% chance to replace it with one random legal troop order. Frames, idle evaluations and unchanged supply settings do not trigger rolls.

The random order picks one of its own nodes with troops, a linked neighbour, and 25%, 50% or 100%. It may attack a strong enemy, send too few troops, or send troops backward to an ally. Tiny garrisons send one whole troop instead of an empty rounded share. A replacement spends one command and obeys normal movement and combat. Twenty percent is a probability, so short matches can vary.

## Compatibility and checks

The October 21 audit ran twelve seeded two-player maps (six tiny, six small), each twice with starting colours swapped, on hard at 1×. The pre-fix checkout produced Savvy 17, Simple 7. After repairing both planners, the first comparison produced Savvy 10, Simple 12 and two unfinished matches at the 36,000-tick limit; both unfinished games were seed 4. These are diagnostic matches, not a requirement that one personality always wins. Run `.\opacha.ps1 :lwjgl3:aiMatches` to repeat them.

That benchmark isolates ordinary troop planning. The [tactical checks](tactics.md#verification-and-future-changes) exercise weapons, digging and diplomacy, including complete mixed-AI matches in each mode.

Campaign playtests added checks against wasteful weapon use and automatic universal alliances. Guns avoid ordinary empty or nearly empty targets, retain enough troops against incoming fleets, and can send half of a large reserve toward a useful foothold. AI preserves economic nodes when choosing where to build guns. A pact needs a credible common rival; AI keeps one simultaneous partner and the strongest rival can refuse. These shared tactical decisions sit above the three ordinary troop-planning personalities. [Balance results](balance.md) describe the larger four-mode test.

The optional `ai_types` section in exported maps records colour-to-type assignments. Older maps without it use Simple. The authored campaign records opponent types explicitly; calendar and older custom maps use Simple unless a map records other types. Restarting a skirmish preserves its seeded assignment.

Run `.\opacha.ps1 :lwjgl3:aiSmokeTest` on Windows for native scenario and UI checks at 480 × 800 and 390 × 844. They exercise capture outcomes for all three troop shares, incoming-threat reserves, combined attacks, redundant-wave avoidance, rear supply, the random-command rate, map round trips, ownership markers, all selector values, AI-only play and pausing. Screenshots are in `lwjgl3/build/ai-smoke/`.

Run `.\opacha.ps1 :lwjgl3:aiRules` for twelve focused regression scenarios and the existing 30,044 assertions. The first eight scenarios were run against a frozen copy of the pre-fix game and all failed: unchanged supply orders spending commands, losing long-route attacks, duplicate captures, saving for disabled Grow, counting late defender waves, refusing useful partial help, supplying a dead end, and game-speed-dependent command rates. Four further scenarios check the order of friendly reinforcements, rescue deadlines, winning flag captures and Simple's ability to expand while keeping a starting-flag reserve. Clock checks cover 60, 120 and 240 Hz at 0.5×, 1× and 2×.

## October 21 balance evidence

The same seven scripted players, four seeds, two seats and four modes produced 63 wins in 224 attempts before the fix and 31 afterwards. Unfinished matches fell from five to zero. These are scripted-player wins, so fewer wins indicate stronger opposition in this sample. They do not measure human enjoyment or establish universal superiority. Both batches use the production simulation, fixed policies and the same random-map rules.

The complete campaign sweep produced 410/672 scripted wins and 18 unfinished matches before the fix, then 400/672 and 11 unfinished after the AI fix and map-11 adjustment. Map 11's artillery lesson had no wins in 70 attempts during AI tuning. Giving its existing forward node Grow, still starting at zero troops, produced 19/70. No other campaign map was changed by this audit. Maps 21 and 36 had no wins on the two main seeds but produced 2/56 and 21/56 wins on fresh seeds 3–10. Every map has a tested winning line across the main and focused runs; map 21 remains narrow.

Raw matches, timings and fingerprints are retained in [playtests/ai-audit](playtests/ai-audit/). Local runnable snapshots remain under `build/ai-audit/` and `build/playtest/ai-before`, `ai-after`, `ai-campaign`, `ai-narrow` and `ai-map11-balance`. The audit preserves pre-fix JARs and a campaign baseline locally, allowing regression scenarios to run against the earlier implementation without reverting the working tree.

For future AI changes: first reproduce the decision in a small production-model scenario, then run it against the preserved baseline and repair. Compare the same seeds and policies across all modes, retain timeouts, and inspect campaign maps without a win on fresh seeds. Keep the troop forecast tied to actual production, shields, direction, travel time and arrival order. Defensive reserves deliberately omit future regeneration, so they are conservative. The AI cannot predict new orders or opposing hidden bombs.

Bomb supplies are local. The shared weapon AI reacts to visible incoming attacks, buys at an economic node with at least 65 troops, and sends a hidden two-person courier. It places a route bomb ahead of an approaching fleet when dispatching from the defending endpoint; otherwise it can trap the threatened owned node. Simple and Silly stop at one reserved node bomb, Savvy at two. Only one courier per faction is planned at once. No policy reads opposing node traps or couriers.
