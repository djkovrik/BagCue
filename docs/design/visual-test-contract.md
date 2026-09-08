# BagCue visual-test contract

## Toolchain and ownership

- Production preview declarations live in `shared/compose/src/commonMain` beside the production UI/resources they render.
- Every preview uses the public sibling `*ComponentPreview` implementations from the component modules; the app shell is driven by `RootComponentPreview`.
- The Android host is the narrow conventional `:shared:compose:visual-test` module. Android-KMP host tests omit transitive generated resource classes required by Paparazzi; the conventional Android library supplies that complete resource classpath while scanner `Classpath` explicitly targets production bytecode in `:shared:compose`. It does not copy UI, resources, theme, or component contracts.
- Paparazzi `2.0.0-alpha05` is selected because it contains the AGP 9 Android Components migration and supports the Android-KMP plugin; ComposablePreviewScanner `0.9.1` performs project-package-only discovery.
- Generated source and test/record/verify tasks have an explicit dependency on `generateBagCuePreviewTests`. Preview discovery is cached, sorted, and encoded with `AndroidPreviewScreenshotIdBuilder`.
- The runtime scanner writes `build/reports/paparazzi/preview-coverage.json`, which maps every preview to its exact stable snapshot ID and locale/theme/font/device parameters.

## Matrix and design contract

| Screen | Canonical Material layout | Primary production components | Required visual risks represented |
| --- | --- | --- | --- |
| SCREEN-001 Today | feed/supporting pane | Scaffold, navigation bar/rail, hero action | first run, active, future-empty, read error, expanded |
| SCREEN-002 Create packing | form/supporting pane | app bar, checkbox rows, controls, filled action | defaults, selected kits, occupied date, empty merge, retained error/IME, expanded |
| SCREEN-003 Active session | grouped checklist/supporting pane | checkbox rows, grouped list, bottom completion actions, snackbar/dialog | partial, source hints, unresolved bag conflict, all packed, Undo, conflict error, expanded |
| SCREEN-004 Result | bounded result surface | result content, buttons, optional injected native-ad seam | hidden, eligible, consent, skipped, failure-without-gap, expanded |
| SCREEN-005 Sessions | chronological list/list-detail | app bar, calendar control, list rows, dialog/snackbar | mixed, calendar/filter, empty, delete/Undo, expanded |
| SCREEN-006 Templates | list/list-detail | navigation, list rows, extended FAB, dialogs | starter, long custom, empty, delete/duplicate, expanded |
| SCREEN-007 Template editor | form/detail pane | app bar, text fields, position list/editor, save action | new, starter, long/IME, retained save failure, expanded |
| SCREEN-008 Catalog | list/list-detail | app bar, list, FAB, dependency dialog | starter, match, dependency delete, empty/error, expanded |
| SCREEN-009 Item editor | form/detail pane | app bar, text fields, supporting/error text, save action | new, seed override/match, contextual item, validation/error/IME, expanded |
| SCREEN-010 Settings | grouped list/bounded pane | switch rows, buttons/links, status/support text | defaults, denied permission, analytics on/off, protected privacy, unresolved endpoint, expanded |
| SCREEN-011 Consent | modal/bounded modal | AlertDialog, policy action, filled/outlined decisions | required, RU 200%, retained save failure, expanded |

Every declared matrix row has light and dark rendering. Compact baselines use 390×844 dp; RU 200% variants use a taller 390×1260 dp capture so wrapping and scroll reachability are inspectable; expanded variants use 960×900 dp and exercise the app’s rail breakpoint. User-authored fixtures have stable IDs and deliberately long text, while bundled starter labels resolve through production Compose resources.

## Material 3 and accessibility audit rules

BagCue uses the Material 3 components available in Compose Multiplatform 1.12 / Material3 1.12.0-alpha03. Newer expressive navigation/list variants are not adopted solely for appearance; the stable baseline Material semantics are the cross-target fallback. App bars own top context, the root scaffold consumes safe drawing insets once, compact widths use the four-destination navigation bar, and expanded widths use the corresponding rail without changing destination identity.

Golden inspection must reject clipped or ambiguous user text, unintended normal-scale wrapping, lost RU glyphs, obscured actions under the modeled IME-risk states, navigation/content inset duplication, absent non-color selection cues, inaccessible modal order, ad overlap or blank ad gaps, and controls below the Material 48 dp target contract. The native Yandex view is replaced only at the preview seam by a production-resource-labelled deterministic surface; SDK initialization and network requests remain impossible in tests/previews.

## Golden and Lazyweb review order

Record only after the full scanner/test compile is green. Inspect every PNG in this strict order: SCREEN-001 through SCREEN-011, with light before dark and compact before risk/expanded variants within a screen. Then submit one representative production PNG for each screen to the current Lazyweb improve workflow in the same numeric order, with exactly one report in flight. Record every report URL, exact state/theme/locale/font-scale coverage, finding classification, and disposition before starting the next screen. Re-record only approved objective fixes, then run a clean full verify.
