# FLOW-006 — Configure local reminders

Linked requirements: REQ-006  
Linked screens: SCREEN-001, SCREEN-002, SCREEN-003, SCREEN-010

## Goal

Offer optional evening and morning reminders without making permission a prerequisite.

## AC-031

Given the app has no stored ReminderPreferences  
When SCREEN-010 first reads settings  
Then evening and morning reminders are disabled with suggested local times 20:00 and 07:30.

## AC-032

Given global settings exist and a session may have copied overrides  
When SCREEN-002 or SCREEN-010 opens  
Then each enabled state and time is read from the correct global or per-session source.

## AC-033

Given reminder controls are visible on SCREEN-002 or SCREEN-010  
When the user enables, disables, or changes a time  
Then the preference persists immediately and affected future local notification occurrences are updated.

## AC-034

Given the user enables a reminder and the OS permission is denied or later revoked  
When they return to SCREEN-001, SCREEN-002, SCREEN-003, or SCREEN-010  
Then packing remains fully available and the reminder state explains that notifications cannot be delivered.

## AC-035

Given a future session reminder is enabled  
When the wall clock, time zone, boot state, or permission changes  
Then the reminder is rescheduled for the same session LocalDate/current local wall time, past occurrences are skipped, and no duplicate request remains.

