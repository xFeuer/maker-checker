package io.github.xfeuer.makerchecker.account;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AccountServiceApplicationTest {

    @Test
    void startsAndKnowsTheVersionOfItsBuild(@Autowired BuildProperties build) {
        assertThat(build.getVersion()).isNotBlank();
    }
}
