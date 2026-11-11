package com.byeolgyeol.saju.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.NoSuchElementException;

/** 에러를 { "message": "..." } 형태로 통일해서 화면에 보여줄 수 있게 한다. */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> invalid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getAllErrors().stream()
                .findFirst()
                .map(err -> err.getDefaultMessage())
                .orElse("입력값을 확인해주세요.");
        return body(HttpStatus.BAD_REQUEST, msg);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> unreadable() {
        return body(HttpStatus.BAD_REQUEST, "날짜나 시간 형식을 확인해주세요.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> illegal(IllegalArgumentException e) {
        log.warn("잘못된 요청: {}", e.getMessage());
        return body(HttpStatus.BAD_REQUEST, safe(e.getMessage()));
    }

    @ExceptionHandler({MissingRequestHeaderException.class,
            MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Map<String, String>> badRequest(Exception e) {
        log.warn("잘못된 요청: {}", e.getMessage());
        return body(HttpStatus.BAD_REQUEST, "잘못된 요청이에요. 페이지를 새로고침해주세요.");
    }

    /** 로그인이 필요한 기능 → 401 + code 로 화면이 로그인 창을 띄운다 */
    @ExceptionHandler(com.byeolgyeol.saju.auth.LoginRequiredException.class)
    public ResponseEntity<Map<String, String>> loginRequired(com.byeolgyeol.saju.auth.LoginRequiredException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", e.getMessage(), "code", "LOGIN_REQUIRED"));
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<Map<String, String>> locked(LockedException e) {
        return body(HttpStatus.FORBIDDEN, e.getMessage());
    }

    /**
     * 예상하지 못한 에러: 자세한 내용은 서버 로그에만 남기고, 화면에는 일반 문구만 보여준다.
     * (에러 종류 · 클래스 이름 · DB 정보가 밖으로 새지 않게)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> unknown(Exception e) {
        log.error("서버 오류", e);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "일시적인 오류가 났어요. 잠시 후 다시 시도해주세요.");
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> notFound(NoSuchElementException e) {
        return body(HttpStatus.NOT_FOUND, e.getMessage());
    }

    /**
     * 우리가 직접 쓴 안내 문구(한글)만 화면에 보여준다.
     * 라이브러리가 던진 영어 메시지(클래스 이름 · 쿼리 등 내부 정보)는 일반 문구로 바꾼다.
     */
    static String safe(String message) {
        if (message == null || !message.matches("(?s).*[가-힣].*") || message.length() > 200) return "잘못된 요청이에요.";
        return message;
    }

    private static ResponseEntity<Map<String, String>> body(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("message", message));
    }
}
