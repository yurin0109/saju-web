package com.byeolgyeol.saju.auth;

/** 로그인이 필요한 기능(결제 · 구매 내역)을 로그인 없이 부를 때 → 401 */
public class LoginRequiredException extends RuntimeException {
    public LoginRequiredException(String message) { super(message); }
}
