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
