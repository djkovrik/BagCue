import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlin.cocoapods)
}

kotlin {
    cocoapods {
        version = "1.0.0"
        summary = "BagCue platform services with explicit Firebase Analytics and Yandex link contracts"
        homepage = "https://github.com/sedsoftware/BagCue"
        ios.deploymentTarget = "16.2"
        pod("FirebaseAnalytics") {
            version = libs.versions.firebaseApple.get()
            moduleName = "FirebaseAnalytics"
        }
        pod("YandexMobileAds") {
            version = libs.versions.yandexMobileAdsApple.get()
            moduleName = "YandexMobileAds"
        }
        framework {
            baseName = "bagcuePlatform"
            isStatic = true
        }
    }

    android {
        namespace = "com.sedsoftware.bagcue.platform"
        compileSdk = 37
        minSdk = 23
        androidResources.enable = true
        withHostTest {}
        compilerOptions { jvmTarget = JvmTarget.JVM_17 }
    }

    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(project(":shared:domain"))
            api(libs.kotlinx.datetime)
            implementation(libs.kotlinx.coroutines.core)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
