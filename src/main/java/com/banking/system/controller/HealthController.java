
package com.banking.system.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    public HealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/health")
    public Map<String, String> healthCheck() {
        return Map.of(
                "status", "UP",
                "application", "Banking Transaction System"
        );
    }

    @GetMapping("/db-check")
    public Map<String, Object> databaseCheck() {
        return jdbcTemplate.queryForMap(
                "SELECT current_database() AS database, current_timestamp AS server_time"
        );
    }
}
