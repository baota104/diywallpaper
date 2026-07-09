pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        val artifactoryUrl = providers.gradleProperty("artifactory_contextUrl").orNull

        google()
        mavenCentral()
        maven { url = java.net.URI("https://jitpack.io") }
        maven { url = java.net.URI("https://android-sdk.is.com") }
        maven { url = java.net.URI("https://artifact.bytedance.com/repository/pangle") }
        maven {
            url = java.net.URI("https://artifacts.applovin.com/android")
            content {
                includeGroupByRegex("com\\.applovin.*")
            }
        }
        maven { url = java.net.URI("https://dl-maven-android.mintegral.com/repository/mbridge_android_sdk_oversea") }
        if (!artifactoryUrl.isNullOrBlank()) {
            maven {
                isAllowInsecureProtocol = true
                url = java.net.URI(artifactoryUrl)
                credentials {
                    username = providers.gradleProperty("artifactory_user").orNull ?: ""
                    password = providers.gradleProperty("artifactory_password").orNull ?: ""
                }
            }
        }
    }
}

rootProject.name = "DIYWallPaper"
include(":app")
 