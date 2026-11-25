import java.io.ByteArrayOutputStream
import java.nio.file.Files

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

// The one command for the system tests: it builds the images, starts the stack, runs the tests, saves the logs of the
// stack and stops it.
val compose = listOf("docker", "compose", "--file", "deploy/compose.yaml")

val saveStackLogs =
    tasks.register<Exec>("saveStackLogs") {
        description = "Saves the logs of the stack to build/stack-logs/stack.log."
        // The command prints what every container wrote, to its standard output and to its standard error.
        commandLine(compose + listOf("logs", "--no-color"))
        val logFile = layout.buildDirectory.file("stack-logs/stack.log")
        outputs.file(logFile)
        // Gradle cannot see the stack, so the logs of an earlier run say nothing about this one.
        doNotTrackState("The stack runs outside the build.")
        // The logs cover the system tests.
        mustRunAfter(":automation:suites:systemTest")
        doFirst {
            // The file never holds the logs of an earlier run, not even when the command fails.
            Files.deleteIfExists(logFile.get().asFile.toPath())
            // An Exec task writes to a stream. The logs of a test run fit in memory.
            standardOutput = ByteArrayOutputStream()
        }
        doLast {
            logFile.get().asFile.writeBytes((standardOutput as ByteArrayOutputStream).toByteArray())
        }
    }

val stopStack =
    tasks.register<Exec>("stopStack") {
        description = "Stops the stack and removes its containers."
        commandLine(compose + "down")
        // The system tests need the stack until they end, and its logs go with the containers.
        mustRunAfter(":automation:suites:systemTest", saveStackLogs)
    }

val startStack =
    tasks.register<Exec>("startStack") {
        description = "Builds the images and starts the stack. The build saves its logs and stops it before it ends."
        // The image of a service packages the jar that Gradle builds.
        dependsOn(":platform:account-service:bootJar")
        // The command returns when every service is healthy.
        commandLine(compose + listOf("up", "--build", "--wait"))
        // Finalizers run even when this task or the tests fail, so no run loses the logs or leaves the stack behind.
        finalizedBy(saveStackLogs, stopStack)
    }

tasks.register("systemTest") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Builds the images, starts the stack, runs the system tests, saves the logs and stops the stack."
    dependsOn(startStack, ":automation:suites:systemTest")
}
