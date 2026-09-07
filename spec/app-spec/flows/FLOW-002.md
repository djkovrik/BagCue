# FLOW-002 — Manage packing templates

Linked requirements: REQ-002  
Linked screens: SCREEN-006, SCREEN-007, SCREEN-008

## Goal

Maintain independent reusable kits and safe snapshots.

## AC-006

Given the user is on SCREEN-006  
When they create and save a template on SCREEN-007  
Then an independent KitTemplate with a stable ID is persisted even when its position list is empty.

## AC-007

Given starter or user templates exist  
When the user opens SCREEN-006 or a template on SCREEN-007  
Then current names and positions are read without mutating any PackingSession snapshot.

## AC-008

Given a KitTemplate is open on SCREEN-007  
When the user changes its name, item membership, quantity from 1–99, bag label, or usual-location source via SCREEN-008  
Then the template persists the change and existing sessions remain unchanged until explicit reapplication.

## AC-009

Given a KitTemplate exists  
When the user chooses Duplicate on SCREEN-006 or SCREEN-007  
Then a new stable template ID with copied positions and a distinguishable editable name is persisted.

## AC-010

Given a KitTemplate exists and may be referenced by session snapshots  
When the user confirms deletion on SCREEN-006 or SCREEN-007  
Then the template is deleted and every existing session snapshot remains readable and unchanged.

## AC-011

Given untouched or user-modified Office and Pool starter data exists  
When the app starts, restarts, upgrades, or the system locale changes while SCREEN-006, SCREEN-007, or SCREEN-008 is visible  
Then untouched names resolve through current EN/RU resources while overrides and deletions remain exactly user-authored.

