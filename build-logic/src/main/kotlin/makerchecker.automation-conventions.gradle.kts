plugins {
    `java-library`
    id("makerchecker.boundary-conventions")
    id("makerchecker.java-conventions")
}

// The automation modules get their own group, so a module name such as "core" cannot clash with the platform.
group = "$group.automation"

val libs = the<VersionCatalogsExtension>().named("libs")

dependencies {
    // One JUnit version for the framework and for its own tests.
    implementation(platform(libs.findLibrary("junit-bom").get()))

    testImplementation(libs.findLibrary("assertj-core").get())
    testImplementation(libs.findLibrary("junit-jupiter").get())
    testRuntimeOnly(libs.findLibrary("junit-platform-launcher").get())
}
