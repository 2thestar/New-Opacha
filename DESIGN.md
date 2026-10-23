---
name: New Opacha
description: A paper-and-ink strategy interface with bright candy nodes, matte troops, and fine routes.
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
  battlefield-lime: "#BAF196"
  battlefield-cyan: "#77E5F2"
  battlefield-rose: "#FFA8B3"
  battlefield-yellow: "#FFDC83"
  battlefield-neutral: "#F3F0EB"
  battlefield-violet: "#CBA4ED"
  node-neutral: "#F7F4EF"
  battlefield-number: "#171C34"
  troop-lime: "#C3E7A9"
  troop-cyan: "#9AE1EA"
  troop-rose: "#F1B6C0"
  troop-yellow: "#EED99F"
  troop-edge-lime: "#91BF74"
  troop-edge-cyan: "#65BBC9"
  troop-edge-rose: "#CB8796"
  troop-edge-yellow: "#C6AA68"
  troop-edge-violet: "#9773B5"
  shield: "#147B9B"
  shield-light: "#85EDFC"
  conveyor-light: "#E6FFCE"
  vehicle-tread: "#849687"
  vehicle-butter: "#EAD8A6"
  vehicle-sage: "#A5BAB1"
  impact-warm: "#E9B486"
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
    fontSize: "0.70r cap height; fit within 1.44r"
    fontWeight: 600
  unit:
    fontFamily: "Quicksand"
    fontSize: "min(0.0225W, 1.32ry) cap height; fit within 1.65rx"
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
    backgroundColor: "{colors.node-neutral}"
    textColor: "{colors.battlefield-number}"
    typography: "{typography.node}"
    rounded: "{rounded.disc}"
  troop:
    textColor: "{colors.battlefield-number}"
    typography: "{typography.unit}"
    rounded: "{rounded.disc}"
  digging-rig:
    textColor: "{colors.battlefield-number}"
---

# Design System: New Opacha

## Overview

**Creative North Star: "Crisp buttons"**

Warm paper, rounded ink lines, and restrained yellow actions make the strategy easy to read and operate. Night mode keeps the same layout with dark surfaces and cream menu text. Bright candy nodes sit above quieter matte troops and fine routes. Fraunces titles give menus character; Quicksand carries choices and numbers.

