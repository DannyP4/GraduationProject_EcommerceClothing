package com.uniform.store.repository;

import com.uniform.store.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // Latest first — Phase 3c (VNPAY/Stripe) may produce multiple attempts per order.
    List<Payment> findByOrderIdOrderByIdDesc(Long orderId);

    Optional<Payment> findFirstByOrderIdOrderByIdDesc(Long orderId);
}
