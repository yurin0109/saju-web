package com.byeolgyeol.saju.profile;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 사주 프로필 (DB 테이블: profile)
 * clientId: 로그인 대신 브라우저마다 발급한 고유 ID로 "내 사주"를 구분한다.
 */
@Entity
@Table(name = "profile", indexes = @Index(name = "idx_profile_client", columnList = "client_id"))
public class Profile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_id", nullable = false, length = 36)
    private String clientId;

    @Column(nullable = false, length = 20)
    private String name;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    /** 태어난 시간을 모르면 null */
    @Column(name = "birth_time")
    private LocalTime birthTime;

    /** M / F */
    @Column(nullable = false, length = 1)
    private String gender;

    @Column(name = "birth_place", length = 30)
    private String birthPlace;

    /** 입력한 달력: SOLAR / LUNAR (예전 데이터는 null = 양력) */
    @Column(name = "calendar_type", length = 5)
    private String calendarType;

    /** 음력으로 입력했을 때 입력한 음력 날짜 "YYYY-MM-DD" (birthDate 에는 변환한 양력이 들어감) */
    @Column(name = "lunar_date", length = 10)
    private String lunarDate;

    @Column(name = "leap_month")
    private Boolean leapMonth;

    /** 요즘 고민 분류: LOVE / CAREER / STUDY / MONEY / RELATION / SELF / FUTURE */
    @Column(name = "concern_type", length = 10)
    private String concernType;

    @Column(name = "concern_text", length = 200)
    private String concernText;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Profile() {}

    public Profile(String clientId, String name, LocalDate birthDate, LocalTime birthTime,
                   String gender, String birthPlace) {
        this.clientId = clientId;
        update(name, birthDate, birthTime, gender, birthPlace);
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public void update(String name, LocalDate birthDate, LocalTime birthTime,
                       String gender, String birthPlace) {
        this.name = name;
        this.birthDate = birthDate;
        this.birthTime = birthTime;
        this.gender = gender;
        this.birthPlace = birthPlace;
    }

    /** 음력 입력 정보 (양력이면 lunarDate = null) */
    public void setCalendar(String calendarType, String lunarDate, Boolean leapMonth) {
        this.calendarType = calendarType;
        this.lunarDate = lunarDate;
        this.leapMonth = leapMonth;
    }

    public void setConcern(String concernType, String concernText) {
        this.concernType = concernType;
        this.concernText = concernText;
    }

    public boolean isLunar() { return "LUNAR".equals(calendarType) && lunarDate != null; }
    public String getCalendarType() { return calendarType == null ? "SOLAR" : calendarType; }
    public String getLunarDate() { return lunarDate; }
    public boolean isLeapMonth() { return Boolean.TRUE.equals(leapMonth); }
    public String getConcernType() { return concernType; }
    public String getConcernText() { return concernText; }

    public Long getId() { return id; }
    public String getClientId() { return clientId; }
    public String getName() { return name; }
    public LocalDate getBirthDate() { return birthDate; }
    public LocalTime getBirthTime() { return birthTime; }
    public String getGender() { return gender; }
    public String getBirthPlace() { return birthPlace; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
