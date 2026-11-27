@file:Suppress("UnstableApiUsage")

import io.github.xfeuer.makerchecker.buildlogic.DatabaseImage
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

// ADR 0019: a service carries its specification, as the bundles that the contracts project makes of it. The
// directory of the specification has the name of the service, so contracts/account-service/openapi.yaml becomes
// contracts/openapi.json in the jar of account-service.
val specificationBundles = configurations.dependencyScope("specificationBundles")
val specificationBundleFiles =
    configurations.resolvable("specificationBundleFiles") {
        extendsFrom(specificationBundles.get())
    }

dependencies {
    specificationBundles(project(mapOf("path" to ":contracts", "configuration" to "specificationBundles")))
}

tasks.processResources {
    val service = project.name
    from(specificationBundleFiles) {
        include("$service/*.json")
        eachFile { relativePath = RelativePath(true, "contracts", name) }
        includeEmptyDirs = false
    }
}

// ADR 0016: the stack and the component tests run the same PostgreSQL, and the Compose file is the one place that
// names it.
val databaseImage =
    providers
        .fileContents(isolated.rootProject.projectDirectory.file("deploy/compose.yaml"))
        .asText
        .map { DatabaseImage.namedIn(it) }

// ADR 0017: a service has two levels of tests. A unit test in src/test needs the JVM alone. A component test in
// src/componentTest runs the whole service, with its database in a container.
testing {
    suites {
        register<JvmTestSuite>("componentTest") {
            dependencies {
                // The classes of the service. The plain jar is not built, so the suite cannot ask the project for it.
                implementation(sourceSets.main.get().output)
                implementation("org.springframework.boot:spring-boot-starter-webmvc-test")
                implementation("org.springframework.boot:spring-boot-testcontainers")
                implementation("org.testcontainers:testcontainers-postgresql")
            }

            targets.all {
                testTask.configure {
                    description = "Runs the component tests: the whole service, with its database in a container."
                    // The tests that need no container come first.
                    shouldRunAfter(tasks.test)
                    val database = objects.newInstance<DatabaseImage>()
                    database.image = databaseImage
                    jvmArgumentProviders.add(database)
                }
            }
        }
    }
}

// The suite gets what the service runs on, and with it the versions of the Spring Boot BOM.
configurations.named("componentTestImplementation") {
    extendsFrom(configurations.implementation.get())
}
configurations.named("componentTestRuntimeOnly") {
    extendsFrom(configurations.runtimeOnly.get())
}

// A component test replaces nothing inside the service, so its classpath has no mocking library.
listOf("componentTestCompileClasspath", "componentTestRuntimeClasspath").forEach { classpath ->
    configurations.named(classpath) {
        exclude(group = "org.mockito")
    }
}

tasks.check {
    dependsOn(testing.suites.named("componentTest"))
}
