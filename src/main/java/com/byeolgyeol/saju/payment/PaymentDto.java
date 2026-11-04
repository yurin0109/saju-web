package com.byeolgyeol.saju.payment;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class PaymentDto {

    private PaymentDto() {}

    /** mode: toss / mock. clientKey 는 화면에 보내도 되는 키 (시크릿 키는 절대 보내지 않음) */
    public record ConfigResponse(String mode, String clientKey, boolean testMode) {}

    public record OrderRequest(
            @NotNull(message = "어떤 사주로 구매할지 알 수 없어요.") Long profileId,
            @NotBlank(message = "상품을 선택해주세요.") String productCode,
            boolean agree
    ) {
        @AssertTrue(message = "구매 내용 확인에 동의해주세요.")
        public boolean isAgreed() { return agree; }
    }

    public record OrderResponse(String orderId, int amount, String orderName, String clientKey, String customerKey) {}

    public record ConfirmRequest(
            @NotBlank String paymentKey,
            @NotBlank String orderId,
            int amount
    ) {}
}
