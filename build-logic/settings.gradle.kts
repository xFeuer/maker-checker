@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        // Fallback. Maven Central sometimes answers "not found" for files it has. Gradle asks this mirror last.
        maven("https://maven-central.storage-download.googleapis.com/maven2/")
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        // Fallback, as in pluginManagement above.
        maven("https://maven-central.storage-download.googleapis.com/maven2/")
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"
