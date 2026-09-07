# SCREEN-007 — Template editor

Linked flows: FLOW-002, FLOW-004, FLOW-008, FLOW-009, FLOW-010  
Linked requirements: REQ-002, REQ-004, REQ-009, REQ-010

## User task and information hierarchy

Name a template and manage its ordered positions. Each position shows item name, quantity, target bag, and optional usual location. Add item opens SCREEN-008 selection; editing a position opens SCREEN-009 with contextual fields. An empty template is valid and explained as a draft.

## States

New empty, existing starter, existing custom, long item list, unsaved edits, matching-name suggestion through item selection, delete position, save error with retained input, and IME visible.

## Responsive layout and insets

Compact scrolls a form/list with IME-safe save action. Expanded is the detail pane owned by Templates list-detail, or a centered standalone pane. App bar owns top inset and content consumes bottom/IME inset once.

## Actions and iconography

| Element | Component/treatment | Asset | Accessibility |
| --- | --- | --- | --- |
| Back | Icon button | ASSET-016 | Warns before discarding unsaved edits |
| Name | Outlined/filled text field | none | Required, error/support text linked |
| Add position | Filled/tonal action | ASSET-006 | Add item to template |
| Edit position | Full list row/menu | ASSET-009, ASSET-017 | Announces item, quantity, bag, source |
| Delete template/position | Confirmed/menu action | ASSET-010 | Object and consequence named |

## Text layout expectations

User template/item/bag/location text wraps up to two meaningful lines in rows; full text is available in editor. At RU 200%, labels/supporting text reflow, the list remains scrollable, and save/back actions are not hidden by IME.

## Preview and golden matrix

| State | Themes | Locale/font | Viewport |
| --- | --- | --- | --- |
| New empty | light, dark | EN 100% | compact |
| Starter Office | light, dark | RU 100% | compact |
| Long custom content/IME | light, dark | RU 200% | compact |
| Save failure | light, dark | EN/RU 100% | compact |
| Detail pane | light, dark | EN 100% | expanded |

## Allowed ad slots

None.

