# Design

## Evidence and design direction

Initial Lazyweb evidence: https://www.lazyweb.com/agentic-search/add6619a-4738-4082-8242-55d9fa00e1ba

Queries covered daily checklist home, packing checklist execution, template multi-select, checklist template editor, activity history, notification/privacy settings, and task completion. Adopted patterns are a single obvious next action, visible date/progress, checkbox multi-selection, direct list checking, compact date-filtered history, contextual permission education, and a restrained completion moment. The evidence is comparative, not a requirement source. Material 3 semantics and this AppSpec win every conflict.

## Product expression

BagCue uses a calm, utilitarian static blue-teal brand scheme shared across Android and iOS. It follows the system light/dark theme, provides no theme switch, and does not use Android dynamic color. One expressive hero card on Today and a short completion state provide identity; editors, lists, dialogs, and Settings remain quiet and conventional.

The original BagCue mark is a text-free open bag plus check mark. No decorative illustrations are required. Standard actions use Material Symbols Rounded with a consistent weight/fill strategy. Product identity never replaces clear text labels or familiar Material behavior.

## Adaptive shell

Primary destinations are Today, Sessions, Templates, and Settings.

| Window width | Navigation | Content |
| --- | --- | --- |
| Compact `<600dp` | Flexible/baseline Material navigation bar with icon and label | One pane, 16dp semantic outer margin |
| Medium `600–839dp` | Collapsed navigation rail | One pane or selective list-detail where the API is stable |
| Expanded `>=840dp` | Collapsed/expanded navigation rail according to available width | List-detail for Sessions/Templates; supporting pane for Active Session details where useful |

Baseline Material 3 components are required fallbacks when an M3 Expressive variant is unavailable or experimental in the repository's Compose Multiplatform version. Insets have one owner: app chrome owns system-bar insets and each content pane consumes scaffold padding once.

## Navigation and screen inventory

- SCREEN-001 Today: nearest session or first-run/create state.
- SCREEN-002 Create session: local date, template multi-select, merged count, reminder overrides.
- SCREEN-003 Active session: grouped checklist and completion actions.
- SCREEN-004 Result: full/skipped outcome and the only allowed ad slot.
- SCREEN-005 Sessions: planned/completed list and collapsible date filter.
- SCREEN-006 Templates: template list and catalog entry.
- SCREEN-007 Template editor: name and positions.
- SCREEN-008 Item catalog/selector: browse, search during selection, create/edit/delete.
- SCREEN-009 Item editor: name, usual location, quantity/bag when opened in a position context.
- SCREEN-010 Settings: reminders, Analytics, advertising privacy, About.
- SCREEN-011 Advertising consent: app-owned required decision before Yandex initialization.

Back returns to the immediately owning screen without discarding unsaved input. Destructive abandonment requires a decision only when data would be lost. Primary destination selection survives child-screen navigation and process recreation.

## Material component map

| Job | Material component | Contract |
| --- | --- | --- |
| Primary navigation | Navigation bar / navigation rail | Four stable labeled destinations; non-color selected cue |
| Top-level title/actions | Small or flexible app bar | At most two visible contextual actions; overflow for rare actions |
| Main create action | Extended FAB or filled button | One highest-prominence action per screen; remove when unavailable rather than disabling a FAB |
| Template choice | Checkbox list | Entire labeled row is one target; zero-to-many selection |
| Today/tomorrow | Filter chips or connected button group | Date chips are shortcuts, not permanent navigation |
| Other date | Material date picker | Calendar plus manual text input; no past creation date |
| Reminder time | Switch plus time picker | Switch takes effect immediately; locale-aware 12/24-hour input |
| Packed state | Checkbox list row | Announces item, quantity, packed state, bag, and optional move hint |
| Bag group | List section heading | Text is primary; bag icon is supplementary |
| Progress | Determinate linear progress indicator plus text | `X of Y`; semantics announce both purpose and value |
| Rare row actions | Icon button plus menu | Familiar more action with tooltip/pointer support |
| Required choice | Basic/full-screen dialog according to space | Explicit consequence and stable focus; no accidental outside dismissal for destructive decisions |
| Undo | Snackbar with Undo | Actionable snackbar remains until user acts or deliberately dismisses; only one at a time |
| Setting | Switch row | Entire row target; immediate state; supporting privacy copy |
| Result | Surface/hero container plus filled Done action | Content first; optional inline ad below result and Done |

