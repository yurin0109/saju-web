package com.byeolgyeol.saju.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/** 컨트롤러에서 로그인 여부 확인 */
public final class Login {

    private Login() {}

    public static boolean isLoggedIn(HttpServletRequest request) {
        HttpSession s = request.getSession(false);
        return s != null && s.getAttribute(LoginFilter.SESSION_MEMBER) != null;
    }

    /** 로그인하지 않았으면 401 (화면에서 로그인 창을 띄운다) */
    public static void require(HttpServletRequest request) {
        if (!isLoggedIn(request)) throw new LoginRequiredException("결제하려면 로그인이 필요해요.");
    }

    /** 로그인한 회원 번호 (아니면 401) */
    public static Long memberId(HttpServletRequest request) {
        HttpSession s = request.getSession(false);
        Object v = s == null ? null : s.getAttribute(LoginFilter.SESSION_MEMBER);
        if (v instanceof Long id) return id;
        throw new LoginRequiredException("로그인이 필요해요.");
    }

    public static String username(HttpServletRequest request) {
        HttpSession s = request.getSession(false);
        Object v = s == null ? null : s.getAttribute(LoginFilter.SESSION_NAME);
        return v instanceof String name ? name : null;
    }
}
