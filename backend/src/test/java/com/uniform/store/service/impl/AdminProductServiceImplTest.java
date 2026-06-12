package com.uniform.store.service.impl;

import com.uniform.store.dto.request.AdminProductFilterRequest;
import com.uniform.store.dto.request.CreateProductRequest;
import com.uniform.store.dto.request.UpdateProductRequest;
import com.uniform.store.dto.response.AdminProductDetailDto;
import com.uniform.store.entity.Brand;
import com.uniform.store.entity.Category;
import com.uniform.store.entity.Product;
import com.uniform.store.enums.Gender;
import com.uniform.store.enums.SaleType;
import com.uniform.store.exception.BadRequestException;
import com.uniform.store.exception.ResourceNotFoundException;
import com.uniform.store.entity.ProductImage;
import com.uniform.store.entity.ProductTranslation;
import com.uniform.store.entity.ProductVariant;
import com.uniform.store.repository.BrandRepository;
import com.uniform.store.repository.CategoryRepository;
import com.uniform.store.repository.OrderItemRepository;
import com.uniform.store.repository.ProductImageRepository;
import com.uniform.store.repository.ProductRepository;
import com.uniform.store.repository.ProductTranslationRepository;
import com.uniform.store.repository.ProductVariantRepository;
import com.uniform.store.service.CloudinaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminProductServiceImplTest {

    @Mock private ProductRepository productRepository;
    @Mock private ProductVariantRepository variantRepository;
    @Mock private ProductImageRepository imageRepository;
    @Mock private ProductTranslationRepository translationRepository;
    @Mock private BrandRepository brandRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private CloudinaryService cloudinaryService;

    @InjectMocks private AdminProductServiceImpl service;

    private Brand brand;
    private Category category;
    private Product product;

    @BeforeEach
    void setUp() {
        brand = Brand.builder().slug("nike").name("Nike").isActive(true).build();
        brand.setId(10L);
        category = Category.builder().slug("tees").name("Tees").sortOrder(0).isActive(true).build();
        category.setId(20L);
        product = Product.builder()
                .brand(brand).category(category)
                .slug("essential-tee").name("Essential Tee")
                .gender(Gender.UNISEX).basePrice(new BigDecimal("250000")).currency("VND")
                .isActive(true)
                .build();
        product.setId(100L);
    }

    @Test
    void create_duplicateSlug_throwsBadRequest() {
        CreateProductRequest req = new CreateProductRequest();
        req.setSlug("essential-tee");
        req.setName("X"); req.setBrandId(10L); req.setCategoryId(20L);
        req.setGender(Gender.UNISEX); req.setBasePrice(new BigDecimal("100"));

        when(productRepository.existsBySlug("essential-tee")).thenReturn(true);

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("slug already exists");
        verify(productRepository, never()).save(any());
    }

    @Test
    void create_missingBrand_throwsNotFound() {
        CreateProductRequest req = new CreateProductRequest();
        req.setSlug("new-tee"); req.setName("New"); req.setBrandId(999L); req.setCategoryId(20L);
        req.setGender(Gender.MEN); req.setBasePrice(new BigDecimal("100"));

        when(productRepository.existsBySlug("new-tee")).thenReturn(false);
        when(brandRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_validRequest_persistsAndReturnsDetail() {
        CreateProductRequest req = new CreateProductRequest();
        req.setSlug("new-tee"); req.setName("New Tee");
        req.setBrandId(10L); req.setCategoryId(20L);
        req.setGender(Gender.MEN); req.setBasePrice(new BigDecimal("100000"));
        req.setDescription("Soft cotton");

        when(productRepository.existsBySlug("new-tee")).thenReturn(false);
        when(brandRepository.findById(10L)).thenReturn(Optional.of(brand));
        when(categoryRepository.findById(20L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(101L);
            return p;
        });

        AdminProductDetailDto dto = service.create(req);

        assertThat(dto.getId()).isEqualTo(101L);
        assertThat(dto.getSlug()).isEqualTo("new-tee");
        assertThat(dto.getGender()).isEqualTo(Gender.MEN);
        assertThat(dto.getBrand().getName()).isEqualTo("Nike");
        assertThat(dto.getCategory().getName()).isEqualTo("Tees");
        assertThat(dto.getVariants()).isEmpty();
        assertThat(dto.getImages()).isEmpty();
    }

    @Test
    void softDelete_setsDeletedAtAndDeactivates() {
        when(productRepository.findById(100L)).thenReturn(Optional.of(product));

        service.softDelete(100L);

        assertThat(product.getDeletedAt()).isNotNull();
        assertThat(product.getIsActive()).isFalse();
    }

    @Test
    void softDelete_alreadyDeleted_throwsBadRequest() {
        product.setDeletedAt(java.time.Instant.now());
        when(productRepository.findById(100L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> service.softDelete(100L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already deleted");
    }

    @Test
    void restore_notDeleted_throwsBadRequest() {
        when(productRepository.findById(100L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> service.restore(100L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not deleted");
    }

    @Test
    void restore_clearsDeletedAt() {
        product.setDeletedAt(java.time.Instant.now());
        when(productRepository.findById(100L)).thenReturn(Optional.of(product));
        when(variantRepository.findByProductIdOrderBySizeAscColorAsc(100L)).thenReturn(List.of());
        when(imageRepository.findByProductIdOrderByIsPrimaryDescSortOrderAsc(100L)).thenReturn(List.of());

        AdminProductDetailDto dto = service.restore(100L);

        assertThat(product.getDeletedAt()).isNull();
        assertThat(dto.getDeletedAt()).isNull();
    }

    @Test
    void list_invalidDeletedFilter_throwsBadRequest() {
        AdminProductFilterRequest filter = new AdminProductFilterRequest();
        filter.setDeleted("garbage");

        assertThatThrownBy(() -> service.list(filter, PageRequest.of(0, 10)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid deleted filter");
    }

    @Test
    void list_includesPrimaryImageAndVariantCount() {
        AdminProductFilterRequest filter = new AdminProductFilterRequest();
        Page<Product> page = new PageImpl<>(List.of(product));
        when(productRepository.searchAdmin(any(), any(), any(), any(), any(), eq(false), eq(false), any(Pageable.class)))
                .thenReturn(page);
        when(imageRepository.findThumbnailCandidatesByProductIds(List.of(100L))).thenReturn(List.of());
        when(variantRepository.findByProductIdOrderBySizeAscColorAsc(100L)).thenReturn(List.of());

        var resp = service.list(filter, PageRequest.of(0, 10));

        assertThat(resp.getContent()).hasSize(1);
        assertThat(resp.getContent().get(0).getId()).isEqualTo(100L);
        assertThat(resp.getContent().get(0).getVariantCount()).isEqualTo(0L);
    }

    @Test
    void hardDelete_notSoftDeleted_throwsBadRequest() {
        when(productRepository.findById(100L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> service.hardDelete(100L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("soft-deleted first");
        verify(productRepository, never()).delete(any());
    }

    @Test
    void hardDelete_variantReferencedByOrder_throwsBadRequest() {
        product.setDeletedAt(java.time.Instant.now());
        ProductVariant variant = ProductVariant.builder()
                .product(product).sku("UNI-1").size("M").color("Black").stockQuantity(0).isActive(false).build();
        variant.setId(500L);

        when(productRepository.findById(100L)).thenReturn(Optional.of(product));
        when(variantRepository.findByProductIdOrderBySizeAscColorAsc(100L)).thenReturn(List.of(variant));
        when(orderItemRepository.existsByVariantId(500L)).thenReturn(true);

        assertThatThrownBy(() -> service.hardDelete(100L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("UNI-1");
        verify(productRepository, never()).delete(any());
    }

    @Test
    void hardDelete_cleanCascade_callsCloudinaryForEachImageWithPublicId() {
        product.setDeletedAt(java.time.Instant.now());
        ProductImage img1 = ProductImage.builder()
                .product(product).url("u1").publicId("pub-1").sortOrder(0).isPrimary(true).build();
        img1.setId(700L);
        ProductImage img2 = ProductImage.builder()
                .product(product).url("u2").publicId(null).sortOrder(1).isPrimary(false).build();
        img2.setId(701L);

        when(productRepository.findById(100L)).thenReturn(Optional.of(product));
        when(variantRepository.findByProductIdOrderBySizeAscColorAsc(100L)).thenReturn(List.of());
        when(imageRepository.findByProductIdOrderByIsPrimaryDescSortOrderAsc(100L)).thenReturn(List.of(img1, img2));

        service.hardDelete(100L);

        verify(productRepository).delete(product);
        verify(cloudinaryService).deleteByPublicId("pub-1");
        verify(cloudinaryService, never()).deleteByPublicId(null);
    }

    @Test
    void update_changesNameAndDescription() {
        UpdateProductRequest req = new UpdateProductRequest();
        req.setName("Renamed Tee");
        req.setDescription("Updated description");
        when(productRepository.findById(100L)).thenReturn(Optional.of(product));
        when(variantRepository.findByProductIdOrderBySizeAscColorAsc(100L)).thenReturn(List.of());
        when(imageRepository.findByProductIdOrderByIsPrimaryDescSortOrderAsc(100L)).thenReturn(List.of());

        AdminProductDetailDto dto = service.update(100L, req);

        assertThat(product.getName()).isEqualTo("Renamed Tee");
        assertThat(product.getDescription()).isEqualTo("Updated description");
        assertThat(dto.getName()).isEqualTo("Renamed Tee");
    }

    @Test
    void create_withPercentSale_persistsSaleFields() {
        CreateProductRequest req = new CreateProductRequest();
        req.setSlug("sale-tee"); req.setName("Sale Tee");
        req.setBrandId(10L); req.setCategoryId(20L);
        req.setGender(Gender.UNISEX); req.setBasePrice(new BigDecimal("250000"));
        req.setSaleType(SaleType.PERCENT); req.setSaleValue(new BigDecimal("30"));

        when(productRepository.existsBySlug("sale-tee")).thenReturn(false);
        when(brandRepository.findById(10L)).thenReturn(Optional.of(brand));
        when(categoryRepository.findById(20L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0); p.setId(102L); return p;
        });

        AdminProductDetailDto dto = service.create(req);

        assertThat(dto.getSaleType()).isEqualTo(SaleType.PERCENT);
        assertThat(dto.getSaleValue()).isEqualByComparingTo("30");
    }

    @Test
    void create_percentSaleOver100_throwsBadRequest() {
        CreateProductRequest req = new CreateProductRequest();
        req.setSlug("bad-sale"); req.setName("Bad"); req.setBrandId(10L); req.setCategoryId(20L);
        req.setGender(Gender.UNISEX); req.setBasePrice(new BigDecimal("100000"));
        req.setSaleType(SaleType.PERCENT); req.setSaleValue(new BigDecimal("150"));

        when(productRepository.existsBySlug("bad-sale")).thenReturn(false);
        when(brandRepository.findById(10L)).thenReturn(Optional.of(brand));
        when(categoryRepository.findById(20L)).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cannot exceed 100");
        verify(productRepository, never()).save(any());
    }

    @Test
    void create_saleValueWithoutType_throwsBadRequest() {
        CreateProductRequest req = new CreateProductRequest();
        req.setSlug("orphan-value"); req.setName("X"); req.setBrandId(10L); req.setCategoryId(20L);
        req.setGender(Gender.UNISEX); req.setBasePrice(new BigDecimal("100000"));
        req.setSaleValue(new BigDecimal("30"));

        when(productRepository.existsBySlug("orphan-value")).thenReturn(false);
        when(brandRepository.findById(10L)).thenReturn(Optional.of(brand));
        when(categoryRepository.findById(20L)).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("saleType is required");
        verify(productRepository, never()).save(any());
    }

    @Test
    void create_saleEndsBeforeStarts_throwsBadRequest() {
        Instant start = Instant.now();
        CreateProductRequest req = new CreateProductRequest();
        req.setSlug("bad-window"); req.setName("X"); req.setBrandId(10L); req.setCategoryId(20L);
        req.setGender(Gender.UNISEX); req.setBasePrice(new BigDecimal("100000"));
        req.setSaleType(SaleType.PERCENT); req.setSaleValue(new BigDecimal("30"));
        req.setSaleStartsAt(start); req.setSaleEndsAt(start.minusSeconds(3600));

        when(productRepository.existsBySlug("bad-window")).thenReturn(false);
        when(brandRepository.findById(10L)).thenReturn(Optional.of(brand));
        when(categoryRepository.findById(20L)).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("must be after");
        verify(productRepository, never()).save(any());
    }

    @Test
    void update_clearSale_nullsSaleFields() {
        product.setSaleType(SaleType.PERCENT);
        product.setSaleValue(new BigDecimal("30"));
        UpdateProductRequest req = new UpdateProductRequest();
        req.setClearSale(true);

        when(productRepository.findById(100L)).thenReturn(Optional.of(product));
        when(variantRepository.findByProductIdOrderBySizeAscColorAsc(100L)).thenReturn(List.of());
        when(imageRepository.findByProductIdOrderByIsPrimaryDescSortOrderAsc(100L)).thenReturn(List.of());

        service.update(100L, req);

        assertThat(product.getSaleType()).isNull();
        assertThat(product.getSaleValue()).isNull();
    }

    @Test
    void create_withTranslations_savesViAndJa() {
        CreateProductRequest req = new CreateProductRequest();
        req.setSlug("i18n-tee"); req.setName("Tee");
        req.setBrandId(10L); req.setCategoryId(20L);
        req.setGender(Gender.UNISEX); req.setBasePrice(new BigDecimal("100000"));
        req.setNameVi("Áo thun"); req.setDescriptionVi("Cotton mềm");
        req.setNameJa("Tシャツ");

        when(productRepository.existsBySlug("i18n-tee")).thenReturn(false);
        when(brandRepository.findById(10L)).thenReturn(Optional.of(brand));
        when(categoryRepository.findById(20L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0); p.setId(103L); return p;
        });
        when(translationRepository.findByProductIdAndLocale(103L, "vi")).thenReturn(Optional.empty());
        when(translationRepository.findByProductIdAndLocale(103L, "ja")).thenReturn(Optional.empty());

        AdminProductDetailDto dto = service.create(req);

        assertThat(dto.getNameVi()).isEqualTo("Áo thun");
        assertThat(dto.getDescriptionVi()).isEqualTo("Cotton mềm");
        assertThat(dto.getNameJa()).isEqualTo("Tシャツ");
        verify(translationRepository, org.mockito.Mockito.times(2)).save(any(ProductTranslation.class));
    }

    @Test
    void update_setsViTranslation() {
        UpdateProductRequest req = new UpdateProductRequest();
        req.setNameVi("Áo thun"); req.setDescriptionVi("Mô tả");
        when(productRepository.findById(100L)).thenReturn(Optional.of(product));
        when(variantRepository.findByProductIdOrderBySizeAscColorAsc(100L)).thenReturn(List.of());
        when(imageRepository.findByProductIdOrderByIsPrimaryDescSortOrderAsc(100L)).thenReturn(List.of());
        when(translationRepository.findByProductIdAndLocale(100L, "vi")).thenReturn(Optional.empty());

        service.update(100L, req);

        verify(translationRepository).save(any(ProductTranslation.class));
    }

    @Test
    void update_blankNameVi_deletesTranslation() {
        ProductTranslation existing = ProductTranslation.builder()
                .product(product).locale("vi").name("Áo cũ").build();
        UpdateProductRequest req = new UpdateProductRequest();
        req.setNameVi("");
        when(productRepository.findById(100L)).thenReturn(Optional.of(product));
        when(variantRepository.findByProductIdOrderBySizeAscColorAsc(100L)).thenReturn(List.of());
        when(imageRepository.findByProductIdOrderByIsPrimaryDescSortOrderAsc(100L)).thenReturn(List.of());
        when(translationRepository.findByProductIdAndLocale(100L, "vi")).thenReturn(Optional.of(existing));

        service.update(100L, req);

        verify(translationRepository).delete(existing);
    }

    @Test
    void update_setsSaleBlock() {
        UpdateProductRequest req = new UpdateProductRequest();
        req.setSaleType(SaleType.FIXED);
        req.setSaleValue(new BigDecimal("50000"));

        when(productRepository.findById(100L)).thenReturn(Optional.of(product));
        when(variantRepository.findByProductIdOrderBySizeAscColorAsc(100L)).thenReturn(List.of());
        when(imageRepository.findByProductIdOrderByIsPrimaryDescSortOrderAsc(100L)).thenReturn(List.of());

        AdminProductDetailDto dto = service.update(100L, req);

        assertThat(product.getSaleType()).isEqualTo(SaleType.FIXED);
        assertThat(product.getSaleValue()).isEqualByComparingTo("50000");
        assertThat(dto.getSaleType()).isEqualTo(SaleType.FIXED);
    }
}
