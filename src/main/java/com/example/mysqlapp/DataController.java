package com.example.mysqlapp;

import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DataController {

    private final JdbcTemplate jdbc;

    public DataController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Quick connectivity check: returns the MySQL server version and time. */
    @GetMapping("/db/ping")
    public Map<String, Object> ping() {
        return jdbc.queryForMap("SELECT VERSION() AS version, NOW() AS server_time, DATABASE() AS current_db");
    }
}
