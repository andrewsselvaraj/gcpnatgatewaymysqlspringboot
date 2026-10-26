package com.example.mysqlapp;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DataController {

    private final JdbcTemplate jdbc;
    private final AppProperties props;

    public DataController(JdbcTemplate jdbc, AppProperties props) {
        this.jdbc = jdbc;
        this.props = props;
    }

    /** Quick connectivity check: returns the MySQL server version and time. */
    @GetMapping("/db/ping")
    public Map<String, Object> ping() {
        return jdbc.queryForMap("SELECT VERSION() AS version, NOW() AS server_time, DATABASE() AS current_db");
    }

    /** Rows from the configured table (APP_TABLE), capped at APP_MAX_ROWS. */
    @GetMapping("/items")
    public List<Map<String, Object>> items(@RequestParam(defaultValue = "20") int limit) {
        int capped = Math.max(1, Math.min(limit, props.maxRows()));
        return jdbc.queryForList("SELECT * FROM " + props.quotedTable() + " LIMIT ?", capped);
    }
}
