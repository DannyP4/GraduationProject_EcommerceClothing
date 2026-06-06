package com.uniform.store.repository;

import com.uniform.store.entity.Review;
import com.uniform.store.enums.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    @EntityGraph(attributePaths = {"user", "product", "variant"})
    Page<Review> findByProductIdAndStatus(Long productId, ReviewStatus status, Pageable pageable);

    long countByProductIdAndStatus(Long productId, ReviewStatus status);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId AND r.status = :status")
    Double averageRating(@Param("productId") Long productId, @Param("status") ReviewStatus status);

    @Query("""
            SELECT r.product.id, AVG(r.rating), COUNT(r)
            FROM Review r
            WHERE r.product.id IN :productIds AND r.status = :status
            GROUP BY r.product.id
            """)
    List<Object[]> aggregateRatingByProductIds(@Param("productIds") Collection<Long> productIds,
                                               @Param("status") ReviewStatus status);

    @EntityGraph(attributePaths = {"product"})
    List<Review> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);

    boolean existsByUserIdAndProductId(Long userId, Long productId);

    Optional<Review> findFirstByUserIdAndProductId(Long userId, Long productId);

    @EntityGraph(attributePaths = {"product"})
    Optional<Review> findByIdAndUserId(Long id, Long userId);

    @EntityGraph(attributePaths = {"user", "product"})
    @Query("""
            SELECT r FROM Review r
            WHERE (:status IS NULL OR r.status = :status)
              AND (:search IS NULL
                   OR LOWER(r.product.name) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(r.user.email)   LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(r.body)         LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Review> adminSearch(@Param("status") ReviewStatus status,
                             @Param("search") String search,
                             Pageable pageable);
}
