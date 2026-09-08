rootProject.name = "BagCue"

pluginManagement {
    repositories {
        google {
            content { 
              	includeGroupByRegex("com\\.android.*")
              	includeGroupByRegex("com\\.google.*")
              	includeGroupByRegex("androidx.*")
              	includeGroupByRegex("android.*")
            }
        }
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            content { 
              	includeGroupByRegex("com\\.android.*")
              	includeGroupByRegex("com\\.google.*")
              	includeGroupByRegex("androidx.*")
              	includeGroupByRegex("android.*")
            }
        }
        mavenCentral()
    }
}
include(":shared:compose")
include(":shared:compose:visual-test")
include(":shared:domain")
include(":shared:data")
include(":shared:platform")
include(":shared:network")
include(":shared:component:catalog")
include(":shared:component:templates")
include(":shared:component:session")
include(":shared:component:history")
include(":shared:component:settings")
include(":shared:root")
include(":androidApp")

