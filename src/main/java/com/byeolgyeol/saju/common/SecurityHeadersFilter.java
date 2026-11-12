package com.byeolgyeol.saju.common;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 모든 응답에 보안 헤더를 붙인다.
 * - CSP: 허용한 곳(내 서버 · 구글 폰트 · 토스 결제)에서만 스크립트/스타일/폰트를 불러오게 해서 XSS 피해를 줄인다
 * - X-Frame-Options / frame-ancestors: 다른 사이트가 우리 화면을 iframe 으로 덮어씌우는 클릭재킹 방지
 * - nosniff: 브라우저가 파일 종류를 멋대로 추측하지 않게
 * - API 응답은 캐시하지 않게 해서 공용 PC에서 남의 사주 결과가 남지 않게
 */
@Component
@Order(1)
public class SecurityHeadersFilter implements Filter {

    static final String CSP = String.join("; ",
            "default-src 'self'",
            // 화면 스크립트는 /app.js 파일로만 (인라인 스크립트 · onclick 같은 속성 실행 금지)
            "script-src 'self' https://js.tosspayments.com https://*.tosspayments.com",
            "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com",
            "font-src 'self' https://fonts.gstatic.com",
            "img-src 'self' data: https:",
            "connect-src 'self' https://*.tosspayments.com",
            // 토스 결제창과 카드사 인증 화면은 iframe 으로 뜬다
            "frame-src https:",
            "form-action 'self' https:",
            "frame-ancestors 'none'",
            "base-uri 'self'",
            "object-src 'none'");

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        String path = request.getRequestURI();

        // 로컬 개발용 H2 콘솔은 iframe 을 써서 제외 (배포 환경에서는 콘솔 자체가 꺼져 있음)
        if (!path.startsWith("/h2-console")) {
            response.setHeader("Content-Security-Policy", CSP);
            response.setHeader("X-Frame-Options", "DENY");
        }
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        response.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
        response.setHeader("Cross-Origin-Opener-Policy", "same-origin-allow-popups");
        response.setHeader("Cross-Origin-Resource-Policy", "same-origin");
        if (path.startsWith("/api/")) {
            response.setHeader("Cache-Control", "no-store");
        } else if (path.equals("/") || path.endsWith(".html")) {
            // 화면(HTML)은 매번 서버에 새 버전이 있는지 확인 → 새로 배포하면 바로 반영 (예전 화면이 캐시로 남지 않게)
            response.setHeader("Cache-Control", "no-cache");
        }
        chain.doFilter(req, res);
    }
}
