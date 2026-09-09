# Prioritized implementation brief

## P0 — evidence repair completed

1. Completed: refreshed SCREEN-008/009 sheets now render the catalog, catalog editor and session item-details dialog.
2. Completed: all 118 goldens/contact sheets were refreshed and visually inspected for light/dark, RU 200% and expanded routing.
3. Completed: SCREEN-003 Undo and SCREEN-010 endpoint error/Retry are visible and uniquely hashed. Remaining duplicate images are deliberate shared rendering or the non-blocking SCREEN-001 first-run/no-current pair.
4. Completed: all eleven Lazyweb reports reached terminal state in strict numeric order and are recorded with coverage and dispositions.

## Verified corrections

1. Completed visually: child navigation is icon-only back. Retain localized content descriptions/tooltips and keep Back distinct from Cancel/discard semantics.
2. Completed in source and current audit: History, Template and Position parent row clickability was removed, eliminating nested interaction nodes. Further action-density reduction is optional follow-up.
3. Completed visually: misleading More/bag actions were replaced by exact edit/delete icons. Verify labels and 48 dp merged targets in accessibility tests.
4. Verified: destructive confirmations name the affected object/consequence and preserve a lower-emphasis Cancel.
5. Verified: Today/Result heroes, Settings sections, History sections and Active bag groups expose heading semantics. Settings error/Retry uses a polite live region.
6. Verified visually: SCREEN-010 disabled offsets remain subordinate and meet approximately 4.84:1 light / 6.09:1 dark composited contrast; endpoint error/Retry is visibly distinct.

## Explicit follow-up — not blockers to current ingest

1. Bound readable content width on expanded Today, Result, Settings and form screens.
2. Implement list-detail for Sessions, Templates and Catalog with preserved Decompose selection/back state.
3. Use a supporting pane for Active metadata only when it does not compete with checklist execution.
4. Validate the custom RU 200% two-by-two destination layout for semantics, focus order, selected state and 80 dp targets; do not present it as a standard NavigationBar variant.
5. Add selected-state semantics to the Today/Tomorrow/date-choice controls.
6. Group progress with contextual purpose and add polite announcements for Active Undo/completion and snackbar feedback outside the resolved Settings error group; add localized context to loading indicators.
7. Behaviorally verify IME-safe scrolling and Save/error reachability plus modal focus trap/return. Current RU 200% editor/consent goldens are tall but legible and unclipped.

## P2 — consolidate the BagCue design system

1. Apply one explicit typography ladder and the proposed app Shapes through MaterialTheme; add semantic spacing/elevation tokens and remove screen-local margin drift.
2. Use stable M3 tonal surfaces and large/extra-large product shape for Today and Result hero moments only.
3. Replace divider-heavy list rhythm with whitespace and occasional surface-container grouping after action hierarchy is fixed.
4. Keep existing semantic color families. Disabled reminder copy and Settings error states are verified; continue inspecting focus/selected states in light/dark after future changes.
5. Use baseline M3 stable-in-practice components for closure. Defer broad M3 Expressive/alpha API adoption; any experiment needs target availability notes and baseline fallback.

## Required verification after implementation

- Compact light/dark for every applicable state.
- RU 200% for all text/action/IME risks.
- Expanded list-detail/supporting-pane cases with bounded widths.
- Focused/selected/error/destructive states and touch target inspection.
- EN/RU resource-key parity, real fonts/glyphs, no language picker, English fallback.
- Reduced-motion behavior for completion/hero motion.
- Preserve the completed sequential Lazyweb report log; rerun only for materially changed evidence.
