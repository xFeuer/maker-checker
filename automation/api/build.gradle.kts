plugins {
    id("makerchecker.automation-conventions")
}

dependencies {
    api(project(":automation:core"))

    implementation(libs.jackson.databind)
}
