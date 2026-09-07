# SCREEN-004 — Packing result

Linked flows: FLOW-004, FLOW-005, FLOW-007, FLOW-008, FLOW-009  
Linked requirements: REQ-004, REQ-005, REQ-007, REQ-008, REQ-009

## User task and information hierarchy

Confirm the result, inspect skipped items if any, and leave. A restrained brand/check moment, date, summary, and Done appear before any monetization. Reopen is secondary. Skipped completion clearly lists remaining items and is not styled as full success.

## States

Full success/ad privacy loading without placeholder, full success consent required, full success ad eligible, ad load failure hidden, completed with skipped, and persistence/read error.

## Responsive layout and insets

Compact scrolls content while keeping Done reachable. Expanded centers a bounded result surface; ad width follows provider-safe adaptive constraints below content. App chrome owns insets once.

## Actions and iconography

| Element | Component/treatment | Asset | Accessibility |
| --- | --- | --- | --- |
| Brand/completion mark | Hero symbol | ASSET-001, ASSET-007 | Outcome text is primary; decorative mark not repeated |
| Done | Filled button | none | Returns to Today |
| Reopen | Text/outlined button | ASSET-009 | Names the dated session |
| Privacy decision | Navigation to SCREEN-011 | ASSET-015 | Explains why choice is requested |

## Text layout expectations

Success headline is one or two intentional lines. At RU 200%, summary, skipped list, actions, and policy link reflow; no action or ad overlaps content. User item snapshots wrap rather than lose identity.

## Preview and golden matrix

| State | Themes | Locale/font | Viewport |
| --- | --- | --- | --- |
| Full success/ad hidden | light, dark | EN 100% | compact |
| Full success/eligible test ad container | light, dark | RU 200% | compact |
| Consent required before ad | light, dark | EN/RU 100% | compact |
| Skipped result/no ad | light, dark | RU 200% | compact |
| Ad failure/no gap | light, dark | EN 100% | compact |
| Full success adaptive | light, dark | EN 100% | expanded |

## Allowed ad slots

Exactly one inline Yandex result card may render below summary and Done only for Android full completion without skipped items after privacy eligibility. No reserved blank space, overlay, fullscreen, sticky overlap, or repeat impression for the same completion.

