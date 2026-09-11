# BagCue

[![Analysis and test](https://github.com/djkovrik/BagCue/actions/workflows/AnalysisAndTest.yml/badge.svg)](https://github.com/djkovrik/BagCue/actions/workflows/AnalysisAndTest.yml)
[![Coverage](https://img.shields.io/endpoint?url=https://gist.githubusercontent.com/djkovrik/4a13b25ab0e1c1dc95983e724cdb221b/raw/bagcue-coverage-badge.json)](https://github.com/djkovrik/BagCue/actions/workflows/CodeCoverageBadge.yml)
[![Last Commit](https://img.shields.io/github/last-commit/djkovrik/BagCue/master.svg)](https://github.com/djkovrik/BagCue/commits/master)
![License](https://img.shields.io/badge/license-GPLv3-green.svg?style=flat)

BagCue is an offline-first daily packing assistant for Android and iOS. It combines reusable kits such as Office and Pool into one checklist for a chosen local date, groups items by bag, highlights items that need to move between bags, and preserves packing progress between evening preparation and the next check.

## Product scope

- reusable item catalog and packing templates;
- one persisted packing session per local calendar date;
- deterministic merging by stable item ID, using the maximum quantity and explicit resolution of conflicting bag labels;
- packed and skipped completion states, history, and repeat-from-history;
- optional local reminders;
- English and Russian resources, light/dark themes, adaptive layouts, and accessibility coverage;
- opt-in Firebase Analytics and privacy-gated Yandex advertising after a fully successful result only.

Accounts, cloud sync, travel planning, automatic schedules, permanent bag inventory, import/export, AI, OCR, and geolocation are outside the MVP.

The approved product contract is [spec/app-spec/app-spec.json](spec/app-spec/app-spec.json), revision `2026-09-10-qg008-production-ready`. Human-readable product, domain, data, design, quality, flow, and screen requirements live beside it in [spec/app-spec](spec/app-spec/).

## Technology

- Kotlin Multiplatform and Compose Multiplatform;
- Material 3 UI;
- Decompose navigation and component lifecycle;
- MVIKotlin stores;
- SQLDelight for local data and Multiplatform Settings for preferences;
- Ktor for the privacy-region boundary;
- Paparazzi plus ComposablePreviewScanner for visual regression tests;
- Detekt and Kover quality gates.

Android requires API 23 or newer and currently targets API 37. The iOS deployment target is 16.2.

## Repository structure

| Path                          | Responsibility                                                         |
|-------------------------------|------------------------------------------------------------------------|
| `androidApp/`                 | Android entry point, manifest, Firebase and release wiring             |
| `iosApp/`                     | Swift/Xcode entry point, CocoaPods workspace and native configuration  |
| `shared/domain/`              | Pure models, invariants, algorithms, and repository contracts          |
| `shared/data/`                | SQLDelight persistence and settings implementations                    |
| `shared/network/`             | Ktor privacy-region transport                                          |
| `shared/platform/`            | Android/iOS notifications, analytics, ads, and other platform services |
| `shared/component/*/`         | Decompose components and MVIKotlin stores by feature                   |
| `shared/root/`                | Production component graph and navigation                              |
| `shared/compose/`             | Shared Compose UI, resources, theme, and previews                      |
| `shared/compose/visual-test/` | Paparazzi harness and committed golden images                          |
| `spec/app-spec/`              | Approved requirements and acceptance contract                          |
| `docs/`                       | CI, release, privacy, design, and platform hand-off documentation      |

## Prerequisites

- JDK 17 or newer;
- Android Studio with an Android SDK compatible with `compileSdk`/`targetSdk` 37;
- for iOS: macOS, Xcode, and CocoaPods.

Use the checked-in Gradle wrapper. On Windows, replace `./gradlew` with `.\gradlew.bat`.

## Run and build

### Android

Open the project in Android Studio and run the `androidApp` configuration, or build a debug APK:

```shell
./gradlew :androidApp:assembleDebug
```

The APK is written to `androidApp/build/outputs/apk/debug/androidApp-debug.apk`.

### iOS

On macOS, resolve CocoaPods dependencies, open the workspace, and run the `iosApp` scheme:

```shell
pod install --project-directory=iosApp
open iosApp/iosApp.xcworkspace
```

See [iOS release setup](docs/IOS-RELEASE-SETUP.md) for repository-specific build, linkage, signing, and CI-parity guidance. A Windows build does not verify iOS compilation or linkage.

## Verification

Run the focused tests for the modules you change. Useful repository-level checks are:

```shell
./gradlew detekt
./gradlew koverVerify koverXmlReport
./gradlew :shared:domain:allTests
./gradlew :shared:data:testAndroidHostTest
./gradlew :shared:compose:visual-test:verifyPaparazziDebug
./gradlew :androidApp:assembleDebug
python .github/workflows/scripts/validate_repository.py
```

The complete Linux/Android matrix and the macOS `Test and link iOS` job are defined in [.github/workflows/AnalysisAndTest.yml](.github/workflows/AnalysisAndTest.yml). Kover covers the declared domain and production component scope; its baseline-derived minimum is configured by `bagcue.kover.minimum` in `gradle.properties`.

## Privacy and release configuration

Core packing remains usable offline and must not depend on Analytics, Ads, or the privacy endpoint. Analytics is off by default. Advertising is fail-closed and may initialize only after the required privacy decision and consent handling.

The Firebase service files are committed repository configuration for the exact application identifiers; signing keys, passwords, service-account credentials, and other secrets must not be committed. Before a release, follow:

- [CI and Android release setup](docs/CI-RELEASE-SETUP.md)
- [Analytics, privacy, and advertising setup](docs/APA-EXTERNAL-SETUP.md)
- [Google Play listing](docs/GOOGLE-PLAY-LISTING.md)
- [Privacy policy](docs/PRIVACY-POLICY.md) and [Data Safety answers](docs/PLAY-DATA-SAFETY.md)

The filtered coverage badge is published as `bagcue-coverage-badge.json` in the project-specific gist configured by `COVERAGE_GIST_ID`.

## Contributing

Read [AGENTS.md](AGENTS.md) before changing the project. Keep implementation, tests, localized resources, previews/goldens, and the AppSpec acceptance surfaces aligned. Do not claim platform or external release gates as verified without their required evidence.

## License

BagCue is licensed under [GNU GPL v3](LICENSE).
