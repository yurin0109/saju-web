package com.byeolgyeol.saju.profile;

import jakarta.validation.constraints.*;

import java.time.LocalTime;

/**
 * 사주 입력 폼 요청.
 * birthDate 는 "YYYY-MM-DD" 문자열로 받는다.
 * (음력은 2월 30일처럼 양력에 없는 날짜가 있을 수 있어서 LocalDate 로 바로 받지 않음)
 */
public record ProfileRequest(
        @NotBlank(message = "이름을 입력해주세요.")
        @Size(max = 20, message = "이름은 20자 이내로 입력해주세요.")
        String name,

        /** SOLAR(양력) / LUNAR(음력). 비어 있으면 양력 */
        @Pattern(regexp = "SOLAR|LUNAR", message = "양력 / 음력 값이 올바르지 않아요.")
        String calendar,

        @NotBlank(message = "생년월일을 입력해주세요.")
        @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "생년월일은 1994.08.17 처럼 8자리로 입력해주세요.")
        String birthDate,

        /** 음력일 때 윤달 여부 */
        Boolean leapMonth,

        /** null 이면 시간 모름 */
        LocalTime birthTime,

        @NotBlank(message = "성별을 선택해주세요.")
        @Pattern(regexp = "[MF]", message = "성별 값이 올바르지 않아요.")
        String gender,

        @Size(max = 30)
        String birthPlace,

        /** 요즘 고민 분류 (선택) */
        @Pattern(regexp = "LOVE|CAREER|MONEY|RELATION|SELF|FUTURE", message = "고민 분류 값이 올바르지 않아요.")
        String concernType,

        /** 요즘 고민 (선택) */
        @Size(max = 200, message = "고민은 200자 이내로 적어주세요.")
        String concernText
) {
    public boolean lunar() {
        return "LUNAR".equals(calendar);
    }

    public boolean leap() {
        return lunar() && Boolean.TRUE.equals(leapMonth);
    }
}
