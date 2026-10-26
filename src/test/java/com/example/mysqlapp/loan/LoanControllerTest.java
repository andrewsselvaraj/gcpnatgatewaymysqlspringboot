package com.example.mysqlapp.loan;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:test;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
@AutoConfigureMockMvc
@Sql("/loan_info.sql")
class LoanControllerTest {

    @Autowired
    MockMvc mvc;

    @Test
    void crudRoundTrip() throws Exception {
        mvc.perform(post("/api/loans").contentType(MediaType.APPLICATION_JSON).content("""
                {"loanId":"L1","orgId":"ORG1","userId":"U1","loanAmount":50000.00,
                 "tenureMonths":12,"purpose":"car","createdUser":"admin"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.interestRate").value(0.0))
                .andExpect(jsonPath("$.received").value(false));

        mvc.perform(get("/api/loans?orgId=ORG1")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));

        mvc.perform(put("/api/loans/L1").contentType(MediaType.APPLICATION_JSON).content("""
                {"orgId":"ORG1","userId":"U1","loanAmount":50000.00,"interestRate":9.5,
                 "tenureMonths":24,"status":"APPROVED","approvalDate":"2026-10-26","updatedUser":"mgr"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenureMonths").value(24))
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.approvalDate").value("2026-10-26"))
                .andExpect(jsonPath("$.updatedUser").value("mgr"));

        mvc.perform(delete("/api/loans/L1")).andExpect(status().isNoContent());
        mvc.perform(get("/api/loans/L1")).andExpect(status().isNotFound());
    }

    @Test
    void generatesIdAndRejectsDuplicatesAndInvalid() throws Exception {
        mvc.perform(post("/api/loans").contentType(MediaType.APPLICATION_JSON).content("""
                {"orgId":"ORG1","userId":"U1","loanAmount":100,"tenureMonths":6}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.loanId").isNotEmpty());

        String body = """
                {"loanId":"DUP","orgId":"ORG1","userId":"U1","loanAmount":100,"tenureMonths":6}""";
        mvc.perform(post("/api/loans").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated());
        mvc.perform(post("/api/loans").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isConflict());

        mvc.perform(post("/api/loans").contentType(MediaType.APPLICATION_JSON).content("""
                {"orgId":"ORG1","loanAmount":100}"""))
                .andExpect(status().isBadRequest());

        mvc.perform(put("/api/loans/NOPE").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
    }
}
