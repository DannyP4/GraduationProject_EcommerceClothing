package com.uniform.store.repository;

import com.uniform.store.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    Page<Order> findByUserIdOrderByPlacedAtDesc(Long userId, Pageable pageable);

    List<Order> findTop50ByUserIdOrderByPlacedAtDesc(Long userId);

    long countByUserId(Long userId);

    // Lookup by orderNumber + userId.
    Optional<Order> findByOrderNumberAndUserId(String orderNumber, Long userId);

    Optional<Order> findByOrderNumber(String orderNumber);

    boolean existsByOrderNumber(String orderNumber);
}
