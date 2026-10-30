# Gameplay controls and rules

## Controls

Swipe from an owned node toward a linked node to set its repeating troop route. Taps on the same node cycle full send and half send. Artillery adds a third state: cannon aiming, shown by a thicker barrel. From that state, swipe from the gun onto an enemy node or tap an enemy node to fire. Tap the gun again to return to full send, or use Cancel. Artillery aiming does not issue troop orders.

Actions opens route construction and artillery building. A targeting command has a Cancel button. Pause cancels targeting and releases any held troop group.

## Bombs

Keep at least two owned Grow factories running. Hold a moving group of at least 51 troops for half a second to convert the entire group into one bomb. The group must have departed a factory and must be on a completed edge, away from its endpoints. Troops leaving an ordinary node and working diggers do not qualify. The group pauses while held; dragging, a second finger, cancellation or releasing early lets it continue.

The bomb replaces the whole group. It keeps its location, direction and route speed, and travels separately from combat troops. At an owned node it follows that node's standard route arrow before ordinary troops. Without a usable arrow it waits. On an enemy or neutral node it waits and cannot plant a mine until its owner controls that node.

Tap your bomb:

- On an edge: destroy the entire edge, all troops of every faction on it, and other bombs on it. Orders using that edge are cleared. Other edges and node garrisons survive.
- At your own node: plant one hidden mine. A node with a mine cannot consume another bomb.

A mine triggers when a hostile invasion would capture its node. All invading survivors die, the node becomes neutral with zero troops, and it receives 14 shields with a capacity of 14 for ten game seconds. After ten game seconds the ordinary capacity of seven returns. Failed invasions, friendly or allied arrivals and artillery do not trigger mines. A mined flag does not award victory to an invasion that the mine destroys.

Bombs and mines are visible only to their owner. Enemy hit testing and AI threat calculations cannot inspect them. AI weapon decisions inspect only their own secret cargo and public troop traffic. Old purchase and delivery controls are retired.

## Artillery and digging

| Action | Rule |
| --- | --- |
| Artillery setup | Costs 20 troops and replaces the existing upgrade. The first shot is ready immediately. |
| Artillery shot | Costs 20 troops. Removes 20% of the target's current troop population, including fractional population. Shields, ownership and upgrades survive. Reload takes five game seconds. |
| Artillery range | Three typical route lengths measured directly between nodes. Only hostile, non-neutral nodes qualify. |
| Route blueprint | Actions > Route blueprint, then choose a second node. Existing connections cannot be duplicated. Crossing another edge does not create a junction. |
| Digging | Troops dig from the blueprint's start and are consumed as work advances. A typical route costs 120 troops, 1.5 times the previous 80. Cost scales with distance and rounds up. |
| Completed route | Public in both directions. Surviving diggers continue as troops. Directional speed is 2 times normal forward and 0.3 times normal in reverse. |

The typical route length is the median original route length on the map. New construction does not change it. Bomb movement, mine shield expiry and artillery reload follow simulation time, including pause, velocity and fast-forward.

## Match modes

- Conquest: the last surviving faction wins.
- Neutral flag: the first successful capture of the marked neutral supernode wins.
- Starting flags: the first successful capture of an enemy starting flag wins.
- Alliances: offers require acceptance. Friendship is pairwise. Allied fleets pass each other; arrivals add 70% of their size without changing ownership. Breaking a pact makes that pair hostile for the rest of the match. Surviving players win together when every surviving pair is allied.

## Maps and editor

The 48 fixed campaign maps are authored in `scripts/build-campaign.py` and generated into `assets/campaign/levels.json`. Campaign briefings and hints describe the current controls. Owned campaign nodes start at zero troops.

The editor supports node owners, upgrades, exact garrisons, AI types, bombs waiting at nodes, a mine toggle, flags, directional routes and blueprints. Bomb and mine placement in the editor authors a starting scenario. Gameplay conversion still requires two factories and an eligible departing troop group.

Exported maps preserve mobile bombs on edges and at nodes, mines, mine shield expiry, construction progress, artillery reload and shields, flags and pacts. Ordinary moving troops remain outside map export. Existing unfinished routes retain their saved work costs; newly created routes use 120 troops per normal length. Legacy route bombs and active couriers load as secret mobile bombs; legacy stock is retained for compatibility but cannot purchase or dispatch bombs. Legacy stacked traps load as one mine.

New records in the optional `tactics` section:

| Record | Fields |
| --- | --- |
| `mobileBomb` | `owner routeIndex targetNodeIndex waitingNodeIndex progress` |
| `mineShield` | `nodeIndex expiryGameSeconds` |
| `depot` | `nodeIndex legacyStock mineCount` |

Indices refer to the preceding node and route lists. An absent route, target or waiting node uses -1. Progress runs from `link.one` at zero to `link.two` at one.

## Verification

Run `.\opacha.ps1 :lwjgl3:weaponsTest` for deterministic combat checks and real pointer gestures at 480 by 800 and 390 by 844. Native screenshots are saved under `lwjgl3/build/weapons/`. Checks cover the full/half/cannon cycle, swipe and tap fire, long-press conversion and its release, edge destruction, mines, owner-only visibility, arrows, pause, cancellation, two-finger input, save/load, AI tools, digging conservation and percentage artillery damage. `:lwjgl3:timingRules` compares simulation timing across frame rates and speeds. `:lwjgl3:campaignRules` validates all authored maps.

When changing these controls, keep pointer down, click, pointer up and long-press consumption distinct: the game dispatches clicks before pointer-up. A conversion release must not become detonation, and a shot must not also send troops. Release held units on drag cancellation, second-finger input, pause and native touch cancellation. Hit tests must match the rendered troop location, which is inset from the route endpoints.

Keep hidden cargo separate from combat units and public threat data. Resolve a mine before assigning hostile ownership, so a destroyed invasion cannot win a flag. Save remaining temporary shield time with the same simulation clock used for movement and reload.

Language resources use UTF-16. Preserve that encoding. Run native tests after finishing source changes; rebuilding a packaged JAR during a graphics-context restoration check can invalidate an asset read.

Autoskill review: project note. These capture-order, input-dispatch and rendering rules are specific to this game; executable checks and this document are their maintained home.
