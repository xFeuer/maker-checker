plugins {
    id("makerchecker.spring-boot-library-conventions")
}

dependencies {
    // The chassis shows types of Spring in its own API, so an application gets them with it.
    api("org.springframework.boot:spring-boot-starter-webmvc")

    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
}

// The tests of the chassis run a small application of their own and replace nothing in it.
listOf("testCompileClasspath", "testRuntimeClasspath").forEach { classpath ->
    configurations.named(classpath) {
        exclude(group = "org.mockito")
    }
}
