# FLOW-008 — Advertising privacy and result placement

Linked requirements: REQ-008  
Linked screens: SCREEN-001, SCREEN-002, SCREEN-003, SCREEN-004, SCREEN-005, SCREEN-006, SCREEN-007, SCREEN-008, SCREEN-009, SCREEN-010, SCREEN-011

## Goal

Show at most one non-blocking Android ad after a fully successful result and never before privacy eligibility.

## AC-040

Given no fresh AdvertisingPrivacyState exists  
When the app refreshes eligibility before an ad-eligible SCREEN-004 result or privacy Settings on SCREEN-010  
Then a valid minimal endpoint response is persisted with freshness clamped to 72 hours and without country/IP/identifier data.

## AC-041

Given an AdvertisingPrivacyState is stored  
When an ad-eligible result on SCREEN-004 or privacy Settings on SCREEN-010 reads it  
Then only a future unexpired schema-1 response is accepted and stale/malformed state is not treated as eligible.

## AC-042

Given a fresh response requires consent and no current choice matches policyVersion  
When the user chooses Allow or Decline on SCREEN-011 reached from SCREEN-004 or SCREEN-010  
Then the choice is stored only with the current minimal response and applied before any permitted Yandex initialization.

## AC-043

Given the stored response is expired, malformed, or has a different policyVersion  
When SCREEN-004, SCREEN-010, or SCREEN-011 evaluates privacy  
Then the obsolete choice is invalidated, Ads remain off, and a required fresh choice is requested only after a fresh protected response.

## AC-044

Given the endpoint fails, privacy is unresolved/expired, or the user declines on SCREEN-011  
When SCREEN-004 evaluates advertising  
Then Yandex is not initialized, no ad is requested or reserved, and the user returns to the intact result.

## AC-045

Given Android has a real release ad unit ID, privacy permits initialization, and SCREEN-004 represents a full no-skips completion  
When the result becomes ad-eligible  
Then at most one inline ad is shown below the result and Done action without obscuring or delaying them.

## AC-046

Given any other context is shown on SCREEN-001 through SCREEN-011, the result contains skipped items, an ad fails, or iOS has null production configuration  
When advertising eligibility is evaluated  
Then no ad container blocks content, no disallowed request occurs, and iOS release/CI never initializes the SDK.

