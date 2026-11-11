package com.byeolgyeol.saju.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {
    Optional<PaymentOrder> findByOrderIdAndClientId(String orderId, String clientId);

    Optional<PaymentOrder> findByPurchaseIdAndClientId(Long purchaseId, String clientId);

    /** 로그인하면 브라우저 ID로 저장된 데이터를 회원 열쇠(ownerKey)로 옮긴다 */
    @Modifying
    @Query("update PaymentOrder x set x.clientId = :to where x.clientId = :from")
    int moveOwner(@Param("from") String from, @Param("to") String to);
}
