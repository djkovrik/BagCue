# Privacy policy release record

The legally reviewed, stable human-readable source policy for BagCue is [`bagcue-policy.html`](bagcue-policy.html). Its canonical public URL is [https://sedsoftware.com/apps/bagcue/policy.html](https://sedsoftware.com/apps/bagcue/policy.html). This record prevents repository implementation, the hosted policy, and store disclosures from drifting.

## Repository contract

| Surface                | Release behavior                                                                                                                       | Policy section |
|------------------------|----------------------------------------------------------------------------------------------------------------------------------------|----------------|
| Core packing data      | Local only; no account or cloud sync                                                                                                   | 1–2            |
| Notifications          | Optional, scheduled locally                                                                                                            | 3              |
| Privacy-region request | Source-IP region classification; minimal response cached for at most 72 hours                                                          | 4, 8           |
| Yandex Mobile Ads      | One non-blocking inline Android slot after an `AllPacked` result; direct Yandex only; no AD_ID; privacy resolved before initialization | 4–7            |
| Firebase Analytics     | Disabled by default; explicit Settings toggle; fixed, content-free event names; no event parameters, user ID, or user properties       | 4–8            |
| iOS advertising        | Unit ID is null, so no Yandex initialization or request                                                                                | 4              |

The reviewed source and hosted HTTPS artifact were fetched and compared byte-for-byte on 2026-09-10. Both have SHA-256 `3bace33ed5b83c21c0901d20f6fca23c622fb9d7f5402f498209b5e119566e7a`. Android and iOS production composition roots expose this exact URL and open it through their native external-URL APIs.

## Publication gate

Legal review of the processor, legal-basis, international-transfer, retention, contact, rights, category, purpose, and sharing language is complete as of 2026-09-10. Publication must still:

1. reconcile the exact Android/iOS candidate dependency and packaged-permission inventories with the policy and `PLAY-DATA-SAFETY.md`;
2. verify the candidate-specific Firebase and RSYA evidence listed in `APA-EXTERNAL-SETUP.md`; QG-008 production endpoint readiness is already owner-confirmed and closed;
3. run `.github/workflows/scripts/validate_privacy_policy.py` so the canonical URL is reachable and byte-identical to the reviewed source;
4. retain the candidate-specific in-app link and Play Console parity evidence.

This file records completed legal approval and policy publication. It does not replace candidate-specific technical, store, or publication receipts.
