package com.byeolgyeol.saju.calc;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 음력 ↔ 양력 변환 (1900 ~ 2050년).
 *
 * 한 해를 숫자 하나로 담은 표를 쓴다.
 *  - 아래 4비트: 윤달이 몇 월인지 (0 = 윤달 없음)
 *  - 0x8000 >> (월-1) 비트: 그 달이 30일(큰달)이면 1, 29일(작은달)이면 0
 *  - 0x10000 비트: 윤달이 30일이면 1
 *
 * 표는 천문 계산(Meeus 알고리즘으로 구한 합삭 · 중기 시각)을 한국 표준시(UTC+9) 기준으로
 * 날짜를 정해 만들었다. 중국 음력과는 시차 1시간 때문에 일부 해가 다르다. (예: 1997 · 2027년 설날, 2012년 윤3월)
 */
public final class LunarCalendar {

    public static final int MIN_YEAR = 1900;
    public static final int MAX_YEAR = 2050;

    /** 음력 1900년 1월 1일 = 양력 1900년 1월 31일 */
    private static final LocalDate BASE = LocalDate.of(1900, 1, 31);

    private static final int[] INFO = {
            0x04bd8, 0x04ae0, 0x0a570, 0x05565, 0x0d2a0, 0x0e950, 0x16554, 0x05aa0, 0x0aad0, 0x056d2,
            0x04ae0, 0x0a5d6, 0x0a4d0, 0x0d250, 0x0da95, 0x0b550, 0x056a0, 0x0ada2, 0x095d0, 0x04bb7,
            0x049b0, 0x0a4b0, 0x0b4b5, 0x06a90, 0x0ad40, 0x0bb54, 0x02b60, 0x095b0, 0x05372, 0x04970,
            0x06566, 0x0e4a0, 0x0ea50, 0x16a95, 0x05b50, 0x02b60, 0x18ae3, 0x092e0, 0x1c8d7, 0x0c950,
            0x0d4a0, 0x1d8a6, 0x0b690, 0x056d0, 0x125b4, 0x025d0, 0x092d0, 0x0d2b2, 0x0a950, 0x0d557,
            0x0b4a0, 0x0b550, 0x15555, 0x04db0, 0x025b0, 0x18573, 0x052b0, 0x0a9b8, 0x06950, 0x06aa0,
            0x0aea6, 0x0ab50, 0x04b60, 0x0aae4, 0x0a570, 0x05270, 0x07263, 0x0d950, 0x06b57, 0x056a0,
            0x09ad0, 0x04dd5, 0x04ae0, 0x0a4e0, 0x0d4d4, 0x0d250, 0x0d598, 0x0b540, 0x0d6a0, 0x195a6,
            0x095b0, 0x049b0, 0x0a9b4, 0x0a4b0, 0x0b27a, 0x06a50, 0x06d40, 0x0b756, 0x02b60, 0x095b0,
            0x04b75, 0x04970, 0x064b0, 0x074a3, 0x0ea50, 0x06d98, 0x05ad0, 0x02b60, 0x096e5, 0x092e0,
            0x0c960, 0x0e954, 0x0d4a0, 0x0da50, 0x07552, 0x056c0, 0x0abb7, 0x025d0, 0x092d0, 0x0cab5,
            0x0a950, 0x0b4a0, 0x1b4a3, 0x0b550, 0x055d9, 0x04ba0, 0x0a5b0, 0x05575, 0x052b0, 0x0a950,
            0x0b954, 0x06aa0, 0x0ad50, 0x06b52, 0x04b60, 0x0a6e6, 0x0a570, 0x05270, 0x06a65, 0x0d930,
            0x05aa0, 0x0b6a3, 0x096d0, 0x04afb, 0x04ae0, 0x0a4d0, 0x1d0d6, 0x0d250, 0x0d520, 0x0dd45,
            0x0b6a0, 0x096d0, 0x055b2, 0x049b0, 0x0a577, 0x0a4b0, 0x0b250, 0x1b255, 0x06d40, 0x0ada0,
            0x18b63
    };

