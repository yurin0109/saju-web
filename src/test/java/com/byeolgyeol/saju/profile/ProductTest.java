package com.byeolgyeol.saju.profile;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ProductTest {

    @Test
    @DisplayName("개별 리포트는 자기 자신만, 묶음은 궁합 · 재회운을 뺀 7종을 연다")
    void unlocks() {
        assertEquals(EnumSet.of(Product.YEAR), Product.YEAR.unlocks());
        assertEquals(7, Product.BUNDLE.unlocks().size());
        assertFalse(Product.BUNDLE.unlocks().contains(Product.REUNION), "재회운은 상대마다 따로 구매");
        assertTrue(Product.REUNION.perPartner() && Product.COMPAT.perPartner() && !Product.LOVE.perPartner());
        assertTrue(Product.BUNDLE.unlocks().contains(Product.LOVE));
        assertFalse(Product.BUNDLE.unlocks().contains(Product.COMPAT), "궁합은 상대마다 따로 구매");
    }

    @Test
    @DisplayName("구매 내역으로 열린 리포트를 계산한다")
    void owned() {
        assertTrue(Product.owned(List.of()).isEmpty());
        assertEquals(EnumSet.of(Product.CAREER, Product.MARRIAGE),
                Product.owned(List.of(Product.CAREER, Product.MARRIAGE)));
        assertEquals(Product.BUNDLE_REPORTS, Product.owned(List.of(Product.BUNDLE)));
    }

    @Test
    @DisplayName("이미 가진 리포트는 다시 살 수 없고, 하나라도 새로 열리면 묶음은 살 수 있다")
    void addsSomething() {
        Set<Product> hasYear = EnumSet.of(Product.YEAR);
        assertFalse(Product.YEAR.addsSomething(hasYear));
        assertTrue(Product.CAREER.addsSomething(hasYear));
        assertTrue(Product.BUNDLE.addsSomething(hasYear));
        assertFalse(Product.BUNDLE.addsSomething(EnumSet.copyOf(Product.BUNDLE_REPORTS)));
    }

    @Test
    @DisplayName("묶음은 개별 3종을 따로 사는 것보다 싸다")
    void bundlePrice() {
        int separate = 0;
        for (Product p : Product.BUNDLE_REPORTS) separate += p.price();
        assertTrue(Product.BUNDLE.price() < separate * 0.7, "묶음은 30% 이상 할인");
    }
}
