package service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import Repository.PaymentTransactionRepo;
import entity.PaymentTransaction;

@Service
public class PaymentTransactionService {

    private final PaymentTransactionRepo transactionRepo;

    public PaymentTransactionService(PaymentTransactionRepo transactionRepo) {
        this.transactionRepo = transactionRepo;
    }

    @Transactional
    public PaymentTransaction save(PaymentTransaction transaction) {
        if (transaction.getCreatedAt() == null) {
            transaction.setCreatedAt(LocalDateTime.now());
        }
        return transactionRepo.save(transaction);
    }

    public Optional<PaymentTransaction> findById(Long id) {
        return transactionRepo.findById(id);
    }

    public Optional<PaymentTransaction> findByRazorpayOrderId(String orderId) {
        return transactionRepo.findByRazorpayOrderId(orderId);
    }

    public Optional<PaymentTransaction> findByIdempotencyKey(String idempotencyKey) {
        return transactionRepo.findByIdempotencyKey(idempotencyKey);
    }

    @Transactional
    public PaymentTransaction markAsSuccess(Long transactionId, String paymentId) {
        Optional<PaymentTransaction> txOpt = transactionRepo.findById(transactionId);
        if (txOpt.isPresent()) {
            PaymentTransaction tx = txOpt.get();
            tx.setStatus(PaymentTransaction.PaymentStatus.SUCCESS);
            tx.setRazorpayPaymentId(paymentId);
            tx.setCompletedAt(LocalDateTime.now());
            tx.setVerifiedAt(LocalDateTime.now());
            return transactionRepo.save(tx);
        }
        return null;
    }

    @Transactional
    public PaymentTransaction markAsFailed(Long transactionId, String reason) {
        Optional<PaymentTransaction> txOpt = transactionRepo.findById(transactionId);
        if (txOpt.isPresent()) {
            PaymentTransaction tx = txOpt.get();
            tx.setStatus(PaymentTransaction.PaymentStatus.FAILED);
            tx.setFailureReason(reason);
            tx.setRetryCount(tx.getRetryCount() != null ? tx.getRetryCount() + 1 : 1);
            tx.setLastRetryAt(LocalDateTime.now());
            return transactionRepo.save(tx);
        }
        return null;
    }

    @Transactional
    public PaymentTransaction incrementRetryCount(Long transactionId) {
        Optional<PaymentTransaction> txOpt = transactionRepo.findById(transactionId);
        if (txOpt.isPresent()) {
            PaymentTransaction tx = txOpt.get();
            tx.setRetryCount(tx.getRetryCount() != null ? tx.getRetryCount() + 1 : 1);
            tx.setLastRetryAt(LocalDateTime.now());
            return transactionRepo.save(tx);
        }
        return null;
    }

    public boolean hasPendingTransaction(Long appointmentId) {
        return transactionRepo.existsByAppointmentIdAndStatus(
                appointmentId,
                PaymentTransaction.PaymentStatus.PENDING);
    }
}
