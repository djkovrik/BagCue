# BagCue primary-screen visual review

Review date: 2026-09-08  
AppSpec revision: `2026-09-08-flow-packages`  
Matrix: 118 scanner-owned Paparazzi renders across `SCREEN-001` through `SCREEN-011`  
Lazyweb research session: <https://www.lazyweb.com/agentic-search/d8209edf-fb81-4161-be7f-5cd164245af6>

## Method

The first complete review used the generated contact sheets in this directory and then submitted one representative production render at a time to Lazyweb, strictly in numeric screen order. AppSpec behavior and accessibility requirements remained normative; external references were used only to evaluate hierarchy, density, action grouping, and familiar Material patterns.

The comparison queue completed in this order:

| Screen | Closest reference cluster | Initial disposition |
| --- | --- | --- |
| SCREEN-001 | Canopi, Wix, Superlist | Conditional; common bottom navigation fails RU/200% |
| SCREEN-002 | Craft, Trello, Amie | Conditional; common bottom navigation fails RU/200% |
| SCREEN-003 | Craft, Trello | Blocked; CTA/action overlap and common navigation clipping at RU/200% |
| SCREEN-004 | Craft, Workday | Conditional; ad-failure collapse is correct, navigation still clips at RU/200% |
| SCREEN-005 | I Am Sober, Lawfully, ParkMobile, Google Home, Calendly | Conditional; empty-state structure accepted, common navigation fix required |
| SCREEN-006 | Craft, Pages, Wix | Blocked; template actions split into character fragments and compete with the FAB at RU/200% |
| SCREEN-007 | Craft, Canopi | Accepted with observation; dense RU/200% editor remains scrollable and controls stay present |
| SCREEN-008 | CapCut, Craft, Trello | Blocked; catalog/list actions split into character fragments at RU/200% |
| SCREEN-009 | CapCut, Craft, Trello | Blocked; catalog/editor actions split into character fragments at RU/200% |
| SCREEN-010 | Remind Me Faster, Duolingo, Stupido, Up Ahead | Blocked; settings cannot expose all content safely above navigation at RU/200% |
| SCREEN-011 | Farfetch, Jackpocket, Snapchat, Speedometer, Crunchyroll | Blocked; privacy-policy action is constrained into unreadable fragments at RU/200% |

## Cross-screen findings

1. The compact four-destination Material navigation bar does not preserve readable Russian labels at 200% font scale. Destination identity, icon, localized label, selection semantics, and a minimum 48 dp target must all remain present after reflow.
2. Template, catalog, and checklist secondary actions require a vertical or width-aware arrangement at high font scale. A button label must wrap as readable words, not into single-character columns.
3. Content, FABs, result actions, and settings scrolling need one consistent inset/Scaffold ownership contract so controls never overlap the outer navigation.
4. The result-consent policy action needs a full-width readable container instead of the narrow `AlertDialog.dismissButton` action row.
5. Standard compact/expanded light and dark states, rail navigation, empty-state hierarchy, ad-failure no-gap behavior, Material color-role pairing, and the original BagCue artwork were otherwise visually stable.

## Final disposition

Accepted after production correction and re-recording, subject to the automated Paparazzi verification recorded by the delivery ledger.

- Every regenerated contact sheet was inspected in light/dark, compact/expanded, and localized stress variants. The corrected `SCREEN-003`, `SCREEN-006`, `SCREEN-008`, `SCREEN-009`, `SCREEN-010`, and `SCREEN-011` RU/200% renders were inspected again after the final FAB relocation.
- At high font scale the four destinations now use a readable two-by-two navigation grid with icon, localized label, selected state, and 80 dp row targets; the normal navigation bar and expanded rail remain unchanged.
- Checklist, template, and catalog creation actions now occupy full-width bottom action areas at high font scale. They no longer obscure content or compete with completion actions.
- Long template/catalog actions reflow vertically, settings remain scrollable above navigation, and the privacy-policy action uses a full-width readable dialog structure.
- Sequential Lazyweb re-comparison of the six materially redesigned screens completed on 2026-09-08. The resulting reference clusters were: `SCREEN-003` Reminders/Amie/Craft; `SCREEN-006` Trello/Craft/Any.do/Asana; `SCREEN-008` and `SCREEN-009` Craft/Trello/Asana/Superlist; `SCREEN-010` Alpenglow/How We Feel/Days; and `SCREEN-011` Snapchat/Farfetch/macOS/CNN. These comparisons support the corrected hierarchy and action placement; AppSpec behavior and accessibility requirements remain normative.
- No unresolved visual blocker remains in the 118-render matrix. External references are evidence for layout judgment only, not a substitute for golden verification.
