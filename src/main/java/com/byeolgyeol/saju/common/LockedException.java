package com.byeolgyeol.saju.common;

/** 구매하지 않은 유료 리포트에 접근했을 때 (403) */
public class LockedException extends RuntimeException {
    public LockedException(String message) {
        super(message);
    }
}
