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
import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * 저장 · 결제 · 로그인 같은 "바꾸는" 요청(POST/PUT/DELETE)을 검사한다.
 *
 * 1) CSRF 방지: 다른 사이트에서 몰래 보낸 요청이면 거절
 *    - 브라우저가 붙이는 Sec-Fetch-Site 가 cross-site 이면 거절
 *    - Origin 헤더가 있으면 우리 주소(Host)와 같아야 함
 * 2) 본문은 JSON 만 받는다 (HTML form 으로 위장한 요청 차단)
 * 3) 본문 크기 64KB 제한 (엄청 큰 요청으로 서버 메모리를 채우는 것 방지)
 */
@Component
@Order(0)
public class RequestGuardFilter implements Filter {

    static final long MAX_BODY = 64 * 1024;

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        String method = request.getMethod();
        boolean write = !"GET".equalsIgnoreCase(method) && !"HEAD".equalsIgnoreCase(method) && !"OPTIONS".equalsIgnoreCase(method);

        if (write && request.getRequestURI().startsWith("/api/")) {
            if ("cross-site".equalsIgnoreCase(request.getHeader("Sec-Fetch-Site"))
                    || !(sameOrigin(request.getHeader("Origin"), request.getHeader("Host"))
                         || sameOrigin(request.getHeader("Origin"), request.getHeader("X-Forwarded-Host")))) {
                reject(response, 403, "다른 사이트에서 보낸 요청은 처리하지 않아요.");
                return;
            }
            long length = request.getContentLengthLong();
            if (length > MAX_BODY) {
                reject(response, 413, "요청 내용이 너무 커요.");
                return;
            }
            String type = request.getContentType();
            if (length > 0 && (type == null || !type.toLowerCase().startsWith("application/json"))) {
                reject(response, 415, "잘못된 요청 형식이에요.");
                return;
            }
        }
        chain.doFilter(req, res);
    }

    /** Origin 이 없으면(같은 사이트의 오래된 브라우저 등) 통과, 있으면 호스트가 같아야 한다 */
    static boolean sameOrigin(String origin, String host) {
        if (origin == null || origin.isBlank()) return true;
        if (host == null) return false;
        try {
            URI o = URI.create(origin);
            String originHost = o.getHost() + (o.getPort() > 0 ? ":" + o.getPort() : "");
            return originHost.equalsIgnoreCase(host) || stripDefaultPort(originHost, o.getScheme()).equalsIgnoreCase(host);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static String stripDefaultPort(String hostPort, String scheme) {
        if ("http".equals(scheme) && hostPort.endsWith(":80")) return hostPort.substring(0, hostPort.length() - 3);
        if ("https".equals(scheme) && hostPort.endsWith(":443")) return hostPort.substring(0, hostPort.length() - 4);
        return hostPort;
    }

    private static void reject(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getOutputStream().write(("{\"message\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8));
    }
}
