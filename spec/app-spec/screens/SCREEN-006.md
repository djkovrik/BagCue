# SCREEN-006 — Templates

Linked flows: FLOW-002, FLOW-004, FLOW-007, FLOW-008, FLOW-009, FLOW-010  
Linked requirements: REQ-002, REQ-004, REQ-007, REQ-009, REQ-010

## User task and information hierarchy

Open, create, duplicate, or delete an independent kit and reach the item catalog. Template rows show localized/user name, position count, and optional bag summary. Catalog is a labeled app-bar action; Create template is the one highest-prominence action.

## States

Starter Office/Pool, mixed starter/custom, empty, duplicate result, delete confirmation, save/delete error, and long names.

## Responsive layout and insets

Compact uses a list and navigation bar. Expanded uses navigation rail and list-detail with SCREEN-007 in a trailing pane when supported. App chrome owns insets; FAB does not cover the last row.

## Actions and iconography

| Element | Component/treatment | Asset | Accessibility |
| --- | --- | --- | --- |
| Templates destination | Navigation item | ASSET-004 | Localized label and selected state |
| Create | Extended FAB | ASSET-006 | Create template |
| Catalog | Labeled app-bar/icon action | ASSET-012 | Open item catalog |
| Duplicate | Menu action | ASSET-013 | Names template |
| Delete | Confirmed menu action | ASSET-010 | Names template and snapshot consequence |

## Text layout expectations

Template name may wrap to two lines; counts stay associated. At RU 200%, app-bar actions remain discoverable in overflow, rows reflow, and the FAB label remains readable or falls back to an accessible FAB where approved.

## Preview and golden matrix

| State | Themes | Locale/font | Viewport |
| --- | --- | --- | --- |
| Starter content | light, dark | EN/RU 100% | compact |
| Long custom templates | light, dark | RU 200% | compact |
| Empty | light, dark | EN 100% | compact |
| Delete/duplicate feedback | light, dark | RU 200% | compact |
| List-detail | light, dark | EN 100% | expanded |

## Allowed ad slots

None.

