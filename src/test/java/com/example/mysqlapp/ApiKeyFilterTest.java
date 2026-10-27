package com.example.mysqlapp;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:keytest;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "app.api-key=s3cret"
})
@AutoConfigureMockMvc
@Sql("/loan_info.sql")
class ApiKeyFilterTest {

    @Autowired
    MockMvc mvc;

    @Test
    void rejectsMissingOrWrongKey() throws Exception {
        mvc.perform(get("/api/loans")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/loans?key=wrong")).andExpect(status().isUnauthorized());
    }

    @Test
    void acceptsHeaderOrQueryKey() throws Exception {
        mvc.perform(get("/api/loans").header("X-API-Key", "s3cret")).andExpect(status().isOk());
        mvc.perform(get("/api/loans?key=s3cret")).andExpect(status().isOk());
    }

    @Test
    void healthStaysOpen() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }
}
