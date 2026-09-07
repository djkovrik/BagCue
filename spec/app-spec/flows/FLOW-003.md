# FLOW-003 — Compose a dated session

Linked requirements: REQ-003  
Linked screens: SCREEN-001, SCREEN-002, SCREEN-003, SCREEN-005

## Goal

Create exactly one useful merged checklist for a chosen local date.

## Ordered steps

1. Start from SCREEN-001, SCREEN-005, or Repeat.
2. Choose today, tomorrow, or a future date on SCREEN-002.
3. Select zero or more independent templates and see the merged count.
4. Inherit or override reminders.
5. Create and open SCREEN-003; resolve any bag conflicts there.

## AC-012

Given no PackingSession exists for the selected today-or-future LocalDate  
When the user chooses at least one resulting item and saves SCREEN-002  
Then exactly one active PackingSession snapshot is persisted for that date and SCREEN-003 opens.

## AC-013

Given selected templates contain the same PackingItem stable ID  
When the session snapshot is composed on SCREEN-002 or recomposed for SCREEN-003  
Then one SessionPackingItem is produced with the maximum requested quantity and no accidental duplicate row.

## AC-014

Given one merged item has distinct non-empty target-bag labels  
When SCREEN-003 renders the merged checklist  
Then the item appears under Choose a bag and completion stays unavailable until the user explicitly assigns one label.

## AC-015

Given an active session contains packed and unpacked items  
When the user explicitly changes/reapplies selected templates through SCREEN-002 or SCREEN-003  
Then matching IDs retain packed state and session bag, new IDs are NotPacked, removed IDs leave, and Undo restores the whole prior snapshot.

## AC-016

Given selected templates produce no session item  
When the user tries to create or complete from SCREEN-002 or SCREEN-003  
Then the action is rejected and the empty state offers Add item and Choose another template.

## AC-017

Given a session already occupies a selected date  
When the user starts creation or Repeat from SCREEN-002, SCREEN-003, or SCREEN-005  
Then the app offers Open existing or explicitly Replace templates, and replacement applies AC-015 preservation rules.

