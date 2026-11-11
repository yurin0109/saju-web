package com.byeolgyeol.saju.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 회원 API
 *
 * GET  /api/auth/me       로그인 상태
 * POST /api/auth/signup   회원가입 (가입 후 바로 로그인)
 * POST /api/auth/login    로그인
 * POST /api/auth/logout   로그아웃
 * POST /api/auth/find-id          아이디 찾기 (복구 코드)
 * POST /api/auth/reset-password   비밀번호 재설정 (아이디 + 복구 코드 → 새 복구 코드)
 * POST /api/auth/change-password  비밀번호 변경 (로그인, 지금 비밀번호 확인)
 * POST /api/auth/recovery-code    복구 코드 다시 받기 (로그인, 비밀번호 확인)
 *
 * 로그인 정보는 서버 세션에 두고, 브라우저에는 HttpOnly 쿠키(자바스크립트로 못 읽음)로 세션 번호만 준다.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final int SESSION_SECONDS = 14 * 24 * 60 * 60;   // 14일

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @GetMapping("/me")
    public AuthDto.MeResponse me(HttpServletRequest request) {
        String name = Login.username(request);
        return new AuthDto.MeResponse(name != null, name, 0);
    }

    @PostMapping("/signup")
    public AuthDto.MeResponse signup(@Valid @RequestBody AuthDto.SignupRequest req, HttpServletRequest request) {
        AuthService.Signed signed = service.signup(req);
        AuthDto.MeResponse me = start(signed.member(), request);
        return new AuthDto.MeResponse(true, me.username(), me.moved(), signed.recoveryCode());
    }

    @PostMapping("/login")
    public AuthDto.MeResponse login(@Valid @RequestBody AuthDto.LoginRequest req, HttpServletRequest request) {
        Member m = service.login(req);
        return start(m, request);
    }

    @PostMapping("/find-id")
    public AuthDto.FindIdResponse findId(@Valid @RequestBody AuthDto.FindIdRequest req) {
        return new AuthDto.FindIdResponse(service.findId(req));
    }

    @PostMapping("/reset-password")
    public AuthDto.RecoveryResponse resetPassword(@Valid @RequestBody AuthDto.ResetPasswordRequest req) {
        return new AuthDto.RecoveryResponse(service.resetPassword(req));
    }

    @PostMapping("/change-password")
    public AuthDto.MeResponse changePassword(@Valid @RequestBody AuthDto.ChangePasswordRequest req, HttpServletRequest request) {
        service.changePassword(Login.memberId(request), req);
        return me(request);
    }

    @PostMapping("/recovery-code")
    public AuthDto.RecoveryResponse recoveryCode(@Valid @RequestBody AuthDto.PasswordCheckRequest req, HttpServletRequest request) {
        return new AuthDto.RecoveryResponse(service.reissueRecovery(Login.memberId(request), req.password()));
    }

    @PostMapping("/logout")
    public AuthDto.MeResponse logout(HttpServletRequest request) {
        HttpSession s = request.getSession(false);
        if (s != null) s.invalidate();
        return new AuthDto.MeResponse(false, null, 0);
    }

    /** 로그인 시작: 이전 세션을 버리고 새 세션 번호를 받는다 (세션 고정 공격 방지) */
    private AuthDto.MeResponse start(Member m, HttpServletRequest request) {
        boolean wasLoggedIn = Login.isLoggedIn(request);
        HttpSession old = request.getSession(false);
        if (old != null) old.invalidate();
        HttpSession s = request.getSession(true);
        s.setMaxInactiveInterval(SESSION_SECONDS);
        s.setAttribute(LoginFilter.SESSION_MEMBER, m.getId());
        s.setAttribute(LoginFilter.SESSION_OWNER, m.getOwnerKey());
        s.setAttribute(LoginFilter.SESSION_NAME, m.getUsername());

        // 다른 계정에서 바로 갈아탄 경우가 아닐 때만, 이 브라우저의 데이터를 계정으로 옮긴다
        int moved = 0;
        Object raw = request.getAttribute(LoginFilter.RAW_CLIENT);
        if (!wasLoggedIn && raw instanceof String browserId
                && browserId.matches("^[0-9a-fA-F-]{36}$")) {
            moved = service.moveBrowserData(browserId, m.getOwnerKey());
        }
        return new AuthDto.MeResponse(true, m.getUsername(), moved);
    }
}
