package io.github.xfeuer.makerchecker.buildlogic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.gradle.api.GradleException;
import org.junit.jupiter.api.Test;

class VacuumPlatformTest {

    @Test
    void namesEachPlatformAsTheReleaseOfVacuumDoes() {
        assertThat(VacuumPlatform.of("Linux", "amd64").classifier()).isEqualTo("linux_x86_64");
        assertThat(VacuumPlatform.of("Linux", "aarch64").classifier()).isEqualTo("linux_arm64");
        assertThat(VacuumPlatform.of("Mac OS X", "x86_64").classifier()).isEqualTo("darwin_x86_64");
        assertThat(VacuumPlatform.of("Mac OS X", "aarch64").classifier()).isEqualTo("darwin_arm64");
        assertThat(VacuumPlatform.of("Windows 11", "amd64").classifier()).isEqualTo("windows_x86_64");
        assertThat(VacuumPlatform.of("Windows 11", "aarch64").classifier()).isEqualTo("windows_arm64");
    }

    @Test
    void namesTheProgramWithItsExtensionOnWindowsOnly() {
        assertThat(VacuumPlatform.of("Windows 11", "amd64").executable()).isEqualTo("vacuum.exe");
        assertThat(VacuumPlatform.of("Linux", "amd64").executable()).isEqualTo("vacuum");
        assertThat(VacuumPlatform.of("Mac OS X", "aarch64").executable()).isEqualTo("vacuum");
    }

    @Test
    void rejectsAnOperatingSystemThatVacuumHasNoReleaseFor() {
        assertThatThrownBy(() -> VacuumPlatform.of("SunOS", "amd64"))
                .isInstanceOf(GradleException.class)
                .hasMessage("vacuum has no release for the operating system \"SunOS\".");
    }

    @Test
    void rejectsAnArchitectureThatVacuumHasNoReleaseFor() {
        assertThatThrownBy(() -> VacuumPlatform.of("Linux", "riscv64"))
                .isInstanceOf(GradleException.class)
                .hasMessage("vacuum has no release for the architecture \"riscv64\".");
    }
}
