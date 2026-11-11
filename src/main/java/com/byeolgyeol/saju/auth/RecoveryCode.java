package com.byeolgyeol.saju.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * 복구 코드: 이메일 · 휴대폰 인증 대신, 가입할 때 한 번 보여주는 16자리 코드.
 * 아이디를 잊었을 때 찾고, 비밀번호를 잊었을 때 새로 정하는 데 쓴다.
 * - 헷갈리는 글자(0 O 1 I L)를 뺀 32글자 중 16자 = 80비트 → 맞춰서 찾기 사실상 불가능
 * - DB에는 SHA-256 값만 저장 (코드 자체는 저장하지 않음)
 */
public final class RecoveryCode {

    private static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private RecoveryCode() {}

    /** 예) K7QM-2XPA-9RTD-HV4N */
    public static String generate() {
        StringBuilder sb = new StringBuilder(19);
        for (int i = 0; i < 16; i++) {
            if (i > 0 && i % 4 == 0) sb.append('-');
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    /** 사용자가 소문자 · 공백 · 하이픈을 섞어 써도 같은 코드로 본다 */
    public static String normalize(String code) {
        return code == null ? "" : code.toUpperCase().replaceAll("[^A-Z0-9]", "");
    }

    public static String hash(String code) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-256")
                    .digest(("ats-recovery:" + normalize(code)).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(h);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
