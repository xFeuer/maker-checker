package io.github.xfeuer.makerchecker.buildlogic;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import org.junit.jupiter.api.Test;

class SpecificationBundlesTest {

    @Test
    void putsABundleIntoTheDirectoryOfItsApplication() {
        File specification = new File("contracts/account-service/openapi.yaml");

        assertThat(SpecificationBundles.placeOf(specification)).isEqualTo("account-service/openapi.json");
    }

    @Test
    void keepsTheHooksOfAnApplicationApartFromItsApi() {
        File hooks = new File("contracts/account-service/hooks.yaml");

        assertThat(SpecificationBundles.placeOf(hooks)).isEqualTo("account-service/hooks.json");
    }
}
