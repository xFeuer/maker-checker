import org.springframework.boot.gradle.plugin.SpringBootPlugin

plugins {
    id("makerchecker.java-conventions")
    id("org.springframework.boot")
}

dependencies {
    // The Spring Boot BOM owns the dependency versions, so a service declares its dependencies without one.
    implementation(platform(SpringBootPlugin.BOM_COORDINATES))
}

springBoot {
    buildInfo {
        // The build time changes on every run. It would defeat up-to-date checks and the build cache.
        excludes = setOf("time")
    }
}

// A service ships as one executable jar, so the plain jar is not built.
tasks.jar {
    enabled = false
}
