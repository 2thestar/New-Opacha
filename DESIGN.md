---
name: New Opacha
description: A quiet paper-and-ink strategy interface, informed by Boris's Inkwell Stars reference.
colors:
  paper: "#FCF0ED"
  board: "#FCFBFA"
  ink: "#302A28"
  text: "#302A28"
  outline: "#302A28"
  neutral: "#FCFBFA"
  white: "#FFFFFF"
  muted: "#716762"
  rule: "#D8CECA"
  gold: "#FED23F"
  green: "#6CD6A0"
  red: "#FF9D91"
  cyan: "#83CDE3"
  night-paper: "#1D232B"
  night-board: "#29323D"
  night-text: "#F4EFE8"
  night-outline: "#93A1AF"
  night-muted: "#A4B1BE"
  night-rule: "#566271"
  night-neutral: "#E0E5E8"
typography:
  display:
    fontFamily: "Fraunces"
    fontSize: "floor(1.9 * B)px"
    fontWeight: 700
  body:
    fontFamily: "Quicksand"
    fontSize: "Bpx"
    fontWeight: 600
  label:
    fontFamily: "Quicksand"
    fontSize: "floor(0.86 * B)px"
    fontWeight: 600
  caption:
    fontFamily: "Quicksand"
    fontSize: "floor(0.76 * B)px"
    fontWeight: 600
  node:
    fontFamily: "Quicksand"
    fontSize: "floor(2.25 * B) * 0.3px"
    fontWeight: 600
  unit:
    fontFamily: "Quicksand"
    fontSize: "floor(1.4 * B) * 0.3px"
    fontWeight: 600
rounded:
  paper-shape: "16 * min(W / 390, min(componentWidth, componentHeight) / 36)px"
  disc: "50%"
components:
  button-primary:
    backgroundColor: "{colors.gold}"
    textColor: "{colors.ink}"
    typography: "{typography.body}"
    rounded: "{rounded.paper-shape}"
  button-secondary:
    backgroundColor: "{colors.board}"
    textColor: "{colors.text}"
    typography: "{typography.body}"
    rounded: "{rounded.paper-shape}"
  button-quiet:
    textColor: "{colors.text}"
    typography: "{typography.label}"
  speed-control:
    backgroundColor: "{colors.paper}"
    textColor: "{colors.ink}"
    typography: "{typography.label}"
    rounded: "{rounded.paper-shape}"
    width: "0.94W"
    height: "0.066H"
  slider:
    backgroundColor: "{colors.rule}"
    textColor: "{colors.text}"
    typography: "{typography.label}"
  checkbox:
    backgroundColor: "{colors.board}"
    textColor: "{colors.text}"
    typography: "{typography.label}"
    rounded: "{rounded.paper-shape}"
  game-navigation:
    backgroundColor: "{colors.board}"
    textColor: "{colors.text}"
    typography: "{typography.body}"
    rounded: "{rounded.paper-shape}"
  button-secondary-night:
    backgroundColor: "{colors.night-board}"
    textColor: "{colors.night-text}"
    typography: "{typography.body}"
    rounded: "{rounded.paper-shape}"
  appearance-night:
    backgroundColor: "{colors.night-paper}"
    textColor: "{colors.night-text}"
    typography: "{typography.body}"
  planet:
    backgroundColor: "{colors.neutral}"
    textColor: "{colors.ink}"
    typography: "{typography.node}"
    rounded: "{rounded.disc}"
---

# Design System: New Opacha

## Overview

Warm paper, rounded ink lines, and restrained yellow actions make the strategy easy to read and operate. Night mode keeps the same layout with dark surfaces and cream menu text. Shallow depth makes buttons and nodes feel tangible while the board stays quiet. Fraunces titles give menus character; Quicksand carries choices and numbers.

This documents the implemented libGDX canvas, scoped to the existing game's interface refinement. Visual reference: [Inkwell Stars](https://inkwellgames.com/games/stars/tutorial).

## Colors

### Primary

**Gold** marks Play, Start match, the Actions control, selected node rings, slider thumbs, and active control states.

### Secondary

**Green, red, and cyan** identify factions. Gold also identifies the yellow faction; on the board it is not exclusively an action color. Neutral nodes use the neutral color, which stays light in Night mode.

### Neutral

