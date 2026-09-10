# SCREEN-011 — Advertising consent

Linked flows: FLOW-008, FLOW-009  
Linked requirements: REQ-008, REQ-009

## User task and information hierarchy

Make the required app-owned advertising choice after understanding that core packing remains available either way. Heading, concise purpose, Allow, Decline, and Privacy policy are visible. This is not an IAB TCF CMP and is not used for Firebase Analytics.

## States

Fresh protected unresolved, choice save in progress, save failure with both choices still available, and policy link return. It never renders for non-protected, missing, malformed, or expired response.

## Responsive layout and insets

Compact uses a non-dismissible full-screen/basic decision surface sized for scroll and 200% text. Expanded uses a bounded modal or side sheet with explicit focus trap/return. Safe-area and dialog insets have one owner.

## Actions and iconography

| Element | Component/treatment | Asset | Accessibility |
| --- | --- | --- | --- |
| Privacy context | Heading/supporting symbol | ASSET-015 | Heading is primary; icon decorative |
| Allow | Filled button | none | Explicit advertising-consent action |
| Decline | Outlined/text button with equal access | none | States that app continues without ad |
| Privacy policy | Text link/button | ASSET-015 supplementary | Opens `https://sedsoftware.com/apps/bagcue/policy.html` with the native external browser and returns focus |
| Back | Icon button only when safe | ASSET-016 | Back behaves as decline/no initialization, never implicit consent |

## Text layout expectations

No legal copy is truncated. At RU 200%, the surface scrolls, both choices and policy link remain reachable, button labels wrap or widen, and focus order follows heading, explanation, policy, Allow, Decline.

## Preview and golden matrix

| State | Themes | Locale/font | Viewport |
| --- | --- | --- | --- |
| Required choice | light, dark | EN/RU 100% | compact |
| Text stress | light, dark | RU 200% | compact |
| Save failure | light, dark | EN/RU 100% | compact |
| Bounded modal | light, dark | EN 100% | expanded |

## Allowed ad slots

None. Yandex SDK initialization and requests are prohibited until an eligible Allow path completes.
