
package com.banking.system.repository;

import com.banking.system.model.Customer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
public class CustomerRepository {

    private final JdbcTemplate jdbcTemplate;

    public CustomerRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Save a new customer in PostgreSQL
    public Customer save(Customer customer) {

        String sql = """
                INSERT INTO customer (full_name, email, phone)
                VALUES (?, ?, ?)
                RETURNING customer_id, created_at
                """;

        return jdbcTemplate.queryForObject(
                sql,
                (rs, rowNum) -> {
                    customer.setCustomerId(
                            rs.getLong("customer_id")
                    );

                    customer.setCreatedAt(
                            rs.getObject(
                                    "created_at",
                                    OffsetDateTime.class
                            )
                    );

                    return customer;
                },
                customer.getFullName(),
                customer.getEmail(),
                customer.getPhone()
        );
    }

    // Find customer using customer ID
    public Optional<Customer> findById(Long customerId) {

        String sql = """
                SELECT customer_id, full_name, email, phone, created_at
                FROM customer
                WHERE customer_id = ?
                """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new Customer(
                        rs.getLong("customer_id"),
                        rs.getString("full_name"),
                        rs.getString("email"),
                        rs.getString("phone"),
                        rs.getObject(
                                "created_at",
                                OffsetDateTime.class
                        )
                ),
                customerId
        ).stream().findFirst();
    }
}
