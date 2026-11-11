package com.byeolgyeol.saju.profile;

import com.byeolgyeol.saju.common.LockedException;

import com.byeolgyeol.saju.auth.Login;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 상품 · 구매 REST API (모의 결제)
 *
 * GET    /api/products         상품 목록 (개별 리포트 7종 + 묶음)
 * GET    /api/purchases        내 구매 내역 (로그인했을 때만, 아니면 빈 목록)
 * POST   /api/purchases        구매 (모의 결제, 로그인 필수)
 * POST   /api/purchases/{id}/refund  환불 요청 (로그인, 7일 이내 · 미열람)
 */
@RestController
public class PurchaseController {

    private static final String CLIENT_HEADER = "X-Client-Id";

    private final PurchaseService service;

    private final com.byeolgyeol.saju.payment.PaymentConfig paymentConfig;

    private final com.byeolgyeol.saju.payment.PaymentService paymentService;

    public PurchaseController(PurchaseService service, com.byeolgyeol.saju.payment.PaymentConfig paymentConfig,
                              com.byeolgyeol.saju.payment.PaymentService paymentService) {
        this.service = service;
        this.paymentConfig = paymentConfig;
        this.paymentService = paymentService;
    }

    /** 환불 요청: 구매 후 7일 이내, 한 번도 열어보지 않은 리포트만 */
    @PostMapping("/api/purchases/{id}/refund")
    public PurchaseDto.Response refund(@RequestHeader(CLIENT_HEADER) String clientId, @PathVariable Long id,
                                       HttpServletRequest http) {
        Login.require(http);
        return paymentService.refund(ProfileController.ClientId.check(clientId), id);
    }

    @GetMapping("/api/products")
    public List<PurchaseDto.ProductResponse> products() {
        return service.products();
    }

    @GetMapping("/api/purchases")
    public List<PurchaseDto.Response> list(@RequestHeader(CLIENT_HEADER) String clientId, HttpServletRequest request) {
        if (!Login.isLoggedIn(request)) return List.of();
        return service.findAll(ProfileController.ClientId.check(clientId));
    }

    /**
     * 모의 결제 (payment.mode=mock 일 때만).
     * 토스 결제 모드에서는 결제 승인(/api/payments/confirm)을 거쳐야만 구매가 생긴다.
     * 그렇지 않으면 누구나 이 주소를 직접 호출해서 공짜로 리포트를 열 수 있기 때문.
     */
    @PostMapping("/api/purchases")
    @ResponseStatus(HttpStatus.CREATED)
    public PurchaseDto.Response create(@RequestHeader(CLIENT_HEADER) String clientId,
                                       @Valid @RequestBody PurchaseDto.CreateRequest request,
                                       HttpServletRequest http) {
        Login.require(http);
        if (!paymentConfig.mock()) throw new LockedException("토스 결제로만 구매할 수 있어요.");
        return service.create(ProfileController.ClientId.check(clientId), request);
    }
}
