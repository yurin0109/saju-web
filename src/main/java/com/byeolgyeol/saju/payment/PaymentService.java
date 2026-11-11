package com.byeolgyeol.saju.payment;

import com.byeolgyeol.saju.profile.Product;
import com.byeolgyeol.saju.profile.PurchaseDto;
import com.byeolgyeol.saju.profile.PurchaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * 토스 결제 흐름
 *  1. createOrder : 서버가 주문번호 · 금액을 정해서 저장 → 화면이 이 값으로 토스 결제창을 띄움
 *  2. (사용자가 결제창에서 결제) → 토스가 successUrl 로 paymentKey · orderId · amount 를 붙여 돌려보냄
 *  3. confirm     : 저장해둔 주문과 금액이 같은지 확인 → 토스 승인 API 호출 → 성공하면 구매 기록 저장
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentConfig config;
    private final PaymentOrderRepository orders;
    private final PurchaseService purchases;
    private final TossClient toss;

    public PaymentService(PaymentConfig config, PaymentOrderRepository orders, PurchaseService purchases, TossClient toss) {
        this.config = config;
        this.orders = orders;
        this.purchases = purchases;
        this.toss = toss;
    }

    @Transactional
    public PaymentDto.OrderResponse createOrder(String clientId, PaymentDto.OrderRequest req) {
        if (!config.toss()) throw new IllegalArgumentException("토스 결제 키가 설정되지 않았어요.");
        Product product = purchases.check(clientId, req.profileId(), req.productCode(), req.partnerId());
        String orderId = "ats-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        orders.save(new PaymentOrder(orderId, clientId, req.profileId(), product.name(), product.price(),
                product == Product.COMPAT ? req.partnerId() : null));
        return new PaymentDto.OrderResponse(orderId, product.price(), product.title(), config.clientKey(), customerKey(clientId));
    }

    @Transactional
    public PurchaseDto.Response confirm(String clientId, PaymentDto.ConfirmRequest req) {
        PaymentOrder order = orders.findByOrderIdAndClientId(req.orderId(), clientId)
                .orElseThrow(() -> new NoSuchElementException("주문 정보를 찾을 수 없어요."));

        // 새로고침 등으로 같은 주문을 다시 승인하려 하면, 이미 만든 구매를 그대로 돌려준다
        if (order.getStatus() == PaymentOrder.Status.PAID) {
            return purchases.findAll(clientId).stream()
                    .filter(p -> p.id().equals(order.getPurchaseId())).findFirst()
                    .orElseThrow(() -> new NoSuchElementException("구매 내역을 찾을 수 없어요."));
        }
        if (order.getStatus() == PaymentOrder.Status.FAILED) {
            throw new IllegalArgumentException("이미 실패한 주문이에요. 다시 결제해주세요.");
        }
        // 금액 검증: 결제창에서 돌아온 금액이 서버가 정한 금액과 다르면 승인하지 않는다
        if (req.amount() != order.getAmount()) {
            order.failed("금액 불일치: " + req.amount());
            throw new IllegalArgumentException("결제 금액이 주문 금액과 달라요.");
        }

        TossClient.Result result = toss.confirm(req.paymentKey(), order.getOrderId(), order.getAmount());
        if (!result.ok()) {
            log.warn("토스 승인 실패 orderId={} code={} message={}", order.getOrderId(), result.code(), result.message());
            order.failed(result.code() + " " + result.message());
            throw new IllegalArgumentException(result.message());
        }

        PurchaseDto.Response purchase = purchases.grant(clientId, order.getProfileId(),
                Product.valueOf(order.getProductCode()), order.getPartnerId());
        order.paid(req.paymentKey(), result.method(), purchase.id());
        return purchase;
    }

    /**
     * 환불: 구매 후 7일 이내 + 한 번도 열어보지 않은 리포트만.
     * 토스로 결제했으면 토스 결제 취소 API로 실제 환불, 모의 결제면 기록만 환불 처리.
     */
    @Transactional
    public PurchaseDto.Response refund(String clientId, Long purchaseId) {
        com.byeolgyeol.saju.profile.Purchase p = purchases.refundable(clientId, purchaseId);
        PaymentOrder order = orders.findByPurchaseIdAndClientId(purchaseId, clientId).orElse(null);
        if (order != null && order.getStatus() == PaymentOrder.Status.PAID && order.getPaymentKey() != null) {
            if (!config.toss()) throw new IllegalArgumentException("지금은 환불을 처리할 수 없어요. 잠시 후 다시 시도해주세요.");
            TossClient.Result result = toss.cancel(order.getPaymentKey(), "고객 환불 요청 (구매 후 7일 이내 · 미열람)", "refund-" + order.getOrderId());
            if (!result.ok()) {
                log.warn("토스 환불 실패 orderId={} code={} message={}", order.getOrderId(), result.code(), result.message());
                throw new IllegalArgumentException("환불 처리에 실패했어요. " + result.message());
            }
            order.canceled();
        }
        return purchases.markRefunded(p);
    }

    public PaymentDto.ConfigResponse config() {
        return new PaymentDto.ConfigResponse(config.toss() ? "toss" : "mock", config.toss() ? config.clientKey() : null,
                config.testMode());
    }

    /**
     * 토스에 보내는 고객 키: 데이터 열쇠(clientId)를 그대로 보내지 않고 SHA-256 으로 바꾼 값.
     * (같은 사람이면 항상 같은 값, 이 값으로는 우리 데이터에 접근할 수 없음)
     */
    static String customerKey(String clientId) {
        try {
            byte[] h = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(("ats-customer:" + clientId).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return "c_" + java.util.HexFormat.of().formatHex(h).substring(0, 40);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
