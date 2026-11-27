plugins {
    id("makerchecker.spring-boot-library-conventions")
    `java-test-fixtures`
}

dependencies {
    // The chassis shows types of Spring in its own API, so an application gets them with it.
    api("org.springframework.boot:spring-boot-starter-webmvc")

    // ADR 0019: the validator that holds an application to its specification. The Spring Boot BOM does not have it.
    implementation(libs.json.schema.validator)

    // The test fixtures are what the tests of an application use to hold it to its specification.
    testFixturesImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")

    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
}

// The tests of the chassis run a small application of their own and replace nothing in it.
listOf("testCompileClasspath", "testRuntimeClasspath").forEach { classpath ->
    configurations.named(classpath) {
        exclude(group = "org.mockito")
    }
}
