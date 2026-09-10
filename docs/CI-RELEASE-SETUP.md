# BagCue CI and Android release setup

> The workflows are committed automation, not evidence that GitHub, Firebase, Google Play, Google Cloud, signing keys, or public hosting are configured. Leave every external item unchecked until it is verified in the named console.

## Workflow inventory and order

- `AnalysisAndTest.yml`: push/PR to `master` and manual runs. It runs strict Detekt, filtered Kover verification/XML, domain/component/persistence/platform host tests, SQLDelight migration verification, Compose resource generation, all Paparazzi goldens, repository invariants, Android debug assembly, and the macOS job named `Test and link iOS`.
- `MeasureTestCoverage.yml`: PRs to `master`. It enforces the committed baseline-derived overall threshold and 70% coverage for changed executable Kotlin lines. Only a same-repository PR gets one bot comment; fork code never receives a write-capable comment job.
- `CodeCoverageBadge.yml`: serialized `master`/manual measurement. It rejects non-numeric task output and updates `bagcue-coverage.json` in one dedicated gist.
- `CreateAndroidRelease.yml`: manual only. An authorized actor on `master` selects patch/minor/major, supplies EN/RU notes, creates one annotated tag and GitHub prerelease, then calls the publish workflow.
- `PublishAndroidRelease.yml`: SemVer tag, manual retry, or reusable call. It checks out the exact tag, builds signed/shrunk APK+AAB, verifies signatures/mapping/invariants, uploads evidence, and publishes only to Google Play Internal testing through protected environment `google-play-publishing`. It never promotes to Production.

Release version mapping is deterministic: `major * 1,000,000 + minor * 1,000 + patch`; major must be at most 2100 and minor/patch below 1000. A retry uses the same immutable tag and versionCode. Changed source requires a new tag.

The committed Kover floor is `72%`. It is the conservative integer floor of the real filtered `72.23%` line baseline measured on 2026-09-08 by `koverXmlReport` plus `printKoverLineCoverage` (receipt `RECEIPT-42f42111-8475-42ae-96f4-fffeb6eeb780.json`). CI verifies that floor and publishes the XML report; lowering it requires a reviewed configuration change, not an environment override.

## GitHub

- [ ] Confirm `master` is the default branch and protect it. Require `Repository quality`, `Test and link iOS`, and the coverage check; require current reviews and conversation resolution.
- [ ] Keep Actions default permission read-only. Permit pull-request writes only for the same-repository coverage-comment job and contents writes only for release preparation.
- [ ] Create protected environment `google-play-publishing`; add required reviewers and allow only protected SemVer tags. Keep this track-neutral name if releases later move through Closed/Open/Production.
- [ ] Set repository variable `RELEASE_ACTORS` to a comma-separated allowlist of GitHub logins authorized to create releases.
- [ ] Create a dedicated public gist containing `bagcue-coverage.json`. Set variable `COVERAGE_GIST_ID` to its ID and secret `GIST_SECRET` to a fine-grained token that can edit only that gist. After the real gist exists, construct the Shields endpoint from `https://img.shields.io/endpoint?url=https://gist.githubusercontent.com/raw/`, the actual `COVERAGE_GIST_ID`, and `/bagcue-coverage.json`, then add that resolved URL to the README.
- [ ] Configure Git LFS checkout for the committed Paparazzi PNG set and verify CI fetches real objects, not pointers.
- [ ] Add these secrets exactly: `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`, `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON`. Do not create GitHub secrets for the Firebase service configuration files; they are committed repository configuration.

## Android upload key

- [ ] Enable Play App Signing for `com.sedsoftware.bagcue` and create a dedicated upload key, never a debug key.
- [ ] Store two encrypted backups in separate controlled locations. Record alias, expiry, SHA-1 and SHA-256 certificate fingerprints in the private credential inventory.
- [ ] Base64-encode the complete keystore without printing it. Store that value as `ANDROID_KEYSTORE_BASE64`; store the three password/alias values in their exact GitHub secret names above.
- [ ] Verify a release-run APK with `apksigner verify --verbose` and the AAB with `jarsigner -verify`; compare its upload certificate to Play Console.

## Firebase

