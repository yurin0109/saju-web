package com.byeolgyeol.saju.calc;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class LunarCalendarTest {

    @Test
    @DisplayName("설날(음력 1월 1일) → 양력")
    void newYear() {
        assertEquals(LocalDate.of(2026, 2, 17), LunarCalendar.toSolar(2026, 1, 1, false));
        assertEquals(LocalDate.of(2000, 2, 5), LunarCalendar.toSolar(2000, 1, 1, false));
        assertEquals(LocalDate.of(1900, 1, 31), LunarCalendar.toSolar(1900, 1, 1, false));
    }

    @Test
    @DisplayName("한국과 중국 음력이 다른 해: 1997 · 2027년 설날은 한국 기준")
    void koreanCalendar() {
        assertEquals(LocalDate.of(1997, 2, 8), LunarCalendar.toSolar(1997, 1, 1, false));
        assertEquals(LocalDate.of(2027, 2, 7), LunarCalendar.toSolar(2027, 1, 1, false));
    }

    @Test
    @DisplayName("윤달: 2012년은 윤3월, 2017년은 윤5월 (한국 기준)")
    void leapMonth() {
        assertEquals(3, LunarCalendar.leapMonth(2012));
        assertEquals(5, LunarCalendar.leapMonth(2017));
        assertEquals(0, LunarCalendar.leapMonth(2026));
        assertEquals(LocalDate.of(2012, 5, 5), LunarCalendar.toSolar(2012, 3, 15, true));
        assertEquals(LocalDate.of(2017, 6, 24), LunarCalendar.toSolar(2017, 5, 1, true));
    }

    @Test
    @DisplayName("양력 → 음력, 음력 → 양력이 서로 맞는다 (1900 ~ 2050년 매일)")
    void roundTrip() {
        LocalDate d = LocalDate.of(1900, 1, 31);
        LocalDate end = LocalDate.of(2050, 12, 31);
        while (d.isBefore(end)) {
            LunarCalendar.LunarDate l = LunarCalendar.fromSolar(d);
            assertEquals(d, LunarCalendar.toSolar(l.year(), l.month(), l.day(), l.leap()), d.toString());
            d = d.plusDays(1);
        }
    }

    @Test
    @DisplayName("한 해는 353 ~ 385일, 한 달은 29 또는 30일")
    void lengths() {
        for (int y = LunarCalendar.MIN_YEAR; y <= LunarCalendar.MAX_YEAR; y++) {
            int days = LunarCalendar.yearDays(y);
            assertTrue(days >= 353 && days <= 385, y + "년 " + days + "일");
        }
    }

    @Test
    @DisplayName("없는 날짜는 거절한다")
    void invalid() {
        assertThrows(IllegalArgumentException.class, () -> LunarCalendar.toSolar(2026, 1, 31, false));
        assertThrows(IllegalArgumentException.class, () -> LunarCalendar.toSolar(2026, 3, 1, true));  // 2026년엔 윤달 없음
        assertThrows(IllegalArgumentException.class, () -> LunarCalendar.toSolar(2012, 4, 1, true));  // 2012년 윤달은 3월
        assertThrows(IllegalArgumentException.class, () -> LunarCalendar.toSolar(1899, 1, 1, false));
    }
}
