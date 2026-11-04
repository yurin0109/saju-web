package com.byeolgyeol.saju.profile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

    List<Purchase> findByClientIdOrderByCreatedAtDesc(String clientId);

    List<Purchase> findByClientIdAndProfileId(String clientId, Long profileId);

    Optional<Purchase> findByIdAndClientId(Long id, String clientId);

    void deleteByClientIdAndProfileId(String clientId, Long profileId);
}
