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

    public static final String VERSION = "2026-11-11 궁합 상대 직접 입력 · 첫 상대 무료 v14";

    private final int questionCount = QuestionBank.load().size();

    @GetMapping("/api/version")
    public Map<String, Object> version() {
        return Map.of("version", VERSION, "reflectionQuestions", questionCount);
    }
}
