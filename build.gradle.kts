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
            "LICENSE",
            ".github/**/*.md",
            ".github/**/*.yml",
            "automation/*.md",
            "contracts/*.md",
            "contracts/**/*.yaml",
            "deploy/*.md",
            "docs/**/*.md",
            "gradle/*.toml",
            "platform/*.md",
        )
    }
}
