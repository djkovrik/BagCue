# FLOW-004 — Pack and complete

Linked requirements: REQ-004  
Linked screens: SCREEN-001, SCREEN-003, SCREEN-004, SCREEN-006, SCREEN-007, SCREEN-008, SCREEN-009

## Goal

Check physical items with minimal interaction and produce an honest completion result.

## AC-018

Given a session was prepared in the evening or previously interrupted  
When the user opens it from SCREEN-001 or SCREEN-003  
Then the exact persisted snapshot, bag groups, source hints, conflicts, and packed states are restored.

## AC-019

Given a SessionPackingItem is NotPacked on SCREEN-003  
When the user activates its full checkbox row  
Then it becomes Packed, progress updates, the move hint stops displaying for that execution, and the change persists.

## AC-020

Given a session item is visible on SCREEN-003  
When the user changes its target bag or source hint through SCREEN-009  
Then only the current session snapshot changes unless a separate save-to-template action is completed.

## AC-021

Given an active nonempty session is open  
When the user creates a new one-off item through SCREEN-003, SCREEN-008, or SCREEN-009  
Then one transaction creates its catalog stable ID and inserts a NotPacked session item without changing any template.

## AC-022

Given an item is present on SCREEN-003  
When the user chooses Remove for today  
Then the item leaves the session and history result, while an actionable Undo restores it without changing templates.

## AC-023

Given the session has at least one item, no bag conflict, and every item is Packed  
When the user chooses Complete packing on SCREEN-003  
Then the same session becomes completed with all-packed outcome and SCREEN-004 shows the full-success result.

## AC-024

Given the session has at least one item, no bag conflict, and some items remain NotPacked  
When the user explicitly chooses Complete with skipped and confirms the exact remaining count on SCREEN-003  
Then the session is completed with skipped snapshots and SCREEN-004 shows them without an ad.

## AC-025

Given a one-off or session-edited item exists  
When the user explicitly selects one or more templates on SCREEN-003, SCREEN-006, or SCREEN-007 and saves  
Then those KitTemplates receive the chosen item position while the session remains a separate snapshot.

