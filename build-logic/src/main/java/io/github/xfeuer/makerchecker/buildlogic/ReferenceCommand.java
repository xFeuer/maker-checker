package io.github.xfeuer.makerchecker.buildlogic;

import java.util.List;

/**
 * The command line that renders the API reference with vacuum. See ADR 0015.
 *
 * <p>vacuum runs in a directory that holds the contracts and nothing else. It renders every specification that it
 * finds there, as one catalog with a page for each operation and each schema.
 */
record ReferenceCommand(String program, String title, String output) {

    /** The program and its arguments, in the order in which the process gets them. */
    List<String> arguments() {
        return List.of(
                program,
                "docs",
                // These two keep vacuum from asking GitHub for a newer release and from printing its banner.
                "--no-update-check",
                "--no-logo",
                // The reference is HTML and nothing else: no page of lint findings, because the build fails on a
                // finding anyway, and no files for programs to read.
                "--no-diagnostics",
                "--no-llm",
                "--no-json",
                // The footer of vacuum names its maker as the holder of a copyright.
                "--no-footer",
                "--catalog-title",
                title,
                "--output",
                output,
                // The directory that vacuum runs in.
                ".");
    }
}
