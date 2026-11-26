import io.github.xfeuer.makerchecker.buildlogic.BoundaryCheck

plugins {
    `lifecycle-base`
}

// ADR 0002: the automation never depends on the code of the applications. A project that applies this plugin fails
// its check when a project under platform/ or the chassis is in one of its dependency graphs.
val checkBoundary =
    tasks.register<BoundaryCheck>("checkBoundary") {
        group = LifecycleBasePlugin.VERIFICATION_GROUP
        description = "Checks that no code of the applications is among the dependencies of this project."
        // Every resolvable configuration counts, so that no classpath is missed.
        configurations.matching { it.isCanBeResolved }.all {
            graphs.put(name, incoming.resolutionResult.rootComponent)
        }
    }

tasks.check {
    dependsOn(checkBoundary)
}
