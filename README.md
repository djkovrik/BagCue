# BagCue

[![Analysis and test](https://github.com/djkovrik/BagCue/actions/workflows/AnalysisAndTest.yml/badge.svg)](https://github.com/djkovrik/BagCue/actions/workflows/AnalysisAndTest.yml)
[![Coverage](https://img.shields.io/endpoint?url=https://gist.githubusercontent.com/djkovrik/4a13b25ab0e1c1dc95983e724cdb221b/raw/bagcue-coverage-badge.json)](https://github.com/djkovrik/BagCue/actions/workflows/CodeCoverageBadge.yml)
[![Last Commit](https://img.shields.io/github/last-commit/djkovrik/BagCue/master.svg)](https://github.com/djkovrik/BagCue/commits/master)
![License](https://img.shields.io/badge/license-GPLv3-green.svg?style=flat)

The filtered Kover percentage badge is published as `bagcue-coverage-badge.json` in the project-specific gist configured by `COVERAGE_GIST_ID`; see [CI and release setup](docs/CI-RELEASE-SETUP.md).

### Android
To run the application on android device/emulator:  
 - open project in Android Studio and run the imported Android run configuration

To build the application bundle:  
 - run `./gradlew :androidApp:assembleDebug`  
 - find `.apk` file in `androidApp/build/outputs/apk/debug/androidApp-debug.apk`  

### iOS
To run the application on iPhone device/simulator:  
 - run `pod install --project-directory=iosApp`, open `iosApp/iosApp.xcworkspace` in Xcode, and run the `iosApp` scheme
 - Or use [Kotlin Multiplatform Mobile plugin](https://plugins.jetbrains.com/plugin/14936-kotlin-multiplatform-mobile) for Android Studio  
