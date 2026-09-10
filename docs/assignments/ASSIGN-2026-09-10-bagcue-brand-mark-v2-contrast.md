# ASSIGN-2026-09-10 — BagCue brand mark check contrast

Owner: `vibe-assets-creator`

Obligation: `ASSET-001`

User request (verbatim):

> Давай немного доработаем получившийся вариант, сейчас синяя галочка + синяя часть сумки слишком сильно сливаются между собой

## Scope

- Preserve the approved backpack, checklist, and front-pocket composition.
- Separate the verification check from both the white checklist card and blue pocket.
- Update the Compose vector and editable SVG from the same geometry.
- Regenerate Android/iOS launcher derivatives and visual-test goldens where the mark appears.
- Validate resources, inspect affected light/dark goldens, and run targeted Paparazzi verification.

## Decision

Use a white check with a dark-teal keyline. This retains the brand palette while giving the check a stable edge on both adjacent surfaces and at small UI sizes.

## File boundaries

- `shared/compose/src/commonMain/composeResources/drawable/bagcue_mark.xml`
- `docs/assets/**`
- `androidApp/src/main/res/mipmap-*/ic_launcher*.png`
- `iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/**`
- affected `shared/compose/visual-test/src/test/snapshots/images/**`
- affected `shared/compose/visual-test/contact-sheets/**`

## Checks

- Asset validation
- Compose resource generation/compilation
- Paparazzi record and visual inspection for SCREEN-001/004/010/011
- Targeted Paparazzi verify for SCREEN-001/004/010/011
