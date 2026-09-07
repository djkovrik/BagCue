# FLOW-007 — Control minimal Analytics

Linked requirements: REQ-007  
Linked screens: SCREEN-001, SCREEN-002, SCREEN-003, SCREEN-004, SCREEN-005, SCREEN-006, SCREEN-010

## Goal

Measure the approved funnel only after explicit Settings opt-in and without packing content.

## AC-036

Given no AnalyticsPreference has been stored  
When Analytics configuration initializes for SCREEN-010  
Then collection is disabled before Firebase Analytics may collect or upload an event.

## AC-037

Given an AnalyticsPreference exists  
When the user opens SCREEN-010  
Then the Usage analytics switch shows its current value and supporting copy discloses app-use data and the app-instance identifier.

## AC-038

Given the Usage analytics switch is visible on SCREEN-010  
When the user enables or disables it  
Then Firebase collection changes immediately; disabling also resets local Analytics data and the app-instance ID without blocking the app.

## AC-039

Given Analytics is enabled and an approved action occurs on SCREEN-001, SCREEN-002, SCREEN-003, SCREEN-004, SCREEN-005, SCREEN-006, or SCREEN-010  
When the Analytics adapter forms an event  
Then only an approved event name and categorical allowlisted parameters are sent, with no user text, entity ID, exact date, bag/source value, or per-item check event.

