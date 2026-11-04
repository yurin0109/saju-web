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
        return body(HttpStatus.BAD_REQUEST, e.getMessage() != null ? e.getMessage() : "잘못된 요청이에요.");
    }

    @ExceptionHandler({MissingRequestHeaderException.class,
            MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Map<String, String>> badRequest(Exception e) {
        log.warn("잘못된 요청: {}", e.getMessage());
        return body(HttpStatus.BAD_REQUEST, "잘못된 요청이에요. 페이지를 새로고침해주세요.");
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<Map<String, String>> locked(LockedException e) {
        return body(HttpStatus.FORBIDDEN, e.getMessage());
    }

    /** 예상하지 못한 에러: 실행 창에 전체 내용을 남기고, 화면에는 에러 종류를 보여준다 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> unknown(Exception e) {
        log.error("서버 오류", e);
        return body(HttpStatus.INTERNAL_SERVER_ERROR,
                "서버 오류가 났어요 (" + e.getClass().getSimpleName() + "). 실행 창의 에러 내용을 확인해주세요.");
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> notFound(NoSuchElementException e) {
        return body(HttpStatus.NOT_FOUND, e.getMessage());
    }

    private static ResponseEntity<Map<String, String>> body(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("message", message));
    }
}
