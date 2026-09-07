# Domain

## Glossary

- **PackingItem**: a reusable catalog identity with stable ID, localized seed identity or user-authored name, and optional usual-location text.
- **KitTemplate**: an independent named set of TemplatePositions. No template is technically primary or base.
- **TemplatePosition**: a PackingItem reference plus quantity and optional target-bag label copied into sessions.
- **PackingSession**: the only session for one local calendar date, containing a durable snapshot of selected template identities/names and merged SessionPackingItems.
- **SessionPackingItem**: a session snapshot entry with catalog ID, display-name snapshot rules, quantity, target-bag label, source hint, packed state, and optional skipped outcome.
- **Bag label**: user-visible text for grouping positions; it is not an inventory entity.
- **Usual location**: optional user-authored source text that may produce “Move from …”; it never asserts actual physical location.
- **ReminderPreferences**: disabled-by-default global evening/morning settings plus per-session copied overrides.
- **AdvertisingPrivacyState**: the minimal endpoint response and policy-version-bound app-owned choice.

## Identity and normalized names

Every PackingItem and KitTemplate has a language-neutral stable ID. Item names are not unique. Before creating an item, trim outer whitespace, collapse internal whitespace, and compare case-insensitively using locale-independent Unicode case folding. A match offers reuse first; only explicit “Create another” produces a distinct stable ID.

Identity, not display name, controls merging. Two different IDs with the same text remain separate session rows. One ID referenced by several templates becomes one SessionPackingItem.

## Quantities and merge

Quantity is an integer from 1 through 99. A template combination uses the maximum quantity for the same item ID, never a sum. There is no unit value, weight, volume, or fractional quantity.

For one merged item:

- equal non-empty target-bag labels remain assigned;
- all empty labels produce “No bag”;
- one non-empty label plus empty labels uses the non-empty label;
- two or more distinct normalized non-empty labels produce an unresolved conflict and the group “Choose a bag”.

No completion operation is permitted while a bag conflict remains.

## Session date and uniqueness

A PackingSession owns a `LocalDate`, not an instant-derived date. Exactly one session of any status may exist for that date. Creation is allowed for today or any future date; past sessions cannot be created retroactively.

A device time-zone change never changes the stored session date. Local reminder occurrences are recalculated for the same local date and wall-clock time in the current zone. Past occurrences are not fired retroactively, and scheduling keys prevent duplicates after time, zone, boot, or permission changes.

## Session snapshot and recomposition

Creation copies selected template IDs/names and current positions into a session snapshot. Later template edits or deletion do not mutate it.

When the user explicitly reapplies or replaces template selections:

- matching PackingItem IDs preserve packed state and current session bag assignment;
- merged quantity is recalculated using maximum quantity;
- new IDs enter as NotPacked;
- removed IDs leave the session;
- the whole change is undoable as one action;
- unresolved bag conflicts are recalculated.

A one-off new item receives a catalog ID atomically with insertion into the session. Saving it to templates is a separate explicit operation.

## Item states and completion

Runtime item state is exactly `NotPacked` or `Packed`. “Move from …” is a derived presentation hint, not a third state. A packed action hides the hint for the current checklist presentation but does not erase the source text.

Full completion requires at least one session item, no unresolved bag conflict, and every remaining item Packed. Completion with skipped items requires at least one item, no unresolved bag conflict, an explicit secondary action, and confirmation that names the count remaining. Skipped items remain in the completed snapshot; temporarily removed items do not.

Completing and reopening mutate the same session status. A new calendar day always creates fresh NotPacked states; no state is inherited from yesterday.

## Starter localization and overrides

Starter templates, starter items, and starter bag labels are stable built-in IDs mapped to shared resource keys. An unedited built-in name is resolved only at presentation. On first rename, a user-authored override is stored. User text remains verbatim across locale changes. Deleting or editing starter content creates durable seed-override state so an app restart or upgrade cannot silently restore removed content.

## Delete semantics

- Deleting a PackingItem requires a dependency summary and confirmation. One database transaction removes it from the catalog, templates, and unfinished session snapshots. Completed session snapshots retain the display name and outcome recorded at completion.
- Deleting a KitTemplate never changes an existing session snapshot.
- Deleting a PackingSession releases its LocalDate uniqueness key and changes no item/template records.
- Deleting a session bag label reassigns its items to “No bag”.
- Template selection changes, session-item removal, and session deletion provide a recoverable undo action; failed persistence means the deletion never becomes visible as committed state.

## Error semantics

Managers expose Kotlin `Result` and preserve causes through `runCatching`/`unwrap`. A failed save retains the user's input and prior durable value, surfaces an actionable localized error adjacent to the action, and allows retry. Multi-record deletes, one-off item insertion, template replacement, and completion are atomic transactions. Cancellation is never swallowed as a generic failure.

## Analytics vocabulary

The only custom event names are `template_created`, `packing_session_created`, `packing_session_completed`, `packing_session_reopened`, `history_repeat_used`, and `reminder_configured`. Parameters are allowlisted categorical buckets only. Domain/user IDs, exact dates, names, source hints, bag labels, and per-item interactions never cross the analytics boundary.