This documents the implemented libGDX canvas. Menu styling retains the [Inkwell Stars](https://inkwellgames.com/games/stars/tutorial) reference. Battlefield authority is the approved [Crisp buttons direction and later decisions](.impeccable/mocks/battlefield/2026-10-21/DECISIONS.md), including Fine ink, the cozy digging rig, and liquid troop flow. Later decisions supersede the initial variant-C composition. The rejected 0.6.0 screenshot does not establish a new visual direction. The corrective implementation exports the original approved Canvas material into managed assets; `BattlefieldArt` and the game renderers supply runtime geometry.

**Key Characteristics:**

- Bright candy nodes with a narrow bevel and fixed upper-left light.
- Softer matte troops, fine contours, and fine ink routes.
- Rounded overhead equipment with upright counts inside the body and on the route.
- Brief motion tied to actual troop, construction, and combat events.

## Colors

### Primary

**Gold** marks Play, Start match, the Actions control, selected node rings, slider thumbs, and active control states. The yellow faction has its own softer battlefield-yellow face.

### Secondary

**Battlefield lime, cyan, rose, and yellow** identify node factions. Node faces use the original multi-stop gradients, retained with their rim gradients in the sidecar. **Node neutral** is the middle neutral face stop; **battlefield neutral** remains the pale primitive color. **Troop lime, cyan, rose, and yellow** are softer matte fills, each with its matching **troop edge**. The older **green, red, and cyan** tokens remain `UiStyle` colors for menus, chapter motifs, and existing interface accents; they are not the node palette.

Colorblind mode keeps the whole candy surface and changes yellow to **battlefield violet**. Transparent white rim notches distinguish green, red, yellow, and cyan by one, two, three, and four marks respectively, following `FractionType.ordinal()`. The notches sit on the existing surface. Troops and tank bodies retain their softer matte palette, with violet fill and contour for yellow. The old opaque pattern atlas, inset discs, and replacement rings are removed.

### Neutral

**Paper** fills menus and HUD margins; **board** fills the field and secondary buttons. **Text**, **outline**, **muted**, **rule**, and **neutral** are separate interface theme roles. **Ink** stays dark for colored-button labels, ticks, and menu shadows. **Battlefield number** supplies node, troop, and rig counts in both themes. **Vehicle butter, sage, and tread** color the warm rig and quiet equipment; **impact warm** marks contact puffs and muzzle flashes. White blending supplies the narrow node highlight.

The `night-*` tokens replace the seven mutable interface roles in Night. Battlefield colors and gold stay shared. Routes use the theme outline. Shields have a dark cyan stroke with a lighter cyan stroke over it in both themes. Enemy vapor has separate warm Day and Night tints, recorded in the sidecar. The existing `.tools/qa/theme-contrast.json` checks interface role pairs; its earlier faction checks do not certify the replacement battlefield palette.

## Typography

The frontmatter uses native formulas, not CSS lengths. `W` and `H` are the game surface's width and height in pixels; `B = floor(min(0.029 × H, 0.059 × W))`. Menu font sizes are truncated to integers. Battlefield formulas use node radius `r` and troop half-width and half-height `rx` and `ry` in game-camera coordinates.

Display is the menu title. Body is the standard button and slider-heading font. Label handles compact actions and slider values; caption handles the smallest labels. `BattlefieldArt.label` draws all battlefield numbers upright through `Fonts.unitFont`, scales to the requested cap height, and shrinks only when the text exceeds its width. Node counts use `0.70r` height within `1.44r`; troop counts use the frontmatter unit formula. Rig counts use `0.79 × rigSize` within `1.20 × rigSize`, inside the hull. Only AI home nodes receive names, below the node; ordinary nodes have no AI badge or name.

Quicksand SemiBold (600) and Fraunces Bold (700) are bundled with OFL licenses. When `lang_characters` is nonempty, both families switch to the original `font.ttf` to preserve glyph coverage. Native font metrics supply line height; there is no separate tracking scale. `Fonts.applyTheme` recolors menu fonts to text. Battlefield rendering sets its own number color and restores the shared font afterward.

## Layout

Positions and sizes use fractions of `W` and `H`, with a bottom-left origin. There is no breakpoint grid or shared spacing scale.

The battlefield remains an overhead map. Troops, tanks, the rig, and their numbers stay centered on the route. The rig's recessed count pocket may move forward along that same route to clear crossing traffic while keeping the rear intake clear; it never becomes an off-route label or leader line. Nodes keep their existing model positions, radii, and camera behavior.

The main menu centers a vertical stack: Play is `0.74W × 0.075H`; the next two choices are `0.74W × 0.060H`. Quit is a smaller text action. Setup uses a centered `0.86W × 0.70H` region, four `0.76W` sliders with `0.015H` row gaps, compact mode choices, and a bottom Start match control (`0.82W × 0.067H`).

Gameplay places Pause, Fast, Pacts, and Menu on the first row, with the speed card below at left `0.03W`, top `0.09H`, sized `0.94W × 0.066H`. Pacts uses `0.21W × 0.052H` at left `0.52W`, top `0.025H`; the target banner begins at top `0.18H`. Three node actions sit below the field. The action row uses `0.29W × 0.058H` buttons and `0.004W` extra touch reach; speed buttons add `0.008W`. The thin-bezel setting raises the bottom controls by `0.025H`. Labels fit within 92% of button width.

`UiSmokeTest` and `ThemeSmokeTest` use private preferences and test packaged JARs at 480 × 800 and 390 × 844. Do not rebuild a distribution while a native test uses it: context restoration reopens JAR assets. Concurrent work needs separate output directories or checkouts.

## Elevation & Depth

Raised buttons use an opaque ink underlay at `(+0.5s, −0.008W)` and a faint underlay at `(+s, −0.012W)`, where `s = max(1.5px, 0.0042W)`. Automatic lift applies to touchable, non-silent buttons shorter than `0.13H`; explicit shadow settings can also enable it.

A press moves the face and text down by up to `0.006W` and adds a 7% ink wash, scaled by the selection factor. The factor resets to one and decays through `destroy(1, 0.5)`; this is engine motion, not a fixed CSS duration.

Nodes use the original approved Canvas face, narrow rounded bevel, short upper-left shine, and tight soft contact shadow. `scripts/render-battlefield-art.cjs` exports that material to `assets/game/battlefield/nodes.png`: twenty 256-pixel cells with nominal radius 96, covering five factions, plain and Tower outlines, and ordinary and colorblind modes. Runtime draw bounds are `8r/3`; mipmapped linear filtering preserves the authored edges. The provenance file records source and asset hashes. Tower spikes share the continuous material. Troops use matte ovals with a fine faction contour and a small board-colored separation margin in free travel; they have no candy bevel, shine, or drop shadow. Equipment uses rounded nine-patch shapes; the rig retains its authored warm hull gradient. Button borders use the theme outline; their depth remains dark ink. Routes have no shadows. Exact material and runtime formulas are retained in the sidecar.

## Shapes

The reusable UI rounded rectangle is a managed 64-pixel nine-patch with 16-pixel corners. Its runtime scale is the `paper-shape` formula in frontmatter; do not replace it with a fixed desktop radius. Battlefield equipment uses its own 64-pixel rounded primitive with 16-pixel corners and a 256 × 128 warm rig-hull primitive with 48-pixel nine-patch splits. Plain nodes remain discs. A Tower has seven spikes molded into its outline, with two countable shield slots between each pair of spikes. Each shield is one dual-stroke segment; no shield badge or numeric shield counter. Troops are low-profile ovals. Lines have round endpoint caps.

## Components

The sidecar contains explicitly labeled HTML/CSS and SVG previews of ten native patterns. Controls use a 390 × 844 sample surface; the battlefield sample uses native source colors and geometry. These illustrate the Java renderers and are not runtime components. Use native screenshots and actual-model traces of newly rendered poses as review evidence; a still image cannot establish animation feel. Sidecar snippets are source transcriptions without a separate browser render. Generated tonal ramps are inspection aids, not additional game colors.

- **Buttons:** gold primary, board secondary, and borderless text actions. Native buttons have press feedback without a separate hover skin.
- **Speed control:** a paper card holds an outlined `0.016W` rail with `0.008W` gold progress. Thirteen evenly spaced stops select 0.1, 0.2, 0.3, 0.4, 0.5, 0.75, 1, 1.25, 1.5, 2, 2.5, 3, or 4x; inactive stop holes use paper. The gold handle is `0.20W` wide and `0.81 × cardHeight` tall, with dark `Fonts.smallFont` text. Pressing increases its half-width by `0.004W` and height to `0.87 × cardHeight`. Tapping away selects immediately; grabbing the handle preserves the pointer offset. Dragging uses nearest-stop rounding, matching the shown value. One pointer owns the drag; cancellation releases it and keeps the last selected value. Left/Right steps the focused control; the selected speed is saved through `VelocityManager`. Changed stops request a tick at most once per 70 ms. Android uses native View feedback (`SEGMENT_FREQUENT_TICK` on API 34+, otherwise `CLOCK_TICK`) and respects the system setting; desktop defaults to no feedback. Physical haptic feel still requires device verification.
- **Sliders:** heading above, value aligned right, theme-text progress over a rule track, gold thumb with a theme outline, and optional segment dots. Default thumb radius is `0.012H`, growing by up to `0.005H` while pressed.
- **Checkboxes:** theme-text label on the left, rounded theme-outline control on the right. Gold fill and a dark tick identify checked state.
- **Navigation:** a compact back arrow in menus; Pause/Resume, Fast/Normal, and Menu controls during play. Labels and gold state fills reflect the current state.
- **Node actions:** show Grow and Defend costs in the labels; hide an upgrade already installed. Actions opens further choices. Send half changes to Send all when active.
- **Nodes:** preserve the original exported candy material and centered dark count in every faction and theme. Gold arcs show selection and capture. Tower shields fill fourteen slots, two between each pair of seven spikes; each segment combines dark and light cyan round strokes at `1.085r`. Economy uses the selected Conveyor rim: fifteen rounded marks at `0.77r`, with every fifth mark pale, the central count clear, and no attached production icon.
- **Routes:** Fine ink uses a single theme-outline line for two-way travel and standalone repeated small chevrons for directed travel. Use `0.0018W` strokes; chevrons repeat every `0.0256W` with `0.006W` arms. Route ends stop outside node silhouettes. Construction shows a solid completed portion and a faint dotted portion ahead of the rig.
- **Troops and tanks:** softer matte troop ovals scale with group size and carry a fine faction contour. Groups above 50 use a tiny top-down tank built from rounded nine-patch hull and tread shapes. Counts stay upright inside both forms. Colorblind mode changes yellow to the matte violet fill and violet contour.
- **Digging rig:** one warm rounded hull, soft treads, a small rounded drill drum, and a recessed count pocket receive incoming diggers. The count stays inside the rig and centered on the route, including zero. Its pocket chooses forward offsets from zero to `2.4 × rigSize` in steps of `0.6 × rigSize`, keeping the rear intake clear. A cached displayed front follows actual construction progress through exponential smoothing with a `0.12` simulation-second time constant while crew remain. At zero, the displayed front and drum phase freeze exactly at their last values, without chasing the model front. A paused simulation clock also holds both. The rig center follows the ordinary troop travel corridor plus `1.7 × rigSize`. The model still has integer resource ticks and catch-up plateaus; this smoothing changes only the displayed pose.
- **Digger arrival:** incoming digger centers follow uninterrupted ordinary troop travel until actual model contact; they are not clamped at the rig's rear. Over the final `0.046W`, an ordinary matte troop shell narrows by up to 36% along the route and 26% across it. Its upright numeral keeps its full size until the real one-time crew transfer. The rig's rear intake meets that travel corridor. Presentation never changes troop speed, arrival, costs, or construction rules.
- **Troop contact:** actual movement and contact events drive curved necks formed from 32 strips at departure and friendly absorption. A local 20 × 20 mesh deforms the existing node material in the contact sector, preserving its fixed upper-left light. Friendly contact settles within 0.38 seconds. Enemy contact emits a warm crescent, five soft radial vapor puffs, and two tiny hot flecks outward from the approach side; they clear within 0.56 seconds. Counts remain clear and the model applies each transfer or combat result once. Reduced motion removes necks, contour deformation, and moving vapor, retaining a short static contact mark.
- **Cannon:** the candy node is the base, with a short top-down barrel. Real firing events trigger brief recoil, muzzle flash, a small projectile, and a warm burst at the target rim. Reload stays a small gold arc inside the node. Presentation never delays model damage or action legality. Reduced motion removes recoil, muzzle flash, and projectile travel.
- **Appearance:** Settings → Appearance offers Day, Night, and Abstract backgrounds. Choices apply and save immediately without restart. Backgrounds default off. Sliding menu transitions use the selected paper color beneath their panels, so the legacy light backdrop cannot flash in Night.
- **Chapter backgrounds:** six static edge motifs cycle with `chapter % 6`: bottom waves, upper-right arcs, left waves, top waves, lower-left arcs, and right waves. The 48-map campaign uses the first four: `chapter = max(0, levelIndex) / 12` with integer division. Menus use the highest unlocked chapter; a campaign map uses its current level; skirmish/editor use chapter 0. Placement shifts by `(chapter % 7) × 0.019`; cyan, green, and gold repeat by motif. Disc opacity is 4.5% in Day and 5.5% in Night; line opacity is 10% and 13%. Three bands use `0.0035 × backgroundWidth` strokes. No clock or randomness drives the art.
- **Capture feedback:** a player's new foothold emits a gold ring for 0.45 real seconds, alongside the existing capture sound. The ring expands from 1.1 to 1.6 node radii and fades. Android requests native CONFIRM feedback, falling back to CLOCK_TICK on older devices; requests are limited to one per 150 ms and respect Settings → Haptics and the Android system setting. Spawning a map, changing a node in the editor and enemy captures do not request this feedback. The simulation and input keep running.
- **Campaign:** six numbered tiles per row, two rows per chapter. Completed tiles have a small check; locked numbers remain readable. The paused concept card has one title, one short briefing and Play. Later tactical hints are opened on demand from Menu. Maps opens a separate free-play view of the same library, with an Edit map action.

Keyboard focus stays in the top scene. Tab or Up/Down moves focus, Enter/Space activates, and Left/Right adjusts a focused slider. Focus starts on a yellow action when available and draws an outer theme-text rectangle (`0.009W` gap; `max(2px, 0.005W)` stroke).

## Do's and Don'ts

### Do:

- Do use plain action labels and show costs before a player spends units.
- Do keep the board clear enough to follow routes and read node counts.
- Do preserve language coverage, whole-surface colorblind materials and rim notches, and visible keyboard focus.
- Do preserve the softer matte troop palette, with violet fill and contour for the yellow faction in colorblind mode.
- Do export the original approved node material and retain its provenance when rebuilding assets.
- Do keep every troop and rig count inside its body and centered on its route.
- Do keep appearance choices saved, with Day and plain backgrounds as the defaults.
- Do check affected states on desktop and phone before release.

### Don't:

- Don't add competing primary menu actions or repeated explanations.
- Don't add perpetual decorative motion or manipulative progression prompts.
- Don't use light menu text on yellow or faction-colored faces.
- Don't add shield counters, detached upgrade badges, or AI labels on ordinary nodes.
- Don't give troops the raised candy material reserved for nodes.
- Don't restore opaque colorblind atlas overlays, inset discs, or replacement rings.
- Don't replace the approved gradients, contours, shine, shadows, or rounded vehicle geometry with approximations.
- Don't replace the native canvas conventions with a generic web or Material interface.
