# SCREEN-001 — Today

Linked flows: FLOW-003, FLOW-004, FLOW-005, FLOW-006, FLOW-007, FLOW-008, FLOW-009  
Linked requirements: REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-009

## User task and information hierarchy

Understand the next action in five to ten seconds. Highest priority is one large nearest/today session surface with date, short template summary, `packed/total`, determinate progress, and Continue. If no today/tomorrow session exists, show benefit copy, starter templates on first run, and Create packing. One later planned session may appear as secondary content.

## States

- first-run starter state;
- no current session;
- active today/tomorrow session;
- completed today session;
- next future session only;
- local read error with retry;
- offline is normal and has no warning banner.

## Responsive layout and insets

Compact is one calm vertical pane with a navigation bar. Expanded uses a rail and centered/max-width primary pane; a supporting pane may show the next planned session without competing with the hero. Scaffold owns top/bottom insets once.

## Actions and iconography

| Element | Component/treatment | Asset | Accessibility |
| --- | --- | --- | --- |
| Today destination | Navigation item | ASSET-002 | Icon and localized label; selected state announced |
| Create packing | Filled/extended FAB according to viewport | ASSET-006 | Localized full action label |
| Continue packing | Filled button in hero | ASSET-007 supplementary | Announces date and remaining count |
| Brand mark | Decorative mark with text name | ASSET-001 | Null semantics when BagCue text is adjacent |

## Text layout expectations

Screen title and date are one line at 100%. Template names may wrap to two lines. At RU 200%, hero content stacks, count/progress remain visible, and Continue/Create remains reachable without ellipsis hiding identity.

## Preview and golden matrix

| State | Themes | Locale/font | Viewport |
| --- | --- | --- | --- |
| First run with starter kits | light, dark | EN 100%, RU 200% | compact |
| Active partial progress | light, dark | EN/RU 100% | compact |
| No current, next future | light, dark | EN 100% | compact |
| Read failure | light, dark | RU 200% | compact |
| Active adaptive | light, dark | EN 100% | expanded |

## Allowed ad slots

None.

