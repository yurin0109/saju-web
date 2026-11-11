package com.byeolgyeol.saju.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RequestGuardFilterTest {

    @Test
    @DisplayName("같은 사이트에서 보낸 요청만 통과하고, 다른 사이트 Origin 은 막는다")
    void sameOrigin() {
        assertTrue(RequestGuardFilter.sameOrigin(null, "a.elasticbeanstalk.com"));
        assertTrue(RequestGuardFilter.sameOrigin("http://a.elasticbeanstalk.com", "a.elasticbeanstalk.com"));
        assertTrue(RequestGuardFilter.sameOrigin("http://localhost:8080", "localhost:8080"));
        assertTrue(RequestGuardFilter.sameOrigin("http://a.com:80", "a.com"));
        assertFalse(RequestGuardFilter.sameOrigin("https://evil.com", "a.elasticbeanstalk.com"));
        assertFalse(RequestGuardFilter.sameOrigin("http://localhost:9999", "localhost:8080"));
        assertFalse(RequestGuardFilter.sameOrigin("null", "a.com"));
    }

    @Test
    @DisplayName("에러 문구는 우리가 쓴 한글 안내만 보여주고, 영어 내부 메시지는 숨긴다")
    void safeMessage() {
        assertEquals("이미 구매한 리포트예요.", ApiExceptionHandler.safe("이미 구매한 리포트예요."));
        assertEquals("잘못된 요청이에요.", ApiExceptionHandler.safe("No enum constant com.byeolgyeol.saju.profile.Product.X"));
        assertEquals("잘못된 요청이에요.", ApiExceptionHandler.safe(null));
    }
}
