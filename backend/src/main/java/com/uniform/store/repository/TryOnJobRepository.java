package com.uniform.store.repository;

import com.uniform.store.entity.TryOnJob;
import com.uniform.store.enums.TryOnStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TryOnJobRepository extends JpaRepository<TryOnJob, Long> {

    Optional<TryOnJob> findByIdAndUserId(Long id, Long userId);

    Optional<TryOnJob> findFirstByUserIdAndProductIdAndUserImageUrlAndGarmentPhotoTypeAndStatusOrderByCreatedAtDesc(
            Long userId, Long productId, String userImageUrl, String garmentPhotoType, TryOnStatus status);
}
