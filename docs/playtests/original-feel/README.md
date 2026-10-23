# Original-feel evidence

See [the interpretation and reference sources](../../original-feel.md).

| Folder | Evidence |
| --- | --- |
| before | 672 games before zero starts and the revised repeating timer |
| zero-first | 672 games after both changes, before map tuning |
| after | 672 games with a zero-troop economic supply node on map 8 |
| holdout | 112 games on maps 8 and 33, fresh seeds 3–10 |
| discarded-start-position | Failed 14-game map-8 experiment moving yellow's start |
| map-8-supply | Focused 14-game map-8 supply-node check |
| speed-check | Final 140-game Quick run: 7.0 seconds with a cached build and emulator stopped |
| phone, desktop | Native previews, zero-opening assertions, wave timings and feedback callback checks |

Each simulation folder contains every outcome, settings, build and policy fingerprints, timings and an HTML report. Defeats, eliminations and unfinished games are retained. The final main sweep uses seeds 1–2; fresh cases change decision errors, not authored geometry. Rendering-only changes made after the balance sweep do not change campaign data, policies or simulation rules. The main sweep and focused supply-node cases reproduce the same results.

Reproduce from the project root:

```powershell
.\playtest.ps1 -Mode Full -Name new-main
.\playtest.ps1 -Mode Quick -Levels '8,33' -SeedStart 3 -Seeds 8 -Name new-holdout
.\playtest.ps1 -Mode Feel -Name new-phone
.\playtest.ps1 -Mode Feel -Screen Desktop -Name new-desktop
```

The included native previews use a controlled scene to show full and partial shields, economic nodes, standing orders and three route states together. They are renderer checks; the campaign balance uses the authored maps. `wave-timing.csv` retains all 90 observed wave departures across five refresh rates and three speeds.

`checksums.csv` records SHA-256 hashes of the evidence files. Android verification records are saved separately under `android/`; OS vibration history establishes software delivery, while physical strength still needs a phone.
