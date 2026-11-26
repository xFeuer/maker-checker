package io.github.xfeuer.makerchecker.buildlogic;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class VacuumOutputTest {

    @Test
    void givesEachMessageALineOfItsOwnWithoutTheColorsOfATerminal() {
        String output = " [HTML] building model (1/4 25%)\r [HTML] completed in 387ms\n\n"
                + "\u001B[1;30mERR\u001B[m error building path item\n";

        assertThat(VacuumOutput.readable(output))
                .isEqualTo(
                        "  [HTML] building model (1/4 25%)\n  [HTML] completed in 387ms\n  ERR error building path item");
    }

    @Test
    void saysSoWhenVacuumWroteNothing() {
        assertThat(VacuumOutput.readable("\n")).isEqualTo("  (nothing)");
    }
}
