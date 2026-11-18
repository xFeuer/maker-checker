plugins {
    id("makerchecker.formatting-conventions")
}

spotless {
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
            ".github/**/*.md",
            ".github/**/*.yml",
            "automation/*.md",
            "contracts/*.md",
            "deploy/*.md",
            "docs/**/*.md",
            "gradle/*.toml",
            "platform/*.md",
        )
    }
}
