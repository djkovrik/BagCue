# SCREEN-002 — Create packing

Linked flows: FLOW-003, FLOW-005, FLOW-006, FLOW-007, FLOW-009, FLOW-010  
Linked requirements: REQ-003, REQ-005, REQ-006, REQ-007, REQ-009, REQ-010

## User task and information hierarchy

Create one dated session on a single screen. Order: date shortcuts/calendar, template checkbox list, merged item count, evening/morning reminder overrides, Create packing. Existing-date conflict replaces the form action with Open existing and explicit Replace templates.

## States

New, Repeat-prefilled, no template selected, templates selected, empty merge, existing date, notification denied, save in progress, and save error with retained input.

## Responsive layout and insets

Compact scrolls one form pane above an IME-safe bottom action. Expanded centers the form and may place the merged summary/reminders in a supporting pane. App bar owns top inset; scaffold/bottom action owns bottom inset.

## Actions and iconography

| Element | Component/treatment | Asset | Accessibility |
| --- | --- | --- | --- |
| Back | Icon button | ASSET-016 | Localized Back |
| Today/Tomorrow | Filter chips/connected group fallback | none | Selected date announced, non-color cue |
| Calendar | Date picker trigger | ASSET-008 | Localized Choose date; manual entry available |
| Template choice | Checkbox list row | ASSET-004 supplementary | Whole row target, selected state announced |
| Reminder | Switch plus time picker | ASSET-014 | Type, enabled state, and time announced |
| Create packing | Filled button | ASSET-006 | Announces merged count when helpful |

## Text layout expectations

Date shortcuts remain legible at 100%; at 200% they wrap/reflow rather than shrink. Template names and permission help may wrap without clipping. The item-count summary and primary action never disappear behind IME/system bars.

## Preview and golden matrix

| State | Themes | Locale/font | Viewport |
| --- | --- | --- | --- |
| Defaults/off reminders | light, dark | EN 100% | compact |
| Selected kits/long RU | light, dark | RU 200% | compact |
| Existing-date choice | light, dark | EN/RU 100% | compact |
| Empty merge | light, dark | EN 100% | compact |
| Save error/IME | light, dark | RU 200% | compact |
| Supporting pane | light, dark | EN 100% | expanded |

## Allowed ad slots

None.

