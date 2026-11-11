package com.byeolgyeol.saju.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class PasswordHasherTest {

    @Test
    @DisplayName("같은 비밀번호도 소금이 달라 해시가 다르고, 맞는 비밀번호만 통과한다")
    void hashAndMatch() {
        String a = PasswordHasher.hash("saju1234!"), b = PasswordHasher.hash("saju1234!");
        assertNotEquals(a, b);
        assertFalse(a.contains("saju1234"), "원문이 저장되면 안 됨");
        assertTrue(PasswordHasher.matches("saju1234!", a));
        assertFalse(PasswordHasher.matches("saju1235!", a));
        assertFalse(PasswordHasher.matches("saju1234!", "broken$value"));
        assertFalse(PasswordHasher.matches(null, a));
    }

    @Test
    @DisplayName("비밀번호를 5번 틀리면 10분 동안 잠기고, 성공하면 초기화된다")
    void lockout() {
        Member m = new Member("yurin", PasswordHasher.hash("abcd1234"));
        LocalDateTime t = LocalDateTime.of(2026, 11, 6, 12, 0);
        for (int i = 0; i < 4; i++) m.loginFailed(t);
        assertFalse(m.isLocked(t));
        m.loginFailed(t);
        assertTrue(m.isLocked(t.plusMinutes(9)));
        assertFalse(m.isLocked(t.plusMinutes(10)));
        m.loginSucceeded(t.plusMinutes(11));
        assertNull(m.getLockedUntil());
        assertEquals(36, m.getOwnerKey().length());
    }
}
