# BagCue

[![Analysis and test](https://github.com/sedsoftware/BagCue/actions/workflows/AnalysisAndTest.yml/badge.svg)](https://github.com/sedsoftware/BagCue/actions/workflows/AnalysisAndTest.yml)
[![Coverage badge update](https://github.com/sedsoftware/BagCue/actions/workflows/CodeCoverageBadge.yml/badge.svg)](https://github.com/sedsoftware/BagCue/actions/workflows/CodeCoverageBadge.yml)

The filtered Kover percentage badge is published as `bagcue-coverage.json` in the project-specific gist configured by `COVERAGE_GIST_ID`; see [CI and release setup](docs/CI-RELEASE-SETUP.md).

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
