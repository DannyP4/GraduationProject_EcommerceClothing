package com.uniform.store.bootstrap;

import com.uniform.store.bootstrap.KaggleCatalogMapper.Row;
import com.uniform.store.enums.Gender;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class KaggleCatalogMapperTest {

    private final KaggleCatalogMapper mapper = new KaggleCatalogMapper();

    @Test
    void parseLine_validRow_mapsAllFields() {
        Optional<Row> r = mapper.parseLine(
                "15970,Men,Apparel,Topwear,Shirts,Navy Blue,Fall,2011,Casual,Turtle Check Men Navy Blue Shirt");
        assertThat(r).isPresent();
        Row row = r.get();
        assertThat(row.id()).isEqualTo(15970L);
        assertThat(row.gender()).isEqualTo("Men");
        assertThat(row.masterCategory()).isEqualTo("Apparel");
        assertThat(row.subCategory()).isEqualTo("Topwear");
        assertThat(row.articleType()).isEqualTo("Shirts");
        assertThat(row.baseColour()).isEqualTo("Navy Blue");
        assertThat(row.productDisplayName()).isEqualTo("Turtle Check Men Navy Blue Shirt");
    }

    @Test
    void parseLine_nameWithCommas_preservesFullName() {
        Optional<Row> r = mapper.parseLine(
                "1234,Women,Apparel,Topwear,Tops,Black,Summer,2012,Casual,Cool, Stylish, Black Top");
        assertThat(r).isPresent();
        assertThat(r.get().productDisplayName()).isEqualTo("Cool, Stylish, Black Top");
    }

    @Test
    void parseLine_malformedOrBadId_returnsEmpty() {
        assertThat(mapper.parseLine("1,Men,Apparel")).isEmpty();
        assertThat(mapper.parseLine("abc,Men,Apparel,Topwear,Shirts,Black,Fall,2011,Casual,Name")).isEmpty();
        assertThat(mapper.parseLine("1,Men,Apparel,Topwear,Shirts,Black,Fall,2011,Casual,")).isEmpty();
        assertThat(mapper.parseLine("")).isEmpty();
    }

    @Test
    void isWearable_keepsTopBottomDress_dropsFootwearAccessoriesAndMisc() {
        assertThat(mapper.isWearable(row("Apparel", "Topwear", "Shirts", "Black"))).isTrue();
        assertThat(mapper.isWearable(row("Apparel", "Bottomwear", "Jeans", "Blue"))).isTrue();
        assertThat(mapper.isWearable(row("Apparel", "Dress", "Dresses", "Red"))).isTrue();
        assertThat(mapper.isWearable(row("Apparel", "Apparel Set", "Clothing Set", "Black"))).isTrue();
        assertThat(mapper.isWearable(row("Apparel", "Innerwear", "Briefs", "Black"))).isFalse();
        assertThat(mapper.isWearable(row("Footwear", "Shoes", "Casual Shoes", "White"))).isFalse();
        assertThat(mapper.isWearable(row("Accessories", "Watches", "Watches", "Silver"))).isFalse();
        assertThat(mapper.isWearable(row("Personal Care", "Fragrance", "Perfume", "Blue"))).isFalse();
    }

    @Test
    void isWearable_dropsNonGarmentArticleTypesMislabeledAsApparel() {
        // Swimwear, Belts, Dupatta, Socks, Suspenders are not wearable garments
        assertThat(mapper.isWearable(row("Apparel", "Bottomwear", "Swimwear", "Blue"))).isFalse();
        assertThat(mapper.isWearable(row("Apparel", "Bottomwear", "Belts", "Black"))).isFalse();
        assertThat(mapper.isWearable(row("Apparel", "Topwear", "Dupatta", "Red"))).isFalse();
        assertThat(mapper.isWearable(row("Apparel", "Topwear", "Socks", "Grey"))).isFalse();
        assertThat(mapper.isWearable(row("Apparel", "Topwear", "Suspenders", "Black"))).isFalse();
    }

    @Test
    void genderOf_mapsBoysGirlsToMenWomen() {
        assertThat(mapper.genderOf("Men")).isEqualTo(Gender.MEN);
        assertThat(mapper.genderOf("Boys")).isEqualTo(Gender.MEN);
        assertThat(mapper.genderOf("Women")).isEqualTo(Gender.WOMEN);
        assertThat(mapper.genderOf("Girls")).isEqualTo(Gender.WOMEN);
        assertThat(mapper.genderOf("Unisex")).isEqualTo(Gender.UNISEX);
        assertThat(mapper.genderOf("???")).isEqualTo(Gender.UNISEX);
    }

    @Test
    void colorHex_knownCaseInsensitive_unknownFallsBack() {
        assertThat(mapper.colorHex("Navy Blue")).isEqualTo("#1F2A44");
        assertThat(mapper.colorHex("black")).isEqualTo("#000000");
        assertThat(mapper.colorHex("FuchsiaXYZ")).isEqualTo("#9CA3AF");
        assertThat(mapper.colorHex("")).isEqualTo("#9CA3AF");
        assertThat(mapper.colorHex(null)).isEqualTo("#9CA3AF");
    }

    @Test
    void sizesFor_bucketsBySubCategory() {
        assertThat(mapper.sizesFor("Topwear")).containsExactly("S", "M", "L", "XL");
        assertThat(mapper.sizesFor("Bottomwear")).containsExactly("28", "30", "32", "34", "36");
        assertThat(mapper.sizesFor("Shoes")).containsExactly("39", "40", "41", "42", "43");
        assertThat(mapper.sizesFor("Bags")).containsExactly("One Size");
        assertThat(mapper.sizesFor(null)).containsExactly("One Size");
    }

    @Test
    void priceFor_deterministicWithinRangeAndRounded() {
        BigDecimal a = mapper.priceFor("Topwear", 15970L);
        BigDecimal b = mapper.priceFor("Topwear", 15970L);
        assertThat(a).isEqualByComparingTo(b);
        assertThat(a.longValueExact()).isBetween(150_000L, 350_000L);
        assertThat(a.longValueExact() % 10_000L).isZero();
    }

    @Test
    void stockFor_deterministicWithinRange() {
        assertThat(mapper.stockFor(15970L)).isEqualTo(mapper.stockFor(15970L));
        assertThat(mapper.stockFor(15970L)).isBetween(10, 69);
    }

    @Test
    void slugForProduct_appendsIdForUniqueness() {
        Row r = new Row(15970L, "Men", "Apparel", "Topwear", "Shirts", "Navy Blue",
                "Fall", "2011", "Casual", "Turtle Check Men Navy Blue Shirt");
        assertThat(mapper.slugForProduct(r)).isEqualTo("turtle-check-men-navy-blue-shirt-15970");
    }

    @Test
    void skuFor_stripsSpacesAndUppercases() {
        assertThat(mapper.skuFor(15970L, "M")).isEqualTo("KAG-15970-M");
        assertThat(mapper.skuFor(15970L, "One Size")).isEqualTo("KAG-15970-ONESIZE");
    }

    private Row row(String master, String sub, String articleType, String colour) {
        return new Row(1L, "Men", master, sub, articleType, colour, "Fall", "2011", "Casual", "Name");
    }
}
