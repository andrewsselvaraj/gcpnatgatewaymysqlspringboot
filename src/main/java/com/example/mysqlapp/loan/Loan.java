package com.example.mysqlapp.loan;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * One row of the loan_info table. Fields left null on insert fall back to the
 * column defaults (interest_rate 0.00, received false, status PENDING,
 * application_date today, pk_loan_id a new UUID).
 */
public record Loan(
        @Size(max = 50) String loanId,
        @NotBlank @Size(max = 50) String orgId,
        @NotBlank @Size(max = 50) String userId,
        @NotNull @DecimalMin("0.00") @Digits(integer = 13, fraction = 2) BigDecimal loanAmount,
        @DecimalMin("0.00") @Digits(integer = 3, fraction = 2) BigDecimal interestRate,
        @NotNull @Positive Integer tenureMonths,
        @Size(max = 255) String purpose,
        LocalDate applicationDate,
        LocalDate approvalDate,
        LocalDate disbursementDate,
        LocalDate dueDate,
        @Digits(integer = 13, fraction = 2) BigDecimal outstandingAmount,
        Boolean received,
        @Size(max = 20) String status,
        LocalDateTime createdDatetime,
        @Size(max = 50) String createdUser,
        LocalDateTime updatedDatetime,
        @Size(max = 50) String updatedUser) {
}
