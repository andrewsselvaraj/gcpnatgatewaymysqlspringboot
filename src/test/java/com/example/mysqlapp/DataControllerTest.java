package com.example.mysqlapp;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:test;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "app.table=items",
        "app.max-rows=2"
})
@AutoConfigureMockMvc
class DataControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        jdbc.execute("DROP TABLE IF EXISTS items");
        jdbc.execute("CREATE TABLE items (id INT PRIMARY KEY, name VARCHAR(50))");
        jdbc.update("INSERT INTO items VALUES (1, 'a'), (2, 'b'), (3, 'c')");
    }

    @Test
    void itemsRespectsMaxRows() throws Exception {
        mvc.perform(get("/api/items?limit=50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void healthIsUp() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }
}
