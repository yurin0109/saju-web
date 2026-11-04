package com.byeolgyeol.saju.profile;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 구매 내역 (DB 테이블: purchase)
 * 모의 결제라서 실제 결제 정보는 저장하지 않고, 어떤 프로필로 어떤 상품을 샀는지만 기록한다.
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

    @Column(nullable = false)
    private int price;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Purchase() {}

    public Purchase(String clientId, Long profileId, Product product) {
        this.clientId = clientId;
        this.profileId = profileId;
        this.productCode = product.name();
        this.price = product.price();
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getClientId() { return clientId; }
    public Long getProfileId() { return profileId; }
    public String getProductCode() { return productCode; }
    public Product getProduct() { return Product.valueOf(productCode); }
    public int getPrice() { return price; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
