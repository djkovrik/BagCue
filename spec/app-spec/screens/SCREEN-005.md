# SCREEN-005 — Sessions

Linked flows: FLOW-003, FLOW-005, FLOW-007, FLOW-008, FLOW-009, FLOW-010  
Linked requirements: REQ-003, REQ-005, REQ-007, REQ-009, REQ-010

## User task and information hierarchy

Find a planned or completed session, optionally filter by date, then open, repeat, reopen, or delete. Planned and Completed sections remain explicit. The calendar is collapsed by default and expands above the list.

## States

Mixed planned/completed, planned only, completed only, empty, date-filtered empty, expanded calendar, delete confirmation/Undo, and read/delete error.

## Responsive layout and insets

Compact is a single chronological list with expandable calendar. Expanded is list-detail: session list on leading pane and selected read-only/active detail on trailing pane. Navigation rail/app bar own insets.

## Actions and iconography

| Element | Component/treatment | Asset | Accessibility |
| --- | --- | --- | --- |
| Sessions destination | Navigation item | ASSET-003 | Localized label and selected state |
| Expand/filter calendar | Icon button plus date picker area | ASSET-008 | Expanded state and selected date announced |
| Row actions | More menu | ASSET-011 | Repeat, reopen, delete fully labeled |
| Delete | Confirmed destructive action | ASSET-010 | Consequence and date named |

## Text layout expectations

Section names and dates remain one line at 100%. Template summary may wrap to two lines. At RU 200%, rows grow and actions remain reachable without clipping or relying on swipe.

## Preview and golden matrix

| State | Themes | Locale/font | Viewport |
| --- | --- | --- | --- |
| Mixed list/calendar collapsed | light, dark | EN 100% | compact |
| Calendar expanded/filter | light, dark | RU 200% | compact |
| Empty/filter empty | light, dark | EN/RU 100% | compact |
| Delete confirmation/Undo | light, dark | RU 200% | compact |
| List-detail | light, dark | EN 100% | expanded |

## Allowed ad slots

None.

