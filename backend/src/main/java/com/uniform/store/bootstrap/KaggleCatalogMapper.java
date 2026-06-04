package com.uniform.store.bootstrap;

import com.uniform.store.enums.Gender;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class KaggleCatalogMapper {

    public record Row(long id, String gender, String masterCategory, String subCategory,
                      String articleType, String baseColour, String season, String year,
                      String usage, String productDisplayName) {}

    private static final Set<String> WEARABLE_SUBCATEGORIES =
            Set.of("topwear", "bottomwear", "dress", "apparel set");

    // filter out non-wearable article types
    private static final Set<String> EXCLUDED_ARTICLE_TYPES = Set.of(
            "belts", "swimwear", "ties", "ties and cufflinks", "cufflinks", "scarves",
            "gloves", "caps", "hat", "headband", "mufflers", "stoles", "socks",
            "suspenders", "dupatta", "booties", "shapewear");

    private static final String COLOR_FALLBACK = "#9CA3AF";
    private static final Map<String, String> COLOR_HEX = Map.ofEntries(
            Map.entry("black", "#000000"), Map.entry("white", "#FFFFFF"), Map.entry("off white", "#FAF9F6"),
            Map.entry("cream", "#F5F0E1"), Map.entry("grey", "#808080"), Map.entry("grey melange", "#A9A9A9"),
            Map.entry("charcoal", "#36454F"), Map.entry("silver", "#C0C0C0"), Map.entry("steel", "#71797E"),
            Map.entry("navy blue", "#1F2A44"), Map.entry("blue", "#1F4E8C"), Map.entry("teal", "#008080"),
            Map.entry("turquoise blue", "#30D5C8"), Map.entry("green", "#2E7D32"), Map.entry("sea green", "#2E8B57"),
            Map.entry("olive", "#5A6240"), Map.entry("lime green", "#32CD32"), Map.entry("red", "#C62828"),
            Map.entry("maroon", "#6B1F2A"), Map.entry("burgundy", "#6B1F2A"), Map.entry("rust", "#B7410E"),
            Map.entry("pink", "#E91E63"), Map.entry("rose", "#FF66CC"), Map.entry("magenta", "#FF00FF"),
            Map.entry("purple", "#7B1FA2"), Map.entry("lavender", "#B57EDC"), Map.entry("mauve", "#E0B0FF"),
            Map.entry("brown", "#6D4C41"), Map.entry("coffee brown", "#4B3621"), Map.entry("tan", "#D2B48C"),
            Map.entry("khaki", "#8F8B66"), Map.entry("beige", "#D6C7A1"), Map.entry("nude", "#E3BC9A"),
            Map.entry("skin", "#FFE0BD"), Map.entry("peach", "#FFCBA4"), Map.entry("yellow", "#FBC02D"),
            Map.entry("mustard", "#FFDB58"), Map.entry("orange", "#FB8C00"), Map.entry("gold", "#C9A227"),
            Map.entry("copper", "#B87333"), Map.entry("bronze", "#CD7F32"), Map.entry("metallic", "#B0B0B0"));

    public Optional<Row> parseLine(String line) {
        if (line == null || line.isBlank()) return Optional.empty();
        String[] p = line.split(",", 10);
        if (p.length < 10) return Optional.empty();
        long id;
        try {
            id = Long.parseLong(p[0].trim());
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
        String name = p[9].trim();
        if (name.isEmpty()) return Optional.empty();
        return Optional.of(new Row(id, p[1].trim(), p[2].trim(), p[3].trim(), p[4].trim(),
                p[5].trim(), p[6].trim(), p[7].trim(), p[8].trim(), name));
    }

    public boolean isWearable(Row r) {
        return "apparel".equalsIgnoreCase(r.masterCategory())
                && WEARABLE_SUBCATEGORIES.contains(r.subCategory().trim().toLowerCase())
                && !EXCLUDED_ARTICLE_TYPES.contains(r.articleType().trim().toLowerCase());
    }

    public Gender genderOf(String g) {
        if (g == null) return Gender.UNISEX;
        return switch (g.trim().toLowerCase()) {
            case "men", "boys" -> Gender.MEN;
            case "women", "girls" -> Gender.WOMEN;
            default -> Gender.UNISEX;
        };
    }

    public String colorHex(String baseColour) {
        if (baseColour == null || baseColour.isBlank()) return COLOR_FALLBACK;
        return COLOR_HEX.getOrDefault(baseColour.trim().toLowerCase(), COLOR_FALLBACK);
    }

    public List<String> sizesFor(String subCategory) {
        if (subCategory == null) return List.of("One Size");
        return switch (subCategory.trim().toLowerCase()) {
            case "topwear", "innerwear", "dress", "apparel set", "loungewear and nightwear", "saree" ->
                    List.of("S", "M", "L", "XL");
            case "bottomwear" -> List.of("28", "30", "32", "34", "36");
            case "shoes", "flip flops", "sandal" -> List.of("39", "40", "41", "42", "43");
            default -> List.of("One Size");
        };
    }

    public BigDecimal priceFor(String subCategory, long id) {
        int[] range = switch (subCategory == null ? "" : subCategory.trim().toLowerCase()) {
            case "topwear", "innerwear", "loungewear and nightwear" -> new int[]{150_000, 350_000};
            case "bottomwear", "dress", "apparel set", "saree" -> new int[]{250_000, 600_000};
            case "shoes" -> new int[]{400_000, 900_000};
            case "watches" -> new int[]{800_000, 2_500_000};
            case "bags" -> new int[]{300_000, 700_000};
            default -> new int[]{150_000, 400_000};
        };
        int steps = (range[1] - range[0]) / 10_000;
        long pick = Math.floorMod(id * 7919L, steps + 1L);
        return BigDecimal.valueOf(range[0] + pick * 10_000L);
    }

    public int stockFor(long id) {
        return 10 + (int) Math.floorMod(id * 131L, 60L);
    }

    public String description(Row r) {
        StringBuilder sb = new StringBuilder(r.productDisplayName()).append('.');
        if (!r.articleType().isBlank()) {
            sb.append(' ').append(r.articleType());
            if (!r.baseColour().isBlank()) sb.append(" in ").append(r.baseColour());
            sb.append('.');
        }
        if (!r.usage().isBlank()) sb.append(' ').append(r.usage()).append(" wear.");
        return sb.toString();
    }

    public String slugify(String s) {
        if (s == null) return "";
        return s.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
    }

    public String slugForProduct(Row r) {
        String base = slugify(r.productDisplayName());
        if (base.isBlank()) base = "product";
        return base + "-" + r.id();
    }

    public String skuFor(long id, String size) {
        return "KAG-" + id + "-" + size.replaceAll("\\s+", "").toUpperCase();
    }
}
