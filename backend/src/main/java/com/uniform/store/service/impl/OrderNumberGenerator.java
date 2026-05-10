package com.uniform.store.service.impl;

import com.uniform.store.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

// Generates ORD-{yyyyMMdd}-{6-char base36}.
// 36^6 combos/day -> collision prob is negligible for store scale,
// but still pre-check existsByOrderNumber to be deterministic.
@Component
@RequiredArgsConstructor
public class OrderNumberGenerator {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int SUFFIX_LEN = 6;
    private static final int MAX_ATTEMPTS = 5;

    private final OrderRepository orderRepository;

    public String next() {
        String date = DATE_FMT.format(Instant.now());
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            String candidate = "ORD-" + date + "-" + randomSuffix();
            if (!orderRepository.existsByOrderNumber(candidate)) {
                return candidate;
            }
        }
        // Five collisions in a row at 1-in-2B implies a serious problem (e.g. RNG seeding) — fail loud.
        throw new IllegalStateException(
                "Could not generate unique order number after " + MAX_ATTEMPTS + " attempts");
    }

    private String randomSuffix() {
        StringBuilder sb = new StringBuilder(SUFFIX_LEN);
        for (int i = 0; i < SUFFIX_LEN; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
