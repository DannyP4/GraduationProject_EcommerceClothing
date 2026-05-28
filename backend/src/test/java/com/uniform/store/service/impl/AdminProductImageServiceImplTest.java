package com.uniform.store.service.impl;

import com.uniform.store.dto.request.CreateProductImageRequest;
import com.uniform.store.dto.request.UpdateProductImageRequest;
import com.uniform.store.entity.Brand;
import com.uniform.store.entity.Category;
import com.uniform.store.entity.Product;
import com.uniform.store.entity.ProductImage;
import com.uniform.store.entity.ProductVariant;
import com.uniform.store.enums.Gender;
import com.uniform.store.exception.BadRequestException;
import com.uniform.store.exception.ResourceNotFoundException;
import com.uniform.store.repository.ProductImageRepository;
import com.uniform.store.repository.ProductRepository;
import com.uniform.store.repository.ProductVariantRepository;
import com.uniform.store.service.CloudinaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminProductImageServiceImplTest {

    @Mock private ProductRepository productRepository;
    @Mock private ProductImageRepository imageRepository;
    @Mock private ProductVariantRepository variantRepository;
    @Mock private CloudinaryService cloudinaryService;

    @InjectMocks private AdminProductImageServiceImpl service;

    private Product product;

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
    }

    @Test
    void create_firstImage_isAutoPrimary() {
        CreateProductImageRequest req = new CreateProductImageRequest();
        req.setUrl("https://cdn/test.jpg");
        req.setIsPrimary(false);
        when(productRepository.findById(50L)).thenReturn(Optional.of(product));
        when(imageRepository.countByProductId(50L)).thenReturn(0L);
        when(imageRepository.save(any(ProductImage.class))).thenAnswer(inv -> {
            ProductImage img = inv.getArgument(0);
            img.setId(700L);
            return img;
        });

        var dto = service.create(50L, req);

        assertThat(dto.getIsPrimary()).isTrue();
    }

    @Test
    void create_secondImageRequestedPrimary_demotesExisting() {
        ProductImage existing = ProductImage.builder()
                .product(product).url("u1").sortOrder(0).isPrimary(true).build();
        existing.setId(700L);

        CreateProductImageRequest req = new CreateProductImageRequest();
        req.setUrl("u2"); req.setIsPrimary(true);

        when(productRepository.findById(50L)).thenReturn(Optional.of(product));
        when(imageRepository.countByProductId(50L)).thenReturn(1L);
        when(imageRepository.findByProductIdOrderByIsPrimaryDescSortOrderAsc(50L)).thenReturn(new ArrayList<>(List.of(existing)));
        when(imageRepository.save(any(ProductImage.class))).thenAnswer(inv -> {
            ProductImage img = inv.getArgument(0);
            img.setId(701L);
            return img;
        });

        service.create(50L, req);

        assertThat(existing.getIsPrimary()).isFalse();
    }

    @Test
    void create_atMaxImageLimit_throwsBadRequest() {
        CreateProductImageRequest req = new CreateProductImageRequest();
        req.setUrl("u");
        when(productRepository.findById(50L)).thenReturn(Optional.of(product));
        when(imageRepository.countByProductId(50L)).thenReturn(8L);

        assertThatThrownBy(() -> service.create(50L, req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("max");
        verify(imageRepository, never()).save(any());
    }

    @Test
    void create_variantOfDifferentProduct_throwsBadRequest() {
        Brand otherBrand = Brand.builder().slug("ob").name("OB").build();
        otherBrand.setId(11L);
        Category otherCat = Category.builder().slug("oc").name("OC").sortOrder(0).build();
        otherCat.setId(12L);
        Product otherProduct = Product.builder()
                .brand(otherBrand).category(otherCat)
                .slug("op").name("OP").gender(Gender.MEN)
                .basePrice(new BigDecimal("1")).currency("VND").isActive(true).build();
        otherProduct.setId(99L);
        ProductVariant foreignVariant = ProductVariant.builder()
                .product(otherProduct).sku("X").size("M").color("Red").stockQuantity(1).isActive(true).build();
        foreignVariant.setId(900L);

        CreateProductImageRequest req = new CreateProductImageRequest();
        req.setUrl("u"); req.setVariantId(900L);
        when(productRepository.findById(50L)).thenReturn(Optional.of(product));
        when(imageRepository.countByProductId(50L)).thenReturn(0L);
        when(variantRepository.findById(900L)).thenReturn(Optional.of(foreignVariant));

        assertThatThrownBy(() -> service.create(50L, req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("does not belong to product");
    }

    @Test
    void delete_callsCloudinaryWhenPublicIdPresent() {
        ProductImage image = ProductImage.builder()
                .product(product).url("u").publicId("uniform/products/x-123").sortOrder(0).isPrimary(false).build();
        image.setId(700L);
        when(imageRepository.findById(700L)).thenReturn(Optional.of(image));

        service.delete(700L);

        verify(imageRepository).delete(image);
        verify(cloudinaryService).deleteByPublicId("uniform/products/x-123");
    }

    @Test
    void delete_swallowsCloudinaryError() {
        ProductImage image = ProductImage.builder()
                .product(product).url("u").publicId("orphan").sortOrder(0).isPrimary(false).build();
        image.setId(700L);
        when(imageRepository.findById(700L)).thenReturn(Optional.of(image));
        org.mockito.Mockito.doThrow(new RuntimeException("Cloudinary 404"))
                .when(cloudinaryService).deleteByPublicId("orphan");

        service.delete(700L);

        verify(imageRepository).delete(image);
    }

    @Test
    void delete_primaryImage_promotesNext() {
        ProductImage primary = ProductImage.builder()
                .product(product).url("u1").sortOrder(0).isPrimary(true).build();
        primary.setId(700L);
        ProductImage next = ProductImage.builder()
                .product(product).url("u2").sortOrder(1).isPrimary(false).build();
        next.setId(701L);

        when(imageRepository.findById(700L)).thenReturn(Optional.of(primary));
        when(imageRepository.findByProductIdOrderByIsPrimaryDescSortOrderAsc(50L))
                .thenReturn(new ArrayList<>(List.of(next)));

        service.delete(700L);

        assertThat(next.getIsPrimary()).isTrue();
    }

    @Test
    void update_setIsPrimaryTrue_demotesOtherAndPromotesSelf() {
        ProductImage other = ProductImage.builder()
                .product(product).url("u1").sortOrder(0).isPrimary(true).build();
        other.setId(700L);
        ProductImage target = ProductImage.builder()
                .product(product).url("u2").sortOrder(1).isPrimary(false).build();
        target.setId(701L);

        UpdateProductImageRequest req = new UpdateProductImageRequest();
        req.setIsPrimary(true);
        when(imageRepository.findById(701L)).thenReturn(Optional.of(target));
        when(imageRepository.findByProductIdOrderByIsPrimaryDescSortOrderAsc(50L))
                .thenReturn(new ArrayList<>(List.of(other, target)));

        service.update(701L, req);

        assertThat(other.getIsPrimary()).isFalse();
        assertThat(target.getIsPrimary()).isTrue();
    }

    @Test
    void update_clearVariantFlag_setsVariantNull() {
        ProductVariant existingVariant = ProductVariant.builder()
                .product(product).sku("S").size("M").color("B").stockQuantity(0).isActive(true).build();
        existingVariant.setId(444L);
        ProductImage image = ProductImage.builder()
                .product(product).variant(existingVariant).url("u").sortOrder(0).isPrimary(false).build();
        image.setId(701L);

        UpdateProductImageRequest req = new UpdateProductImageRequest();
        req.setClearVariant(true);
        when(imageRepository.findById(701L)).thenReturn(Optional.of(image));

        service.update(701L, req);

        assertThat(image.getVariant()).isNull();
    }

    @Test
    void listByProduct_productMissing_throwsNotFound() {
        when(productRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> service.listByProduct(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
