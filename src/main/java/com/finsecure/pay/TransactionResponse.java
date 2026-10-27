package com.finsecure.pay;

import java.util.List;

public record TransactionResponse(
        String transactionId,
        String status,
        List<String> errors
) {
}