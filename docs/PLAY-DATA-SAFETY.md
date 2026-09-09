# Google Play Data Safety worksheet

Status: **draft; Play Console submission is externally blocked**.

This worksheet is intentionally conservative. It describes the release architecture and current provider disclosures, but the owner must answer the Play Console questionnaire from the final signed artifact, provider contracts, actual console configuration, and legal classification. “Collected” in Play terminology can include data transmitted off device by an SDK even when BagCue does not retain it.

## Proposed inventory for final review

| Play category                                    | When transmitted                                                                                  | Recipient/purpose                                                                                         | Current minimization                                                                                                                           |
|--------------------------------------------------|---------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------|
| Approximate location / source IP-derived region  | Privacy-region request; provider network connections                                              | Service Provider for consent-region decision; Google/Yandex may derive coarse geography from network data | No location permission; response contains no country or IP; response expires within 72 hours                                                   |
| App interactions                                 | Firebase only after user enables Usage analytics; Yandex only when the permitted ad loads/is used | Analytics; advertising delivery/measurement/fraud prevention                                              | Six content-free custom event names; no parameters or user-entered content; exactly one eligible ad surface                                    |
| Device or other identifiers                      | Firebase only after opt-in; Yandex subject to its SDK/network operation                           | Analytics; advertising delivery/measurement/fraud prevention                                              | Android AD_ID and AdServices ID/attribution permissions removed; no User-ID/user properties; no account; iOS IDFA/AdSupport product not linked |
| App/device technical information and diagnostics | When a configured provider SDK or endpoint connection operates                                    | Service operation, security, diagnostics, advertising/analytics                                           | No user-created packing content; no stable identifier sent to the privacy endpoint                                                             |

Do not mark precise location, contacts, photos, user-created packing text, payment information, health data, or account data as collected by BagCue unless final artifact/network testing reveals a new path.

## Console questions that require owner/legal evidence

- Whether each provider relationship is “sharing” or service-provider processing under the final contract and Play definitions.
- Whether every transmitted category is required or optional. Firebase Analytics is optional and user-controlled; advertising and the privacy-region decision are independent.
- Purposes for each provider: analytics, advertising/marketing, app functionality, fraud prevention/security/compliance, or another applicable Play choice.
- Encryption in transit for every final endpoint and SDK route, verified from the signed release candidate; repository intent alone is not evidence.
- Data deletion response: local deletion is available through app-data clearing/uninstall and supported in-app record deletion, but provider-held data and identifier-based requests require the final provider/owner process.
- Retention and ephemeral-processing answers after Firebase two-month retention and provider settings are verified in their consoles.

## Required release evidence

1. Generate the final dependency/SBOM and merged release manifest; confirm no `AD_ID` and no undeclared SDK.
2. Run clean-install network diagnostics for analytics off, analytics on, analytics off again, protected accept, protected decline, non-protected, endpoint failure/expiry, debug, preview/test, and iOS.
3. Record Firebase retention/signals/linkage/sharing configuration, RSYA ownership/configuration, and privacy-region logging/GeoIP controls.
4. Have the owner and legal reviewer approve the category/purpose/sharing choices.
5. Submit in Play Console, export or screenshot the final answers, and compare them to the hosted privacy policy.

Primary references: [Google Play Data Safety guidance](https://support.google.com/googleplay/android-developer/answer/10787469), [Firebase Android disclosure guidance](https://firebase.google.com/docs/android/play-data-disclosure), [Google Analytics disclosure details](https://support.google.com/analytics/answer/11582702), and [Yandex Mobile Ads privacy/security guidance](https://ads.yandex.com/helpcenter/en/easy/integration/android/advanced-settings/security-privacy).
