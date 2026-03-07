package com.system.PayGuard;

import com.system.PayGuard.dto.PaymentRequestDTO;
import com.system.PayGuard.model.UserWallet;
import com.system.PayGuard.repository.PaymentTransactionRepository;
import com.system.PayGuard.repository.UserWalletRepository;
import com.system.PayGuard.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class ConcurrencyIntegrationTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private UserWalletRepository walletRepository;

    @Autowired
    private PaymentTransactionRepository transactionRepository;

    @BeforeEach
    void setUp() {
        // Clean the database before the test runs
        transactionRepository.deleteAll();
        walletRepository.deleteAll();

        // Give our test user exactly ₹500
        UserWallet wallet = UserWallet.builder()
                .userId("concurrent_user")
                .balance(new BigDecimal("500.00"))
                .build();
        walletRepository.save(wallet);
    }

    @Test
    void testOptimisticLockingWithConcurrentRecharges() throws InterruptedException {
        // 1. Setup 10 simultaneous threads
        int numberOfThreads = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        // Thread-safe counters
        AtomicInteger successfulTransactions = new AtomicInteger(0);
        AtomicInteger failedTransactions = new AtomicInteger(0);

        // 2. Fire 10 concurrent requests!
        for (int i = 0; i < numberOfThreads; i++) {
            final int threadNum = i;
            executorService.submit(() -> {
                try {
                    PaymentRequestDTO request = new PaymentRequestDTO();
                    // IMPORTANT: We use a DIFFERENT idempotency key for each thread.
                    // If we used the same key, Redis would block them immediately.
                    // We want them to bypass Redis and hit the database at the exact same time!
                    request.setIdempotencyKey("concurrent-key-" + threadNum);
                    request.setUserId("concurrent_user");
                    request.setAmount(new BigDecimal("500.00")); // Every thread tries to drain the entire ₹500

                    paymentService.processRechargePayment(request);
                    successfulTransactions.incrementAndGet();

                } catch (ObjectOptimisticLockingFailureException | IllegalStateException e) {
                    // We EXPECT 9 of these threads to crash and burn safely.
                    // Spring throws ObjectOptimisticLockingFailureException when @Version fails
                    failedTransactions.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        // 3. Wait for all threads to finish their chaos
        latch.await();
        executorService.shutdown();

        // 4. The Ultimate Assertions
        UserWallet finalWallet = walletRepository.findByUserId("concurrent_user").get();

        // Even though 10 threads tried to steal ₹500, only ONE should have succeeded!
        assertEquals(1, successfulTransactions.get(), "Only one transaction should succeed");
        assertEquals(9, failedTransactions.get(), "Nine transactions should be blocked by Optimistic Locking");
        assertEquals(0, finalWallet.getBalance().compareTo(BigDecimal.ZERO), "Final balance should be exactly 0");
    }
}