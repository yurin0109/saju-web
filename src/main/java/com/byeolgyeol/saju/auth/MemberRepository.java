package com.byeolgyeol.saju.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByOwnerKey(String ownerKey);

    Optional<Member> findByRecoveryHash(String recoveryHash);
}
