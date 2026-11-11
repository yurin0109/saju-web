package com.byeolgyeol.saju.admin;

import com.byeolgyeol.saju.common.LockedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;

/**
 * 관리자 확인.
 * 관리자 비밀번호는 코드에 쓰지 않고 환경 변수 ADMIN_PASSWORD 로만 넣는다 (EB 환경 속성).
 * - 비어 있으면 관리자 기능 자체가 꺼져 있다
 * - 비교는 시간 차이로 맞춰보지 못하게 MessageDigest.isEqual
 * - 10번 틀리면 15분 동안 관리자 기능 잠금 (비밀번호 대입 방지)
 */
@Component
public class AdminGuard {

    static final int MAX_FAILS = 10;
    static final long LOCK_SECONDS = 15 * 60;

    private final String password;
    private int fails;
    private Instant lockedUntil = Instant.EPOCH;

    public AdminGuard(@Value("${admin.password:}") String password) {
        this.password = password == null ? "" : password;
    }

    public synchronized void check(String given) {
        if (password.length() < 8) throw new LockedException("관리자 비밀번호(ADMIN_PASSWORD)가 설정되지 않았어요. 8자 이상으로 설정해주세요.");
        Instant now = Instant.now();
        if (now.isBefore(lockedUntil)) throw new LockedException("관리자 비밀번호를 여러 번 틀려서 15분 동안 잠겼어요.");
        boolean ok = given != null && MessageDigest.isEqual(
                password.getBytes(StandardCharsets.UTF_8), given.getBytes(StandardCharsets.UTF_8));
        if (!ok) {
            if (++fails >= MAX_FAILS) { lockedUntil = now.plusSeconds(LOCK_SECONDS); fails = 0; }
            throw new LockedException("관리자 비밀번호가 맞지 않아요.");
        }
        fails = 0;
    }
}
