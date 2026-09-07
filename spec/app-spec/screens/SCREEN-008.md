# SCREEN-008 — Item catalog and selector

Linked flows: FLOW-001, FLOW-002, FLOW-004, FLOW-008, FLOW-009, FLOW-010  
Linked requirements: REQ-001, REQ-002, REQ-004, REQ-009, REQ-010

## User task and information hierarchy

Browse reusable item identities. When opened from Add position/session item, search and selection are primary; otherwise catalog management is primary. Rows show name and optional usual location, not template-specific quantity/bag. Dependency-aware delete and Create another prevent hidden duplication/data loss.

## States

Starter/custom list, selection mode, search results, no results, normalized-name match suggestion, empty catalog, dependency delete dialog, delete error, and long user text.

## Responsive layout and insets

Compact uses one searchable list only when selection mode starts. Expanded may pair list with SCREEN-009 detail. Search never permanently occupies the ordinary catalog screen. Insets are owned by app bar/scaffold once.

## Actions and iconography

| Element | Component/treatment | Asset | Accessibility |
| --- | --- | --- | --- |
| Back | Icon button | ASSET-016 | Localized Back |
| Search in selection | Contained search | ASSET-012 | Result count announced without chatter |
| Create item | FAB/filled action | ASSET-006 | Create new catalog item |
| Edit | Row/menu action | ASSET-009 | Names item |
| Delete | Dependency dialog | ASSET-010 | Lists affected templates and unfinished sessions |
| More | Menu | ASSET-011 | Fully labeled actions |

## Text layout expectations

Names/locations wrap to two lines in the list and remain fully editable. At RU 200%, search, suggestion actions, dependency names, and Create another reflow without clipping or ambiguous truncation.

## Preview and golden matrix

| State | Themes | Locale/font | Viewport |
| --- | --- | --- | --- |
| Starter catalog | light, dark | EN/RU 100% | compact |
| Search and match suggestion | light, dark | RU 200% | compact |
| Dependency deletion | light, dark | EN/RU 100% | compact |
| Empty/error | light, dark | RU 200% | compact |
| List-detail | light, dark | EN 100% | expanded |

## Allowed ad slots

None.

