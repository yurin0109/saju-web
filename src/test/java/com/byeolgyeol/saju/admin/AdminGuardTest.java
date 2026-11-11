package com.byeolgyeol.saju.admin;

import com.byeolgyeol.saju.common.LockedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AdminGuardTest {

    @Test
    @DisplayName("관리자 비밀번호가 없으면 꺼져 있고, 맞으면 통과, 10번 틀리면 잠긴다")
    void guard() {
        assertThrows(LockedException.class, () -> new AdminGuard("").check("anything"));
        AdminGuard g = new AdminGuard("admin-pass-123");
        g.check("admin-pass-123");
        assertThrows(LockedException.class, () -> g.check("wrong"));
        for (int i = 0; i < 9; i++) assertThrows(LockedException.class, () -> g.check("wrong"));
        assertThrows(LockedException.class, () -> g.check("admin-pass-123"));   // 잠긴 동안은 맞아도 막힘
    }
}
