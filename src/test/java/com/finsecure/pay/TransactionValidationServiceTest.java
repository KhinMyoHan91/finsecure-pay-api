package com.finsecure.pay;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TransactionValidationServiceTest {

    private TransactionValidationService service;

    @BeforeEach
    void setUp() {
        service = new TransactionValidationService(
                new BigDecimal("10000.00"));
    }

    private TransactionRequest transaction(
            String id, String sender, String receiver,
            String amount, String currency) {
        return new TransactionRequest(
                id, sender, receiver,
                amount == null ? null : new BigDecimal(amount),
                currency);
    }

    private void assertInvalid(
            TransactionRequest request, String... expectedErrors) {
        TransactionResponse result = service.validate(request);

        assertEquals("INVALID", result.status());
        assertEquals(List.of(expectedErrors), result.errors());
    }

    @Test
    void acceptsValidTransaction() {
        TransactionResponse result = service.validate(transaction(
                "TXN-1001", "ACC-001", "ACC-002", "250.00", "SGD"));

        assertEquals("TXN-1001", result.transactionId());
        assertEquals("VALID", result.status());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void rejectsMissingRequiredFields() {
        assertInvalid(
                transaction(null, " ", "", null, null),
                "Transaction ID is required",
                "Sender account is required",
                "Receiver account is required",
                "Amount is required",
                "Currency is required");
    }

    @Test
    void rejectsSameSenderAndReceiver() {
        assertInvalid(
                transaction("TXN-1001", "ACC-001", "ACC-001",
                        "250.00", "SGD"),
                "Sender and receiver accounts must be different");
    }

    @Test
    void rejectsSameAccountsWithSurroundingSpaces() {
        assertInvalid(
                transaction("TXN-1001", " ACC-001 ", "ACC-001",
                        "250.00", "SGD"),
                "Sender and receiver accounts must be different");
    }

    @Test
    void rejectsZeroAmount() {
        assertInvalid(
                transaction("TXN-1001", "ACC-001", "ACC-002",
                        "0.00", "SGD"),
                "Amount must be greater than zero");
    }

    @Test
    void rejectsNegativeAmount() {
        assertInvalid(
                transaction("TXN-1001", "ACC-001", "ACC-002",
                        "-50.00", "SGD"),
                "Amount must be greater than zero");
    }

    @Test
    void acceptsSmallestSupportedPositiveAmount() {
        TransactionResponse result = service.validate(transaction(
                "TXN-1001", "ACC-001", "ACC-002", "0.01", "SGD"));

        assertEquals("VALID", result.status());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void acceptsAmountAtMaximumLimit() {
        TransactionResponse result = service.validate(transaction(
                "TXN-1001", "ACC-001", "ACC-002", "10000.00", "SGD"));

        assertEquals("VALID", result.status());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void rejectsAmountAboveMaximumLimit() {
        assertInvalid(
                transaction("TXN-1001", "ACC-001", "ACC-002",
                        "10000.01", "SGD"),
                "Amount exceeds the maximum transaction limit");
    }

    @Test
    void rejectsFractionOfOneCent() {
        assertInvalid(
                transaction("TXN-1001", "ACC-001", "ACC-002",
                        "250.001", "SGD"),
                "Amount must have at most two decimal places");
    }

    @Test
    void acceptsTrailingZerosWithoutRounding() {
        TransactionResponse result = service.validate(transaction(
                "TXN-1001", "ACC-001", "ACC-002", "250.000", "SGD"));

        assertEquals("VALID", result.status());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void acceptsAllSupportedCurrencies() {
        for (String currency : List.of("SGD", "USD", "MYR")) {
            TransactionResponse result = service.validate(transaction(
                    "TXN-1001", "ACC-001", "ACC-002", "250.00",
                    currency));

            assertEquals("VALID", result.status(), currency);
            assertTrue(result.errors().isEmpty(), currency);
        }
    }

    @Test
    void rejectsUnsupportedCurrency() {
        assertInvalid(
                transaction("TXN-1001", "ACC-001", "ACC-002",
                        "250.00", "XYZ"),
                "Currency must be SGD, USD or MYR");
    }

    @Test
    void returnsMultipleValidationErrors() {
        assertInvalid(
                transaction("TXN-1002", "ACC-001", "ACC-001",
                        "-50.00", "XYZ"),
                "Sender and receiver accounts must be different",
                "Amount must be greater than zero",
                "Currency must be SGD, USD or MYR");
    }

    @Test
    void respectsConfiguredMaximumLimit() {
        TransactionValidationService lowerLimitService =
                new TransactionValidationService(
                        new BigDecimal("500.00"));

        TransactionResponse result = lowerLimitService.validate(
                transaction("TXN-1001", "ACC-001", "ACC-002",
                        "500.01", "SGD"));

        assertEquals("INVALID", result.status());
        assertEquals(
                List.of("Amount exceeds the maximum transaction limit"),
                result.errors());
    }
}