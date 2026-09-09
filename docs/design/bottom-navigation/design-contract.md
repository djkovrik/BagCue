# BagCue bottom navigation design contract

## Evidence

- Lazyweb Agentic Search: https://www.lazyweb.com/agentic-search/0736c037-7430-404a-9258-48b63e6da3cf
- Lazyweb improvement report: https://www.lazyweb.com/report/lazyweb/d10a76e7-529a-418c-90c5-de780f9d10ff/?source=create
- Local baseline: `docs/design/visual-review/screens/SCREEN-001-contact-sheet.png`

The selected references support a conventional persistent icon-and-label model, a clearly differentiated active destination, and a visually distinct navigation container. Center-action navigation was rejected because BagCue has four peer destinations and no primary global creation action.

## Visual behavior

- Keep Material 3 `NavigationBar` and `NavigationBarItem` as the interaction and accessibility primitives.
- Present the compact bar as a softly floating dock: 12dp horizontal breathing room, 8dp vertical breathing room, the theme's `extraLarge` shape, `surfaceContainerHigh`, and restrained 3dp shadow.
- Retain the Material active indicator; use the current semantic color roles instead of introducing a new palette.
- Keep the existing Material Rounded tab icons. They are coherent, tintable, recognizable, and already mapped to AppSpec assets ASSET-002 through ASSET-005.
- Keep labels visible in every state. At font scale 150% and above, retain the accessible two-row layout rather than truncating labels.

## Motion

- On selection, the icon settles at 1.08x scale and 2dp upward lift using `MaterialTheme.motionScheme.fastSpatialSpec()`.
- The spring is interruptible and reversible because the animation target follows current selection state.
- Material's existing indicator and color effects remain responsible for the state transition around the icon.
- The common navigation implementation reads Compose's platform-provided `MotionDurationScale`. At a zero duration scale it explicitly swaps both spatial animations to `snap()`, so the visual state changes immediately with no animated travel on Android and iOS. Selection remains redundant through indicator, color, icon position/scale, label, and tab semantics.

## Responsive and accessibility contract

- Compact widths below 600dp use the dock; 600dp and above continue to use the navigation rail.
- Each destination remains a Material selectable tab with the existing stable test tag.
- The large-font two-row variant remains fully reachable and avoids label ellipsis at 200% font scale.
- The dock owns the same system-bar insets as the canonical `NavigationBar`; no parent duplicates them.

## Verification record

- Android-host Compose tests: passed.
- Navigation contract tests cover the zero-duration reduced-motion branch and the stable one-row / two-by-two destination model.
- AppSpec 2.0 validation: passed.
- Asset delivery validation: passed with no errors or warnings; ASSET-002 through ASSET-005 remain unchanged.
- Paparazzi record and verify: passed for the complete 118-preview matrix.
- Visual inspection: 96 compact goldens changed as expected, including 48 dark-theme and 38 RU/200% cases; navigation-rail renders did not change.
- Inspected representative active states for Today, Sessions, Templates, and Settings plus the two-row RU/200% layout. No clipping, overlap, label truncation, snackbar collision, or theme inconsistency was found.
- Root detekt reached one pre-existing unrelated `LongMethod` finding in `SessionScreen.ResultScreen`; the modified navigation source reported no finding.
- Post-golden Lazyweb report: https://www.lazyweb.com/report/lazyweb/92c7359a-86cb-4763-963b-03606ffedadf/?source=create
- A fresh focused re-audit returned PASS after the explicit reduced-motion policy and stable large-font row-model tests were added. A composed semantics-tree/animation-clock test remains optional defense-in-depth rather than a blocker because the production control remains canonical `NavigationBarItem` inside `selectableGroup`.
