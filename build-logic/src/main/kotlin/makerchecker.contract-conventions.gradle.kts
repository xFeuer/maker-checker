import io.github.xfeuer.makerchecker.buildlogic.RulesetTest
import io.github.xfeuer.makerchecker.buildlogic.SpecificationLint
import io.github.xfeuer.makerchecker.buildlogic.VacuumPlatform
import io.github.xfeuer.makerchecker.buildlogic.VerifiedUnpack

plugins {
    `lifecycle-base`
    id("makerchecker.formatting-conventions")
}

// ADR 0009: vacuum lints every specification of a project that applies this plugin and tests the rules of its
// ruleset. check runs both.

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

spotless {
    format("misc") {
        // The specifications lie one directory down, and so do the functions of the rules.
        target("*.md", "**/*.yaml", "functions/*.js")
    }
}
