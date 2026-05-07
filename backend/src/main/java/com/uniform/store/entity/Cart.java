package com.uniform.store.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "carts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cart extends BaseEntity {

    // Schema allows multiple carts per user (no UNIQUE on user_id) but business rule is one.
    // Enforced at app level via CartRepository.findByUserId + lazy-create.
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id",
            foreignKey = @ForeignKey(name = "fk_carts_user"))
    private User user;

    // Reserved for future server-side guest cart; currently always null (guest cart lives in FE localStorage).
    @Column(name = "session_id", length = 64)
    private String sessionId;
}
