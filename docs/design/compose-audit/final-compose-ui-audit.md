# Final Compose UI audit — refreshed implementation

Date: 2026-09-09  
Assignment: `final-compose-ui-audit` re-audit  
Scope: read-only review of the current production `commonMain` Compose UI, navigation shell, theme, resources, icon mapping, and refreshed golden index/contact sheets. No production source was changed and this specialist did not run Gradle.

## Verdict

The corrective pass resolved the first audit's critical preview-routing/evidence defect and most high-value component-semantic issues. The refreshed index is internally sound: all 118 golden paths exist and every file hash matches `docs/design/visual-review/index.json`; all 11 contact sheets were inspected. SCREEN-008 now renders Catalog and SCREEN-009 renders the intended catalog and session-position editors.

The remaining objective gaps are narrower: expanded layouts are still rail-plus-single-pane rather than adaptive list-detail/supporting layouts, and progress/loading/undo announcements are incomplete outside the newly corrected Settings error state. SCREEN-003 and SCREEN-010 now expose their named states in the initial viewport. SCREEN-007 RU/200% is tall but legible rather than clipped. The custom two-row 200%-font NavigationBar and date-selection controls remain component-semantic risks.

## Evidence inspected

- App/navigation: `shared/compose/src/commonMain/kotlin/com/sedsoftware/bagcue/compose/App.kt` (`App`, `ProductShell`, `PrimaryNavigationBar`, `PrimaryNavigationRail`) and `shared/root/src/commonMain/kotlin/com/sedsoftware/bagcue/root/integration/RootComponentPreview.kt` (`RootComponentPreview`).
- Every production screen: `SessionScreen.kt` (`TodayScreen`, `CreateScreen`, `ActiveScreen`, `ChecklistRow`, `ResultScreen`), `HistoryScreen.kt` (`HistoryScreen`, `HistoryRow`), `TemplateScreen.kt` (`TemplateScreen`, `TemplateEditor`, `PositionEditorDialog`), `CatalogScreen.kt` (`CatalogScreen`, `CatalogList`, `CatalogEditor`), and `SettingsScreen.kt` (`SettingsScreen`).
- Theme/assets/resources: `theme/Theme.kt`, `theme/Color.kt`, `theme/BagCueDesignSystem.kt`, `assets/BagCueAssets.kt`, feature resource adapters, EN/RU strings, and all drawable XML.
- Contracts: `.vibe/requests/packet-final-compose-ui-audit.json`, `docs/assignments/REQUEST-2026-09-09-final-design-review.md`, `spec/app-spec/design.md`, and `spec/app-spec/quality.md`.
- Visual evidence: all 118 entries in `docs/design/visual-review/index.json` and SCREEN-001 through SCREEN-011 contact sheets. Integrity result: `entries=118`, `screens=11`, `sheets=11`, `missing=0`, `hash mismatch=0`.

## Resolved findings from the first audit

### Visual routing and evidence freshness — resolved

`BagCuePreviewMatrix.previewApp` passes `initialCatalog = showCatalog` (`BagCuePreviewMatrix.kt:29-49`); SCREEN-008 and catalog-based SCREEN-009 cases set `showCatalog = true` (`241-290`); `SCREEN_009_SessionPosition` supplies `active(editing = true)` (`276-278`). `RootComponentPreview` honors `initialCatalog` (`RootComponentPreview.kt:14-27`). The refreshed SCREEN-008/009 sheets visibly match those functions and their indexed hashes match the files.

### Redundant and misleading actions — mostly resolved

- Create, Active, Catalog list/editor, and Template editor now use icon-only `IconButton` navigation affordances with localized descriptions (`SessionScreen.kt:211-217,274-280`; `CatalogScreen.kt:103-109,217-223`; `TemplateScreen.kt:236-242`). The arrow-plus-Back/Cancel duplication is gone.
- Checklist removal and History deletion now use `BagCueAssets.Delete`, not More (`SessionScreen.kt:370-395`; `HistoryScreen.kt:169-177`). Position edit now uses `BagCueAssets.Edit`, not Bag (`TemplateScreen.kt:293-307`).
- Compact Template and Catalog actions use described icon buttons instead of repeating long icon-plus-object labels (`TemplateScreen.kt:213-222`; `CatalogScreen.kt:182-191`).
- Clickable parent rows were removed from `HistoryRow`, `TemplateRow`, and `PositionRow`, eliminating nested/duplicated row actions (`HistoryScreen.kt:152-179`; `TemplateScreen.kt:178-222,284-310`).

One minor mismatch remains: Template editor's back arrow is described as “Cancel” (`TemplateScreen.kt:239-241`). Use a localized “Close editor”/“Back” description matching the arrow, or a text-only Cancel affordance if cancellation is the intended concept.

### Theme hierarchy, hero treatment, and component variants — resolved or materially improved

