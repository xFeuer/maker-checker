@file:Suppress("UnstableApiUsage")

plugins {
    id("makerchecker.automation-conventions")
    id("makerchecker.allure-report-conventions")
}

testing {
    suites {
        register<JvmTestSuite>("systemTest") {
            useJUnitJupiter(libs.versions.junit)

            dependencies {
                implementation(project(":automation:api"))
                implementation(libs.assertj.core)

                // The report: Allure's adapter writes a result for every test, and our listener attaches the
                // exchanges to it.
                runtimeOnly(platform(libs.allure.bom))
                runtimeOnly(libs.allure.jupiter)
                runtimeOnly(project(":automation:allure"))
            }

            targets.all {
                testTask.configure {
                    description = "Runs the system tests against a running stack."
                    // Gradle cannot see the stack, so an earlier result says nothing about this run.
                    doNotTrackState("The system under test runs outside the build.")
                    // When the root build starts the stack for these tests, they wait until it is up. This sets
                    // an order, not a dependency: the tests still run alone against a stack that is already up.
                    mustRunAfter(":startStack")
                    // Every run ends with the report, also when a test fails.
                    finalizedBy("allureReport")
                }
            }
        }
    }
}

// Every build compiles the system tests, so they cannot rot. Running them takes the stack.
tasks.check {
    dependsOn("systemTestClasses")
}
