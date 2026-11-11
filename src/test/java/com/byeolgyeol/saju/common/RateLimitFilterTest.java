package com.byeolgyeol.saju.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitFilterTest {

    @Test
    @DisplayName("1분 안에 정해진 횟수까지만 허용하고, 1분이 지나면 다시 허용한다")
    void window() {
        RateLimitFilter f = new RateLimitFilter();
        for (int i = 0; i < 3; i++) assertTrue(f.allow("ip", 3, 1000));
        assertFalse(f.allow("ip", 3, 2000));
        assertTrue(f.allow("other", 3, 2000), "다른 IP는 따로 센다");
        assertTrue(f.allow("ip", 3, 62_000));
    }
}
