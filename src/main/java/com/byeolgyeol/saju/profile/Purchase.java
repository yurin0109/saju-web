package com.byeolgyeol.saju.profile;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 구매 내역 (DB 테이블: purchase)
 * 어떤 프로필로 어떤 상품을 샀는지 기록한다. (결제 정보는 payment_order 에)
 * 가격은 구매 당시 금액을 그대로 남긴다.
 */
@Entity
@Table(name = "purchase", indexes = {
        @Index(name = "idx_purchase_client", columnList = "client_id"),
        @Index(name = "idx_purchase_profile", columnList = "profile_id")
})
public class Purchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_id", nullable = false, length = 36)
    private String clientId;

    @Column(name = "profile_id", nullable = false)
    private Long profileId;

    @Column(name = "product_code", nullable = false, length = 20)
    private String productCode;

    /** 궁합(COMPAT)은 상대마다 따로 산다 → 어떤 상대와의 궁합인지 */
    @Column(name = "partner_id")
    private Long partnerId;

    @Column(nullable = false)
    private int price;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 처음 리포트를 열어본 때 (열어보면 환불 불가) */
    @Column(name = "opened_at")
    private LocalDateTime openedAt;

    /** 환불한 때 (null 이면 사용 중) */
    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    /** 환불 가능 기간: 구매 후 7일 */
    public static final int REFUND_DAYS = 7;

    protected Purchase() {}

    public Purchase(String clientId, Long profileId, Product product, Long partnerId) {
        this.clientId = clientId;
        this.profileId = profileId;
        this.productCode = product.name();
        this.price = product.price();
        this.partnerId = product == Product.COMPAT ? partnerId : null;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public boolean isActive() { return refundedAt == null; }

    public void markOpened(LocalDateTime now) {
        if (openedAt == null) openedAt = now;
    }

    public void refund(LocalDateTime now) { this.refundedAt = now; }

    /** 환불할 수 없는 이유 (가능하면 null) */
    public String refundBlockedReason(LocalDateTime now) {
        if (!isActive()) return "이미 환불한 리포트예요.";
        if (openedAt != null) return "이미 열어본 리포트는 환불할 수 없어요.";
        if (createdAt != null && now.isAfter(createdAt.plusDays(REFUND_DAYS))) return "구매 후 " + REFUND_DAYS + "일이 지나서 환불할 수 없어요.";
        return null;
    }

    public LocalDateTime getOpenedAt() { return openedAt; }
    public LocalDateTime getRefundedAt() { return refundedAt; }
    public Long getId() { return id; }
    public String getClientId() { return clientId; }
    public Long getProfileId() { return profileId; }
    public String getProductCode() { return productCode; }
    public Product getProduct() { return Product.valueOf(productCode); }
    public int getPrice() { return price; }
    public Long getPartnerId() { return partnerId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
