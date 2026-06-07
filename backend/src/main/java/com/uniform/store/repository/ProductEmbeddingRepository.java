package com.uniform.store.repository;

import com.uniform.store.entity.ProductEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductEmbeddingRepository extends JpaRepository<ProductEmbedding, Long> {

    Optional<ProductEmbedding> findByProductId(Long productId);

    @Query("SELECT e.product.id AS productId, e.dim AS dim, e.embedding AS embedding "
            + "FROM ProductEmbedding e JOIN e.product p "
            + "WHERE p.isActive = true AND p.deletedAt IS NULL "
            + "AND EXISTS (SELECT 1 FROM ProductImage img WHERE img.product = p AND img.publicId IS NOT NULL)")
    List<EmbeddingRow> findActiveEmbeddings();

    @Query("SELECT e.product.id AS productId, e.contentHash AS contentHash, e.model AS model, e.dim AS dim "
            + "FROM ProductEmbedding e")
    List<EmbeddingMetaRow> findAllMeta();

    interface EmbeddingRow {
        Long getProductId();
        Integer getDim();
        byte[] getEmbedding();
    }

    interface EmbeddingMetaRow {
        Long getProductId();
        String getContentHash();
        String getModel();
        Integer getDim();
    }
}
