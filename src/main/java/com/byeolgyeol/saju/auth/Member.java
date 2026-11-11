package com.byeolgyeol.saju.auth;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 회원 (DB 테이블: app_member)
 * 비밀번호는 원문을 저장하지 않고 PBKDF2 해시만 저장한다.
 * ownerKey: 이 회원의 사주 · 구매 · 성찰 일기를 묶는 열쇠.
 *           (로그인 전에는 브라우저 ID로 저장되고, 로그인하면 이 열쇠로 옮겨진다)
 */
@Entity
@Table(name = "app_member")   // MEMBER 는 MySQL 8 예약어라 app_member 로
public class Member {

    /** 비밀번호를 연속으로 이만큼 틀리면 잠시 잠근다 */
    public static final int MAX_FAILS = 5;
    public static final int LOCK_MINUTES = 10;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 200)
    private String passwordHash;

    @Column(name = "owner_key", nullable = false, unique = true, length = 36)
    private String ownerKey;

    @Column(name = "failed_count", nullable = false)
    private int failedCount;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    /** 복구 코드의 SHA-256 (원래 코드는 가입할 때 한 번만 보여주고 저장하지 않음) */
    @Column(name = "recovery_hash", unique = true, length = 64)
    private String recoveryHash;

    protected Member() {}

    public Member(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.ownerKey = UUID.randomUUID().toString();
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public boolean isLocked(LocalDateTime now) {
        return lockedUntil != null && now.isBefore(lockedUntil);
    }

    /** 비밀번호 틀림: 5번째면 10분 잠금 */
    public void loginFailed(LocalDateTime now) {
        failedCount++;
        if (failedCount >= MAX_FAILS) {
            lockedUntil = now.plusMinutes(LOCK_MINUTES);
            failedCount = 0;
        }
    }

    public void loginSucceeded(LocalDateTime now) {
        failedCount = 0;
        lockedUntil = null;
        lastLoginAt = now;
    }

    public void changePassword(String hash) { this.passwordHash = hash; }

    public void changeRecovery(String hash) { this.recoveryHash = hash; }

    /** 관리자가 잠금 해제 */
    public void unlock() { this.lockedUntil = null; this.failedCount = 0; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }

    public String getRecoveryHash() { return recoveryHash; }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getOwnerKey() { return ownerKey; }
    public LocalDateTime getLockedUntil() { return lockedUntil; }
}
