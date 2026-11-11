package com.byeolgyeol.saju.profile;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class PurchaseRefundTest {

    private static Purchase bought(LocalDateTime at) throws Exception {
        Purchase p = new Purchase("c", 1L, Product.YEAR, null);
        Field f = Purchase.class.getDeclaredField("createdAt");
        f.setAccessible(true);
        f.set(p, at);
        return p;
    }

    @Test
    @DisplayName("7일 이내 · 열어보지 않은 구매만 환불 가능하고, 환불하면 리포트가 닫힌다")
    void rules() throws Exception {
        LocalDateTime t = LocalDateTime.of(2026, 11, 1, 10, 0);
        Purchase p = bought(t);
        assertNull(p.refundBlockedReason(t.plusDays(6)));
        assertNotNull(p.refundBlockedReason(t.plusDays(8)), "7일 지나면 불가");
        p.markOpened(t.plusHours(1));
        assertNotNull(p.refundBlockedReason(t.plusDays(1)), "열어보면 불가");

        Purchase q = bought(t);
        q.refund(t.plusDays(1));
        assertFalse(q.isActive());
        assertNotNull(q.refundBlockedReason(t.plusDays(1)), "두 번 환불 불가");
        assertTrue(Product.owned(java.util.List.of()).isEmpty());
    }
}
