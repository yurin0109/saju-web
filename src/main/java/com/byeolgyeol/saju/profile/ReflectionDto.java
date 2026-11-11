package com.byeolgyeol.saju.profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.Set;

/** 성찰 일기 요청 · 응답 모음 */
public final class ReflectionDto {

    private ReflectionDto() {}

    /** 질문 종류: core(내 결과) · year(올해 운세) · career(직장운) · marriage(결혼운) */
    public static final Set<String> TYPES = Set.of("core", "year", "career", "marriage");

    /** 새 성찰 일기 저장 */
    public record CreateRequest(
            @NotNull(message = "어떤 사주에 대한 기록인지 알 수 없어요.")
            Long profileId,

            @NotBlank @Pattern(regexp = "core|year|career|marriage", message = "질문 종류가 올바르지 않아요.")
            String type,

            @NotBlank(message = "질문 정보가 없어요. 페이지를 새로고침해주세요.")
            @Size(max = 30)
            String questionCode,

            @NotBlank(message = "답을 한 글자 이상 적어주세요.")
            @Size(max = Reflection.MAX_ANSWER, message = "1000자까지 적을 수 있어요.")
            String answer
    ) {}

    /** 답 수정 */
    public record UpdateRequest(
            @NotBlank(message = "답을 한 글자 이상 적어주세요.")
            @Size(max = Reflection.MAX_ANSWER, message = "1000자까지 적을 수 있어요.")
            String answer
    ) {}

    public record Response(Long id, Long profileId, String type, String questionCode,
                           String question, String answer,
                           LocalDateTime createdAt, LocalDateTime updatedAt) {
        static Response of(Reflection r) {
            return new Response(r.getId(), r.getProfileId(), r.getQuestionType(), r.getQuestionCode(),
                    r.getQuestionText(), r.getAnswer(), r.getCreatedAt(), r.getUpdatedAt());
        }
    }

    /** 오늘의 성찰 질문 */
    public record Question(String code, String type, String text, int offset, int total) {}
}
