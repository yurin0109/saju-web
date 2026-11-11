package com.byeolgyeol.saju.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RecoveryCodeTest {

    @Test
    @DisplayName("복구 코드는 XXXX-XXXX-XXXX-XXXX 형식이고, 소문자 · 공백을 섞어 입력해도 같은 해시가 된다")
    void codeAndHash() {
        String c = RecoveryCode.generate();
        assertTrue(c.matches("[A-Z2-9]{4}(-[A-Z2-9]{4}){3}"), c);
        assertNotEquals(c, RecoveryCode.generate());
        assertEquals(RecoveryCode.hash(c), RecoveryCode.hash(" " + c.toLowerCase().replace("-", " ") + " "));
        assertEquals(64, RecoveryCode.hash(c).length());
        assertFalse(RecoveryCode.hash(c).contains(c.replace("-", "")));
    }
}
