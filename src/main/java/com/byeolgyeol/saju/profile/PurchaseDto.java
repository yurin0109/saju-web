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

            /** 궁합일 때 상대 프로필 */
            Long partnerId,

            boolean agree
    ) {
        @AssertTrue(message = "구매 내용 확인에 동의해주세요.")
        public boolean isAgreed() {
            return agree;
        }
    }

    /**
     * 구매 내역 한 줄.
     * status: ACTIVE(사용 중) / REFUNDED(환불됨), refundable: 지금 환불 가능한지, refundNote: 안 되면 이유
     */
    public record Response(Long id, Long profileId, String productCode, String title,
                           int price, List<String> unlocks, Long partnerId, LocalDateTime createdAt,
                           String status, boolean opened, boolean refundable, String refundNote,
                           LocalDateTime refundedAt, String partnerName) {
        static Response of(Purchase p) { return of(p, null); }

        static Response of(Purchase p, String partnerName) {
            Product product = p.getProduct();
            String blocked = p.refundBlockedReason(LocalDateTime.now());
            return new Response(p.getId(), p.getProfileId(), product.name(), product.title(), p.getPrice(),
                    product.unlocks().stream().map(Enum::name).toList(), p.getPartnerId(), p.getCreatedAt(),
                    p.isActive() ? "ACTIVE" : "REFUNDED", p.getOpenedAt() != null, blocked == null, blocked,
                    p.getRefundedAt(), partnerName);
        }
    }
}
