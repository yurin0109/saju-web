package com.byeolgyeol.saju.payment;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class PaymentDto {

    private PaymentDto() {}

    /** mode: toss / mock. clientKey 는 화면에 보내도 되는 키 (시크릿 키는 절대 보내지 않음) */
    public record ConfigResponse(String mode, String clientKey, boolean testMode) {}

    public record OrderRequest(
            @NotNull(message = "어떤 사주로 구매할지 알 수 없어요.") Long profileId,
            @NotBlank(message = "상품을 선택해주세요.") @Size(max = 20) String productCode,
            /** 궁합일 때 상대 프로필 */
            Long partnerId,
            boolean agree
    ) {
        @AssertTrue(message = "구매 내용 확인에 동의해주세요.")
        public boolean isAgreed() { return agree; }
    }

    public record OrderResponse(String orderId, int amount, String orderName, String clientKey, String customerKey) {}

    public record ConfirmRequest(
            // 토스가 돌려주는 값 형식만 허용 (이상한 문자열을 외부 API로 넘기지 않게)
            @NotBlank @Size(max = 200) @Pattern(regexp = "[A-Za-z0-9_\\-]+", message = "결제 정보가 올바르지 않아요.") String paymentKey,
            @NotBlank @Size(max = 64) @Pattern(regexp = "[A-Za-z0-9_\\-]+", message = "주문 정보가 올바르지 않아요.") String orderId,
            int amount
    ) {}
}
