package com.uniform.store.service.impl;

import com.uniform.store.entity.Brand;
import com.uniform.store.entity.Category;
import com.uniform.store.entity.Product;
import com.uniform.store.enums.Gender;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProductDocumentBuilderTest {

    private final ProductDocumentBuilder builder = new ProductDocumentBuilder();

    private Product product(String name, Gender gender, String description) {
        return Product.builder()
                .name(name)
                .gender(gender)
                .description(description)
                .brand(Brand.builder().name("Nike").build())
                .category(Category.builder().name("Jackets").build())
                .build();
    }

    @Test
    void build_concatenatesSemanticAttributes_excludesPrice() {
        Product p = product("Black Wool Coat", Gender.MEN, "Warm winter coat");
        String doc = builder.build(p, List.of("Black", "Navy"));

        assertThat(doc)
                .contains("Black Wool Coat")
                .contains("For men")
                .contains("Category: Jackets")
                .contains("Brand: Nike")
                .contains("Colors: Black, Navy")
                .contains("Warm winter coat");
        assertThat(doc).doesNotContain("Price");
    }

    @Test
    void build_handlesNullColorsAndBlankDescription() {
        Product p = product("Plain Tee", Gender.UNISEX, "  ");
        String doc = builder.build(p, null);

        assertThat(doc).contains("Plain Tee").contains("Unisex");
        assertThat(doc).doesNotContain("Colors:");
    }

    @Test
    void contentHash_isDeterministic_forSameInput() {
        String a = builder.contentHash("gemini-embedding-001", 3072, "doc");
        String b = builder.contentHash("gemini-embedding-001", 3072, "doc");
        assertThat(a).isEqualTo(b).hasSize(64);
    }

    @Test
    void contentHash_changesWhenDocumentModelOrDimChanges() {
        String base = builder.contentHash("gemini-embedding-001", 3072, "doc");
        assertThat(builder.contentHash("gemini-embedding-001", 3072, "doc2")).isNotEqualTo(base);
        assertThat(builder.contentHash("other-model", 3072, "doc")).isNotEqualTo(base);
        assertThat(builder.contentHash("gemini-embedding-001", 768, "doc")).isNotEqualTo(base);
    }
}
