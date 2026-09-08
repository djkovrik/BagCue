import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.android.kmp.library)
}

kotlin {
    android {
        namespace = "com.sedsoftware.bagcue.compose"
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
            api(libs.compose.runtime)
            api(libs.compose.ui)
            api(libs.compose.foundation)
            api(libs.compose.resources)
            api(libs.compose.ui.tooling.preview)
            api(libs.compose.material3)
            implementation(project(":shared:root"))
            implementation(project(":shared:component:catalog"))
            implementation(project(":shared:component:templates"))
            implementation(project(":shared:component:session"))
            implementation(project(":shared:component:history"))
            implementation(project(":shared:component:settings"))
            implementation(libs.decompose.extensions.compose)
            implementation(libs.kermit)
            implementation(libs.kotlinx.coroutines.core)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.compose.ui.test)
            implementation(libs.kotlinx.coroutines.test)
        }

        androidMain.dependencies { implementation(libs.kotlinx.coroutines.android) }

        iosMain.dependencies {
            implementation(project(":shared:data"))
            implementation(project(":shared:platform"))
            implementation(project(":shared:network"))
            implementation(libs.mvikotlin)
            implementation(libs.mvikotlin.main)
        }
    }

    targets.withType<KotlinNativeTarget>()
        .matching { it.konanTarget.family.isAppleFamily }
        .configureEach {
            binaries.framework {
                baseName = "compose"
                isStatic = true
            }
        }
}

dependencies { androidRuntimeClasspath(libs.compose.ui.tooling) }
