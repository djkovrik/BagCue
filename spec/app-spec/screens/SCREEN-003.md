# SCREEN-003 — Active packing session

Linked flows: FLOW-003, FLOW-004, FLOW-005, FLOW-006, FLOW-007, FLOW-008, FLOW-009, FLOW-010  
Linked requirements: REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-009, REQ-010

## User task and information hierarchy

Check items rapidly and honestly. Top context shows LocalDate, `X of Y`, progress, and template summary. The list groups unresolved bag conflicts first, then named bags, then No bag. Each row prioritizes a full-target Packed checkbox, name/quantity, and optional Move from hint. Completion remains visible at the bottom.

## States

All NotPacked, partial, all Packed, unresolved bag, no bag, source hints, long names, temporary removal Undo, empty-after-removal, save error, and restored evening progress.

## Responsive layout and insets

Compact uses one list pane and IME/system-safe sticky completion region. Expanded may use a supporting pane for session metadata/edit actions; checking remains in the primary pane. Insets are consumed once and snackbar never covers the completion control.

## Actions and iconography

| Element | Component/treatment | Asset | Accessibility |
| --- | --- | --- | --- |
| Back | Icon button | ASSET-016 | Localized Back |
| Packed | Checkbox row | ASSET-007 supplementary | Announces name, quantity, bag, packed state |
| Bag heading/choice | Section heading/menu | ASSET-017 | Text names bag or unresolved requirement |
| More actions | Icon button/menu | ASSET-011 | Localized actions; tooltip on pointer |
| Add item | FAB/filled action | ASSET-006 | Add item for today |
| Complete packing | Filled bottom action | ASSET-007 | Enabled only for full valid completion |
| Complete with skipped | Lower-emphasis text/outlined action | none | Confirmation names remaining count |

## Text layout expectations

At 100%, item names may occupy two lines; quantity remains associated. At RU 200%, rows grow, hints wrap, group headings remain visible, and no checkbox/menu loses its target. Long user text is not silently ellipsized where it would make items ambiguous.

## Preview and golden matrix

| State | Themes | Locale/font | Viewport |
| --- | --- | --- | --- |
| New Office + Pool | light, dark | EN 100% | compact |
| Partial with source hints | light, dark | RU 200% | compact |
| Unresolved bag conflict | light, dark | EN/RU 100% | compact |
| All packed | light, dark | EN 100% | compact |
| Remove Undo | light, dark | RU 200% | compact |
| Save error | light, dark | EN 100% | compact |
| Supporting pane | light, dark | EN 100% | expanded |

## Allowed ad slots

None.

