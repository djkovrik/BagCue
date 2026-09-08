import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.kover)
}

kotlin {
    android {
        namespace = "com.sedsoftware.bagcue.root"
        compileSdk = 37
        minSdk = 23
        withHostTest {}
        compilerOptions { jvmTarget = JvmTarget.JVM_17 }
    }

    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(project(":shared:domain"))
            api(project(":shared:platform"))
            api(project(":shared:component:catalog"))
            api(project(":shared:component:templates"))
            api(project(":shared:component:session"))
            api(project(":shared:component:history"))
            api(project(":shared:component:settings"))
            api(libs.decompose)
            api(libs.kotlinx.datetime)
            implementation(libs.essenty.lifecycle.coroutines)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.mvikotlin)
            implementation(libs.mvikotlin.main)
            implementation(libs.mvikotlin.logging)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.mvikotlin.main)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
