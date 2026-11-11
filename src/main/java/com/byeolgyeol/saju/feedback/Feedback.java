package com.byeolgyeol.saju.feedback;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 건의사항 (DB 테이블: feedback)
 * 사용자가 화면 오른쪽 아래 "의견 보내기"로 남긴 글. 관리자 페이지(/admin.html)에서 본다.
 */
@Entity
@Table(name = "feedback", indexes = @Index(name = "idx_feedback_created", columnList = "created_at"))
public class Feedback {

    public enum Category { BUG, IDEA, READING, ETC }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 누가 보냈는지 구분용 (브라우저 ID 또는 회원 열쇠). 관리자 화면에는 보여주지 않음 */
    @Column(name = "client_id", length = 36)
    private String clientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Category category;

    @Column(nullable = false, length = 1000)
    private String message;

    /** 어느 화면에서 보냈는지 (예: result, love-love) */
    @Column(length = 30)
    private String page;

    /** 로그인한 회원이면 아이디 (아니면 null) */
    @Column(length = 20)
    private String username;

    @Column(nullable = false)
    private boolean done;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Feedback() {}

    public Feedback(String clientId, Category category, String message, String page, String username) {
        this.clientId = clientId;
        this.category = category;
        this.message = message;
        this.page = page;
        this.username = username;
    }

    @PrePersist
    void onCreate() { this.createdAt = LocalDateTime.now(); }

    public void markDone(boolean done) { this.done = done; }

    public Long getId() { return id; }
    public String getClientId() { return clientId; }
    public Category getCategory() { return category; }
    public String getMessage() { return message; }
    public String getPage() { return page; }
    public String getUsername() { return username; }
    public boolean isDone() { return done; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
