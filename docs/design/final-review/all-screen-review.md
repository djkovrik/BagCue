# BagCue final product-design review

Date: 2026-09-09  
Assignment: `final-product-design-review`  
Scope: SCREEN-001 through SCREEN-011, 118 checked-in golden renders, current production Compose sources, Material 3, accessibility/localization contracts, and current Lazyweb evidence. No production UI or resources were edited by this assignment.

## Verdict

**Verified for the current design-correction request and ready for ingest.** The refreshed 118-image golden set proves the corrected catalog/editor routing, typography, shapes, hero surfaces, icon actions, state evidence and Settings accessibility corrections. The original Lazyweb report findings remain useful historical inputs, but the current disposition is based on the final Compose audit and current goldens.

Verified corrections include unique visible SCREEN-003 Undo evidence; unique visible SCREEN-010 endpoint error plus Retry; a polite live region for the Settings error group; semantic headings for Today/Result, Settings sections, History sections and Active bag groups; non-nested History/Template/Position rows; and disabled reminder-offset composite contrast of approximately 4.84:1 light and 6.09:1 dark.

Explicit follow-up, not a blocker to this correction ingest: adaptive list/detail and supporting panes; broader contextual progress/loading/Undo/snackbar semantics outside the resolved Settings error; selected semantics for date choices; semantics/focus validation of the custom RU 200% two-row navigation; and behavioral IME/focus-return verification.

The earlier SCREEN-008/009 misrouting finding is superseded by the refreshed sheets (SHA-256 `407fcbe2…` and `0c8fa6a7…`), which visibly render the catalog, catalog editor and session item-details dialog. Goldens are evidence, not authority; behavioral semantics still require implementation tests.

## Evidence boundary

- Golden baseline: `docs/design/visual-review/index.json`, 118 entries and 11 contact sheets.
- AppSpec: `spec/app-spec/design.md`, `quality.md`, and SCREEN-001..011.
- Lazyweb exact-pattern corpus: <https://www.lazyweb.com/agentic-search/3550ac0a-8b22-497b-a78b-9efbf58964ee>.
- Sequential report URLs, exact coverage, terminal state and recorded findings: [lazyweb-report-log.md](lazyweb-report-log.md).
- Production-source corroboration: `docs/design/compose-audit/final-compose-ui-audit.md` plus direct inspection of the current working tree.

## Material 3 compliance matrix

Legend: **Pass** = evidenced and conforming; **Partial** = suitable foundation with objective gaps; **Fail** = current evidence shows a defect; **Blocked** = evidence does not render the declared screen.

