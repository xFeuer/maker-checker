plugins {
    // Spotless resolves the Java formatter in this project. With the JVM ecosystem, Gradle reads a BOM among the
    // formatter's dependencies as a platform, as it does in the Java projects. Without it, Gradle looks for a jar
    // of that BOM.
    `jvm-ecosystem`
    id("makerchecker.formatting-conventions")
}

spotless {
    java {
        target("build-logic/src/**/*.java")
        palantirJavaFormat()
    }
    kotlinGradle {
        target(
            "*.gradle.kts",
            "build-logic/*.gradle.kts",
            "build-logic/src/**/*.gradle.kts",
        )
    }
    format("misc") {
        target(
            "*.md",
            "*.properties",
            ".editorconfig",
            ".gitattributes",
            ".gitignore",
            "LICENSE",
            ".github/**/*.md",
            ".github/**/*.yml",
            "automation/*.md",
            "contracts/*.md",
            "contracts/**/*.yaml",
            "deploy/*.md",
            "deploy/*.yaml",
            "docs/**/*.md",
            "gradle/*.toml",
            "platform/*.md",
        )
    }
}

// build-logic is an included build. Its checks and tests run with this build only because of this dependency.
tasks.check {
    dependsOn(gradle.includedBuild("build-logic").task(":check"))
}

// The one command for the system tests: it builds the images, starts the stack, runs the tests and stops the stack.
val compose = listOf("docker", "compose", "--file", "deploy/compose.yaml")

val stopStack =
    tasks.register<Exec>("stopStack") {
        description = "Stops the stack and removes its containers."
        commandLine(compose + "down")
        // The system tests need the stack until they end.
        mustRunAfter(":automation:suites:systemTest")
    }

val startStack =
    tasks.register<Exec>("startStack") {
        description = "Builds the images and starts the stack. The build stops the stack again before it ends."
        // The image of a service packages the jar that Gradle builds.
        dependsOn(":platform:account-service:bootJar")
        // The command returns when every service is healthy.
        commandLine(compose + listOf("up", "--build", "--wait"))
        // A finalizer runs even when this task or the tests fail, so no run leaves the stack behind.
        finalizedBy(stopStack)
    }

tasks.register("systemTest") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Builds the images, starts the stack, runs the system tests and stops the stack."
    dependsOn(startStack, ":automation:suites:systemTest")
}
