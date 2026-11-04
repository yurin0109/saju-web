package com.byeolgyeol.saju.profile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProfileRepository extends JpaRepository<Profile, Long> {

    List<Profile> findByClientIdOrderByCreatedAtDesc(String clientId);

    Optional<Profile> findByIdAndClientId(Long id, String clientId);
}
