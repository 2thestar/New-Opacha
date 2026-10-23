# Expedited test evidence

- `before/`: 672 current-version campaign games before revising map 28.
- `after/`: the same 672 cases after adding map 28's rear supply node.
- `final/`: the final source, including corrected first-lesson wording. Every match row is identical to `after/`.
- `holdout/`: 56 map 28 games on seeds 3–10.
- `random/`: 448 hard random-map games, with both starting seats and seven policies.
- `flag-phone.csv` and `flag-desktop.csv`: old versus new flag travel spread on 64 generated maps at each viewport.
- `sha256.json`: checksums of the retained evidence files.

Campaign difficulty is authored in each map. The earliest `before` manifest says `hard`, which was the requested default; import replaces it with the authored difficulty. Accelerated batches use a 480 × 800 engine window. Older manifests' `screen` field records the command's default screen preference; later runner versions also record the actual viewport explicitly. Native touch checks run at 390 × 844 and 480 × 800.

These are scripted policies using the production game model, not recruited players. Final campaign results are 439 wins and 16 timeouts out of 672. Every map has a winning line in this sample. The matched map change gains ten wins and loses none; all other maps' rows are identical. All eight holdout seeds retain a win, but nine holdout matches time out. Hard random-map results are 69/448 wins.

The final validation also passed both native interface tests, 3,367 campaign assertions, 119 tactics assertions, Android compilation and lint (zero errors, seven existing warnings), and an update installation in the 1080 × 2400 Android emulator. Build outputs and screenshots remain under `build/playtest/`; they are intentionally excluded from Git.

See [the fast playtest guide](../../fast-playtest.md) for interpretation, research references and the human playtest procedure.
