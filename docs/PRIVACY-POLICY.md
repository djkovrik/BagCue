# Privacy policy release record

The human-readable source policy for BagCue is [`bagcue-policy.html`](bagcue-policy.html). This record prevents repository implementation, the hosted policy, and store disclosures from drifting.

## Repository contract

| Surface | Release behavior | Policy section |
|---|---|---|
| Core packing data | Local only; no account or cloud sync | 1–2 |
| Notifications | Optional, scheduled locally | 3 |
| Privacy-region request | Source-IP region classification; minimal response cached for at most 72 hours | 4, 8 |
| Yandex Mobile Ads | One non-blocking inline Android slot after an `AllPacked` result; direct Yandex only; no AD_ID; privacy resolved before initialization | 4–7 |
| Firebase Analytics | Disabled by default; explicit Settings toggle; fixed, content-free event names; no event parameters, user ID, or user properties | 4–8 |
| iOS advertising | Unit ID is null, so no Yandex initialization or request | 4 |

The repository intentionally contains no hosted policy URL. `AppActivity` must continue receiving `privacyPolicyUrl = null` until the owner supplies the reviewed HTTPS URL. A fake or placeholder URL is not acceptable.

## Publication gate

Before publication, the owner and legal reviewer must:

1. approve the processor, legal-basis, international-transfer, retention, contact, and rights language;
2. reconcile the final Android/iOS dependency and packaged-permission inventories with the policy and `PLAY-DATA-SAFETY.md`;
3. verify the Firebase, RSYA, and privacy-region external evidence listed in `APA-EXTERNAL-SETUP.md`;
4. host the exact approved HTML over HTTPS and record its immutable content hash and final URL;
5. configure that URL in the app and both store listings; and
6. verify the hosted artifact, in-app copy/link, and Play Console answers are identical in substance.

Until those steps are recorded, this file is a parity worksheet—not a legal approval or publication receipt.
