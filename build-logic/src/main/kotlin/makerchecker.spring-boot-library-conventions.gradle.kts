import org.springframework.boot.gradle.plugin.SpringBootPlugin

plugins {
    id("makerchecker.java-conventions")
    `java-library`
}

dependencies {
    // The Spring Boot BOM owns the dependency versions, as it does for a service.
    api(platform(SpringBootPlugin.BOM_COORDINATES))

    // Gradle starts the tests through the launcher of JUnit. A service gets it from the Spring Boot plugin, which a
    // library does not apply.
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
