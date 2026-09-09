# Icon and action inventory

The frozen inventory is coherent: one original BagCue mark and sixteen tintable Material Symbols Rounded resources. A new custom action-icon family is not recommended. Product style should come from the mark, semantic tokens and selective hero treatment; familiar actions should remain familiar.

| Asset | Resource | Intended use | Observed disposition |
| --- | --- | --- | --- |
| ASSET-001 | `bagcue_mark` | Today/Result/Settings brand moment | Keep original artwork. Decide whether it is decorative everywhere or expose one localized BagCue description; current placements often lack adjacent BagCue text. |
| ASSET-002 | `ic_today` | Today destination | Keep; icon plus navigation label is required, not redundant. |
| ASSET-003 | `ic_sessions` | Sessions destination | Keep; icon plus navigation label is required. |
| ASSET-004 | `ic_templates` | Templates destination / supplementary checklist concept | Keep for navigation; avoid repeating it next to already clear ordinary text unless it aids grouping. |
| ASSET-005 | `ic_settings` | Settings destination | Keep; icon plus navigation label is required. |
| ASSET-006 | `ic_add` | Create session/template/item; add item/position | Keep inside extended FAB/filled action. Adjacent object-specific text is useful and not redundant because `+` alone does not name the object. |
| ASSET-007 | `ic_check` | Packed/completed state | Keep as supplementary non-color state cue; checkbox remains the control. Do not announce decorative check beside outcome text twice. |
| ASSET-008 | `ic_calendar` | Date selection/filter | Keep for date picker/filter. Make paired date shortcuts visually and semantically consistent; do not icon only Today while Tomorrow is text-only. |
| ASSET-009 | `ic_edit` | Edit item/position, reopen/edit where applicable | Use for actual Edit, preferably icon-only with tooltip in constrained rows or text-only when object wording is necessary. Do not combine object-specific text and icon by default. |
| ASSET-010 | `ic_delete` | Confirmed delete/remove | Use for direct delete, not More. Error emphasis belongs in confirmation. Icon-only needs localized object/action description and tooltip. |
| ASSET-011 | `ic_more` | Opens overflow menu | Historical baseline misuse as direct Remove/Delete is corrected in refreshed goldens. Only retain when it opens a real labeled menu. |
| ASSET-012 | `ic_search` | Selector-mode catalog search | Keep as decorative leading search-field icon with null semantics when the field label already names Search. |
| ASSET-013 | `ic_duplicate` | Duplicate template | In roomy surfaces icon + concise `Duplicate` is acceptable; avoid `Duplicate {long object name}` plus icon in the row. Use overflow on compact lists. |
| ASSET-014 | `ic_notifications` | Reminder rows | Keep decorative beside a fully labeled reminder row; avoid duplicate content description. |
| ASSET-015 | `ic_privacy` | Privacy group/consent context | Keep as section cue; decorative when heading/policy text already supplies meaning. Do not repeat on every privacy link. |
| ASSET-016 | `ic_arrow_back` | Back navigation, auto-mirrored | Use a canonical 48 dp IconButton with localized Back description. Remove visible `Back` text. Do not pair the arrow with `Cancel`; use a close icon or text Cancel only when dismissal—not navigation—is intended. |
| ASSET-017 | `ic_bag` | Bag heading/assignment | Keep as supplementary bag/location cue. Historical misuse as Edit is corrected in refreshed goldens. |

## Redundant or misleading icon + text actions

| Location | Baseline treatment | Finding | Required treatment |
| --- | --- | --- | --- |
| SCREEN-002/003 child app bars | back arrow + `Back` | Historical defect in the superseded baseline; absent from refreshed goldens | Resolved: icon-only Back with localized semantics; retain pointer/focus tooltip |
| SCREEN-007 editor app bar | back arrow + `Cancel` | Historical semantic mismatch in the superseded baseline; absent from refreshed goldens | Resolved: icon-only Back; keep any discard-specific Cancel separate |
| SCREEN-003 item row | More icon + `Remove for today` | Historical baseline defect; refreshed rows use edit/delete glyphs | Resolved visually; retain localized labels/tooltips and merged 48 dp targets |
| SCREEN-005 session row | More icon + `Delete` | Historical baseline defect; refreshed rows use Delete | Resolved visually; reduce the wider row action cluster separately |
| SCREEN-006 template row | Duplicate/Delete icons + object-specific labels | Repeats meaning and causes RU width pressure | One row primary action; secondary actions in overflow with fully labeled menu items |
| SCREEN-007 position row | Bag icon + `Edit {item}` | Historical baseline defect; refreshed rows use Edit | Resolved visually; bag remains only in metadata |
| SCREEN-008 catalog row | Edit/Delete icons + object-specific labels | Historical baseline defect; refreshed catalog uses compact icon-only actions | Resolved visually; verify tooltips, labels and 48 dp merged targets |
| SCREEN-010/011 privacy | Privacy icon next to repeated privacy wording | Often decorative repetition | One section/context icon; null semantics when adjacent heading names it |

Four repository drawables outside the frozen inventory (`ic_rotate_right`, `ic_dark_mode`, `ic_light_mode`, `ic_cyclone`) have no production references and should be removed in a later cleanup or formally added to an approved inventory before use. They are not a reason to expand the visible icon set.
