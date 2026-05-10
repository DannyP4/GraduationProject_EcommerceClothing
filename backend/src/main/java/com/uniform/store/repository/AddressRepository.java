package com.uniform.store.repository;

import com.uniform.store.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {

    // Default first, then chronological — drives address-list UI ordering.
    List<Address> findByUserIdOrderByIsDefaultDescIdAsc(Long userId);

    Optional<Address> findByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);

    // clearDefaultsForUser
    @Modifying
    @Query("UPDATE Address a SET a.isDefault = false WHERE a.user.id = :userId AND a.isDefault = true")
    int clearDefaultsForUser(@Param("userId") Long userId);

    Optional<Address> findFirstByUserIdOrderByIdAsc(Long userId);
}
