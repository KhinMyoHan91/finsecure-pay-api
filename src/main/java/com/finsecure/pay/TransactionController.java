package com.finsecure.pay;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TransactionController {

    private final TransactionValidationService validationService;

    public TransactionController(
            TransactionValidationService validationService) {
        this.validationService = validationService;
    }

    @PostMapping("/validate")
    public TransactionResponse validate(
            @RequestBody TransactionRequest transaction) {
        return validationService.validate(transaction);
    }
}