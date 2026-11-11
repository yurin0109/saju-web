package com.byeolgyeol.saju.calc;

import java.time.LocalDate;
import java.time.MonthDay;

/**
 * 절기(월이 바뀌는 12절기) 날짜표.
 *
 * 사주에서 한 달은 음력 1일이 아니라 "절기"에 바뀌고,
 * 한 해는 1월 1일이 아니라 "입춘"에 바뀐다.
 *
 * [한계] 실제 절기 날짜는 해마다 ±1일 정도 달라지지만,
 * 이 프로젝트에서는 평균 날짜로 고정해서 사용한다.
 * (개선 계획: 한국천문연구원 KASI 절기 API로 교체)
 */
public final class SolarTerms {

    private SolarTerms() {}

    /** 인월(寅)부터 축월(丑)까지, 각 달이 시작되는 절기 날짜 */
    private static final MonthDay[] MONTH_START = {
            MonthDay.of(2, 4),   // 입춘 → 인월
            MonthDay.of(3, 6),   // 경칩 → 묘월
            MonthDay.of(4, 5),   // 청명 → 진월
            MonthDay.of(5, 5),   // 입하 → 사월
            MonthDay.of(6, 6),   // 망종 → 오월
            MonthDay.of(7, 7),   // 소서 → 미월
            MonthDay.of(8, 7),   // 입추 → 신월
            MonthDay.of(9, 8),   // 백로 → 유월
            MonthDay.of(10, 8),  // 한로 → 술월
            MonthDay.of(11, 7),  // 입동 → 해월
            MonthDay.of(12, 7),  // 대설 → 자월
            MonthDay.of(1, 6),   // 소한 → 축월 (다음 해 1월)
    };

    public static final String[] TERM_NAMES = {
            "입춘", "경칩", "청명", "입하", "망종", "소서",
            "입추", "백로", "한로", "입동", "대설", "소한"
    };

    /** 입춘 날짜 */
    public static MonthDay ipchun() {
        return MONTH_START[0];
    }

    /**
     * 날짜가 사주상 몇 번째 달인지 (0 = 인월, 1 = 묘월 ... 10 = 자월, 11 = 축월)
     */
    public static int monthOrder(LocalDate date) {
        MonthDay md = MonthDay.from(date);

        // 12/7(대설) 이후 → 자월
        if (!md.isBefore(MONTH_START[10])) return 10;

        // 입동 ~ 입춘까지 역순으로 확인
        for (int order = 9; order >= 0; order--) {
            if (!md.isBefore(MONTH_START[order])) return order;
        }

        // 1/1 ~ 2/3
        if (!md.isBefore(MONTH_START[11])) return 11; // 1/6(소한) 이후 → 축월
        return 10;                                     // 1/1 ~ 1/5 → 전년도 자월
    }
}
