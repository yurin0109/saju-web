package com.byeolgyeol.saju.common;

import com.byeolgyeol.saju.calc.QuestionBank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 지금 실행 중인 서버 버전 확인용.
 * 브라우저에서 http://localhost:8080/api/version 을 열면 버전과 성찰 질문 개수가 보인다.
 */
@RestController
public class VersionController {

    public static final String VERSION = "2026-11-12 화면 캐시 방지 · 시간 24시간 입력 v18";

    private final int questionCount = QuestionBank.load().size();

    @GetMapping("/api/version")
    public Map<String, Object> version() {
        return Map.of("version", VERSION, "reflectionQuestions", questionCount);
    }
}