| Screen | Canonical layout / navigation | Components and variants | Tokens / hierarchy | States / responsive evidence | Accessibility / icons | Result |
| --- | --- | --- | --- | --- | --- | --- |
| SCREEN-001 Today | Feed-like single task; bar/rail adaptation exists, expanded pane is unbounded | Refreshed tonal hero, filled action and outlined secondary actions are appropriate | Hero establishes date/status/progress hierarchy; lower viewport remains intentionally calm | Compact content/error and expanded light/dark present; first-run/no-current fixtures are visually duplicated | Navigation semantics are plausible; brand mark purpose is ambiguous when no BagCue text is adjacent | **Partial** |
| SCREEN-002 Create | One-pane form; expanded remains stretched rather than a supporting pane | Checkbox rows are correct; date choices lack clear selected semantics; refreshed app bar uses icon-only back | Title/date/templates/count hierarchy is compressed; repeated outlined pills dominate | EN/RU 200, conflict, error/IME and expanded are present | Back redundancy is fixed; Today has calendar icon while Tomorrow is text-only | **Partial** |
| SCREEN-003 Active | One-pane checklist; expanded supporting pane remains follow-up | Checkbox rows, determinate progress and refreshed extended FAB are appropriate | Active bag groups have heading semantics; progress purpose/live announcement remains follow-up | Undo is unique, visible and no longer hash-matches the partial/source-hint state | Back/edit/delete glyphs and non-nested rows are corrected; broader dynamic semantics remain follow-up | **Pass for current correction** |
| SCREEN-004 Result | Centered result is appropriate; expanded is unbounded | Filled Done then outlined Reopen is correct; ad-failure collapses correctly; consent dialog is canonical in principle | Refreshed tonal hero improves the completion moment; full/skipped distinction still relies mostly on copy | Full/skipped/ad/consent themes exist; no-gap evidence is visible | Decorative mark plus check usage needs one semantics decision; consent action order is readable | **Partial** |
| SCREEN-005 Sessions | Feed/list compact; expanded list-detail remains follow-up | Date picker and direct Delete icon are appropriate; action-density refinement remains optional follow-up | Planned/Completed sections now expose heading semantics | Compact mixed, empty, filter and dialog states exist; expanded remains a stretched list | Parent row clickability was removed, eliminating nested interaction nodes | **Pass for current correction** |
| SCREEN-006 Templates | List compact; required expanded list-detail is absent | Extended FAB is appropriate; app-bar Catalog text action is discoverable; row action set should be overflow | Rows rely on dividers and loose text; empty state hierarchy is acceptable | Long RU 200 reflows vertically; expanded remains sparse/stretched | Duplicate/Delete icon + object-name text repeats meaning and increases width; clickable row plus actions | **Partial** |
| SCREEN-007 Template editor | Form/list compact; expanded supporting structure remains follow-up | Text fields, bottom Save and Add action are appropriate; RU 200 dialog is tall but legible | Editor titles are consistent; row metadata/actions remain readable | IME/error/long RU coverage exists without clipping | Icon-only Back and Edit/Delete semantics are corrected; parent row clickability was removed | **Pass for current correction** |
| SCREEN-008 Catalog/selector | Refreshed compact catalog is correct; expanded is still a stretched list rather than list-detail | Search/duplicate and delete confirmations are visible; compact rows use clear icon-only Edit/Delete and extended Add-item action | Long item names wrap without clipping; row metadata hierarchy is serviceable | Ten correct catalog states cover dependency delete, empty/error, search match and list in light/dark/RU/expanded | Icon meanings are now accurate; verify 48 dp merged targets and row/child-action semantics | **Partial** |
| SCREEN-009 Item/position editor | Refreshed catalog editor and session item-details dialog are correctly routed; expanded editor remains single pane | Outlined fields, filled Save, validation and duplicate resolution are appropriate | Form hierarchy is clear; long RU copy makes a tall, dense form and narrow session dialog | Ten correct states cover edit/new, save/error, validation, duplicate choice and session position | Icon-only back is correct; verify IME scroll, dialog focus/return and error announcement | **Partial** |
| SCREEN-010 Settings | Grouped settings list; expanded readable-width refinement remains follow-up | Switch rows and reminder timing controls are appropriate | Section headings are semantic; disabled offsets remain subordinate but readable | Endpoint error plus Retry is unique and visible; broad light/dark and RU 200 coverage is current | Error group uses a polite live region; disabled offset contrast is ~4.84:1 light / 6.09:1 dark | **Pass for current correction** |
| SCREEN-011 Consent | Modal/full-screen decision intent is valid; compact implementation remains a narrow dialog over Result | Filled Allow and lower-emphasis Decline/policy are appropriate; policy needs full-width readable action at stress scale | Title/body/action hierarchy is understandable but overly compressed | Compact, RU 200, failure and expanded present; stress dialog forces awkward word wrapping | Privacy icon is decorative; focus trap/return and Back-as-decline are not visually provable | **Partial** |

## Cross-screen system audit

### Typography and title hierarchy

