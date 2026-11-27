plugins {
    id("makerchecker.spring-boot-service-conventions")
}

dependencies {
    implementation(project(":chassis"))

    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")

    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")

    // ADR 0019: what holds the service to its contract in the component tests.
    componentTestImplementation(testFixtures(project(":chassis")))
}
