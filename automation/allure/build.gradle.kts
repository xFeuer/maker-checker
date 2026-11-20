plugins {
    id("makerchecker.automation-conventions")
}

dependencies {
    implementation(platform(libs.allure.bom))

    // The listener of this module implements an interface of the api module.
    api(project(":automation:api"))

    implementation(libs.allure.java.commons)

    testImplementation(libs.jackson.databind)
}