- The refreshed goldens show an explicit M3 type system, though top-level titles, dates, group headings and row titles still sit too close in size/weight on dense screens.
- Adopt one explicit ladder: app-bar/page context -> `titleLarge`; Today/Result hero -> `headlineMedium`; major screen section -> `titleLarge`; row/object title -> `titleMedium`; body/support -> `bodyMedium`/`bodySmall`; control labels -> `labelLarge`.
- Current source confirms `heading()` semantics for Today/Result heroes, Settings groups, History Planned/Completed and Active bag groups. Remaining empty-state heading semantics are a broader follow-up.
- The refreshed sheets now prove `BagCueTypography` and shared shapes are applied; role separation is improved but still subtle in dense lists.
- Remove the unused display font unless an approved hero-only use is specified and EN/RU glyph coverage is rendered. Novelty alone is not a reason to introduce it.

### Shape, spacing and elevation

- Baseline lists are predominantly flat columns with frequent dividers, while buttons use pill-like defaults. That reads as generic Material rather than a coherent BagCue silhouette.
- Use component defaults for routine controls and one product mapping for containers. The current 8/12/18/26/32 dp mapping is visible in fresh light/dark goldens; avoid unrelated screen-local radii.
- Today and Result may each use one large/extra-large tonal hero surface. Do not spread hero shapes into editors, settings or every list row.
- Normalize primary content margins through semantic spacing tokens; current source uses 12, 16, 20 and 24 dp by screen without a documented breakpoint rule.
- Prefer open space and `surfaceContainer*` hierarchy to additional borders/shadows. Keep lists at elevation 0 unless overlap communicates real z-order.

### Color and contrast

Direct source-role analysis found no incorrect container/on-container pair. Measured current role contrast from the production-source audit is strong: primary/onPrimary 6.47 light and 7.66 dark; surface/onSurface 16.26 and 14.27; surfaceVariant/onSurfaceVariant 7.23 and 5.49. Light outline on surface is 4.27: acceptable for UI boundaries but not normal body text.

Observed risks:

- SCREEN-010 disabled reminder offsets now measure approximately 4.84:1 in light and 6.09:1 in dark for their composited text, remaining visibly subordinate without becoming illegible.
- SCREEN-003 Undo and SCREEN-010 endpoint error/Retry are unique and visible in the current sheets. The Settings error group additionally has polite live-region semantics.
- Destructive confirmation actions do not consistently use error-role emphasis; do not color every row delete red, but standardize error emphasis inside confirmation UI.
- Selection/status must continue to use icon/text/semantics in addition to color.

### Expressive identity

- Do not replace familiar action icons with custom art. The original bag/check mark is the correct identity anchor.
- The safest expressive pass is stable baseline M3: branded tonal hero surfaces on Today/Result, explicit product typography/shapes, calmer lists, and coherent motion with reduced-motion fallback.
- Flexible app bars, expressive loading, button groups and broad `MaterialExpressiveTheme` adoption are unnecessary for closure and risky on the repository's Material 3 alpha dependency. Any later experiment should be isolated to Today/Result previews with baseline fallback.

### Responsive and state coverage

- The shell swaps bottom navigation to rail, but expanded screens mostly stretch compact content. Implement bounded panes and actual list-detail/supporting-pane behavior for Sessions, Templates, Catalog, and their editors.
- The RU 200% two-by-two destination grid preserves labels and targets, but it is a custom use inside NavigationBar and needs explicit semantics/focus/golden validation.
- SCREEN-003 Undo and SCREEN-010 endpoint error have unique hashes and visible content. Remaining duplicate hashes are deliberate shared renders or the visually identical SCREEN-001 first-run/no-current pair; this is not a cross-screen evidence blocker.
- Assign one inset owner per edge and keep IME-safe bottom actions consistent. CatalogEditor remains a known IME-risk surface until refreshed SCREEN-009 evidence proves Save/error recovery remains reachable.

## Required disposition

The current correction request is **verified and ready for ingest**. No false unresolved fix remains in this review.

Adaptive list/detail, broader dynamic semantics, date-choice selected semantics, custom two-row navigation semantics/focus, and IME/dialog focus-return checks are explicitly tracked as follow-up. Any later implementation change still requires fresh light/dark, RU 200% and expanded golden verification.

The strict Lazyweb queue is complete: SCREEN-001 through SCREEN-011 are terminal, non-degraded and recorded with URLs, coverage and findings.