    private LunarCalendar() {}

    public record LunarDate(int year, int month, int day, boolean leap) {
        @Override
        public String toString() {
            return "%d-%02d-%02d%s".formatted(year, month, day, leap ? " (윤달)" : "");
        }
    }

    /** 윤달이 몇 월인지 (없으면 0) */
    public static int leapMonth(int year) {
        return info(year) & 0xf;
    }

    /** 평달 일수 (29 또는 30) */
    public static int monthDays(int year, int month) {
        return (info(year) & (0x8000 >> (month - 1))) != 0 ? 30 : 29;
    }

    /** 윤달 일수 (윤달이 없으면 0) */
    public static int leapDays(int year) {
        if (leapMonth(year) == 0) return 0;
        return (info(year) & 0x10000) != 0 ? 30 : 29;
    }

    public static int yearDays(int year) {
        int sum = leapDays(year);
        for (int m = 1; m <= 12; m++) sum += monthDays(year, m);
        return sum;
    }

    /** 음력 → 양력. 없는 날짜(예: 작은달 30일, 윤달이 없는 달의 윤달)는 IllegalArgumentException */
    public static LocalDate toSolar(int year, int month, int day, boolean leap) {
        if (year < MIN_YEAR || year > MAX_YEAR)
            throw new IllegalArgumentException("음력은 %d ~ %d년만 계산할 수 있어요.".formatted(MIN_YEAR, MAX_YEAR));
        if (month < 1 || month > 12) throw new IllegalArgumentException("음력 월은 1 ~ 12 사이여야 해요.");
        if (leap && leapMonth(year) != month)
            throw new IllegalArgumentException(leapMonth(year) == 0
                    ? "음력 %d년에는 윤달이 없어요.".formatted(year)
                    : "음력 %d년의 윤달은 %d월이에요.".formatted(year, leapMonth(year)));
        int max = leap ? leapDays(year) : monthDays(year, month);
        if (day < 1 || day > max)
            throw new IllegalArgumentException("음력 %d년 %s%d월은 %d일까지 있어요.".formatted(year, leap ? "윤" : "", month, max));

        long offset = 0;
        for (int y = MIN_YEAR; y < year; y++) offset += yearDays(y);
        for (int m = 1; m < month; m++) {
            offset += monthDays(year, m);
            if (m == leapMonth(year)) offset += leapDays(year);
        }
        if (leap) offset += monthDays(year, month);
        offset += day - 1;
        return BASE.plusDays(offset);
    }

    /** 양력 → 음력 */
    public static LunarDate fromSolar(LocalDate date) {
        long offset = ChronoUnit.DAYS.between(BASE, date);
        if (offset < 0) throw new IllegalArgumentException("1900년 1월 31일 이후만 변환할 수 있어요.");
        int year = MIN_YEAR;
        while (year <= MAX_YEAR && offset >= yearDays(year)) {
            offset -= yearDays(year);
            year++;
        }
        if (year > MAX_YEAR) throw new IllegalArgumentException("음력 %d년까지만 변환할 수 있어요.".formatted(MAX_YEAR));
        int leap = leapMonth(year);
        for (int m = 1; m <= 12; m++) {
            int days = monthDays(year, m);
            if (offset < days) return new LunarDate(year, m, (int) offset + 1, false);
            offset -= days;
            if (m == leap) {
                if (offset < leapDays(year)) return new LunarDate(year, m, (int) offset + 1, true);
                offset -= leapDays(year);
            }
        }
        throw new IllegalStateException("음력 변환 실패: " + date);
    }

    private static int info(int year) {
        if (year < MIN_YEAR || year > MAX_YEAR)
            throw new IllegalArgumentException("음력은 %d ~ %d년만 계산할 수 있어요.".formatted(MIN_YEAR, MAX_YEAR));
        return INFO[year - MIN_YEAR];
    }
}
