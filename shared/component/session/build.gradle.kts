import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kover)
}

kotlin {
    android {
        namespace = "com.sedsoftware.bagcue.component.session"
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
            api(libs.decompose)
            api(libs.kotlinx.datetime)
            implementation(libs.essenty.lifecycle.coroutines)
            implementation(libs.mvikotlin)
            implementation(libs.mvikotlin.main)
            implementation(libs.mvikotlin.logging)
            implementation(libs.mvikotlin.extensions.coroutines)
            implementation(libs.kotlinx.coroutines.core)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.mvikotlin.main)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
