# Product

## Approved delivery revision

Revision `2026-09-08-flow-packages` is approved for a fresh BagCue development iteration under the current Vibe skills. Product scope, AC-001 through AC-051, QG-001 through QG-012, SCREEN-001 through SCREEN-011, and ASSET-001 through ASSET-017 remain required as declared. This revision supersedes the previous delivery assumptions.

Treat existing application code as reusable implementation material requiring inspection and new verification. Do not inherit verified statuses, intermediate receipts, handoffs, or completion claims from the paused iteration. Preserve that iteration as history outside the new active delivery state; do not delete its code or relabel its evidence. Starting implementation is a separate action from updating this specification.

AppSpec defines product behavior and acceptance obligations. The installed current Vibe skills define capability-package scheduling, assignment-local handoffs, registered-input targeted checks, recovery, and global audit/final evidence. There is no older AppSpec or intermediate-evidence compatibility requirement.

## Product position

BagCue is a daily packing assistant for adults aged 25–45 who combine a stable work routine with one to three recurring activities such as a gym, pool, course, music school, or time with a child. It is not a travel planner or a generic inventory system. The core promise is: combine today's kits into one clean checklist, move the right things into the right bag, and leave knowing the list was actually checked.

Brand and application name: **BagCue** in every locale. Store titles are `BagCue: Daily Packing` in English and `BagCue: Что взять с собой` in Russian. These names are working release identity, not a trademark or store-availability guarantee.

## Problem and first value

The highest-risk moment is the transition between work and a scheduled activity. The user must remember what remains at home or in another bag, avoid accidental duplicates, and prepare in the evening without losing progress before the morning check.

The minimum successful experience is:

1. Select Office plus Pool for a local date.
2. Receive one merged list, grouped by target bag, with no accidental duplicate IDs.
3. See any “Move from …” hint or unresolved bag conflict.
4. Check each physical item as packed.
5. Finish and see a clear result.

## Audience and release scope

- Intended audience: adults 18+; primary segment is working adults 25–45.
- No date-of-birth prompt or age gate.
- Android application ID: `com.sedsoftware.bagcue`.
- iOS bundle ID: `com.sedsoftware.bagcue.iosApp`.
- Android is the first public release through Google Play without a product-level country restriction.
- Android and iOS are implementation targets. iOS must compile, test, and link on macOS CI, but iOS store publication and production advertising are not part of the first public release.
- English is the complete base locale; Russian is the initial additional locale; language follows the operating system only.

## Goals

- Make evening preparation the primary value and preserve its exact progress for a morning check.
- Keep one predictable PackingSession per local calendar date.
- Reuse independent templates without silently changing an already-created session snapshot.
- Merge the same stable item ID using maximum quantity and explicitly resolve conflicting target bags.
- Keep all core packing data local, offline, editable, and recoverable from ordinary save failures.
- Provide optional local reminders without requiring permission for the core task.
- Measure the smallest useful product funnel only after the user enables Analytics in Settings.
- Monetize only after a fully successful completion and only when privacy-before-init allows Yandex Ads.

## Success metrics

Primary retained-use metric: an analytics-enabled installation records `packing_session_completed` on at least three distinct relative days in the first seven days. Exact selected dates are never transmitted.

Supporting measures:

- created-to-completed session ratio;
- use of Repeat from history;
- percentage of completions that intentionally contain skipped items;
- completion duration buckets after session creation;
- reminder-type configuration counts.

Guardrails:

- no user-created text, local entity IDs, or exact packing dates in analytics;
- no ad impression before the packing result or after a completion with skipped items;
- no network failure blocks the checklist;
- no automatic carry-over of “Packed” between calendar dates.

## User stories

- As a commuter, I can prepare tomorrow's Office plus Pool list in the evening and continue the same state in the morning.
- As a repeat user, I can keep independent kits and combine any subset for a date.
- As someone carrying two bags, I can see where every item should go without maintaining a permanent bag inventory.
- As a cautious user, I can distinguish removing an irrelevant item from deliberately finishing with a skipped item.
- As a returning user, I can inspect history and repeat a prior combination without reusing its checked states.
- As an offline user, I can pack, edit, and review history without Ads, Analytics, or the privacy endpoint.
- As a privacy-conscious user, I can leave Analytics off, decline advertising consent when asked, and still use every core feature.

## First-run content

There is no onboarding carousel. The Today empty state explains the benefit and presents two editable starter templates:

- Office: Laptop, Charger, Headphones, Badge, Keys, Water bottle, Lunch/snack; quantity 1; target label Backpack.
- Pool: Swimsuit/trunks, Swim cap, Goggles, Towel, Shampoo/shower gel, Flip-flops, Wet-items bag; quantity 1; target label Sports bag.

Starter template, item, and label names use stable IDs and shared resource keys. Unedited values follow the system locale. The first user rename becomes user-authored text and no longer auto-translates. User deletion or modification persists and is never silently re-seeded.

## Approved boundaries

The MVP includes catalog CRUD, template CRUD and duplication, one dated session, manual template selection, merged session snapshots, two item states, bag labels, source hints, history, repeat, optional local reminders, Firebase Analytics disabled by default, Yandex-only result advertising, light/dark UI, EN/RU, accessibility, previews, and golden testing.

## Explicit non-goals

- accounts, cloud sync, family sharing, school integrations;
- travel planning or several-workplace specialization;
- automatic weekly schedules or automatic session creation;
- permanent bag contents or automatic packed-state carry-over;
- import/export;
- AI, OCR, geolocation, or automatic bag detection;
- measurement units beyond integer item count 1–99;
- interstitial, rewarded, mediation, Google Mobile Ads, advertising identifiers, analytics ad audiences, or Google Ads linkage;
- a language picker, theme picker, or Android dynamic color;
- iOS production advertising or App Store publication in the first public delivery.

## Product evidence

Initial UI-pattern evidence: https://www.lazyweb.com/agentic-search/add6619a-4738-4082-8242-55d9fa00e1ba

The evidence supports a date-and-progress home focus, direct checklist interaction, explicit multi-selection, compact history, contextual notification permission, and a restrained completion moment. It does not define BagCue's requirements and is not evidence of measured monetization lift. The monetization experiment search at https://www.lazyweb.com/agentic-search/d77b3a7d-d63c-4c8d-9a0f-0bc8fb2e37ad found no directly relevant measured checklist-ad experiment.

