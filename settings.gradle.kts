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
        // ADR 0009: vacuum, the linter of the contracts, ships as archives on its GitHub releases, one for each
        // platform. Gradle asks this repository for that one module and for nothing else, and it asks no other
        // repository for it.
        exclusiveContent {
            forRepository {
                ivy {
                    name = "vacuumReleases"
                    url = uri("https://github.com/daveshanley/vacuum/releases/download/")
                    // For example v0.43.0/vacuum_0.43.0_linux_x86_64.tar.gz.
                    patternLayout { artifact("v[revision]/[module]_[revision]_[classifier].[ext]") }
                    // A release has no module descriptor. The archive is all that Gradle can ask for.
                    metadataSources { artifact() }
                }
            }
            filter { includeModule("daveshanley", "vacuum") }
        }
    }
}

rootProject.name = "maker-checker"

include("automation:allure")
include("automation:api")
include("automation:core")
include("automation:suites")
include("chassis")
include("contracts")
include("platform:account-service")
