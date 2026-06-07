package com.uniform.store.service.impl;

import com.uniform.store.entity.Product;
import com.uniform.store.enums.Gender;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

@Component
public class ProductDocumentBuilder {

    public String build(Product product, List<String> colors) {
        StringBuilder sb = new StringBuilder();
        sb.append(product.getName());
        sb.append(". ").append(genderText(product.getGender()));
        if (product.getCategory() != null) {
            sb.append(". Category: ").append(product.getCategory().getName());
        }
        if (product.getBrand() != null) {
            sb.append(". Brand: ").append(product.getBrand().getName());
        }
        if (colors != null && !colors.isEmpty()) {
            sb.append(". Colors: ").append(String.join(", ", colors));
        }
        String description = product.getDescription();
        if (description != null && !description.isBlank()) {
            sb.append(". ").append(description.trim());
        }
        return sb.toString();
    }

    public String contentHash(String model, int dim, String document) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((model + "|" + dim + "|" + document).getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private String genderText(Gender gender) {
        if (gender == null) return "Unisex";
        return switch (gender) {
            case MEN -> "For men";
            case WOMEN -> "For women";
            case KIDS -> "For kids";
            case UNISEX -> "Unisex";
        };
    }
}
