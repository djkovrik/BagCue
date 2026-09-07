# FLOW-005 — History and session lifecycle

Linked requirements: REQ-005  
Linked screens: SCREEN-001, SCREEN-002, SCREEN-003, SCREEN-004, SCREEN-005

## Goal

Find, correct, repeat, and delete dated sessions without corrupting templates or dates.

## AC-026

Given planned or completed sessions exist  
When the user opens SCREEN-005 or views the nearest entry on SCREEN-001  
Then sessions appear chronologically under Planned and Completed and an expanded calendar selection filters the list by LocalDate.

## AC-027

Given a completed session exists  
When the user chooses Reopen from SCREEN-004 or SCREEN-005  
Then the same dated entity becomes active on SCREEN-003 with all recorded states preserved.

## AC-028

Given a completed session appears on SCREEN-005  
When the user chooses Repeat and selects an available date on SCREEN-002  
Then a new session uses the historical template combination/current template snapshots but begins with every item NotPacked.

## AC-029

Given any session is visible on SCREEN-001 or SCREEN-005  
When the user confirms Delete  
Then the session is removed, its date becomes available, templates/items remain unchanged, and actionable Undo can restore it.

## AC-030

Given a PackingSession owns a LocalDate and the device zone or clock changes  
When SCREEN-001, SCREEN-003, or SCREEN-005 next reads it  
Then the session remains on the same selected calendar date rather than shifting from an instant conversion.

