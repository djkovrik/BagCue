# FLOW-001 — Manage item catalog

Linked requirements: REQ-001  
Linked screens: SCREEN-008, SCREEN-009

## Goal

Maintain reusable item identities without accidental duplicates or broken dependencies.

## Entry and exit

Enter from Templates or an item-selection action. Exit after a durable create, edit, reuse, or confirmed delete; failures retain input and previous data.

## AC-001

Given starter items or user-created PackingItems exist  
When the user opens SCREEN-008  
Then the catalog shows current localized starter identities, user overrides, and user-created items without restoring deleted starter content.

## AC-002

Given the user is on SCREEN-008 or SCREEN-009  
When a valid nonblank item name is saved  
Then one PackingItem with a new stable ID is persisted and becomes selectable.

## AC-003

Given a PackingItem exists  
When the user saves a new name or usual-location value on SCREEN-009  
Then the same stable ID is updated and future reads show the new user-authored values.

## AC-004

Given a PackingItem is referenced by templates or unfinished sessions  
When the user reviews the dependency list and confirms deletion on SCREEN-008 or SCREEN-009  
Then one transaction removes the item from the catalog, templates, and unfinished sessions while completed history retains its completion-time text snapshot.

## AC-005

Given an existing item name matches the normalized proposed name  
When the user attempts to create it on SCREEN-008 or SCREEN-009  
Then the existing item is offered first and a distinct ID is created only through explicit Create another.