- `AppTheme` now supplies explicit `BagCueTypography` and `BagCueShapes` (`Theme.kt:86-99`; `BagCueDesignSystem.kt:11-69`). The type hierarchy and 8/12/18/26/32 dp shape progression are deliberate.
- Today and Result use tonal hero `Surface`s with correct role pairs, large shapes, and heading semantics (`SessionScreen.kt:169-193,502-521`).
- Active uses `ExtendedFloatingActionButton` in the FAB slot (`SessionScreen.kt:296-303`).
- Settings sections, History sections, and Active bag groups expose heading semantics (`SettingsScreen.kt:98-140`; `HistoryScreen.kt:182-186`; `SessionScreen.kt:322-331`).
- CatalogEditor applies IME padding to its scrolling content (`CatalogScreen.kt:227-230`).

Spacing and elevation remain raw values rather than named tokens, but this is now a maintainability opportunity rather than a missing type/shape system.

### Final visibility and contrast corrections — resolved

- Active places its Undo action immediately after progress when `undoAvailable` is true (`SessionScreen.kt:311-329`). The current SCREEN-003 `removeundo_ru200` cells visibly show “Вернуть удалённое” and no longer hash-match `partialsourcehints_ru200`.
- Settings renders its error and Retry action before reminder rows and marks the group as a polite live region (`SettingsScreen.kt:98-120`). The current SCREEN-010 `endpointunresolved` cells visibly differ from `alldefaultsoff`.
- Disabled reminder-offset buttons now use `onSurfaceVariant` at 0.78 alpha (`SettingsScreen.kt:275-299`). In the current SCREEN-010 sheets the disabled labels are visibly stronger in both themes while remaining subordinate to enabled controls.
- The current SCREEN-007 `longcustomime_ru200` modal was re-inspected at original contact-sheet resolution. Its quantity value, validation message, multi-line bag/source values, supporting text, Save, and Cancel are all present within the dialog. The layout is dense and tall, but there is no demonstrated truncation or overlap; the prior clipping finding is resolved.

## Remaining objective defects

### P1 — expanded layouts still miss the adaptive contract

`ProductShell` switches only from bottom bar to rail at 600 dp (`App.kt:42-48,80-107`). At 960 dp it renders one unconstrained `Box` containing the compact screen. The refreshed SCREEN-003/005/006/007/008/009 expanded sheets show wide single panes; Sessions/Templates do not become list-detail and Active has no supporting pane. Add readable max-width constraints, then two-pane content for the declared list-detail/editor states.

### P1 — dynamic state semantics remain incomplete

Settings now correctly uses `LiveRegionMode.Polite` for its error/Retry group (`SettingsScreen.kt:98-120`). Elsewhere, production code still has no `stateDescription`, and no live region for Active undo/completion or Snackbar feedback. `LinearProgressIndicator` provides numeric range semantics, while “X of Y packed” is a separate node (`SessionScreen.kt:311-320`; `HistoryScreen.kt:159-167`). Loading indicators lack localized task context (`SessionScreen.kt:150`; `TemplateScreen.kt:153`; `CatalogScreen.kt:132,264`; `SettingsScreen.kt:155`). Group progress with a localized purpose/status, announce non-focus-moving feedback politely, and describe loading context. Empty-state `headlineSmall` text also remains visual-only (`HistoryScreen.kt:188-193`; `TemplateScreen.kt:156-163`; `CatalogScreen.kt:134-140`).

### P2 — one scenario pair remains visually identical

SCREEN-003 undo and SCREEN-010 endpoint coverage are now visibly distinct and have unique hashes. Remaining duplicate hashes are either deliberate shared rendering (the same consent dialog across SCREEN-004/011; hidden/failed ad leaves no gap) or SCREEN-001 `firstrunstarter` and `nocurrentnextfuture`, which remain identical in both themes. If the SCREEN-001 scenarios are contractually distinct, expose the difference; otherwise consolidate the redundant case. This is no longer a cross-screen evidence blocker.

### P2 — large-font navigation remains custom

At font scale 1.5+ `PrimaryNavigationBar` nests a `Column` of two 80 dp `Row`s inside `NavigationBar` (`App.kt:112-137`). It prevents truncation but consumes substantial height and bypasses the intended direct RowScope arrangement. Explicitly test traversal and selected-state announcements; prefer a documented adaptive navigation component if behavior is not equivalent.

### P2 — Create date selection lacks selected state

Create uses two `OutlinedButton`s for Today/Tomorrow (`SessionScreen.kt:226-235`). Neither exposes selected state and no “other date” path is visible. Use stable `FilterChip`s or a date field/button opening `DatePicker`; do not adopt alpha `ButtonGroup` merely for connected styling.

## Screen-by-screen disposition

