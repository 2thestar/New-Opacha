# New Opacha gameplay additions

## Controls and costs

Select your node and tap **Actions**. A targeting command shows a **Cancel** button. Pause cancels targeting.

| Action | Rule |
| --- | --- |
| Directional route | Chevrons point toward the fast end: 2× normal speed, 0.3× in reverse. |
| Route blueprint | Choose a second node. The first node is the fast-direction start. Existing connections cannot be duplicated. Crossing another route creates no junction. |
| Digging | Send troops from the blueprint's start. They build from that end and are consumed as the construction front advances. A typical route costs 80 diggers; cost scales with distance and rounds up. The number beside the blueprint is the remaining work. |
| Completed route | Becomes available to everyone in both directions, with directional speeds. Surviving diggers continue to the destination as ordinary troops. |
| Bomb purchase | Costs 20 troops at an owned economic node. Stock belongs to the player and can be placed from any owned node's Actions menu. |
| Bomb placement | Tap your moving fleet. Consume one bomb and two troops from that fleet; leave a visible bomb at its current route position. A two-troop crew is fully consumed. |
| Bomb hit | The first hostile fleet to cross loses up to 12 troops. The bomb is consumed. Own and allied fleets pass safely. |
| Artillery setup | Costs 20 troops and replaces the node's current type. The first shot is ready immediately. |
| Artillery shot | Costs 20 troops from the gun node, deals 10 damage, and reloads for five game seconds. Damage consumes shields before population. |
| Artillery range | Three typical route lengths, measured directly between nodes. Target an enemy node inside the dashed range circle. |

The typical route length is the median original route length on that map. New construction does not change it. Reloading pauses with the match and follows velocity and fast-forward settings. Artillery cannot capture a node or damage a neutral node. A hit that exhausts shields and population leaves the same owner with an ordinary, empty node.

## Match modes

- **Conquest:** the original last-player-standing victory condition.
- **Flag: neutral supernode:** one larger, marked neutral node starts with 50 troops. Its location favours similar route counts from the starting nodes. The first player to capture it wins immediately.
- **Flag: starting nodes:** each player's original starting node carries a flag. The first player to capture an enemy starting flag wins immediately; no return trip is required.
- **Alliances:** offers require acceptance. Friendship is pairwise: your ally's ally can still be your enemy. Allied fleets pass through each other; arrivals add 70% of their size to the recipient's garrison without changing ownership or node type. Fractional reinforcements accumulate in the garrison. Either partner can break a pact immediately. That pair stays hostile for the rest of the match. A surviving group wins together when every surviving pair is allied.

Pacts shows incoming offers, pending offers, active alliances and permanently broken alliances. AI players consider offers while at least three players remain. Simple accepts useful cooperation, Savvy weighs other threats and may later betray a much weaker partner, and Silly sometimes changes its mind. Acceptance is not automatic. Bombs already on routes use the current relationship when a fleet reaches them. Fleets that already passed peacefully do not collide retroactively after a betrayal.

## AI and map compatibility

All personalities can buy bombs, place them ahead of an approaching enemy fleet, and operate artillery while keeping troops for defence. Savvy can fund a short new route when a safe, large garrison can cover construction, the destination and a reserve. Ordinary troop planning ignores unfinished routes; the construction decision funds them separately. Savvy includes directional travel speed when sizing attacks and coordinating arrivals.

Exported maps preserve mode, directional endpoints, construction progress, bomb stock and positions, artillery reload and shields, original flag locations, and pact state. Original maps without the optional fields load as conquest with normal routes. Map export remains a map feature; it does not save moving fleets.

## Campaign and editor

The 48 fixed boards in `assets/campaign/levels.json` replace the generated campaign. `scripts/build-campaign.py` builds their explicit graphs, objectives, visible starting forces and lesson text. There are twelve maps in each mode. Maps remain available outside campaign progression through Play → Maps.

New lesson tools unlock one at a time. The optional fifth `game_rules` token records a capability mask: half send 1, Grow 2, Defend 4, directional routes 8, construction 16, bombs 32, artillery 64. Older maps default to all 127. The editor's Unlock all tools control removes lesson restrictions from the edited copy.

In editor Settings, cycle the mode and the New route control. Drag between two nodes to create or change a normal, forward, reverse or unfinished route. An unfinished route starts from the first node and requires an owned builder. Tap a node to change its owner, upgrade, exact garrison, AI type or player's bomb stock, or to place a flag. Neutral flag placement sets the required 50 troops. Starting flags must belong to players; deleting or changing their ownership clears stale references. Launch checks for a green player, opponents and a valid objective. Save and export retain the new rules.

## Verification and future changes

Run `.\opacha.ps1 :lwjgl3:tacticsTest` for deterministic simulation checks and a touch-driven tour at 480 × 800 and 390 × 844. Screenshots are written to `lwjgl3/build/tactics/`. The tour uses an isolated preference directory and slow simulation for reliable moving-fleet targeting. It tests real menu input, both flag selectors, buying and placing a bomb, constructing a route, artillery targeting, pausing, context restoration and pact acceptance/breaking.

Run `:lwjgl3:tacticsRules` for the combat scenarios, AI tool decisions and eight complete mixed-AI matches across the four modes. `:lwjgl3:victoryDesktop` and `:lwjgl3:victoryPhone` check the flag and coalition result screens and the Continue control without repeating the full tour.

When changing traffic rules, test several opposing fleets on the same route, allied crossings followed by betrayal, and unit removal during collision iteration. When changing digging, test partial funding, two simultaneous crews, final distance rounding, and troop conservation. Generated textures retain their CPU pixels so Android can restore them after graphics context loss. Touch tests must use `getViewPosition()` for controls inside a parent panel.

When multiple chats share a checkout, finish edits before taking a source snapshot, then build and test in that separate directory. Rebuilding the same packaged JAR during a running texture-restoration test can produce an invalid ZIP header when libGDX reopens an asset. Preserve the tested source hashes and publish the APK from that exact snapshot.
