package com.example.mysqlapp;

import java.util.regex.Pattern;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings for the data endpoint. The table name is injected into SQL, so it is
 * validated as a plain identifier (letters, digits, underscore, optional schema prefix).
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(String table, int maxRows) {

    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)?");

    public AppProperties {
        if (table == null || !IDENTIFIER.matcher(table).matches()) {
            throw new IllegalArgumentException("app.table must be a plain table name, got: " + table);
        }
        if (maxRows <= 0) {
            maxRows = 100;
        }
    }

    /** Table name quoted with backticks for MySQL, e.g. `db`.`table`. */
    public String quotedTable() {
        return "`" + table.replace(".", "`.`") + "`";
    }
}
