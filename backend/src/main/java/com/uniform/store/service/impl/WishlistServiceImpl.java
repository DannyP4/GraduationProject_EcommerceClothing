package com.uniform.store.service.impl;

import com.uniform.store.dto.response.ProductSummaryDto;
import com.uniform.store.dto.response.WishlistToggleResponse;
import com.uniform.store.entity.Product;
import com.uniform.store.entity.User;
import com.uniform.store.entity.Wishlist;
import com.uniform.store.exception.BadRequestException;
import com.uniform.store.exception.ResourceNotFoundException;
import com.uniform.store.repository.ProductRepository;
import com.uniform.store.repository.UserRepository;
import com.uniform.store.repository.WishlistRepository;
import com.uniform.store.service.ProductService;
import com.uniform.store.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WishlistServiceImpl implements WishlistService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final WishlistRepository wishlistRepository;
    private final ProductService productService;

    @Override
    public List<ProductSummaryDto> getWishlist(String userEmail, String locale) {
        User user = loadUser(userEmail);
        List<Long> productIds = wishlistRepository.findActiveProductIdsByUserId(user.getId());
        return productService.getSummariesByIds(productIds, locale);
    }

    @Override
    public List<Long> getWishlistProductIds(String userEmail) {
        User user = loadUser(userEmail);
        return wishlistRepository.findProductIdsByUserId(user.getId());
    }

    @Override
    @Transactional
    public WishlistToggleResponse toggle(String userEmail, Long productId) {
        User user = loadUser(userEmail);
        return wishlistRepository.findByUserIdAndProductId(user.getId(), productId)
                .map(existing -> {
                    wishlistRepository.delete(existing);
                    return WishlistToggleResponse.builder().productId(productId).wishlisted(false).build();
                })
                .orElseGet(() -> {
                    Product product = loadAvailableProduct(productId);
                    wishlistRepository.save(Wishlist.builder().user(user).product(product).build());
                    return WishlistToggleResponse.builder().productId(productId).wishlisted(true).build();
                });
    }

    @Override
    @Transactional
    public void remove(String userEmail, Long productId) {
        User user = loadUser(userEmail);
        wishlistRepository.findByUserIdAndProductId(user.getId(), productId)
                .ifPresent(wishlistRepository::delete);
    }

    @Override
    @Transactional
    public void clear(String userEmail) {
        User user = loadUser(userEmail);
        wishlistRepository.deleteAllByUserId(user.getId());
    }

    private User loadUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private Product loadAvailableProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
        if (Boolean.FALSE.equals(product.getIsActive()) || product.getDeletedAt() != null) {
            throw new BadRequestException("Product is not available");
        }
        return product;
    }
}
