package io.github.xfeuer.makerchecker.buildlogic;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import org.gradle.api.GradleException;
import org.gradle.process.ExecOperations;
import org.gradle.process.ExecResult;

/** Runs vacuum to bundle one specification: a JSON document that refers to no other file. */
final class Bundling {

    private Bundling() {}

    /** vacuum resolves a reference from its working directory, so it runs in the directory of the specification. */
    static void bundle(ExecOperations processes, File program, File specification, File bundle) {
        BundleCommand command =
                new BundleCommand(program.getAbsolutePath(), specification.getName(), bundle.getAbsolutePath());
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ExecResult run = processes.exec(vacuum -> {
            vacuum.commandLine(command.arguments());
            vacuum.workingDir(specification.getParentFile());
            // Only a failure shows what vacuum wrote.
            vacuum.setStandardOutput(output);
            vacuum.setErrorOutput(output);
            vacuum.setIgnoreExitValue(true);
        });
        if (run.getExitValue() != 0 || !bundle.isFile()) {
            throw new GradleException("vacuum could not bundle %s. It ended with exit code %d and wrote:\n%s"
                    .formatted(
                            specification,
                            run.getExitValue(),
                            VacuumOutput.readable(output.toString(StandardCharsets.UTF_8))));
        }
    }
}
