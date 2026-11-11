package com.byeolgyeol.saju.payment;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 결제 주문 (DB 테이블: payment_order).
 * 결제창을 띄우기 전에 서버가 주문번호와 금액을 먼저 정해서 저장해두고,
 * 결제가 끝나 돌아왔을 때 금액이 같은지 비교한다. (화면에서 금액을 바꿔치기하는 것을 막기 위해)
 */
@Entity
@Table(name = "payment_order", indexes = @Index(name = "idx_order_client", columnList = "client_id"))
public class PaymentOrder {

    public enum Status { READY, PAID, FAILED, CANCELED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, unique = true, length = 64)
    private String orderId;

    @Column(name = "client_id", nullable = false, length = 36)
    private String clientId;

    @Column(name = "profile_id", nullable = false)
    private Long profileId;

    @Column(name = "product_code", nullable = false, length = 20)
    private String productCode;

    @Column(nullable = false)
    private int amount;

    /** 궁합 주문일 때 상대 프로필 */
    @Column(name = "partner_id")
    private Long partnerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Status status;

    @Column(name = "payment_key", length = 200)
    private String paymentKey;

    /** 결제 수단 (카드, 간편결제 등. 토스 응답 그대로) */
    @Column(length = 30)
    private String method;

    @Column(name = "fail_reason", length = 300)
    private String failReason;

    @Column(name = "purchase_id")
    private Long purchaseId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    protected PaymentOrder() {}

    public PaymentOrder(String orderId, String clientId, Long profileId, String productCode, int amount, Long partnerId) {
        this.partnerId = partnerId;
        this.orderId = orderId;
        this.clientId = clientId;
        this.profileId = profileId;
        this.productCode = productCode;
        this.amount = amount;
        this.status = Status.READY;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public void paid(String paymentKey, String method, Long purchaseId) {
        this.status = Status.PAID;
        this.paymentKey = paymentKey;
        this.method = method;
        this.purchaseId = purchaseId;
        this.approvedAt = LocalDateTime.now();
    }

    public void failed(String reason) {
        this.status = Status.FAILED;
        this.failReason = reason == null ? null : reason.substring(0, Math.min(300, reason.length()));
    }

    /** 환불(결제 취소) 완료 */
    public void canceled() {
        this.status = Status.CANCELED;
    }

    public String getPaymentKey() { return paymentKey; }
    public Long getId() { return id; }
    public String getOrderId() { return orderId; }
    public String getClientId() { return clientId; }
    public Long getProfileId() { return profileId; }
    public String getProductCode() { return productCode; }
    public int getAmount() { return amount; }
    public Long getPartnerId() { return partnerId; }
    public Status getStatus() { return status; }
    public Long getPurchaseId() { return purchaseId; }
}
