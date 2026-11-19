package io.github.xfeuer.makerchecker.automation.suites.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.xfeuer.makerchecker.automation.api.ApiResponse;
import io.github.xfeuer.makerchecker.automation.api.account.AccountServiceClient;
import io.github.xfeuer.makerchecker.automation.api.account.ServiceStatus;
import io.github.xfeuer.makerchecker.automation.api.junit.ApiTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@ApiTest
@Tag("smoke")
class StatusSmokeTest {

    @Test
    void accountServiceReportsItsNameAndVersion(AccountServiceClient accountService) {
        ApiResponse<ServiceStatus> response = accountService.status();

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.body().service()).isEqualTo("account-service");
        assertThat(response.body().version()).isNotBlank();
    }
}
