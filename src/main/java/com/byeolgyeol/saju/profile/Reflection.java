package com.byeolgyeol.saju.profile;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 성찰 일기 (DB 테이블: reflection)
 * 성찰 질문에 사용자가 적은 답을 저장한다.
 * 질문 문구는 나중에 질문 데이터가 바뀌어도 그대로 보이도록 함께 저장한다.
 */
@Entity
@Table(name = "reflection", indexes = {
        @Index(name = "idx_reflection_client", columnList = "client_id"),
        @Index(name = "idx_reflection_profile", columnList = "profile_id")
})
public class Reflection {

    public static final int MAX_ANSWER = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_id", nullable = false, length = 36)
    private String clientId;

    @Column(name = "profile_id", nullable = false)
    private Long profileId;

    /** core = 내 결과, year = 올해 운세, career = 직장운, marriage = 결혼운 탭 질문 */
    @Column(name = "question_type", nullable = false, length = 10)
    private String questionType;

    @Column(name = "question_code", nullable = false, length = 30)
    private String questionCode;

    @Column(name = "question_text", nullable = false, length = 200)
    private String questionText;

    @Column(nullable = false, length = MAX_ANSWER)
    private String answer;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Reflection() {}

    public Reflection(String clientId, Long profileId, String questionType,
                      String questionCode, String questionText, String answer) {
        this.clientId = clientId;
        this.profileId = profileId;
        this.questionType = questionType;
        this.questionCode = questionCode;
        this.questionText = questionText;
        this.answer = answer;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public void updateAnswer(String answer) {
        this.answer = answer;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getClientId() { return clientId; }
    public Long getProfileId() { return profileId; }
    public String getQuestionType() { return questionType; }
    public String getQuestionCode() { return questionCode; }
    public String getQuestionText() { return questionText; }
    public String getAnswer() { return answer; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
