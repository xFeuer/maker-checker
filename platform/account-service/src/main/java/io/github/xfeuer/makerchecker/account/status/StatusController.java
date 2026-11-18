package io.github.xfeuer.makerchecker.account.status;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Reports which service is running and which build it runs. */
@RestController
class StatusController {

    private final ServiceStatus status;

    StatusController(@Value("${spring.application.name}") String service, BuildProperties build) {
        this.status = new ServiceStatus(service, build.getVersion());
    }

    @GetMapping("/v1/status")
    ServiceStatus status() {
        return status;
    }
}
