# New Opacha 0.5.0 validation

Version 0.5.0, Android version code 5, package `io.github.twothestar.newopacha`. Runtime sources and assets match the frozen `.tools/release-0.5.0-ship` build. The preview tag identifies the committed source.

Published as [v0.5.0-preview](https://github.com/2thestar/New-Opacha/releases/tag/v0.5.0-preview), from source commit `74aacd24c4e128055cff3398ef9be4073990da69`. After publishing, the public tag was checked against that commit. The install ZIP was downloaded anonymously and verified against the SHA-256 below; its extracted APK matches the tested signed APK. Later documentation-only corrections do not change the release tag or package.

## Checks completed

- 119 deterministic rules assertions cover local stock, crew charges, multi-leg delivery, directional travel, fastest supply paths, pause/resume, concealed couriers and node traps, route mines, allied crossings and betrayal, three-bomb reservations, post-capture casualties, flag wins, legacy stock migration, export/import and editor cleanup. Existing digging, artillery and AI scenarios also pass.
- Eight complete mixed-AI games finish across the four modes and two seeds.
- 3,367 campaign data checks pass across all 48 maps, including import/export and editor operations.
- Full campaign touch tours pass at 480 x 800 and 390 x 844: every map, lesson brief, library/editor entry, capture, hint, victory/Next, defeat and final reward. Bomb and artillery touch tours also pass at both sizes, including a measured targeting-prompt width check, courier dispatch and delivery, node trapping, route planting, graphics restoration and diplomacy.
- The editor cleanup was added after the full visual sweeps. Its new scenarios pass in the final 119-check suite. The final APK's editor was also used on Android to set three traps and local stock, then launch that map successfully.
- A separate 112-match, seven-policy regression sweep covers two later maps per mode and two seeds. All eight maps have winning approaches; 68 policy runs win and four reach the 300-second stop unfinished. Only nine runs use a tool, so this is weak evidence about bomb balance. See [raw results](playtests/bombs-0.5.0.csv) and [balance notes](balance.md).
- Android lint: zero errors, six existing manifest/build warnings.

## Android package

The final APK installs successfully over the earlier signed app on the owned Pixel 8a emulator, Android 15, x86_64. Manual touch checks cover the economic-node purchase, local node trap, courier dispatch, visible route bomb after delivery, editor stock and three-trap controls, editor launch, and the shorter targeting prompt. No AndroidRuntime or System.err failures were reported during the final check. Physical Pixel haptic strength and human enjoyment remain unmeasured.

APK Signature Scheme v2 verifies with the existing certificate SHA-256:
`270631fbbc296fba0e6156cd6a0bda2377b5eb7761665c61b4c714c5b35c461a`.

All four ARM64/x86_64 native libraries have ELF load alignment of at least 16 KiB. APK ZIP alignment passes `zipalign -c -P 16 4`. The install ZIP contains exactly the same signed APK. Sizes and hashes were read after packaging completed.

| File | Bytes | SHA-256 |
| --- | ---: | --- |
| New-Opacha-0.5.0-install.zip | 6,352,284 | `ab0667bd44e3db81bb78714d12c5c4a8115fe7a1177628e25607b79ff06e8802` |
| New-Opacha-0.5.0.apk | 7,776,709 | `c8cac3f96719f762e45d5409b65ac5849105812200a9ca866942066124e361f8` |

## Reproduce

Run the test tasks through `opacha.ps1`. In PowerShell pass dotted or comma-separated properties as quoted arguments:

```powershell
.\opacha.ps1 -GradleArguments @(':android:exportInstallZip', ':android:lintDebug', ':lwjgl3:tacticsRules', ':lwjgl3:tacticsTest', ':lwjgl3:campaignTest')
.\opacha.ps1 -GradleArguments @(':lwjgl3:balancePlaytest', '-PcampaignLab', '-PlabLevels=10,12,22,24,34,36,46,48', '-PlabSeeds=2', '-PlabName=bombs-0.5.0')
```

Keep running JVMs on immutable packaged JARs; rebuild a separate snapshot when sources change. [Bomb regression notes](tactics.md#bomb-regression-notes) record the reusable pause, ownership and editor cases.