- [ ] Create/select the production Firebase project and register Android app `com.sedsoftware.bagcue` and Apple app `com.sedsoftware.bagcue.iosApp`.
- [ ] Enable Analytics only. Do not enable Crashlytics or another Firebase product unless AppSpec and privacy artifacts are revised first.
- [ ] Download the Android config for exactly `com.sedsoftware.bagcue`, place it at `androidApp/google-services.json`, and commit it directly. Do not base64-encode it or create a GitHub secret for it. Review the project/app identifiers before commit; never substitute a file from another Firebase project.
- [ ] Download the Apple config for exactly `com.sedsoftware.bagcue.iosApp`, place it at `iosApp/iosApp/GoogleService-Info.plist`, add it to the `iosApp` application target, and commit it directly. Do not create a GitHub secret for it; never invent or reuse another app's file.
- [ ] Set Analytics user/event retention to two months. Disable Google Signals, Ads linking, ads personalization, User-ID, user properties, and advertising data sharing.
- [ ] Verify collection is disabled before first opt-in, disabling takes effect immediately and resets local Analytics/app-instance data, and only the six allowlisted content-free events appear. Never send item/template/bag text, IDs, ad unit IDs, region payloads, or user-authored content.
- [ ] Firebase Crashlytics/mapping verification is **NOT APPLICABLE**: BagCue does not ship Crashlytics. R8 mapping remains a Google Play release artifact.

## Google Play Console

- [ ] Create BagCue with package `com.sedsoftware.bagcue`, default language English, store titles `BagCue: Daily Packing` (EN) and `BagCue: Что взять с собой` (RU), and production owner account.
- [ ] Complete localized EN/RU descriptions/screenshots, support contact, hosted privacy-policy URL, Ads declaration, App access, Content rating, Target audience (18+ positioning without an age gate), country availability, and every current policy form.
- [ ] Complete Data safety from the exact shipped Firebase Analytics, Yandex Mobile Ads, privacy-region endpoint, permissions, retention, local deletion, and no-AD_ID behavior. Recompare it to `docs/bagcue-policy.html` before every public release.
- [ ] Create Internal testing and tester list. Upload the first AAB manually if the Android Publisher API cannot see the new package before bootstrap.
- [ ] Confirm production ad block `R-M-19857241-1` belongs to this app in Yandex RSYA and run the separate physical-device diagnostic. CI never substitutes a demo ID.

## Google Cloud Console

- [ ] In the Cloud project linked to Play Console, enable Google Play Android Developer API.
- [ ] Create a dedicated release-automation service account and JSON key. Invite its email in Play Console and grant only app-scoped permission to view app data and manage Internal releases for BagCue.
- [ ] Store the complete JSON as `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON`. Do not commit, paste into logs, or reuse it for Firebase administration.

## First end-to-end dry run

- [ ] Make `AnalysisAndTest` and `MeasureTestCoverage` green on protected `master`, including macOS CocoaPods/Xcode evidence.
- [ ] Run `Create Android release` with intended bump and 20-500 character EN/RU notes. Verify annotated tag, GitHub prerelease, signed APK/AAB, mapping, optional native symbols, and exact version mapping.
- [ ] Confirm the publish job waited for `google-play-publishing` approval and created only an Internal-track release.
- [ ] Install/upgrade on a physical Android device and exercise offline catalog/templates/session/history, reminders, process recreation, EN/RU, 200% text, privacy opt-in/out/reset, and eligible/ineligible/no-fill ad paths.
- [ ] Compare the exact Internal build to approved goldens and policy/Data safety. Record anything not executed as `NOT RUN`, never `PASS`.

## Retry, promotion, and rollback

Retry an external failure by manually invoking `Publish Android release` with the same existing tag. If bytes or source change, create a new SemVer tag/versionCode. Promote the exact accepted AAB manually through Play Console; automation never promotes to Production. Roll back by halting rollout or publishing a higher-versionCode corrective build—never move or overwrite an existing tag.

## Current external blockers

- `QG-003`: **NOT RUN on Windows** until CocoaPods resolution, shared iOS tests/framework link, and workspace `xcodebuild` pass on the specified macOS/Xcode toolchain.
- `QG-005`: **NOT RUN** until the real Play account, signing, listings, policy forms, hosted policy, service account, Internal upload, and publication result are verified.
- Firebase/Yandex console ownership, the correctness of the committed Firebase service files, configured non-Firebase secrets, public policy hosting, device diagnostics, environment protection, reviewers, and branch protection remain external state; committed automation/configuration does not prove them.
