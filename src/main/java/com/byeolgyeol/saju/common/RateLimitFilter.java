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
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * API 요청 횟수 제한 (IP마다 1분 기준).
 * 프로그램으로 사주를 수천 개 만들거나 결제 API를 마구 두드리는 것을 막는다.
 * - 조회(GET): 1분에 1200번
 * - 저장 · 수정 · 삭제 · 결제(POST/PUT/DELETE): 1분에 120번
 * - 로그인 · 회원가입: 1분에 20번 (+ 계정마다 비밀번호 5번 틀리면 10분 잠금)
 * - 관리자 API: 1분에 60번 (+ 관리자 비밀번호 10번 틀리면 15분 잠금)
 * (발표장처럼 여러 명이 같은 와이파이 = 같은 IP로 접속해도 막히지 않을 만큼 넉넉하게 잡았다)
 * 넘으면 429 + 안내 문구. 서버 한 대(EB 단일 인스턴스) 기준이라 메모리에 센다.
 */
@Component
@Order(2)
public class RateLimitFilter implements Filter {

    static final int READ_LIMIT = 1200;
    static final int WRITE_LIMIT = 120;
    static final int AUTH_LIMIT = 20;
    static final int ADMIN_LIMIT = 60;
    private static final long WINDOW_MS = 60_000;
    private static final int MAX_KEYS = 50_000;

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        if (!request.getRequestURI().startsWith("/api/")) {
            chain.doFilter(req, res);
            return;
        }
        boolean write = !"GET".equalsIgnoreCase(request.getMethod()) && !"HEAD".equalsIgnoreCase(request.getMethod());
        // 로그인 · 가입은 비밀번호 대입 공격을 막으려고 훨씬 적게 허용
        boolean auth = write && request.getRequestURI().startsWith("/api/auth/");
        boolean admin = request.getRequestURI().startsWith("/api/admin/");   // 관리자 API 도 비밀번호 대입 방지로 적게
        String key = (admin ? "m:" : auth ? "a:" : write ? "w:" : "r:") + clientIp(request);
        int limit = admin ? ADMIN_LIMIT : auth ? AUTH_LIMIT : write ? WRITE_LIMIT : READ_LIMIT;
        if (!allow(key, limit, System.currentTimeMillis())) {
            HttpServletResponse response = (HttpServletResponse) res;
            response.setStatus(429);
            response.setHeader("Retry-After", "60");
            response.setContentType("application/json;charset=UTF-8");
            response.getOutputStream().write(
                    "{\"message\":\"요청이 너무 많아요. 1분 뒤에 다시 시도해주세요.\"}".getBytes(StandardCharsets.UTF_8));
            return;
        }
        chain.doFilter(req, res);
    }

    /** 1분 창 안에서 limit 번까지 허용 */
    boolean allow(String key, int limit, long now) {
        if (windows.size() > MAX_KEYS) windows.entrySet().removeIf(e -> now - e.getValue().start > WINDOW_MS);
        Window w = windows.compute(key, (k, old) ->
                old == null || now - old.start > WINDOW_MS ? new Window(now) : old);
        synchronized (w) {
            return ++w.count <= limit;
        }
    }

    /**
     * EB 앞단의 nginx 가 X-Forwarded-For 맨 끝에 실제 접속 IP를 붙인다.
     * 사용자가 헤더를 직접 써서 보내도 맨 앞에만 들어가므로 맨 끝 값을 쓴다.
     */
    static String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            String[] parts = xff.split(",");
            String last = parts[parts.length - 1].trim();
            if (!last.isEmpty()) return last;
        }
        return request.getRemoteAddr();
    }

    private static final class Window {
        final long start;
        int count;
        Window(long start) { this.start = start; }
    }
}
