# Analytics, privacy-region, and advertising release setup

This is a release-blocking checklist, not evidence that an external console or production service is configured. Repository checks cannot substitute for the owner, legal, console, hosted-service, or physical-device evidence below.

## Firebase Analytics

The application adapter accepts only the six names declared by `AnalyticsEventName` and sends no event parameters, user ID, or user properties. Collection is disabled in native configuration before Firebase starts. The Settings switch is the only application control that enables collection; disabling it stops future collection and calls the Firebase reset API.

Before a release that contains Firebase, the owner must:

1. Create the Android and iOS Firebase applications, download their correct production configuration files, and commit them directly as `androidApp/google-services.json` and `iosApp/iosApp/GoogleService-Info.plist`. These service files are approved repository configuration and must not be stored in GitHub Secrets. Verify their project/app identifiers and never commit a file for an unrelated project.
2. Confirm in both packaged applications that Analytics collection starts disabled.
3. In the Analytics/Firebase consoles, set event and user-level retention to two months and disable Google Signals, ads personalization, User-ID, custom user properties, Google Ads linking, and advertising-data sharing.
4. Confirm the Android merged release manifest has no `com.google.android.gms.permission.AD_ID`, `ACCESS_ADSERVICES_AD_ID`, `ACCESS_ADSERVICES_ATTRIBUTION`, or `android.ext.adservices`; confirm IDFA is not linked on iOS. The repository uses the Firebase Analytics/Core product rather than the AdSupport product.
5. Run a clean-install opt-in/opt-out diagnostic: no Analytics request before opt-in; only allowlisted, parameter-free events afterward; no event after opt-out; reset produces no readable current app-instance ID until collection is enabled again.
6. Record redacted console screenshots, packaged-artifact inspection, timestamp, app version, SDK versions, device/OS, and tester. Do not record identifiers in the evidence.

Official references: [Firebase Android collection controls](https://firebase.google.com/docs/analytics/android/configure-data-collection), [Firebase Apple collection controls](https://firebase.google.com/docs/analytics/ios/configure-data-collection), [Firebase Android Play disclosure](https://firebase.google.com/docs/android/play-data-disclosure), and [Google Analytics retention](https://support.google.com/analytics/answer/7667196).

## Privacy-region endpoint

The endpoint is `https://168.222.252.178/v1/privacy/region`. On 2026-09-08 at 14:20:46 GMT, one ordinary route returned HTTP 200, `application/json`, 132 bytes, `Cache-Control: no-store`, HSTS, `nosniff`, `DENY`, `no-referrer`, and this minimal schema-1 body:

```json
{"schemaVersion":1,"consentRequired":false,"policyVersion":"2026-08-privacy-region-v1","expiresAt":"2026-09-11T14:20:46.958248244Z"}
```

That observation proves only that one public route was reachable with a fresh response. Release remains blocked until controlled tests establish protected, non-protected, and unknown routes; trusted-proxy spoof resistance; GeoIP source and freshness; aggregate-only telemetry; raw-IP and forwarding-header redaction in application, edge, trace, and error logs; operational ownership; policy ownership; and legal review. The app rejects redirects, non-200 status, any Content-Type other than exactly `application/json`, invalid/extended schema, invalid UTF-8, bodies over 4 KiB, blank policy versions, and expiry outside `(now, now + 72h]`. Connect, request, and socket timeouts are five seconds. Every failure leaves ads off without blocking packing.

## Yandex Mobile Ads

Android advertising is direct Yandex Mobile Ads only; there is no mediation or Google demand. The production ad unit is `R-M-19857241-1` and must exist only in the Android release variant. Debug, tests, previews, and iOS use a blank/null unit ID and must make no real ad request. Android removes platform and Google Play services advertising-ID/attribution permissions and the optional AdServices library from the merged manifest, and disables Yandex automatic SDK initialization. The app calls `YandexAds.setUserConsent(...)` before initialization, waits for initialization, and reports `Ready` only after the SDK's load callback.

A request is allowed only for the single non-blocking inline slot on an Android `AllPacked` completed-session result, after fresh privacy eligibility, at most once for that retained completion. Decline, withdrawal, unresolved or expired privacy, endpoint error, skipped completion, missing configuration, load failure, cancellation, preview/test, iOS, and every other surface suppress the slot and do not affect core use.

Before release, the owner must verify RSYA account and ad-unit ownership, direct-only demand configuration, and a real load on a physical Android device using the signed release candidate. Evidence must include the redacted RSYA screen, variant/app version, SDK version, device/OS, endpoint route, consent path, and a network trace showing exactly one request only on the permitted surface and none on forbidden surfaces. Confirm the decline behavior with the exact SDK version; if it cannot be demonstrated and approved, keep ads disabled after decline.

Official references: [Yandex Android quick start and automatic initialization](https://ads.yandex.com/helpcenter/en/dev/android/quick-start), [Yandex consent guidance](https://ads.yandex.com/helpcenter/en/dev/android/gdpr), [Yandex Advertising ID guidance](https://ads.yandex.com/helpcenter/en/dev/android/ad-id), and [Yandex adaptive inline banner callbacks](https://ads.yandex.com/helpcenter/en/dev/android/adaptive-inline-banner).

## Policy and Play Console

The source privacy policy is `docs/bagcue-policy.html`; `docs/PRIVACY-POLICY.md` is its parity/release record and `docs/PLAY-DATA-SAFETY.md` is a conservative Console worksheet. A public HTTPS policy URL has not been supplied. Do not invent one. Publication remains blocked until the owner and counsel approve the text, host the exact approved artifact, configure the in-app and store links, complete Play Data Safety from the final dependency/artifact inventory, and compare the submitted answers with the hosted policy.