## Tokens

- Color: static brand primary/secondary/tertiary families mapped to documented Material container/on-container pairs. Error handles destructive actions and failures. A distinct accessible success family may be added only as complete semantic roles and never as the sole state cue.
- Typography: semantic M3 roles only. Today hero uses one emphasized headline/title treatment; routine lists use title/body/label roles.
- Shape: component defaults; large hero surface may use the product large/extra-large shape token. Do not scatter raw radii.
- Spacing: semantic tokens based on the Material 8dp system and component-required smaller values.
- Elevation: tonal surfaces first; at most a small coherent set of Material elevation levels.
- Motion: restrained standard/expressive Material motion. Checking an item may animate state and progress without delaying input. Completion uses a brief check/shape/color transition with a reduced-motion static alternative. No confetti dependency.

## Content and text layout

All bundled text is in Compose Multiplatform Resources with English base and Russian parity. User-authored text is never translated. Important status starts each label; UI copy uses sentence case and explains destructive consequences.

At 100% font scale, short navigation labels, count badges, and compact action labels remain one line on the reference compact phone. User names and template combinations may wrap to two lines; they are not silently truncated where identity would be lost. At 200%, rows and buttons grow/reflow, the bottom completion action remains reachable, dialogs scroll, and no action is covered by the IME or navigation bar. Exact `maxLines` and overflow are declared per screen.

## Accessibility

- Minimum interactive target 48x48dp.
- Full row targets for checkbox and switch controls.
- Packed, selected, skipped, unresolved, disabled, and error states use text/icon/semantics in addition to color.
- Determinate progress and changing remaining counts are announced without excessive live-region chatter.
- Source hints and bag assignments are included in reading order but not duplicated by decorative icons.
- Every icon-only action has a localized content description and pointer/keyboard tooltip.
- Keyboard, mouse/trackpad, switch access, focus order, visible focus, and an alternative to swipe/drag are required on supported targets.
- Reduced motion replaces large translation/scale/morph with subtle opacity/color or immediate changes.
- Date/time controls support manual input and locale conventions.

## Privacy and advertising UX

Usage analytics is off by default. Settings copy says that enabling it sends app-use data and an app-instance identifier but never packing names/content; it links to the privacy policy. Turning it off takes effect immediately.

Advertising consent is shown only at the first ad-eligible full completion when a fresh endpoint response says consent is required and no current choice matches policyVersion. SCREEN-011 offers Allow, Decline, and Privacy policy. Decline returns to SCREEN-004 without an ad. Missing, failed, expired, or unresolved state produces no empty ad placeholder. A non-protected response causes no consent screen.

The only ad slot is an inline card below the result and primary Done action on SCREEN-004 after a full no-skips completion. It appears no more than once for that completion. There is no ad after skipped completion, in the checklist, creation, editors, history, Settings, notifications, or consent UI.

## Iconography and assets

`app-spec.json.assetRequirements` is the complete frozen inventory: ASSET-001 is the original BagCue mark; ASSET-002 through ASSET-017 are approved Material Symbols Rounded resources. Assets Creator must produce/adapt portable Compose Resource vectors, record provenance, and generate platform launcher derivatives under the platform contract. Standard icons remain supplementary when adjacent text already names the action.

## Preview and golden contract

Every applicable primary state of every screen has deterministic production-resource previews for light and dark. The verification cadence and risk-based stress selection are defined in quality.md; this contract does not require record/verify after every edit. Add RU 200% font scale, compact phone, and expanded/adaptive variants where the screen matrix declares risk. Permission, endpoint, Analytics, and Ads are fakes with no real network/SDK initialization. ComposablePreviewScanner discovers entry points and generates stable parameterized Paparazzi tests in the Compose UI/resource-owning module.

## Post-golden review

After primary-screen goldens stabilize, perform one complete review of SCREEN-001 through SCREEN-011 in numeric order using the current Lazyweb improve workflow. Repeat only materially changed screens or screens with unresolved findings; closing an unrelated AC does not restart the full queue. Exactly one screen/report may be in flight. Record theme, locale, font-scale, viewport, URL, findings, and disposition before submitting the next screen. Re-record only approved changes, then repeat golden verification and audit cross-screen Material/accessibility consistency.

