package io.github.xfeuer.makerchecker.chassis.problem;

import org.apache.catalina.core.StandardHost;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.webmvc.autoconfigure.error.ErrorMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Makes every error response of an application a problem document. An application gets this by having the chassis
 * among its dependencies, and it adds nothing of its own. See ADR 0010 and ADR 0018.
 *
 * <p>An error has three ways out of an application, and each has its part here: Spring MVC, the error page of the
 * server, and Tomcat itself.
 */
// Before the error handling of Spring Boot, which then leaves out its own error controller.
@AutoConfiguration(before = ErrorMvcAutoConfiguration.class)
public class ProblemAutoConfiguration {

    @Bean
    ProblemExceptionHandler problemExceptionHandler() {
        return new ProblemExceptionHandler();
    }

    @Bean
    ProblemErrorController problemErrorController() {
        return new ProblemErrorController();
    }

    /**
     * Names the valve as the one that reports the errors of the host. Tomcat then creates it when it starts, and
     * puts it closest to the application, so it reports before the valve of Spring Boot does.
     */
    @Bean
    WebServerFactoryCustomizer<TomcatServletWebServerFactory> problemReportValve() {
        return factory -> factory.addContextCustomizers(context -> {
            if (context.getParent() instanceof StandardHost host) {
                host.setErrorReportValveClass(ProblemReportValve.class.getName());
            }
        });
    }
}
