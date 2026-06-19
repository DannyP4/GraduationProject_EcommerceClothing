package com.uniform.store.service;

// suggest clothing size based on height and weight
public final class SizeAdvisor {

    private SizeAdvisor() {
    }

    private static final String[] SIZES = {"S", "M", "L", "XL", "XXL", "XXXL"};

    public record SizeAdvice(String recommended, String comfortable, int heightCm, int weightKg) {
    }

    public static SizeAdvice recommend(int heightCm, int weightKg) {
        int idx = indexByWeight(weightKg);
        if (heightCm >= 185 && idx < SIZES.length - 1) {
            idx++;
        }
        int comfortIdx = Math.min(idx + 1, SIZES.length - 1);
        return new SizeAdvice(SIZES[idx], SIZES[comfortIdx], heightCm, weightKg);
    }

    private static int indexByWeight(int kg) {
        if (kg <= 52) return 0;
        if (kg <= 60) return 1;
        if (kg <= 68) return 2;
        if (kg <= 77) return 3;
        if (kg <= 87) return 4;
        return 5;
    }
}
