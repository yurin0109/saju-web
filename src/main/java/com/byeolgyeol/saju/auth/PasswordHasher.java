package com.byeolgyeol.saju.auth;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 비밀번호 해시 (JDK 기본 PBKDF2-HMAC-SHA256, 별도 라이브러리 없음).
 * 저장 형식: pbkdf2$반복횟수$소금(base64)$해시(base64)
 * - 소금(salt): 회원마다 랜덤 16바이트 → 같은 비밀번호라도 해시가 다름
 * - 반복 12만 번: 해시를 훔쳐도 하나씩 맞춰보기 어렵게 일부러 느리게
 * - 비교는 MessageDigest.isEqual 로 (비교 시간으로 정답을 추측하는 공격 방지)
 */
public final class PasswordHasher {

    private static final int ITERATIONS = 120_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {}

    public static String hash(String password) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] h = pbkdf2(password.toCharArray(), salt, ITERATIONS);
        Base64.Encoder b64 = Base64.getEncoder();
        return "pbkdf2$" + ITERATIONS + "$" + b64.encodeToString(salt) + "$" + b64.encodeToString(h);
    }

    public static boolean matches(String password, String stored) {
        if (password == null || stored == null) return false;
        String[] parts = stored.split("\\$");
        if (parts.length != 4 || !parts[0].equals("pbkdf2")) return false;
        try {
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            return MessageDigest.isEqual(expected, pbkdf2(password.toCharArray(), salt, iterations));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /** 없는 아이디로 로그인할 때도 같은 시간이 걸리게 쓰는 가짜 해시 (아이디 존재 여부 추측 방지) */
    static final String DUMMY = hash("dummy-password-for-timing");

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_BITS);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("비밀번호 해시 실패", e);
        }
    }
}
