import io.github.xfeuer.makerchecker.buildlogic.ApiReference
import io.github.xfeuer.makerchecker.buildlogic.ProblemPages
import io.github.xfeuer.makerchecker.buildlogic.RulesetTest
import io.github.xfeuer.makerchecker.buildlogic.SpecificationBundles
import io.github.xfeuer.makerchecker.buildlogic.SpecificationLint
import io.github.xfeuer.makerchecker.buildlogic.VacuumPlatform
import io.github.xfeuer.makerchecker.buildlogic.VerifiedUnpack

plugins {
    `lifecycle-base`
    id("makerchecker.formatting-conventions")
}

// ADR 0009: vacuum lints every specification of a project that applies this plugin and tests the rules of its
// ruleset. check runs both.
//
// ADR 0015: vacuum also renders the specifications as an API reference, and the build writes a page for each of
// their problem types. assemble does both.
//
// ADR 0019: vacuum bundles each specification into one JSON document, for the application that implements it.

val libs = the<VersionCatalogsExtension>().named("libs")
val vacuumVersion = libs.findVersion("vacuum").get().requiredVersion

// vacuum is a program for one platform, and its release holds one archive for each. The platform is read only when
// a task needs the archive, so a machine without a release of vacuum can still build everything else.
val platform =
    providers.systemProperty("os.name").zip(providers.systemProperty("os.arch")) { name, architecture ->
        VacuumPlatform.of(name, architecture)
    }

// Gradle resolves the archive like any other dependency, from the repository that settings.gradle.kts names for it.
val vacuum = configurations.dependencyScope("vacuum")
val vacuumArchive =
    configurations.resolvable("vacuumArchive") {
        extendsFrom(vacuum.get())
    }

dependencies {
    vacuum(platform.map { "daveshanley:vacuum:$vacuumVersion:${it.classifier()}@tar.gz" })
}

val unpackVacuum =
    tasks.register<VerifiedUnpack>("unpackVacuum") {
        description = "Checks the archive of vacuum against its checksum and unpacks it."
        archive.from(vacuumArchive)
        // The name of the archive on the release page, which is its name in the checksums file.
        archiveName = platform.map { "vacuum_${vacuumVersion}_${it.classifier()}.tar.gz" }
        // The checksums file of the release, next to the version that it belongs to.
        checksums = rootProject.layout.projectDirectory.file("gradle/vacuum-checksums.txt")
        destination = layout.buildDirectory.dir("vacuum")
    }

// A project can have functions of its own for its rules: JavaScript files in functions/, which vacuum loads. The lint
// passes the directory on, and the suite of the tests names it.
val functionsDirectory = layout.projectDirectory.dir("functions")

val lintSpecifications =
    tasks.register<SpecificationLint>("lintSpecifications") {
        group = LifecycleBasePlugin.VERIFICATION_GROUP
        description = "Lints every specification with vacuum."
        vacuumHome = unpackVacuum.flatMap { it.destination }
        executable = platform.map { it.executable() }
        ruleset = layout.projectDirectory.file("ruleset.yaml")
        if (functionsDirectory.asFile.isDirectory) {
            functions = functionsDirectory
        }
        // Each service has a directory with its API and, next to it, its testability hooks: ADR 0014.
        specifications.from(
            fileTree(layout.projectDirectory) {
                include("*/openapi.yaml", "*/hooks.yaml")
                exclude("build/**")
            },
        )
        sources.from(
            fileTree(layout.projectDirectory) {
                include("**/*.yaml")
                exclude("build/**")
            },
        )
        workingDirectory = layout.projectDirectory
        findings = layout.buildDirectory.file("reports/vacuum/findings.txt")
    }

val testRuleset =
    tasks.register<RulesetTest>("testRuleset") {
        group = LifecycleBasePlugin.VERIFICATION_GROUP
        description = "Tests the rules of the ruleset against documents that keep them or break them."
        vacuumHome = unpackVacuum.flatMap { it.destination }
        executable = platform.map { it.executable() }
        suite = layout.projectDirectory.file("ruleset-tests/suite.yaml")
        sources.from(
            fileTree(layout.projectDirectory) {
                include("**/*.yaml", "functions/*.js")
                exclude("build/**")
            },
        )
        workingDirectory = layout.projectDirectory
        result = layout.buildDirectory.file("reports/vacuum/ruleset-tests.json")
    }

tasks.check {
    dependsOn(lintSpecifications, testRuleset)
}

// The two directories have the places that they have on the site: the reference under api, and the problem types
// next to it. So a link from one to the other also holds here, on disk.
val site = layout.buildDirectory.dir("site")
val titleOfTheReference = "${rootProject.name} API reference"

val renderReference =
    tasks.register<ApiReference>("renderReference") {
        group = "documentation"
        description = "Renders the API reference of every specification with vacuum."
        vacuumHome = unpackVacuum.flatMap { it.destination }
        executable = platform.map { it.executable() }
        catalogTitle = titleOfTheReference
        // The specifications, and the parts that they share. A specification refers to nothing else.
        contracts.from(layout.projectDirectory)
        contracts.include("*/openapi.yaml", "*/hooks.yaml", "common/*.yaml")
        contracts.exclude("build/**")
        reference = site.map { it.dir("api") }
    }

val problemPages =
    tasks.register<ProblemPages>("problemPages") {
        group = "documentation"
        description = "Writes a page for each problem type that the specifications name."
        vacuumHome = unpackVacuum.flatMap { it.destination }
        executable = platform.map { it.executable() }
        referenceTitle = titleOfTheReference
        specifications.from(
            fileTree(layout.projectDirectory) {
                include("*/openapi.yaml", "*/hooks.yaml")
                exclude("build/**")
            },
        )
        sources.from(
            fileTree(layout.projectDirectory) {
                include("**/*.yaml")
                exclude("build/**")
            },
        )
        pages = site.map { it.dir("problems") }
    }

val bundleSpecifications =
    tasks.register<SpecificationBundles>("bundleSpecifications") {
        group = LifecycleBasePlugin.BUILD_GROUP
        description = "Bundles every specification into one JSON document, for the application that implements it."
        vacuumHome = unpackVacuum.flatMap { it.destination }
        executable = platform.map { it.executable() }
        specifications.from(
            fileTree(layout.projectDirectory) {
                include("*/openapi.yaml", "*/hooks.yaml")
                exclude("build/**")
            },
        )
        sources.from(
            fileTree(layout.projectDirectory) {
                include("**/*.yaml")
                exclude("build/**")
            },
        )
        bundles = layout.buildDirectory.dir("bundles")
    }

tasks.assemble {
    dependsOn(renderReference, problemPages, bundleSpecifications)
}

// The root project puts both directories into the site that GitHub Pages serves. It asks this project for them by
// these two names, so it knows no task and no path of this project.
configurations.consumable("apiReference") {
    outgoing.artifact(renderReference.flatMap { it.reference })
}
configurations.consumable("problemTypes") {
    outgoing.artifact(problemPages.flatMap { it.pages })
}

// An application asks for the bundles in the same way, and takes the ones of its own directory.
configurations.consumable("specificationBundles") {
    outgoing.artifact(bundleSpecifications.flatMap { it.bundles })
}

spotless {
    format("misc") {
        // The specifications lie one directory down, and so do the functions of the rules.
        target("*.md", "**/*.yaml", "functions/*.js")
    }
}
