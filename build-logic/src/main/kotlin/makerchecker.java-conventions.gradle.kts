import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    java
    id("makerchecker.formatting-conventions")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    // Every warning fails the build, so none can pile up. The path lint is the exception: Gradle lists
    // output directories that may not exist, such as the resources of a module that has none.
    options.compilerArgs.addAll(listOf("-Xlint:all,-path", "-Werror"))
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
        events(TestLogEvent.FAILED, TestLogEvent.SKIPPED)
        exceptionFormat = TestExceptionFormat.FULL
    }
}

spotless {
    java {
        palantirJavaFormat()
    }
}
