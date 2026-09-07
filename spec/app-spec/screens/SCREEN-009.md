# SCREEN-009 — Item and position editor

Linked flows: FLOW-001, FLOW-004, FLOW-008, FLOW-009, FLOW-010  
Linked requirements: REQ-001, REQ-004, REQ-009, REQ-010

## User task and information hierarchy

Edit a reusable item name/usual location or contextual position fields. Catalog context shows name and usual location. Template/session context additionally shows quantity 1–99 and target-bag label; source override is clearly scoped. Saving into templates is always a separate explicit choice.

## States

New item, existing seed unedited, first seed-name override, custom item, contextual template position, contextual session item, duplicate-name suggestion, invalid blank/range, unsaved edits, save failure, and IME visible.

## Responsive layout and insets

Compact is an IME-safe form/full-screen dialog according to navigation context. Expanded is a side/detail pane. One surface owns focus and insets; Save remains reachable through scrolling.

## Actions and iconography

| Element | Component/treatment | Asset | Accessibility |
| --- | --- | --- | --- |
| Back/close | Icon button | ASSET-016 | Warns before losing edits |
| Edit fields | Material text/number inputs | ASSET-009 supplementary | Labels, errors, and ranges announced |
| Bag assignment | Text field/suggestion menu | ASSET-017 | User text; No bag is explicit |
| Delete catalog item | Confirmed destructive action | ASSET-010 | Dependency consequence shown |
| Save | Filled button with text | none | One scoped Save action |

## Text layout expectations

Labels, helper/error copy, and user text wrap naturally. Quantity remains visually bound to its input. At RU 200%, IME and scroll never cover Save or error recovery; no placeholder is the sole field label.

## Preview and golden matrix

| State | Themes | Locale/font | Viewport |
| --- | --- | --- | --- |
| New catalog item | light, dark | EN/RU 100% | compact |
| Seed override/match | light, dark | RU 200% | compact |
| Session position | light, dark | EN 100% | compact |
| Validation/save error/IME | light, dark | RU 200% | compact |
| Detail pane | light, dark | EN 100% | expanded |

## Allowed ad slots

None.

