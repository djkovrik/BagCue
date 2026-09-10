# BagCue navy/apricot asset and theme review

Reviewed 2026-09-10 for the user-approved variant 2 integration (ASSET-001). Earlier blue/teal v2 source files are historical provenance.

## Delivered artwork

The selected image-generation concept is preserved under `sources/bagcue_mark_v3_imagegen_concept.png`. `create-brand-source.cjs` adapts its backpack/card/check composition into one shared path inventory for SVG and Compose XML. Modest linear gradients preserve depth; runtime resources have no baked background and BagCueBrandMark retains Color.Unspecified.

Reviewed the 1024px iOS icon and `brand-review/brand-sizes.png` at 24, 32, 48 and 96px on both theme backgrounds. The apricot pocket and dark check remain distinct. The backpack body is intentionally subdued against dark blue surfaces. No clipping or raster background halo occurs.

Android legacy icons use 48dp per density; adaptive layers use 108dp per density and an inset foreground. Background is a solid cream fill, fixing the previous duplicated mark. The monochrome version has a transparent check cutout rather than an all-black merged shape. iOS icons use an opaque cream background with no alpha channel; all existing catalog entries are regenerated.

## Theme

Color.kt now uses navy actions, apricot secondary containers and neutral surfaces: warm off-white in light mode and blue-charcoal in dark mode. Error semantics and component role pairings are preserved. `brand-contrast.json` records all tested foreground/background roles: text pairs >= 4.5:1, pocket/body >= 5.19:1 and check/pocket >= 6.65:1.

Lazyweb reference lookup: daily checklist planner, mobile (Rootd daily mission checklist; Structured daily planner timeline; Me+ daily planner). Existing app evidence remains in `spec/app-spec/design.md`. This bounded theme update preserves the existing component hierarchy; the approved logo palette determines brand colors. No new hosted growth report was requested or run.

## Golden review and validation

`brand-review/inventory.json` lists all 118 snapshots grouped by SCREEN-001 through SCREEN-011. Reviewed all screen contact sheets in sequence, light/dark, compact/expanded and EN/RU 200% variants. Brand placement and color changes are consistent. Existing stress-layout issues (e.g. narrow Russian toolbar titles and snackbar action color use) are outside this brand-only change; this is not a full accessibility certification.

- `:androidApp:assembleDebug`: passed.
- `:shared:compose:visual-test:recordPaparazziDebug`: passed, 118 goldens.
- `:shared:compose:visual-test:verifyPaparazziDebug`: passed, complete matrix.
- Commands and outputs: `brand-record-build.log`, `brand-verify.log`.
- iOS image exports are checked locally; native iOS build/device validation requires macOS and was not run on this Windows host.

Reproduction: run `node docs/assets/create-brand-source.cjs`, `node docs/assets/generate-assets.cjs` (Sharp on NODE_PATH), then the Gradle record/verify tasks. Theme color source of truth is Color.kt; `update-brand-theme.cjs` documents the applied role map. After visual review, regenerate the manifest with `node docs/assets/build-asset-manifest.cjs` and validate via `validate-adapted-assets.py` with Pillow available.
