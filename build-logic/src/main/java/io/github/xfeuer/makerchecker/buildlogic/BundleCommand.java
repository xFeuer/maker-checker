package io.github.xfeuer.makerchecker.buildlogic;

import java.util.List;

/**
 * The command line that bundles one specification with vacuum: one JSON document, with every reference to another
 * file resolved.
 *
 * <p>vacuum runs in the directory of the specification, so it gets the specification by its file name. It resolves a
 * reference to another file from the directory that it runs in.
 */
record BundleCommand(String program, String specification, String bundle) {

    /** The program and its arguments, in the order in which the process gets them. */
    List<String> arguments() {
        // The flags keep vacuum from asking GitHub for a newer release and from decorating its output. The name
        // of the bundle ends in .json, and that makes it JSON.
        return List.of(program, "bundle", "--no-update-check", "--no-style", specification, bundle);
    }
}
