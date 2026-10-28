package com.finsecure.pay;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TransactionValidationService {
	
    private static final Set<String> SUPPORTED_CURRENCIES =
            Set.of("SGD", "USD", "MYR");

    private final BigDecimal maxAmount;
    private static final int MAX_DECIMAL_PLACES = 2;
    public TransactionValidationService(
            @Value("${transactions.max-amount:10000.00}")
            BigDecimal maxAmount) {
        this.maxAmount = maxAmount;
    }

    public TransactionResponse validate(TransactionRequest transaction) {
        List<String> errors = new ArrayList<>();

        if (isBlank(transaction.transactionId())) {
            errors.add("Transaction ID is required");
        }

        if (isBlank(transaction.senderAccount())) {
            errors.add("Sender account is required");
        }

        if (isBlank(transaction.receiverAccount())) {
            errors.add("Receiver account is required");
        }

        if (!isBlank(transaction.senderAccount())
                && !isBlank(transaction.receiverAccount())
                && transaction.senderAccount().trim()
                        .equals(transaction.receiverAccount().trim())) {
            errors.add("Sender and receiver accounts must be different");
        }

        if (transaction.amount() == null) {
            errors.add("Amount is required");
        } else {
            if (transaction.amount().compareTo(BigDecimal.ZERO) <= 0) {
                errors.add("Amount must be greater than zero");
            }

            if (transaction.amount().compareTo(maxAmount) > 0) {
                errors.add("Amount exceeds the maximum transaction limit");
            }

           if (transaction.amount().stripTrailingZeros().scale()
        > MAX_DECIMAL_PLACES) {
                errors.add("Amount must have at most two decimal places");
            }
        }

        if (isBlank(transaction.currency())) {
            errors.add("Currency is required");
        } else if (!SUPPORTED_CURRENCIES.contains(transaction.currency())) {
            errors.add("Currency must be SGD, USD or MYR");
        }

        return new TransactionResponse(
                transaction.transactionId(),
                errors.isEmpty() ? "VALID" : "INVALID",
                List.copyOf(errors)
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}