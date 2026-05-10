package com.uniform.store.service.impl;

import com.uniform.store.config.CartProperties;
import com.uniform.store.dto.request.AddCartItemRequest;
import com.uniform.store.dto.request.MergeCartRequest;
import com.uniform.store.dto.request.UpdateCartItemRequest;
import com.uniform.store.dto.response.CartDto;
import com.uniform.store.dto.response.CartItemDto;
import com.uniform.store.entity.Cart;
import com.uniform.store.entity.CartItem;
import com.uniform.store.entity.Product;
import com.uniform.store.entity.ProductImage;
import com.uniform.store.entity.ProductVariant;
import com.uniform.store.entity.User;
import com.uniform.store.enums.StockStatus;
import com.uniform.store.exception.BadRequestException;
import com.uniform.store.exception.ResourceNotFoundException;
import com.uniform.store.repository.CartItemRepository;
import com.uniform.store.repository.CartRepository;
import com.uniform.store.repository.ProductImageRepository;
import com.uniform.store.repository.ProductVariantRepository;
import com.uniform.store.repository.UserRepository;
import com.uniform.store.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartServiceImpl implements CartService {

    private static final String DEFAULT_CURRENCY = "VND";

    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductImageRepository imageRepository;
    private final CartProperties cartProperties;

    @Override
    public CartDto getCart(String email) {
        User user = loadUser(email);
        // Cart row is lazily created on first add - read returns an empty DTO
        return cartRepository.findByUserId(user.getId())
                .map(this::buildCartDto)
                .orElseGet(this::emptyCartDto);
    }

    @Override
    @Transactional
    public CartDto addItem(String email, AddCartItemRequest req) {
        User user = loadUser(email);
        ProductVariant variant = loadAvailableVariant(req.getVariantId());
        Cart cart = getOrCreateCart(user);

        Optional<CartItem> existingOpt = cartItemRepository
                .findByCartIdAndVariantId(cart.getId(), variant.getId());
        int currentQty = existingOpt.map(CartItem::getQuantity).orElse(0);
        int newQty = currentQty + req.getQuantity();
        validateQuantity(newQty, variant);

        CartItem item = existingOpt.orElseGet(() -> CartItem.builder()
                .cart(cart)
                .variant(variant)
                .build());
        item.setQuantity(newQty);
        cartItemRepository.save(item);

        return buildCartDto(cart);
    }

    @Override
    @Transactional
    public CartDto updateItem(String email, Long itemId, UpdateCartItemRequest req) {
        User user = loadUser(email);
        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", itemId));

        // The (id, cartId) lookup doubles as the ownership check.
        CartItem item = cartItemRepository.findByIdAndCartId(itemId, cart.getId())
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", itemId));

        if (req.getQuantity() == 0) {
            cartItemRepository.delete(item);
            return buildCartDto(cart);
        }

        // Re-check against current stock may have dropped since the item was added.
        validateQuantity(req.getQuantity(), item.getVariant());
        item.setQuantity(req.getQuantity());
        cartItemRepository.save(item);

        return buildCartDto(cart);
    }

    @Override
    @Transactional
    public CartDto removeItem(String email, Long itemId) {
        User user = loadUser(email);
        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", itemId));

        CartItem item = cartItemRepository.findByIdAndCartId(itemId, cart.getId())
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", itemId));

        cartItemRepository.delete(item);
        return buildCartDto(cart);
    }

    @Override
    @Transactional
    public CartDto clearCart(String email) {
        User user = loadUser(email);
        Optional<Cart> cartOpt = cartRepository.findByUserId(user.getId());
        if (cartOpt.isEmpty()) {
            return emptyCartDto();
        }
        Cart cart = cartOpt.get();
        // Keep the cart row to avoid row create/delete churn on every clear.
        cartItemRepository.deleteAllByCartId(cart.getId());
        return buildCartDto(cart);
    }

    @Override
    @Transactional
    public CartDto mergeCart(String email, MergeCartRequest req) {
        User user = loadUser(email);
        Cart cart = getOrCreateCart(user);

        // De-dup: same variantId sent twice → sum quantities once.
        Map<Long, Integer> incoming = new LinkedHashMap<>();
        for (MergeCartRequest.MergeItem in : req.getItems()) {
            incoming.merge(in.getVariantId(), in.getQuantity(), Integer::sum);
        }

        // Bulk-load variants with product so we can validate availability without N+1.
        Map<Long, ProductVariant> available = variantRepository
                .findAllByIdInWithProduct(incoming.keySet()).stream()
                .filter(this::isVariantAvailable)
                .collect(Collectors.toMap(ProductVariant::getId, Function.identity()));

        for (Map.Entry<Long, Integer> entry : incoming.entrySet()) {
            ProductVariant variant = available.get(entry.getKey());
            // Skip unknown/inactive variants silently
            if (variant == null) continue;

            Optional<CartItem> existingOpt = cartItemRepository
                    .findByCartIdAndVariantId(cart.getId(), variant.getId());
            int currentQty = existingOpt.map(CartItem::getQuantity).orElse(0);

            int merged = currentQty + entry.getValue();
            int cap = Math.min(safeStock(variant), cartProperties.getMaxQuantityPerItem());
            int capped = Math.min(merged, cap);
            if (capped <= 0) continue; // Out of stock — don't create a zero-qty row.

            CartItem item = existingOpt.orElseGet(() -> CartItem.builder()
                    .cart(cart)
                    .variant(variant)
                    .build());
            item.setQuantity(capped);
            cartItemRepository.save(item);
        }

        return buildCartDto(cart);
    }

    // helpers

    private User loadUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private ProductVariant loadAvailableVariant(Long variantId) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", variantId));
        if (!isVariantAvailable(variant)) {
            throw new BadRequestException("This item is no longer available");
        }
        return variant;
    }

    private boolean isVariantAvailable(ProductVariant variant) {
        if (Boolean.FALSE.equals(variant.getIsActive())) return false;
        Product product = variant.getProduct();
        if (Boolean.FALSE.equals(product.getIsActive())) return false;
        return product.getDeletedAt() == null;
    }

    private void validateQuantity(int qty, ProductVariant variant) {
        int stock = safeStock(variant);
        if (qty > stock) {
            throw new BadRequestException(
                    "Requested quantity (" + qty + ") exceeds available stock (" + stock + ")");
        }
        int max = cartProperties.getMaxQuantityPerItem();
        if (qty > max) {
            throw new BadRequestException("Quantity exceeds per-item limit (" + max + ")");
        }
    }

    private int safeStock(ProductVariant variant) {
        return variant.getStockQuantity() == null ? 0 : variant.getStockQuantity();
    }

    private Cart getOrCreateCart(User user) {
        return cartRepository.findByUserId(user.getId())
                .orElseGet(() -> cartRepository.save(Cart.builder().user(user).build()));
    }

    private CartDto emptyCartDto() {
        return CartDto.builder()
                .items(List.of())
                .itemCount(0)
                .subtotal(BigDecimal.ZERO)
                .currency(DEFAULT_CURRENCY)
                .build();
    }

    private CartDto buildCartDto(Cart cart) {
        List<CartItem> items = cartItemRepository.findByCartIdOrderByIdAsc(cart.getId());
        if (items.isEmpty()) {
            return CartDto.builder()
                    .id(cart.getId())
                    .items(List.of())
                    .itemCount(0)
                    .subtotal(BigDecimal.ZERO)
                    .currency(DEFAULT_CURRENCY)
                    .build();
        }

        List<Long> variantIds = items.stream().map(i -> i.getVariant().getId()).toList();
        Map<Long, ProductVariant> variantMap = variantRepository
                .findAllByIdInWithProduct(variantIds).stream()
                .collect(Collectors.toMap(ProductVariant::getId, Function.identity()));

        List<Long> productIds = variantMap.values().stream()
                .map(v -> v.getProduct().getId())
                .distinct()
                .toList();

        Map<Long, String> primaryImageUrls = new LinkedHashMap<>();
        for (ProductImage img : imageRepository.findThumbnailCandidatesByProductIds(productIds)) {
            primaryImageUrls.putIfAbsent(img.getProduct().getId(), img.getUrl());
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        String currency = DEFAULT_CURRENCY;
        List<CartItemDto> dtos = new ArrayList<>(items.size());
        int totalQty = 0;

        for (CartItem ci : items) {
            ProductVariant v = variantMap.get(ci.getVariant().getId());
            // Defensive: ON DELETE CASCADE should keep cart_items in sync, but guard anyway.
            if (v == null) {
                dtos.add(CartItemDto.builder()
                        .id(ci.getId())
                        .variantId(ci.getVariant().getId())
                        .quantity(ci.getQuantity())
                        .stockStatus(StockStatus.UNAVAILABLE)
                        .stockQuantity(0)
                        .build());
                totalQty += ci.getQuantity();
                continue;
            }

            Product p = v.getProduct();
            // Dynamic pricing: variant override wins, else product base — matches catalog rule.
            BigDecimal unitPrice = v.getPriceOverride() != null ? v.getPriceOverride() : p.getBasePrice();
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(ci.getQuantity()));
            currency = p.getCurrency();

            dtos.add(CartItemDto.builder()
                    .id(ci.getId())
                    .variantId(v.getId())
                    .sku(v.getSku())
                    .productId(p.getId())
                    .productSlug(p.getSlug())
                    .productName(p.getName())
                    .size(v.getSize())
                    .color(v.getColor())
                    .colorHex(v.getColorHex())
                    .imageUrl(primaryImageUrls.get(p.getId()))
                    .unitPrice(unitPrice)
                    .currency(p.getCurrency())
                    .quantity(ci.getQuantity())
                    .lineTotal(lineTotal)
                    .stockStatus(computeStockStatus(v, p))
                    .stockQuantity(safeStock(v))
                    .build());

            subtotal = subtotal.add(lineTotal);
            totalQty += ci.getQuantity();
        }

        return CartDto.builder()
                .id(cart.getId())
                .items(dtos)
                .itemCount(totalQty)
                .subtotal(subtotal)
                .currency(currency)
                .build();
    }

    private StockStatus computeStockStatus(ProductVariant v, Product p) {
        if (Boolean.FALSE.equals(v.getIsActive())
                || Boolean.FALSE.equals(p.getIsActive())
                || p.getDeletedAt() != null) {
            return StockStatus.UNAVAILABLE;
        }
        int stock = safeStock(v);
        if (stock <= 0) return StockStatus.OUT_OF_STOCK;
        if (stock <= cartProperties.getLowStockThreshold()) return StockStatus.LOW_STOCK;
        return StockStatus.IN_STOCK;
    }
}
