# Quality

## Non-functional requirements

Core data and checklist interactions are deterministic, offline-first, and responsive from local storage. Network, Analytics, privacy endpoint, and Ads work is asynchronous and never blocks packing. All user-visible failures are localized, preserve input, and offer a meaningful recovery action.

## Test matrix

The machine-readable obligation inventory is `app-spec.json`. Required coverage includes:

- pure domain tests for name normalization, stable-ID merging, maximum quantity, bag conflicts, LocalDate uniqueness, session recomposition, empty-session rejection, completion rules, time-zone behavior, and analytics buckets;
- Manager/persistence integration tests for every required CRUD operation, snapshots, seed overrides, cascades, transactions, upgrades from supported shipped schemas when applicable, restart, and failure-cause preservation;
- MVIKotlin Store tests for startup actions, async success/failure, retained state, retries, cancellation, labels, and no direct persistence/network access;
- Decompose public-component tests for navigation, outputs, back handling, child stacks/slots, lifecycle/resume, permission denial, and process restoration;
- localization contract tests for equal EN/RU keys, system-locale changes, English fallback, seed resource-key behavior, user overrides, native notification resources, and hardcoded-string scans;
- controlled privacy endpoint tests for protected, non-protected, unknown, malformed, redirect, timeout, 429/503, expiry, and policy change;
- Analytics adapter tests for default-off, enable/disable/reset, exact event/parameter allowlist, no user text/IDs/dates, disabled advertising identifiers/signals/personalization/linkage;
- ad lifecycle tests for privacy-before-init, setUserConsent before every permitted init, decline/error suppression, full-result-only placement, once-per-completion, missing Android ID, null iOS config, and no real calls in previews/tests;
- notification platform tests for denial, enablement, cancel/update, reboot/resume, time/time-zone changes, past-time suppression, and deduplication;
- repository Detekt, filtered Kover, resources, debug builds, Android checks, macOS iOS Test and link, CI/release workflow validation.

Detekt uses the newest official compatible release and a complete generated current-schema configuration; warnings/errors fail the gate. Kover measures only declared domain and production Decompose scope, records a first filtered baseline, then commits the baseline-derived minimum without automatic regeneration.

## Production integration and execution boundaries

For AC-001 through AC-051, every UI-facing behavior must be reachable through the real BagCue production root on its applicable target. Production wiring, navigation and persistence evidence are acceptance prerequisites; isolated components and previews alone do not prove an integrated application. QG-001 covers integration tests and QG-002/QG-003 cover platform entry points, launch and restoration. The first capability package must expose a real catalog save/read/restart path from the production root; later packages connect their flows to that root before moving to the next package.

Plan related ACs together by capability and shared contracts, while retaining each AC's declared verification surfaces and dependency order. Compile changed modules and run meaningful affected tests before final handoff. Run combined relevant suites at stable package boundaries; reserve the complete matrix for final integration. Do not repeat unaffected checks solely because another assignment finished. Record integrated flows separately from verified AC counts.

The current skills own assignment and receipt schemas: assignment-local handoffs and registered-input targeted checks are mandatory during implementation. Global integration/audit checks and the post-audit final receipt retain full-workspace evidence. A deferred required check keeps its obligation unverified. Final acceptance still requires every applicable declared surface, a fresh independent audit, and the covering post-audit final receipt.

## Preview and golden matrix

Every SCREEN file declares its state matrix. Every applicable primary screen state renders in light and dark on compact phone. Risk variants include RU at 200%, long user text, empty/error/permission states, expanded width, unresolved bag conflict, skipped completion, consent required, Analytics off/on, Ads eligible/hidden/failure, and IME-visible editors.

Previews use production resources and component-module `*ComponentPreview` implementations, never Compose-local substitutes. ComposablePreviewScanner generates stable Paparazzi IDs. Build and scan a small production preview early to validate the harness. Compile previews during UI edits. At a stable screen/capability boundary, record the affected baseline, inspect every new or changed PNG, then require clean verify for that set. Select additional locale/font/device stress combinations by risk and pairwise coverage; the listed risk cases remain covered without requiring a full Cartesian product. Deferred goldens keep the corresponding AC implemented-unverified. Final integration verifies the complete required golden set.

## Visual quality and Lazyweb review

Run the complete review once on stable approved primary-screen PNGs in numeric screen order using exactly one Lazyweb report at a time. Repeat affected screens only after material visual changes or unresolved findings, not after each AC or handoff. Do not batch screens. Record coverage and resolve/waive every blocking finding. Independently audit the full golden set for fonts/glyphs, wrapping/clipping, hierarchy, light/dark contrast, Material components/states, icon correctness, touch targets, insets, focus, semantics, EN/RU expansion, 200% text, adaptive panes, and reduced motion.

## Android platform gate

