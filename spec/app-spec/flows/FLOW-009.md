# FLOW-009 — Adaptive localized product shell

Linked requirements: REQ-009  
Linked screens: SCREEN-001, SCREEN-002, SCREEN-003, SCREEN-004, SCREEN-005, SCREEN-006, SCREEN-007, SCREEN-008, SCREEN-009, SCREEN-010, SCREEN-011

## Goal

Keep every task understandable and operable across supported locales, themes, text scales, and window sizes.

## AC-047

Given the app opens on any supported width  
When the user navigates among SCREEN-001, SCREEN-005, SCREEN-006, and SCREEN-010  
Then the same four labeled destinations use a navigation bar on compact widths and a navigation rail on wide widths while preserving selected destination and child back behavior.

## AC-048

Given the system locale is English, Russian, or unsupported  
When any SCREEN-001 through SCREEN-011 resolves bundled copy or starter content  
Then English is the complete base/fallback, Russian uses the same keys, untouched starter values follow the system, and user-authored overrides remain verbatim.

## AC-049

Given light/dark system theme, compact/expanded width, EN/RU, keyboard/pointer/touch, reduced motion, or 200% text scale  
When any SCREEN-001 through SCREEN-011 renders and receives input  
Then content reflows without clipping or hidden actions, Material roles/components remain accessible, focus/state are perceivable without color alone, and targets remain at least 48dp.

