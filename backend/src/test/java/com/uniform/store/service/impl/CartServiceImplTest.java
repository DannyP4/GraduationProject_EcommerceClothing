package com.uniform.store.service.impl;

import com.uniform.store.config.CartProperties;
import com.uniform.store.dto.request.AddCartItemRequest;
import com.uniform.store.dto.request.UpdateCartItemRequest;
import com.uniform.store.dto.response.CartDto;
import com.uniform.store.entity.Brand;
import com.uniform.store.entity.Cart;
import com.uniform.store.entity.CartItem;
import com.uniform.store.entity.Category;
import com.uniform.store.entity.Product;
import com.uniform.store.entity.ProductVariant;
import com.uniform.store.entity.Role;
import com.uniform.store.entity.User;
import com.uniform.store.enums.Gender;
import com.uniform.store.enums.UserStatus;
import com.uniform.store.exception.BadRequestException;
import com.uniform.store.repository.CartItemRepository;
import com.uniform.store.repository.CartRepository;
import com.uniform.store.repository.ProductImageRepository;
import com.uniform.store.repository.ProductVariantRepository;
import com.uniform.store.repository.UserRepository;
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
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock CartRepository cartRepository;
    @Mock CartItemRepository cartItemRepository;
    @Mock ProductVariantRepository variantRepository;
    @Mock ProductImageRepository imageRepository;

    CartProperties cartProperties;

    @InjectMocks CartServiceImpl cartService;

    User user;
    Cart cart;
    Product product;
    ProductVariant variant;

    @BeforeEach
    void setUp() {
        cartProperties = new CartProperties();
        cartService = new CartServiceImpl(
                userRepository, cartRepository, cartItemRepository,
                variantRepository, imageRepository, cartProperties);

        user = User.builder()
                .email("buyer@uniform.test")
                .passwordHash("hash")
                .fullName("Buyer")
                .preferredLocale("vi")
                .role(Role.builder().name("customer").displayName("Customer").build())
                .status(UserStatus.ACTIVE)
                .build();
        user.setId(1L);

        cart = Cart.builder().user(user).build();
        cart.setId(10L);

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

        variant = ProductVariant.builder()
                .product(product)
                .sku("ET-M-BLK")
                .size("M").color("Black").colorHex("#000000")
                .stockQuantity(20)
                .isActive(true)
                .build();
        variant.setId(400L);
    }

    @Test
    void addItem_newVariant_savesItemAtRequestedQuantity() {
        when(userRepository.findByEmail("buyer@uniform.test")).thenReturn(Optional.of(user));
        when(variantRepository.findById(400L)).thenReturn(Optional.of(variant));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndVariantId(10L, 400L)).thenReturn(Optional.empty());
        when(cartItemRepository.findByCartIdOrderByIdAsc(10L)).thenReturn(List.of());

        cartService.addItem("buyer@uniform.test", new AddCartItemRequest(400L, 2));

        ArgumentCaptor<CartItem> saved = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).save(saved.capture());
        assertThat(saved.getValue().getQuantity()).as("new line uses requested qty").isEqualTo(2);
        assertThat(saved.getValue().getCart().getId()).isEqualTo(10L);
        assertThat(saved.getValue().getVariant().getId()).isEqualTo(400L);
    }

    @Test
    void addItem_sameVariantTwice_incrementsExistingLineQuantity() {
        CartItem existing = CartItem.builder().cart(cart).variant(variant).quantity(3).build();
        existing.setId(50L);

        when(userRepository.findByEmail("buyer@uniform.test")).thenReturn(Optional.of(user));
        when(variantRepository.findById(400L)).thenReturn(Optional.of(variant));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndVariantId(10L, 400L)).thenReturn(Optional.of(existing));
        when(cartItemRepository.findByCartIdOrderByIdAsc(10L)).thenReturn(List.of(existing));
        when(variantRepository.findAllByIdInWithProduct(anyCollection())).thenReturn(List.of(variant));
        when(imageRepository.findThumbnailCandidatesByProductIds(anyCollection())).thenReturn(List.of());

        cartService.addItem("buyer@uniform.test", new AddCartItemRequest(400L, 4));

        assertThat(existing.getQuantity()).as("3 + 4 = 7").isEqualTo(7);
        verify(cartItemRepository).save(existing);
        verify(cartItemRepository, never()).delete(any());
    }

    @Test
    void addItem_quantityAboveStock_throwsBadRequest() {
        when(userRepository.findByEmail("buyer@uniform.test")).thenReturn(Optional.of(user));
        when(variantRepository.findById(400L)).thenReturn(Optional.of(variant));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndVariantId(10L, 400L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                cartService.addItem("buyer@uniform.test", new AddCartItemRequest(400L, 50)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("exceeds available stock");

        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void addItem_quantityAbovePerItemCap_throwsBadRequest() {
        variant.setStockQuantity(500);

        when(userRepository.findByEmail("buyer@uniform.test")).thenReturn(Optional.of(user));
        when(variantRepository.findById(400L)).thenReturn(Optional.of(variant));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndVariantId(10L, 400L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                cartService.addItem("buyer@uniform.test", new AddCartItemRequest(400L, 100)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("per-item limit");

        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void updateItem_quantityZero_deletesLine() {
        CartItem existing = CartItem.builder().cart(cart).variant(variant).quantity(3).build();
        existing.setId(50L);

        when(userRepository.findByEmail("buyer@uniform.test")).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByIdAndCartId(50L, 10L)).thenReturn(Optional.of(existing));
        when(cartItemRepository.findByCartIdOrderByIdAsc(10L)).thenReturn(List.of());

        UpdateCartItemRequest req = new UpdateCartItemRequest();
        req.setQuantity(0);
        CartDto result = cartService.updateItem("buyer@uniform.test", 50L, req);

        verify(cartItemRepository).delete(existing);
        verify(cartItemRepository, never()).save(any());
        assertThat(result.getItems()).isEmpty();
    }
}
