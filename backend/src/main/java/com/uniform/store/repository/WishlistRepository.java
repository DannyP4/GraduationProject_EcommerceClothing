package com.uniform.store.repository;

import com.uniform.store.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    boolean existsByUserIdAndProductId(Long userId, Long productId);

    Optional<Wishlist> findByUserIdAndProductId(Long userId, Long productId);

    @Query("SELECT w.product.id FROM Wishlist w WHERE w.user.id = :userId ORDER BY w.createdAt DESC")
    List<Long> findProductIdsByUserId(@Param("userId") Long userId);

    @Query("SELECT w.product.id FROM Wishlist w WHERE w.user.id = :userId "
            + "AND w.product.isActive = true AND w.product.deletedAt IS NULL "
            + "ORDER BY w.createdAt DESC")
    List<Long> findActiveProductIdsByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM Wishlist w WHERE w.user.id = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);
}
