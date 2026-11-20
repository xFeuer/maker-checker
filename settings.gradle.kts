@file:Suppress("UnstableApiUsage")

pluginManagement {
    includeBuild("build-logic")
    repositories {
        mavenCentral()
        gradlePluginPortal()
        // Fallback. Maven Central sometimes answers "not found" for files it has. Gradle asks this mirror last.
        maven("https://maven-central.storage-download.googleapis.com/maven2/")
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenCentral()
        // Fallback, as in pluginManagement above.
        maven("https://maven-central.storage-download.googleapis.com/maven2/")
    }
}

rootProject.name = "maker-checker"

include("automation:allure")
include("automation:api")
include("automation:core")
include("automation:suites")
include("platform:account-service")
