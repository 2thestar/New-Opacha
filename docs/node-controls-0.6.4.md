# Node controls, 0.6.4

The selected-node full/half handler returned before the stop-arrow handler, making it impossible to stop repeating sends. The native regression reproduced this on 0.6.3 with `Tapping an unselected sending node must stop its arrow`.

## Corrected behavior

| Interaction | Result |
| --- | --- |
| Swipe from an owned node toward a linked target | Start or redirect repeating sends; keep the source selected |
| Tap that sending source, or press Stop sending | Clear its arrow; keep the current full/half choice |
| Tap a selected source with no repeating route | Cycle full/half; an artillery node adds cannon aiming |
| Tap a linked target after selecting a source | Send once; retain the target's own route |
| Tap an unlinked friendly node | Switch selection in one tap |
| Arm a cannon | Stop automatic troop departures while aiming |
| Tap the armed gun again, empty space, or Cancel | Leave aiming; no shot or troop order |

Stopping prevents future launches. Troops already on a route keep moving. Tactical pause still accepts orders.

The contextual Stop control is visible only while the selected source has a repeating route. It works with the optional half button disabled. Removing a route also removes the Stop control. The current thin-bezel offset applies to both node-control rows when opened. Stop uses existing gold/ink tokens; the battlefield material and weapon rules are unchanged.

## Verification scope

The native libGDX walkthrough drives the real input dispatcher, model and renderer at 390 × 844 and 480 × 800. It covers ordinary nodes, factories and Towers; stopping selected/unselected sources; keeping travelling troops; full/half single sends; redirects; friendly target routes; forbidden blueprints; long presses; cancellation; two-finger input; ownership loss; upgrades; cannon tap/swipe modes; Day/Night; hidden half control; thin bezels; and the opened Actions menu.

Commands:

```powershell
.\opacha.ps1 :lwjgl3:nodeControlsTest
.\opacha.ps1 :lwjgl3:weaponsTest :lwjgl3:uiTest
.\opacha.ps1 :android:exportInstallZip :android:lintDebug
```

Screenshots: `lwjgl3/build/node-controls/final-Phone/` and `final-Desktop/`. Runtime assertions establish control behavior; native captures establish the affected layout. These are native desktop runs at phone dimensions. Android emulator testing remains paused at Boris's request; this update has no new physical-phone interaction or haptic verification.

Both sizes passed 47 node-control assertions, 50 weapon-rule assertions, 75 tactics assertions, the full weapon gesture sequence and the general UI tour. Nine node states per size were visually inspected, including the opened Actions menu. The APK build and Android lint passed with zero errors and six existing warnings.

## Package

Android version 0.6.4, code 10, package `io.github.twothestar.newopacha`. It uses the existing signing certificate and contains ARM64 and x86_64 libraries. The install ZIP contains exactly the signed APK. Signature, ZIP alignment, 16 KiB native segment alignment and all 48 campaign maps are checked separately.

APK SHA-256: `15da66e3b4aea91ea14e6d628a26b99049135722530a5d310c1775ad8292d8e4`.

ZIP SHA-256: `ce34b68e2bdc6efc7cc612777fce6e72768dff9519bd1f76a97e84b091af6cb3`.

## Reuse

Autoskill outcome: project note and executable regression. Check stopping with an active arrow before declaring a tap-cycle change complete. Preserve tap, long-press and pointer-up consumption separately. Let native button reactions settle before asserting. This extends the existing project controls guidance and game UI testing; no skill was installed or published.
