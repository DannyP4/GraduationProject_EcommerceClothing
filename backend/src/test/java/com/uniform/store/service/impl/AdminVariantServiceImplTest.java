package com.uniform.store.service.impl;

import com.uniform.store.dto.request.CreateVariantRequest;
import com.uniform.store.dto.request.UpdateVariantRequest;
import com.uniform.store.entity.Brand;
import com.uniform.store.entity.Category;
import com.uniform.store.entity.Product;
import com.uniform.store.entity.ProductVariant;
import com.uniform.store.enums.Gender;
import com.uniform.store.exception.BadRequestException;
import com.uniform.store.exception.ResourceNotFoundException;
import com.uniform.store.repository.OrderItemRepository;
import com.uniform.store.repository.ProductRepository;
import com.uniform.store.repository.ProductVariantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminVariantServiceImplTest {

    @Mock private ProductRepository productRepository;
    @Mock private ProductVariantRepository variantRepository;
    @Mock private OrderItemRepository orderItemRepository;

    @InjectMocks private AdminVariantServiceImpl service;

    private Product product;
    private ProductVariant existingVariant;

    @BeforeEach
    void setUp() {
        Brand brand = Brand.builder().slug("b").name("B").build();
        brand.setId(1L);
        Category category = Category.builder().slug("c").name("C").sortOrder(0).build();
        category.setId(2L);
        product = Product.builder()
                .brand(brand).category(category)
                .slug("p").name("P").gender(Gender.UNISEX)
                .basePrice(new BigDecimal("100")).currency("VND").isActive(true)
                .build();
        product.setId(50L);
        existingVariant = ProductVariant.builder()
                .product(product).sku("SKU-1").size("M").color("Black")
                .stockQuantity(10).isActive(true).build();
        existingVariant.setId(500L);
    }

    @Test
    void create_duplicateSku_throwsBadRequest() {
        CreateVariantRequest req = req("SKU-1", "S", "Red");
        when(productRepository.findById(50L)).thenReturn(Optional.of(product));
        when(variantRepository.existsBySku("SKU-1")).thenReturn(true);

        assertThatThrownBy(() -> service.create(50L, req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("SKU already exists");
        verify(variantRepository, never()).save(any());
    }

    @Test
    void create_duplicateSizeColor_throwsBadRequest() {
        CreateVariantRequest req = req("SKU-NEW", "M", "Black");
        when(productRepository.findById(50L)).thenReturn(Optional.of(product));
        when(variantRepository.existsBySku("SKU-NEW")).thenReturn(false);
        when(variantRepository.existsByProductIdAndSizeAndColor(50L, "M", "Black")).thenReturn(true);

        assertThatThrownBy(() -> service.create(50L, req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("size 'M' and color 'Black'");
    }

    @Test
    void create_valid_persists() {
        CreateVariantRequest req = req("SKU-2", "L", "Navy");
        req.setColorHex("#1F2A44");
        req.setStockQuantity(15);
        when(productRepository.findById(50L)).thenReturn(Optional.of(product));
        when(variantRepository.existsBySku("SKU-2")).thenReturn(false);
        when(variantRepository.existsByProductIdAndSizeAndColor(50L, "L", "Navy")).thenReturn(false);
        when(variantRepository.save(any(ProductVariant.class))).thenAnswer(inv -> {
            ProductVariant v = inv.getArgument(0);
            v.setId(501L);
            return v;
        });

        var dto = service.create(50L, req);

        assertThat(dto.getId()).isEqualTo(501L);
        assertThat(dto.getSku()).isEqualTo("SKU-2");
        assertThat(dto.getColorHex()).isEqualTo("#1F2A44");
        assertThat(dto.getStockQuantity()).isEqualTo(15);
    }

    @Test
    void update_sizeOrColorCollision_throwsBadRequest() {
        UpdateVariantRequest req = new UpdateVariantRequest();
        req.setSize("M");
        req.setColor("Red");
        when(variantRepository.findById(500L)).thenReturn(Optional.of(existingVariant));
        when(variantRepository.existsByProductIdAndSizeAndColor(50L, "M", "Red")).thenReturn(true);

        assertThatThrownBy(() -> service.update(500L, req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void update_stockOnly_doesNotCheckCollision() {
        UpdateVariantRequest req = new UpdateVariantRequest();
        req.setStockQuantity(99);
        when(variantRepository.findById(500L)).thenReturn(Optional.of(existingVariant));

        var dto = service.update(500L, req);

        assertThat(dto.getStockQuantity()).isEqualTo(99);
        verify(variantRepository, never()).existsByProductIdAndSizeAndColor(any(), any(), any());
    }

    @Test
    void delete_variantReferencedByOrder_throwsBadRequest() {
        when(variantRepository.findById(500L)).thenReturn(Optional.of(existingVariant));
        when(orderItemRepository.existsByVariantId(500L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(500L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("referenced by existing orders");
        verify(variantRepository, never()).delete(any());
    }

    @Test
    void delete_orphanVariant_succeeds() {
        when(variantRepository.findById(500L)).thenReturn(Optional.of(existingVariant));
        when(orderItemRepository.existsByVariantId(500L)).thenReturn(false);

        service.delete(500L);

        verify(variantRepository).delete(existingVariant);
    }

    @Test
    void listByProduct_productMissing_throwsNotFound() {
        when(productRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> service.listByProduct(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private static CreateVariantRequest req(String sku, String size, String color) {
        CreateVariantRequest r = new CreateVariantRequest();
        r.setSku(sku); r.setSize(size); r.setColor(color);
        r.setStockQuantity(0); r.setIsActive(true);
        return r;
    }
}
