@file:Suppress("UnstableApiUsage")

import io.github.xfeuer.makerchecker.buildlogic.AllureReport
import io.github.xfeuer.makerchecker.buildlogic.AllureResults

plugins {
    id("makerchecker.java-conventions")
}

// ADR 0007: the test tasks of a project that applies this plugin write Allure results, and allureReport turns them
// into one report.

val libs = the<VersionCatalogsExtension>().named("libs")

// Allure Report 2 is a Java program that ships as one zip. Gradle resolves it like any other dependency.
val allureCommandline = configurations.dependencyScope("allureCommandline")
val allureCommandlineZip =
    configurations.resolvable("allureCommandlineZip") {
        extendsFrom(allureCommandline.get())
        // The zip holds every jar that the program needs.
        isTransitive = false
    }

dependencies {
    allureCommandline(variantOf(libs.findLibrary("allure-commandline").get()) { artifactType("zip") })
}

val unpackAllure =
    tasks.register<Sync>("unpackAllure") {
        description = "Unpacks Allure Report."
        from(zipTree(allureCommandlineZip.map { it.singleFile }))
        // The zip has one top directory, named after the version. Without it the paths are the same in every version.
        eachFile { relativePath = RelativePath(true, *relativePath.segments.drop(1).toTypedArray()) }
        includeEmptyDirs = false
        into(layout.buildDirectory.dir("allure/commandline"))
    }

// One directory of results for each test task, the way Gradle keeps its own test results.
val allureResults = layout.buildDirectory.dir("allure-results")

interface Services {
    @get:Inject
    val files: FileSystemOperations
}

tasks.withType<Test>().configureEach {
    val results = objects.newInstance<AllureResults>()
    results.directory = allureResults.map { it.dir(name) }
    jvmArgumentProviders.add(results)
    // Allure's extension for JUnit Jupiter reports the fixtures of a test. JUnit registers it only when this is on.
    systemProperty("junit.jupiter.extensions.autodetection.enabled", "true")
    // A report shows one run, so each run starts without the results of the last one.
    val services = objects.newInstance<Services>()
    doFirst {
        services.files.delete { delete(results.directory) }
    }
}

val allureReport =
    tasks.register<AllureReport>("allureReport") {
        group = LifecycleBasePlugin.VERIFICATION_GROUP
        description = "Generates the Allure report from the results that the test tasks wrote."
        allureHome = layout.dir(unpackAllure.map { it.destinationDir })
        resultsDirectory = allureResults
        reportName = rootProject.name
        reportDirectory = layout.buildDirectory.dir("reports/allure")
        launcher = javaToolchains.launcherFor(java.toolchain)
        // The report follows the test tasks that run in the same build. It never starts one.
        mustRunAfter(tasks.withType<Test>())
    }

// ADR 0015: the root project puts the report into the site that GitHub Pages serves. It asks this project for it
// by this name, so it knows no task and no path of this project.
configurations.consumable("testReport") {
    outgoing.artifact(allureReport.flatMap { it.reportDirectory })
}
