package com.chatbot.simplechatbot.service;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class TransactionTool {

    private final JdbcTemplate jdbcTemplate;

    public TransactionTool(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Tool("Get the total transaction amount for an account ID")
    public String getTotalAmount(@P("Account ID for which to calculate the total transaction amount") String accountId) {

        String sql = """
                SELECT SUM(amount) FROM transaction WHERE account_id = ?
                """;
        BigDecimal total = jdbcTemplate.queryForObject(sql, BigDecimal.class, accountId);
        if (total == null) {
            return "No transaction data available for account " + accountId;
        }
        return "Total transaction amount for account " + accountId + " is $" + total.toPlainString();
    }
}
