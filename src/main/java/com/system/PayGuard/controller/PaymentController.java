package com.system.PayGuard.controller;

import com.system.PayGuard.dto.PaymentRequestDTO;
import com.system.PayGuard.model.PaymentStatus;
import com.system.PayGuard.model.PaymentTransaction;
import com.system.PayGuard.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/recharge")
    public ResponseEntity<PaymentTransaction> processRecharge(@Valid @RequestBody PaymentRequestDTO request){
        PaymentTransaction transaction = paymentService.processRechargePayment(request);
        return ResponseEntity.ok(transaction);
    }
}