QG-002 requires Android compilation/tests, application ID `com.sedsoftware.bagcue`, a committed `androidApp/google-services.json` registered for that exact application ID, min/target SDK decisions from current project standards, local notifications without exact alarms unless later justified, Firebase Analytics default-off configuration, disabled Advertising ID collection, no Google Ads linkage/signals/personalization, Yandex privacy-before-init, release-only real ad ID injection, manifest permission audit, install smoke, process recreation, and offline behavior. The Firebase service file is repository configuration and has no GitHub-secret counterpart.

## iOS platform gate

QG-003 requires a macOS CI job named `Test and link iOS` (or an explicitly mapped stable equivalent) that compiles/tests the shared target, links the iOS application with bundle ID `com.sedsoftware.bagcue.iosApp` and the committed `iosApp/iosApp/GoogleService-Info.plist` registered for that exact bundle ID, resolves CocoaPods/Xcode dependencies including Yandex SDK, verifies localized native resources, and proves nullable advertising configuration prevents SDK initialization and requests. The Firebase service file is repository configuration and has no GitHub-secret counterpart. CI/release contains no production, invented, or demo iOS ad unit ID. Windows does not claim iOS verification.

## Privacy, ads, and analytics

QG-007 and QG-012 require parity among code, `docs/bagcue-policy.html`, endpoint contract, manifests, SDK configuration, Analytics console, and Google Play Data safety.

- Endpoint cache maximum 72 hours; no country/IP stored in-app; fail closed for ads.
- Consent form appears only when required, includes allow/decline/policy, and remains separate from Analytics.
- Decline, unknown, error, expiry, and policy mismatch prevent Yandex init and requests.
- Before every eligible init call `YandexAds.setUserConsent(...)` with current state.
- Android removes/disables advertising identifier use; exact Yandex SDK behavior is verified for the selected published version.
- Analytics is off by default. The Settings toggle enables/disables collection and disable resets local Analytics data/app-instance ID.
- Only six approved custom events and categorical allowlisted parameters are permitted; automatic collection, app-instance identifier, retention, and deletion limitations are disclosed.
- Firebase property retention is two months; Google Signals, ads personalization, User-ID, user properties, Ads linking, and data sharing for advertising are disabled.
- Logs contain no item/template/session names, tokens, identifiers, consent content, or raw endpoint IP/forwarding headers.

## Asset delivery

QG-006 requires exact ASSET inventory/variant coverage, valid portable vectors, hashes, provenance/rights, generation record for ASSET-001, production `Res.drawable` usages, launcher derivatives, resource compilation, and visual evidence at actual sizes in both themes. `docs/assets/asset-manifest.json` is mutable delivery evidence outside AppSpec. Placeholders, generated prompts, unused files, or unreviewed silhouettes do not pass.

## macOS continuation guide

QG-010 requires a delivery-time Markdown guide under `docs/` for the user's MacBook/agent. It must be based on the implemented repository and include prerequisites, exact checkout/setup commands, Xcode/CocoaPods/signing configuration, bundle ID, selected schemes, `Test and link iOS` parity commands, expected output, nullable Yandex configuration, optional manual official-demo diagnostic isolation, remaining external actions, troubleshooting evidence, and an explicit list of what Windows/CI did and did not verify. Do not create a speculative success guide before implementation.

## External publication gates

- QG-005: Google Play developer account, signing, EN/RU listing, hosted policy URL, Data safety answers, content rating, 18+ positioning without an age gate, country availability, credentials, and actual publication result.
- QG-008: live endpoint HTTPS/schema/expiry, protected/non-protected/unknown controlled routes, trusted-proxy spoof resistance, GeoIP freshness, aggregate-only observability, raw-IP/header redaction, policy owner, and legal review of the protected set/copy.
- QG-009: real user-supplied Android Yandex ad unit ID for monetized public build, safe secret/config injection, debug/test separation, and controlled physical-device diagnostics.
- QG-011: Firebase project/app ownership; Android `com.sedsoftware.bagcue` and Apple `com.sedsoftware.bagcue.iosApp` registration; direct commit of the matching files at `androidApp/google-services.json` and `iosApp/iosApp/GoogleService-Info.plist` with no GitHub secrets for either file; packaged-file verification; two-month retention; disabled Ads features/linkage/data sharing; user deletion/control capabilities; and Data safety disclosure.

External claims remain blocked until verified; committed CI/configuration is not proof of external publication readiness.

## Release acceptance

Release requires all active ACs verified on every declared surface, all required gates closed, active conditional gates evaluated, current AppSpec/workspace fingerprints, zero failing latest receipts, approved goldens, completed ordered Lazyweb reviews, complete asset manifest, privacy parity, Android release checks, macOS iOS build/link evidence, the macOS continuation guide, adapted CI workflows (`AnalysisAndTest.yml`, `MeasureTestCoverage.yml`, `CodeCoverageBadge.yml`, `CreateAndroidRelease.yml`, `PublishAndroidRelease.yml`), and `docs/CI-RELEASE-SETUP.md`.

Implementation-complete and release-ready are separate verdicts. Google Play publication, Android production ad ID, Firebase console, signing, and other external state cannot be claimed ready without direct evidence.