**Paper** fills menus and HUD margins; **board** fills the field and secondary buttons. **Text**, **outline**, **muted**, **rule**, and **neutral** are separate theme roles. **Ink** stays dark for colored-button labels, node and unit counts, node rims, ticks, and shadows. **White** supplies node highlights.

Unsuffixed tokens describe Day, the default. The `night-*` tokens replace the seven mutable roles in Night. Faction colors and gold stay shared. The ten declared role pairs in `.tools/qa/theme-contrast.json` pass APCA and WCAG, including coral node counts and Night outlines.

## Typography

The frontmatter uses native formulas, not CSS lengths. `W` and `H` are the game surface's width and height in pixels; `B = floor(min(0.029 × H, 0.059 × W))`. Each generated font size is truncated to an integer before the node and unit render scales are applied.

Display is the menu title. Body is the standard button and slider-heading font. Label handles compact actions and slider values; caption handles the smallest labels. Node and unit fonts render through the game camera.

Quicksand SemiBold (600) and Fraunces Bold (700) are bundled with OFL licenses. When `lang_characters` is nonempty, both families switch to the original `font.ttf` to preserve glyph coverage. Native font metrics supply line height; there is no separate tracking scale. `Fonts.applyTheme` recolors menu fonts to text and keeps node/unit fonts in dark ink.

## Layout

Positions and sizes use fractions of `W` and `H`, with a bottom-left origin. There is no breakpoint grid or shared spacing scale.

The main menu centers a vertical stack: Play is `0.74W × 0.075H`; the next two choices are `0.74W × 0.060H`. Quit is a smaller text action. Setup uses a centered `0.86W × 0.70H` region, four `0.76W` sliders with `0.015H` row gaps, compact mode choices, and a bottom Start match control (`0.82W × 0.067H`).

Gameplay places Pause, Fast, Pacts, and Menu on the first row, with the speed card below at left `0.03W`, top `0.09H`, sized `0.94W × 0.066H`. Pacts uses `0.21W × 0.052H` at left `0.52W`, top `0.025H`; the target banner begins at top `0.18H`. Three node actions sit below the field. The action row uses `0.29W × 0.058H` buttons and `0.004W` extra touch reach; speed buttons add `0.008W`. The thin-bezel setting raises the bottom controls by `0.025H`. Labels fit within 92% of button width.

`UiSmokeTest` and `ThemeSmokeTest` use private preferences and test packaged JARs at 480 × 800 and 390 × 844. Do not rebuild a distribution while a native test uses it: context restoration reopens JAR assets. Concurrent work needs separate output directories or checkouts.

## Elevation & Depth

Raised buttons use an opaque ink underlay at `(+0.5s, −0.008W)` and a faint underlay at `(+s, −0.012W)`, where `s = max(1.5px, 0.0042W)`. Automatic lift applies to touchable, non-silent buttons shorter than `0.13H`; explicit shadow settings can also enable it.

A press moves the face and text down by up to `0.006W` and adds a 7% ink wash, scaled by the selection factor. The factor resets to one and decays through `destroy(1, 0.5)`; this is engine motion, not a fixed CSS duration.

Nodes use two low-opacity offset discs beneath an ink rim, a raised color face, and one small white highlight. Button borders use the theme outline; their depth remains dark ink. Routes have no shadows. Exact native formulas are retained in the sidecar.

## Shapes

The reusable rounded rectangle is a managed 64-pixel nine-patch with 16-pixel corners. Its runtime scale is the `paper-shape` formula in frontmatter; do not replace it with a fixed desktop radius. Circles remain true discs. Lines have round endpoint caps. Button borders use the stroke formula in Elevation & Depth.

## Components

The sidecar contains explicitly labeled HTML/CSS previews of ten native patterns at a 390 × 844 sample surface. They illustrate the Java controls; they are not runtime components. Generated tonal ramps are inspection aids, not additional game colors.

