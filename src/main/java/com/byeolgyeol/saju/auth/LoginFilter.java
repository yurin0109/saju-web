package com.byeolgyeol.saju.auth;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;

/**
 * 로그인 상태 연결.
 * 로그인한 요청이면 X-Client-Id 헤더를 회원 열쇠(ownerKey)로 바꿔서 컨트롤러에 넘긴다.
 * → 사주 · 구매 · 성찰 일기 API는 고치지 않아도, 로그인하면 자동으로 "내 계정 데이터"를 보게 된다.
 * (사용자가 헤더를 직접 바꿔 보내도, 로그인 중에는 세션의 값이 우선이라 남의 계정 데이터는 못 본다)
 *
 * 로그인하지 않은 요청이 회원 열쇠를 X-Client-Id 로 보내면 막는다.
 * → 회원 데이터는 비밀번호로 로그인해야만 볼 수 있다 (열쇠 값만 알아내서 들어오는 것 방지).
 */
@Component
@Order(3)
public class LoginFilter implements Filter {

    private final MemberRepository members;

    public LoginFilter(MemberRepository members) {
        this.members = members;
    }

    public static final String CLIENT_HEADER = "X-Client-Id";
    static final String SESSION_MEMBER = "login.memberId";
    static final String SESSION_OWNER = "login.ownerKey";
    static final String SESSION_NAME = "login.username";
    /** 브라우저가 원래 보낸 X-Client-Id (로그인할 때 데이터 옮기기에 사용) */
    public static final String RAW_CLIENT = "login.rawClientId";

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        request.setAttribute(RAW_CLIENT, request.getHeader(CLIENT_HEADER));
        HttpSession session = request.getSession(false);
        Object owner = session == null ? null : session.getAttribute(SESSION_OWNER);
        if (owner instanceof String key) {
            chain.doFilter(new OwnerRequest(request, key), res);
        } else {
            String raw = request.getHeader(CLIENT_HEADER);
            if (raw != null && raw.length() == 36 && request.getRequestURI().startsWith("/api/") && members.existsByOwnerKey(raw)) {
                jakarta.servlet.http.HttpServletResponse response = (jakarta.servlet.http.HttpServletResponse) res;
                response.setStatus(401);
                response.setContentType("application/json;charset=UTF-8");
                response.getOutputStream().write("{\"message\":\"로그인이 필요해요. 다시 로그인해주세요.\",\"code\":\"LOGIN_REQUIRED\"}"
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8));
                return;
            }
            chain.doFilter(req, res);
        }
    }

    /** X-Client-Id 헤더만 회원 열쇠로 바꿔 보여주는 요청 */
    private static final class OwnerRequest extends HttpServletRequestWrapper {
        private final String ownerKey;

        OwnerRequest(HttpServletRequest request, String ownerKey) {
            super(request);
            this.ownerKey = ownerKey;
        }

        @Override
        public String getHeader(String name) {
            return CLIENT_HEADER.equalsIgnoreCase(name) ? ownerKey : super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            return CLIENT_HEADER.equalsIgnoreCase(name) ? Collections.enumeration(java.util.List.of(ownerKey)) : super.getHeaders(name);
        }
    }
}
