package com.uniform.store.service.impl;

import com.uniform.store.dto.response.ProductSummaryDto;
import com.uniform.store.dto.response.WishlistToggleResponse;
import com.uniform.store.entity.Brand;
import com.uniform.store.entity.Category;
import com.uniform.store.entity.Product;
import com.uniform.store.entity.Role;
import com.uniform.store.entity.User;
import com.uniform.store.entity.Wishlist;
import com.uniform.store.enums.Gender;
import com.uniform.store.enums.UserStatus;
import com.uniform.store.exception.BadRequestException;
import com.uniform.store.exception.ResourceNotFoundException;
import com.uniform.store.repository.ProductRepository;
import com.uniform.store.repository.UserRepository;
import com.uniform.store.repository.WishlistRepository;
import com.uniform.store.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WishlistServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock ProductRepository productRepository;
    @Mock WishlistRepository wishlistRepository;
    @Mock ProductService productService;

    @InjectMocks WishlistServiceImpl wishlistService;

    User user;
    Product product;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .email("buyer@uniform.test")
                .passwordHash("hash")
                .fullName("Buyer")
                .preferredLocale("vi")
                .role(Role.builder().name("customer").displayName("Customer").build())
                .status(UserStatus.ACTIVE)
                .build();
        user.setId(1L);

        Brand brand = Brand.builder().slug("uniform").name("UNIFORM").build();
        brand.setId(100L);
        Category category = Category.builder().slug("tee").name("Tee").build();
        category.setId(200L);

        product = Product.builder()
                .brand(brand).category(category)
                .slug("essential-tee").name("Essential Tee")
                .gender(Gender.UNISEX)
                .basePrice(new BigDecimal("250000"))
                .currency("VND")
                .isActive(true)
                .build();
        product.setId(300L);
    }

    @Test
    void toggle_whenAbsent_addsAndReturnsWishlistedTrue() {
        when(userRepository.findByEmail("buyer@uniform.test")).thenReturn(Optional.of(user));
        when(wishlistRepository.findByUserIdAndProductId(1L, 300L)).thenReturn(Optional.empty());
        when(productRepository.findById(300L)).thenReturn(Optional.of(product));

        WishlistToggleResponse res = wishlistService.toggle("buyer@uniform.test", 300L);

        assertThat(res.isWishlisted()).isTrue();
        assertThat(res.getProductId()).isEqualTo(300L);

        ArgumentCaptor<Wishlist> saved = ArgumentCaptor.forClass(Wishlist.class);
        verify(wishlistRepository).save(saved.capture());
        assertThat(saved.getValue().getUser().getId()).isEqualTo(1L);
        assertThat(saved.getValue().getProduct().getId()).isEqualTo(300L);
        verify(wishlistRepository, never()).delete(any());
    }

    @Test
    void toggle_whenPresent_removesAndReturnsWishlistedFalse() {
        Wishlist existing = Wishlist.builder().user(user).product(product).build();
        existing.setId(55L);

        when(userRepository.findByEmail("buyer@uniform.test")).thenReturn(Optional.of(user));
        when(wishlistRepository.findByUserIdAndProductId(1L, 300L)).thenReturn(Optional.of(existing));

        WishlistToggleResponse res = wishlistService.toggle("buyer@uniform.test", 300L);

        assertThat(res.isWishlisted()).isFalse();
        verify(wishlistRepository).delete(existing);
        verify(wishlistRepository, never()).save(any());
    }

    @Test
    void toggle_inactiveProduct_throwsBadRequest() {
        product.setIsActive(false);

        when(userRepository.findByEmail("buyer@uniform.test")).thenReturn(Optional.of(user));
        when(wishlistRepository.findByUserIdAndProductId(1L, 300L)).thenReturn(Optional.empty());
        when(productRepository.findById(300L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> wishlistService.toggle("buyer@uniform.test", 300L))
                .isInstanceOf(BadRequestException.class);

        verify(wishlistRepository, never()).save(any());
    }

    @Test
    void toggle_unknownProduct_throwsResourceNotFound() {
        when(userRepository.findByEmail("buyer@uniform.test")).thenReturn(Optional.of(user));
        when(wishlistRepository.findByUserIdAndProductId(1L, 300L)).thenReturn(Optional.empty());
        when(productRepository.findById(300L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> wishlistService.toggle("buyer@uniform.test", 300L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(wishlistRepository, never()).save(any());
    }

    @Test
    void getWishlist_delegatesActiveIdsToProductService() {
        List<Long> ids = List.of(300L, 301L);
        List<ProductSummaryDto> cards = List.of(ProductSummaryDto.builder().id(300L).build());

        when(userRepository.findByEmail("buyer@uniform.test")).thenReturn(Optional.of(user));
        when(wishlistRepository.findActiveProductIdsByUserId(1L)).thenReturn(ids);
        when(productService.getSummariesByIds(ids, "vi")).thenReturn(cards);

        List<ProductSummaryDto> result = wishlistService.getWishlist("buyer@uniform.test", "vi");

        assertThat(result).isSameAs(cards);
        verify(productService).getSummariesByIds(ids, "vi");
    }

    @Test
    void remove_whenAbsent_isNoop() {
        when(userRepository.findByEmail("buyer@uniform.test")).thenReturn(Optional.of(user));
        when(wishlistRepository.findByUserIdAndProductId(1L, 300L)).thenReturn(Optional.empty());

        wishlistService.remove("buyer@uniform.test", 300L);

        verify(wishlistRepository, never()).delete(any());
    }

    @Test
    void clear_removesAllForUser() {
        when(userRepository.findByEmail("buyer@uniform.test")).thenReturn(Optional.of(user));

        wishlistService.clear("buyer@uniform.test");

        verify(wishlistRepository).deleteAllByUserId(1L);
    }
}
