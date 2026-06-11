package com.uniform.store.repository;

import com.uniform.store.entity.EmailLog;
import com.uniform.store.enums.EmailStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {

    boolean existsByOrderIdAndTypeAndStatus(Long orderId, String type, EmailStatus status);
}
