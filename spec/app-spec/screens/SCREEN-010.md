# SCREEN-010 — Settings

Linked flows: FLOW-006, FLOW-007, FLOW-008, FLOW-009, FLOW-010  
Linked requirements: REQ-006, REQ-007, REQ-008, REQ-009, REQ-010

## User task and information hierarchy

Manage four groups: reminder defaults, Usage analytics, advertising privacy/policy, and About. Settings take effect immediately. No language or theme setting exists.

## States

Reminders off/on, permission denied, Analytics off/on/reset failure, fresh protected advertising choice, fresh non-protected state with no choice control, privacy endpoint unresolved/expired, policy link, and About/version.

## Responsive layout and insets

Compact is a grouped scrollable list with navigation bar. Expanded uses navigation rail and centered settings pane; groups may use two columns only if reading/focus order remains clear. App chrome owns insets.

## Actions and iconography

| Element | Component/treatment | Asset | Accessibility |
| --- | --- | --- | --- |
| Settings destination | Navigation item | ASSET-005 | Localized label and selected state |
| Reminder enable/time | Full switch rows and time picker | ASSET-014 | Enabled state and time announced |
| Usage analytics | Full switch row, default off | none | Supporting text discloses app-instance identifier and no packing content |
| Advertising privacy | Labeled row/choice entry | ASSET-015 | Only shown when fresh protected result makes choice applicable |
| Privacy policy | Text row/link | ASSET-015 supplementary | Opens policy with localized label |
| Brand/About | Static content | ASSET-001 | Mark decorative when app name adjacent |

## Text layout expectations

Group titles remain one line at 100%; privacy/analytics supporting text may wrap freely. At RU 200%, rows grow, switch associations remain unambiguous, time values remain readable, and links/actions are not clipped.

## Preview and golden matrix

| State | Themes | Locale/font | Viewport |
| --- | --- | --- | --- |
| All defaults off | light, dark | EN/RU 100% | compact |
| Reminders on/permission denied | light, dark | RU 200% | compact |
| Analytics on/off | light, dark | EN/RU 100% | compact |
| Protected privacy choice | light, dark | RU 200% | compact |
| Endpoint unresolved | light, dark | EN 100% | compact |
| Adaptive groups | light, dark | EN 100% | expanded |

## Allowed ad slots

None.

