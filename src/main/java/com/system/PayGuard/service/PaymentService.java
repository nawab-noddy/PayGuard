package com.system.PayGuard.service;

import com.system.PayGuard.dto.PaymentRequestDTO;
import com.system.PayGuard.model.PaymentStatus;
import com.system.PayGuard.model.PaymentTransaction;
import com.system.PayGuard.model.UserWallet;
import com.system.PayGuard.repository.PaymentTransactionRepository;
import com.system.PayGuard.repository.UserWalletRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final IdempotencyService idempotencyService;
    private final UserWalletRepository userWalletRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;

    @Transactional
    public PaymentTransaction processRechargePayment(PaymentRequestDTO request) {

//        1.check Idempotency Key (Redis Lock)
        boolean isNewRequest = idempotencyService.lockRequest(request.getIdempotencyKey());

        if (!isNewRequest) {
//            if it's not newRequest than fetch the existing transaction and return it.
//            this generates EXACTLY-ONCE semantics. The user get tha same response again.
            return paymentTransactionRepository.findByIdempotencyKey(request.getIdempotencyKey())
                    .orElseThrow(() -> new IllegalStateException("Transaction Processing or Failed to save"));
        }
//        2. Fetch the User's wallet
        UserWallet wallet = userWalletRepository.findByUserId(request.getUserId())
                .orElseThrow(()-> new IllegalArgumentException("Wallet not found user: " + request.getUserId()));

//        3. Check Balance
        if(wallet.getBalance().compareTo(request.getAmount()) < 0){
            throw new IllegalStateException("Insufficient Balance");
        }
//        4. Deduct Account
        wallet.setBalance(wallet.getBalance().subtract(request.getAmount()));
        userWalletRepository.save(wallet); // This triggers the @Version Optimistic Lock check!

//         5. Save the Payment Transaction Record
        PaymentTransaction transaction = PaymentTransaction.builder()
                .idempotencyKey(request.getIdempotencyKey())
                .userId(request.getUserId())
                .amount(request.getAmount())
                .status(PaymentStatus.SUCCESS)
                .build();

        return paymentTransactionRepository.save(transaction);
    }
}
