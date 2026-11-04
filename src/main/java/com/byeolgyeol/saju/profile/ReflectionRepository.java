package com.byeolgyeol.saju.profile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReflectionRepository extends JpaRepository<Reflection, Long> {

    List<Reflection> findByClientIdOrderByCreatedAtDesc(String clientId);

    List<Reflection> findByClientIdAndProfileIdOrderByCreatedAtDesc(String clientId, Long profileId);

    Optional<Reflection> findByIdAndClientId(Long id, String clientId);

    void deleteByClientIdAndProfileId(String clientId, Long profileId);
}