- **Buttons:** gold primary, board secondary, and borderless text actions. Native buttons have press feedback without a separate hover skin.
- **Speed control:** a paper card holds an outlined `0.016W` rail with `0.008W` gold progress. Thirteen evenly spaced stops select 0.1, 0.2, 0.3, 0.4, 0.5, 0.75, 1, 1.25, 1.5, 2, 2.5, 3, or 4x; inactive stop holes use paper. The gold handle is `0.20W` wide and `0.81 × cardHeight` tall, with dark `Fonts.smallFont` text. Pressing increases its half-width by `0.004W` and height to `0.87 × cardHeight`. Tapping away selects immediately; grabbing the handle preserves the pointer offset. Dragging uses nearest-stop rounding, matching the shown value. One pointer owns the drag; cancellation releases it and keeps the last selected value. Left/Right steps the focused control; the selected speed is saved through `VelocityManager`. Changed stops request a tick at most once per 70 ms. Android uses native View feedback (`SEGMENT_FREQUENT_TICK` on API 34+, otherwise `CLOCK_TICK`) and respects the system setting; desktop defaults to no feedback. Physical haptic feel still requires device verification.
- **Sliders:** heading above, value aligned right, theme-text progress over a rule track, gold thumb with a theme outline, and optional segment dots. Default thumb radius is `0.012H`, growing by up to `0.005H` while pressed.
- **Checkboxes:** theme-text label on the left, rounded theme-outline control on the right. Gold fill and a dark tick identify checked state.
- **Navigation:** a compact back arrow in menus; Pause/Resume, Fast/Normal, and Menu controls during play. Labels and gold state fills reflect the current state.
- **Node actions:** show Grow and Defend costs in the labels; hide an upgrade already installed. Actions opens further choices. Send half changes to Send all when active.
- **Nodes and routes:** keep dark unit counts centered, use a gold selection ring, and retain the original patterned textures in colorblind mode. Owned routes have theme-outline edges and faction centers; neutral routes are muted dashes. Five repeated chevrons and gun symbols use theme text, dark in Day and cream in Night. Artillery range, reload, and digger cues use gold in Night; shot lines use coral.
- **Appearance:** Settings → Appearance offers Day, Night, and Abstract backgrounds. Choices apply and save immediately without restart. Backgrounds default off. Sliding menu transitions use the selected paper color beneath their panels, so the legacy light backdrop cannot flash in Night.
- **Chapter backgrounds:** six static edge motifs cycle with `chapter % 6`: bottom waves, upper-right arcs, left waves, top waves, lower-left arcs, and right waves. The 48-map campaign uses the first four: `chapter = max(0, levelIndex) / 12` with integer division. Menus use the highest unlocked chapter; a campaign map uses its current level; skirmish/editor use chapter 0. Placement shifts by `(chapter % 7) × 0.019`; cyan, green, and gold repeat by motif. Disc opacity is 4.5% in Day and 5.5% in Night; line opacity is 10% and 13%. Three bands use `0.0035 × backgroundWidth` strokes. No clock or randomness drives the art.
- **Capture feedback:** a player's new foothold emits a gold ring for 0.45 real seconds, alongside the existing capture sound. The ring expands from 1.1 to 1.6 node radii and fades. Android requests native CONFIRM feedback, falling back to CLOCK_TICK on older devices; requests are limited to one per 150 ms and respect Settings → Haptics and the Android system setting. Spawning a map, changing a node in the editor and enemy captures do not request this feedback. The simulation and input keep running.
- **Campaign:** six numbered tiles per row, two rows per chapter. Completed tiles have a small check; locked numbers remain readable. The paused concept card has one title, one short briefing and Play. Later tactical hints are opened on demand from Menu. Maps opens a separate free-play view of the same library, with an Edit map action.

Keyboard focus stays in the top scene. Tab or Up/Down moves focus, Enter/Space activates, and Left/Right adjusts a focused slider. Focus starts on a yellow action when available and draws an outer theme-text rectangle (`0.009W` gap; `max(2px, 0.005W)` stroke).

## Do's and Don'ts

### Do:

- Do use plain action labels and show costs before a player spends units.
- Do keep the board clear enough to follow routes and read node counts.
- Do preserve language coverage, colorblind textures, and visible keyboard focus.
- Do keep appearance choices saved, with Day and plain backgrounds as the defaults.
- Do check affected states on desktop and phone before release.

### Don't:

- Don't add competing primary menu actions or repeated explanations.
- Don't add perpetual decorative motion or manipulative progression prompts.
- Don't use light menu text on yellow or faction-colored faces.
- Don't replace the native canvas conventions with a generic web or Material interface.
