package com.byeolgyeol.saju.profile;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

/** 상품 · 구매 요청과 응답 */
public final class PurchaseDto {

    private PurchaseDto() {}

    public record ProductResponse(String code, String title, int price, String includes, List<String> unlocks) {
        static ProductResponse of(Product p) {
            return new ProductResponse(p.name(), p.title(), p.price(), p.includes(),
                    p.unlocks().stream().map(Enum::name).toList());
        }
    }

    public record CreateRequest(
            @NotNull(message = "어떤 사주로 구매할지 알 수 없어요.")
            Long profileId,

            @NotBlank(message = "상품을 선택해주세요.")
            String productCode,

            boolean agree
    ) {
        @AssertTrue(message = "구매 내용 확인에 동의해주세요.")
        public boolean isAgreed() {
            return agree;
        }
    }

    public record Response(Long id, Long profileId, String productCode, String title,
                           int price, List<String> unlocks, LocalDateTime createdAt) {
        static Response of(Purchase p) {
            Product product = p.getProduct();
            return new Response(p.getId(), p.getProfileId(), product.name(), product.title(), p.getPrice(),
                    product.unlocks().stream().map(Enum::name).toList(), p.getCreatedAt());
        }
    }
}
