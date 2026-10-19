# AI players

AI type belongs to a player (colour). Every node it owns shows that player's symbol. A captured node immediately uses its new owner's symbol. Human and neutral nodes have no symbol. In AI-only matches, green also has an AI type.

Skirmish has an **AI opponents** selector: Mixed, Simple, Savvy, Silly. Mixed shuffles the three types using the map seed, then assigns them without repetition until all three have been used. A four-colour game with a human has one of each. With fewer opponents, it uses a subset. Difficulty remains a separate setting.

## Original AI: Simple, triangular flag

The original implementation is `AiCustomEntity`, driven by `AbstractAiEntity`.

- It checks linked neighbours rather than searching for a whole-map victory plan.
- It sends the full garrison when it can match a neutral node's troops and shields. Against an enemy it asks for five extra troops.
- It avoids attacking from nodes with incoming hostile troops worth more than half their current troops and shields.
- Safe nodes automatically supply neighbours that need upgrades or have neutral neighbours. Normal and hard can route rear supplies toward a frontier.
- Normal and hard buy economic upgrades on safe nodes. Normal caps economic expansion. Hard also builds defensive nodes on exposed fronts and occasionally combines neighbouring garrisons against one target.
- Difficulty changes the interval between evaluations: 400, 180 or 20 simulation ticks for easy, normal or hard. The existing nominal action allowance refills to two every 60 ticks. Some legacy supply and coordinated-attack paths do not enforce that allowance strictly; Simple retains this behaviour.
- Campaign adaptive difficulty also changes evaluation frequency and the economic limit. Troop generation follows the normal game rules.

## Savvy: crown

Savvy chooses orders from the visible map. It uses fixed rules and does not learn between matches or read another player's future orders.

- **Economical captures:** it tries 25%, then 50%, then 100% of a node's whole troops. It chooses the smallest share that can take the target with a small margin.
- **Travel matters:** the calculation includes target production and shield regeneration during travel, visible friendly waves, defender reinforcements, and enemies travelling toward it on the same link. Growth uses the actual regeneration interval and link movement speed. This is an estimate, since other players can change their orders during flight.
- **Reserves:** it leaves troops against incoming attacks. Possible attacks from other adjacent enemies lower an order's score, but do not force it to miss a cheap expansion. It may commit everything when taking that risk is worthwhile.
- **Two-node attacks:** if two neighbours can take a target together, it evaluates their 25/50/100% combinations. Both orders are issued together, cost two commands, and require similar journey lengths to limit isolated arrivals.
- **Reinforcement:** threatened allies receive help first. Spare rear troops move along owned links toward the nearest frontier. On the frontier, weaker nodes can rally into a stronger neighbouring army. It does not install automatic routes that can send troops back and forth.
- **Upgrades:** it protects the ten-troop budget for safe economic upgrades before supplying the front. It can fortify a node facing several enemies. Upgrade costs are unchanged; easy disables upgrades.
- It uses the original evaluation cadence and enforces the two-command allowance, including both parts of a combined attack. It issues explicit troop orders instead of automatic full-garrison supply routes.

## Silly: circle

Silly uses Simple's planning. For each planned command that would change the game, it independently rolls a 20% chance to replace it with one random legal troop order. Frames, idle evaluations and unchanged supply settings do not trigger rolls.

The random order picks one of its own nodes with troops, a linked neighbour, and 25%, 50% or 100%. It may attack a strong enemy, send too few troops, or send troops backward to an ally. Tiny garrisons send one whole troop instead of an empty rounded share. A replacement spends one command and obeys normal movement and combat. Twenty percent is a probability, so short matches can vary.

## Compatibility and checks

The 2026-10-19 balance check ran twelve seeded two-player maps (six tiny, six small), each twice with starting colours swapped, on hard at the default speed. Savvy won 19, Simple won 5, with no unfinished matches at the 36,000-tick limit. This is a small regression benchmark, not a claim about every map, difficulty or multiplayer matchup. Run `.\opacha.ps1 :lwjgl3:aiMatches` to repeat it.

That benchmark isolates ordinary troop planning. The [tactical checks](tactics.md#verification-and-future-changes) exercise weapons, digging and diplomacy, including complete mixed-AI matches in each mode.

The optional `ai_types` section in exported maps records colour-to-type assignments. Older maps without it use Simple. Campaign, calendar and original custom levels retain Simple unless a map explicitly records other types. Restarting a skirmish preserves its seeded assignment.

Run `.\opacha.ps1 :lwjgl3:aiSmokeTest` on Windows for native scenario and UI checks at 480 × 800 and 390 × 844. They exercise capture outcomes for all three troop shares, incoming-threat reserves, combined attacks, redundant-wave avoidance, rear supply, the random-command rate, map round trips, ownership markers, all selector values, AI-only play and pausing. Screenshots are in `lwjgl3/build/ai-smoke/`.
