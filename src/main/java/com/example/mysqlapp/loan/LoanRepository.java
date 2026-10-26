package com.example.mysqlapp.loan;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class LoanRepository {

    private static final String COLUMNS = """
            pk_loan_id, loan_org_id, loan_user_id, loan_amount, interest_rate, tenure_months,
            purpose, application_date, approval_date, disbursement_date, due_date,
            outstanding_amount, received, status, created_datetime, created_user,
            updated_datetime, updated_user""";

    private final JdbcTemplate jdbc;

    public LoanRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Loan> findAll(String orgId, String userId, String status, int limit, int offset) {
        StringBuilder sql = new StringBuilder("SELECT " + COLUMNS + " FROM loan_info WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (orgId != null) {
            sql.append(" AND loan_org_id = ?");
            args.add(orgId);
        }
        if (userId != null) {
            sql.append(" AND loan_user_id = ?");
            args.add(userId);
        }
        if (status != null) {
            sql.append(" AND status = ?");
            args.add(status);
        }
        sql.append(" ORDER BY created_datetime DESC LIMIT ? OFFSET ?");
        args.add(limit);
        args.add(offset);
        return jdbc.query(sql.toString(), LoanRepository::mapRow, args.toArray());
    }

    public Optional<Loan> findById(String loanId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM loan_info WHERE pk_loan_id = ?",
                LoanRepository::mapRow, loanId).stream().findFirst();
    }

    /** Inserts the loan and returns its id (generated when not supplied). */
    public String insert(Loan l) {
        String id = l.loanId() != null && !l.loanId().isBlank() ? l.loanId() : UUID.randomUUID().toString();
        jdbc.update("""
                INSERT INTO loan_info (pk_loan_id, loan_org_id, loan_user_id, loan_amount, interest_rate,
                    tenure_months, purpose, application_date, approval_date, disbursement_date, due_date,
                    outstanding_amount, received, status, created_user, updated_user)
                VALUES (?, ?, ?, ?, COALESCE(?, 0.00), ?, ?, COALESCE(?, CURRENT_DATE), ?, ?, ?, ?,
                    COALESCE(?, FALSE), COALESCE(?, 'PENDING'), ?, ?)""",
                id, l.orgId(), l.userId(), l.loanAmount(), l.interestRate(), l.tenureMonths(), l.purpose(),
                l.applicationDate(), l.approvalDate(), l.disbursementDate(), l.dueDate(),
                l.outstandingAmount(), l.received(), l.status(), l.createdUser(), l.createdUser());
        return id;
    }

    /** Full update of the editable columns. Returns the number of rows changed (0 = not found). */
    public int update(String loanId, Loan l) {
        return jdbc.update("""
                UPDATE loan_info SET loan_org_id = ?, loan_user_id = ?, loan_amount = ?,
                    interest_rate = COALESCE(?, interest_rate), tenure_months = ?, purpose = ?,
                    application_date = COALESCE(?, application_date), approval_date = ?,
                    disbursement_date = ?, due_date = ?, outstanding_amount = ?,
                    received = COALESCE(?, received), status = COALESCE(?, status),
                    updated_user = ?, updated_datetime = CURRENT_TIMESTAMP
                WHERE pk_loan_id = ?""",
                l.orgId(), l.userId(), l.loanAmount(), l.interestRate(), l.tenureMonths(), l.purpose(),
                l.applicationDate(), l.approvalDate(), l.disbursementDate(), l.dueDate(),
                l.outstandingAmount(), l.received(), l.status(), l.updatedUser(), loanId);
    }

    public int delete(String loanId) {
        return jdbc.update("DELETE FROM loan_info WHERE pk_loan_id = ?", loanId);
    }

    private static Loan mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Loan(
                rs.getString("pk_loan_id"),
                rs.getString("loan_org_id"),
                rs.getString("loan_user_id"),
                rs.getBigDecimal("loan_amount"),
                rs.getBigDecimal("interest_rate"),
                rs.getInt("tenure_months"),
                rs.getString("purpose"),
                rs.getObject("application_date", java.time.LocalDate.class),
                rs.getObject("approval_date", java.time.LocalDate.class),
                rs.getObject("disbursement_date", java.time.LocalDate.class),
                rs.getObject("due_date", java.time.LocalDate.class),
                rs.getBigDecimal("outstanding_amount"),
                rs.getBoolean("received"),
                rs.getString("status"),
                rs.getObject("created_datetime", java.time.LocalDateTime.class),
                rs.getString("created_user"),
                rs.getObject("updated_datetime", java.time.LocalDateTime.class),
                rs.getString("updated_user"));
    }
}
