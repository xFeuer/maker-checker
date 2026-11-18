plugins {
    id("com.diffplug.spotless")
}

spotless {
    kotlinGradle {
        target("*.gradle.kts")
        ktlint()
    }
    format("misc") {
        target(
            "*.md",
            "*.properties",
            "*.toml",
            "*.yaml",
            "*.yml",
        )
        trimTrailingWhitespace()
        endWithNewline()
    }
}
