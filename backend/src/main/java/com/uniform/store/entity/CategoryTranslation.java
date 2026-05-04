package com.uniform.store.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "category_translations",
        uniqueConstraints = @UniqueConstraint(name = "uk_category_locale", columnNames = {"category_id", "locale"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryTranslation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_category_translations_category"))
    private Category category;

    @Column(name = "locale", nullable = false, length = 5)
    private String locale;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "is_auto_translated", nullable = false)
    @Builder.Default
    private Boolean isAutoTranslated = false;

    @Column(name = "translated_at", nullable = false)
    private Instant translatedAt;
}
