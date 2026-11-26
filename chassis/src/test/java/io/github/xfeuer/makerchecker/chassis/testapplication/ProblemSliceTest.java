package io.github.xfeuer.makerchecker.chassis.testapplication;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * A unit test of a slice of Spring MVC gets the problem documents too, without a line of its own. So a test of one
 * controller sees the refusals as a client of the running application sees them.
 */
@WebMvcTest(TestApplication.Operations.class)
class ProblemSliceTest {

    @Test
    void answersARefusalAsTheRunningApplicationDoes(@Autowired MockMvcTester mvc) {
        assertThat(mvc.get().uri("/refuses"))
                .hasStatus(422)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .isStrictlyEqualTo("""
                    {
                      "type": "https://xfeuer.github.io/maker-checker/problems/teapot-empty",
                      "title": "The teapot is empty.",
                      "status": 422
                    }
                    """);
    }

    @Test
    void answersWhatSpringFindsAsTheRunningApplicationDoes(@Autowired MockMvcTester mvc) {
        assertThat(mvc.get().uri("/cups/many"))
                .hasStatus(400)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .extractingPath("$.errors[0].parameter")
                .isEqualTo("number");
    }
}
