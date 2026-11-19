@file:Suppress("UnstableApiUsage")

plugins {
    id("makerchecker.automation-conventions")
}

testing {
    suites {
        register<JvmTestSuite>("systemTest") {
            useJUnitJupiter(libs.versions.junit)

            dependencies {
                implementation(project(":automation:api"))
                implementation(libs.assertj.core)
            }

            targets.all {
                testTask.configure {
                    description = "Runs the system tests against a running stack."
                    // Gradle cannot see the stack, so an earlier result says nothing about this run.
                    doNotTrackState("The system under test runs outside the build.")
                }
            }
        }
    }
}

// Every build compiles the system tests, so they cannot rot. Running them takes the stack.
tasks.check {
    dependsOn("systemTestClasses")
}
