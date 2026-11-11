package com.byeolgyeol.saju.feedback;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class FeedbackDto {

    private FeedbackDto() {}

    public record CreateRequest(
            @NotBlank(message = "어떤 의견인지 골라주세요.")
            @Pattern(regexp = "BUG|IDEA|READING|ETC", message = "의견 종류가 올바르지 않아요.")
            String category,

            @NotBlank(message = "내용을 적어주세요.")
            @Size(min = 5, max = 1000, message = "내용은 5자 이상 1000자 이내로 적어주세요.")
            String message,

            @Size(max = 30) @Pattern(regexp = "[a-z\\-]*", message = "잘못된 요청이에요.")
            String page
    ) {}

    /** 관리자 화면용 (보낸 사람의 브라우저 ID는 내보내지 않음) */
    public record Response(Long id, String category, String message, String page, String username,
                           boolean done, LocalDateTime createdAt) {
        public static Response of(Feedback f) {
            return new Response(f.getId(), f.getCategory().name(), f.getMessage(), f.getPage(), f.getUsername(),
                    f.isDone(), f.getCreatedAt());
        }
    }
}
