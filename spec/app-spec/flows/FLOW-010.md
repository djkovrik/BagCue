# FLOW-010 — Recover from persistence failures

Linked requirements: REQ-010  
Linked screens: SCREEN-002, SCREEN-003, SCREEN-005, SCREEN-006, SCREEN-007, SCREEN-008, SCREEN-009, SCREEN-010

## Goal

Never represent an uncommitted local mutation as success or discard recoverable user input.

## AC-050

Given the user has entered or changed valid data on SCREEN-002, SCREEN-003, SCREEN-006, SCREEN-007, SCREEN-008, SCREEN-009, or SCREEN-010  
When the local save fails  
Then the input remains visible, the prior durable state remains authoritative, an adjacent localized error preserves the cause category, and Retry is available.

## AC-051

Given an item, template, or session delete on SCREEN-005, SCREEN-006, SCREEN-007, SCREEN-008, or SCREEN-009 affects several local records  
When any step of the persistence transaction fails  
Then no partial cascade is committed, the UI reports failure rather than removal, and a retry can reattempt the complete atomic operation.

