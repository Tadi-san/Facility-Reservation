package com.eagle.app;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest(properties = "eagle.rate-limit.enabled=true")
@AutoConfigureMockMvc
class RateLimitingFilterTest {
    @Autowired
    private MockMvc mvc;

    @Test
    void limiterTriggersAtSixtyPerMinute() throws Exception {
        boolean hit429 = false;
        for (int i = 0; i < 75; i++) {
            int status = mvc.perform(get("/api/v1/health"))
                    .andReturn()
                    .getResponse()
                    .getStatus();
            if (status == 429) {
                hit429 = true;
                break;
            }
        }
        Assertions.assertTrue(hit429, "Expected rate limiter to trigger at 60 requests/minute.");
    }
}
