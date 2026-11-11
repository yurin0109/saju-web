package com.byeolgyeol.saju.profile;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

    List<Purchase> findByClientIdOrderByCreatedAtDesc(String clientId);

    List<Purchase> findByClientIdAndProfileId(String clientId, Long profileId);

    boolean existsByClientIdAndProfileId(String clientId, Long profileId);

    boolean existsByClientIdAndPartnerId(String clientId, Long partnerId);

    Optional<Purchase> findByIdAndClientId(Long id, String clientId);

    void deleteByClientIdAndProfileId(String clientId, Long profileId);

    /** 로그인하면 브라우저 ID로 저장된 데이터를 회원 열쇠(ownerKey)로 옮긴다 */
    @Modifying
    @Query("update Purchase x set x.clientId = :to where x.clientId = :from")
    int moveOwner(@Param("from") String from, @Param("to") String to);
}