| Screen | Current result |
|---|---|
| SCREEN-001 Today | Hero and hierarchy fixed. Expanded width remains unconstrained; first-run/no-current fixtures are identical. |
| SCREEN-002 Create | Back duplication fixed; toggle rows remain accessible. Date selection still lacks state/other-date semantics. |
| SCREEN-003 Active | FAB, headings, icons, nested actions, and visible Undo evidence are fixed. Contextual/live progress and supporting pane remain. |
| SCREEN-004 Result | Hero hierarchy fixed; refreshed consent/ad states are legible. Live completion/reduced-motion behavior is unproven. |
| SCREEN-005 Sessions | Nested click and More-as-delete fixed. Expanded list-detail and contextual progress remain. |
| SCREEN-006 Templates | Compact actions and nested click fixed. Expanded list-detail remains; RU/200% is still action-dense. |
| SCREEN-007 Template editor | Navigation duplication and bag-as-edit fixed. RU/200% position dialog is tall but fully legible; expanded editor remains single-pane. |
| SCREEN-008 Catalog | Routing/evidence, back, compact actions, and IME behavior fixed. Expanded list-detail remains single-pane. |
| SCREEN-009 Item/position editor | Correct editor states and validation are visible. Expanded detail is single-pane; continue RU scroll/IME checks. |
| SCREEN-010 Settings | Section headings/toggles are sound; endpoint error + Retry is visible with polite live semantics, and disabled offset contrast is improved. |
| SCREEN-011 Navigation/dialog | Dialog stress is legible. Two-row large-font navigation needs traversal/selection verification. |

## Theme, color, shape, spacing, and elevation

Colors remain centralized in `Color.kt` and assigned to roles in `Theme.kt`; no wrong foreground/container pair was found. Palette contrast is unchanged:

| Pair | Light | Dark | Result |
|---|---:|---:|---|
| primary / onPrimary | 6.47 | 7.66 | AA |
| primaryContainer / onPrimaryContainer | 7.28 | 7.28 | AA |
| secondary / onSecondary | 6.43 | 7.74 | AA |
| secondaryContainer / onSecondaryContainer | 7.23 | 7.23 | AA |
| error / onError | 6.45 | 7.73 | AA |
| errorContainer / onErrorContainer | 13.25 | 7.27 | AA |
| surface / onSurface | 16.26 | 14.27 | AA |
| surfaceVariant / onSurfaceVariant | 7.23 | 5.49 | AA |
| primary text on surface | 6.15 | 10.82 | AA |
| outline on surface | 4.27 | 5.82 | passes 3:1 UI-boundary contrast; light is not normal-text AA |

The type/shape definitions resolve the major token gap. Raw spacings still include 4, 6, 8, 10, 12, 16, 18, 20, 24, and 32 dp; primary margins vary (Today 24, Settings 20, Template/Catalog 16, Active 12). Introduce spacing/elevation tokens during the next affected-screen edit, without churn solely to replace literals. Today/Result tonal surfaces provide hierarchy without gratuitous elevation.

## Icons and Compose Multiplatform resources

`BagCueAssets` maps all 17 frozen assets through `DrawableResource`; `BagCueIcon` uses Compose Multiplatform `painterResource` (`BagCueAssets.kt:30-59`). Navigation icons are decorative beside labels (`App.kt:145-178`), while compact icon-only edit/delete/back controls have localized descriptions. `ic_arrow_back.xml` is auto-mirrored. EN/RU `session_edit_item_named` and `session_remove_today_named` give object-specific descriptions in Active.

No custom replacement icon family is warranted. Optional polish: `TooltipBox` for desktop hover/long-press discoverability. `BagCueBrandMark` remains descriptionless (`BagCueAssets.kt:63-83`); document it as decorative or expose one localized brand description where meaningful. Four unused drawables and the unused Indie Flower font remain cleanup opportunities.

## Material 3 Expressive API maturity

The repository uses Kotlin 2.4.10, Compose Multiplatform 1.12.0, and Material 3 1.12.0-alpha03 (`gradle/libs.versions.toml:3-5,40`). Local alpha03 sources mark `MediumFlexibleTopAppBar`, `LargeFlexibleTopAppBar`, `TwoRowsTopAppBar`, `FlexibleBottomAppBar`, and `LoadingIndicator` with `ExperimentalMaterial3ExpressiveApi`. `MaterialExpressiveTheme`, `ButtonGroup`, and floating toolbars still carry alpha-artifact upgrade risk.

The corrective pass chose the right maturity boundary: explicit typography/shapes, tonal `Surface`, `ExtendedFloatingActionButton`, `IconButton`, and stable-in-practice primitives. Do not adopt `MaterialExpressiveTheme` or flexible app bars for closure. Any later experiment should be isolated to Today/Result previews with light/dark, RU/200%, reduced-motion, interaction, and golden checks.

## Minimal remaining plan

1. Add contextual progress/loading semantics and polite announcements outside the resolved Settings error state; mark remaining empty-state titles as headings.
2. Add max-width constraints and list-detail/supporting panes for expanded contracts.
3. Replace Create's date buttons with a selected-state/date-picker pattern.
4. Accessibility-test the two-row 200%-font navigation and replace it if traversal/selection behavior is not equivalent.
5. Decide whether the identical SCREEN-001 first-run/no-current scenarios should differ or be consolidated.

## Stylistic opportunities (non-blocking)

- Add tooltips to compact icon-only actions for desktop/long-press discoverability.
- Centralize spacing/elevation values during the next affected-screen edit.
- Add restrained completion motion only with a reduced-motion path.
- Remove the unused font and four unused vectors during resource cleanup.
