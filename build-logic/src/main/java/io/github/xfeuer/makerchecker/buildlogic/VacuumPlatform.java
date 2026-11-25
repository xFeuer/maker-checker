package io.github.xfeuer.makerchecker.buildlogic;

import java.util.Locale;
import org.gradle.api.GradleException;

/**
 * The platform of a machine, in the words of the vacuum releases. See ADR 0009.
 *
 * <p>vacuum is a program for one platform. A release holds one archive for each, and the name of an archive ends
 * with the platform: {@code vacuum_0.43.0_linux_x86_64.tar.gz}.
 */
public record VacuumPlatform(String operatingSystem, String architecture) {

    /** Reads the platform from the values that a JVM has for {@code os.name} and {@code os.arch}. */
    public static VacuumPlatform of(String osName, String osArch) {
        return new VacuumPlatform(operatingSystem(osName), architecture(osArch));
    }

    /** The part of an archive name that tells the platform, such as {@code linux_x86_64}. */
    public String classifier() {
        return operatingSystem + "_" + architecture;
    }

    /** The name of the program in the archive. */
    public String executable() {
        return operatingSystem.equals("windows") ? "vacuum.exe" : "vacuum";
    }

    private static String operatingSystem(String osName) {
        String name = osName.toLowerCase(Locale.ROOT);
        if (name.startsWith("linux")) {
            return "linux";
        }
        if (name.startsWith("mac")) {
            return "darwin";
        }
        if (name.startsWith("windows")) {
            return "windows";
        }
        throw new GradleException("vacuum has no release for the operating system \"%s\".".formatted(osName));
    }

    private static String architecture(String osArch) {
        return switch (osArch.toLowerCase(Locale.ROOT)) {
            case "amd64", "x86_64" -> "x86_64";
            case "aarch64", "arm64" -> "arm64";
            default ->
                throw new GradleException("vacuum has no release for the architecture \"%s\".".formatted(osArch));
        };
    }
}
