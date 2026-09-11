# Follow-up assignment: Settings privacy error copy

The clean-install Settings investigation found that `PrivacyRefreshFailed` is rendered with the reminder-save error string. Correct this narrow presentation defect while preserving the existing Settings layout and recovery action.

Scope and constraints:

- Map a privacy refresh failure to localized, privacy-specific copy instead of “Couldn’t save the reminder.”
- Keep English as the complete base locale and add the matching Russian resource.
- Preserve the endpoint-unresolved state and `Try again` recovery action.
- Re-record and inspect only the affected SCREEN-010 endpoint-unresolved light/dark goldens, then verify the visual suite.
- Preserve unrelated user work and the already-completed network/Store fix.

Obligations: SCREEN-010, FLOW-008, AC-040, AC-044.
