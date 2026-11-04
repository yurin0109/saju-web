package com.byeolgyeol.saju.calc;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 사주 계산 경계 케이스 테스트.
 * 입춘 전후, 절기 전후, 자시(23:30) 전후처럼 틀리기 쉬운 지점을 검증한다.
 */
class SajuCalculatorTest {

    private final SajuCalculator calc = new SajuCalculator();

    // ───────── 년주 ─────────

    @Test
    @DisplayName("2026년은 병오년")
    void year2026() {
        assertEquals("병오", calc.yearPillar(LocalDate.of(2026, 6, 1)).korean());
    }

    @Test
    @DisplayName("입춘 전날 출생이면 전년도 년주 (1990-02-03 → 기사)")
    void beforeIpchun() {
        assertEquals("기사", calc.yearPillar(LocalDate.of(1990, 2, 3)).korean());
    }

    @Test
    @DisplayName("입춘 당일 출생이면 해당 년도 년주 (1990-02-04 → 경오)")
    void onIpchun() {
        assertEquals("경오", calc.yearPillar(LocalDate.of(1990, 2, 4)).korean());
    }

    @Test
    @DisplayName("1월 1일 출생도 입춘 전이면 전년도 (2000-01-01 → 기묘년)")
    void newYearsDay() {
        assertEquals("기묘", calc.yearPillar(LocalDate.of(2000, 1, 1)).korean());
    }

    // ───────── 월주 ─────────

    @Test
    @DisplayName("2026년 10월 27일은 무술월")
    void month2026Oct() {
        assertEquals("무술", calc.monthPillar(LocalDate.of(2026, 10, 27)).korean());
    }

    @Test
    @DisplayName("한로(10/8) 전날은 정유월, 당일은 무술월")
    void monthBoundary() {
        assertEquals("정유", calc.monthPillar(LocalDate.of(2026, 10, 7)).korean());
        assertEquals("무술", calc.monthPillar(LocalDate.of(2026, 10, 8)).korean());
    }

    @Test
    @DisplayName("입춘이 되면 월주가 인월로 바뀐다 (2026-02-04 → 경인월)")
    void firstMonth() {
        assertEquals("경인", calc.monthPillar(LocalDate.of(2026, 2, 4)).korean());
    }

    @Test
    @DisplayName("1월 초(소한 전)는 전년도 자월")
    void earlyJanuary() {
        // 2026-01-03 → 사주상 2025 을사년, 자월 → 무자월
        assertEquals("무자", calc.monthPillar(LocalDate.of(2026, 1, 3)).korean());
        // 2026-01-10 → 소한 이후 축월 → 기축월
        assertEquals("기축", calc.monthPillar(LocalDate.of(2026, 1, 10)).korean());
    }

    // ───────── 일주 ─────────

    @Test
    @DisplayName("기준일 2000-01-01은 무오일")
    void baseDay() {
        assertEquals("무오", calc.dayPillar(LocalDate.of(2000, 1, 1)).korean());
    }

    @Test
    @DisplayName("1900-01-01은 갑술일 (기준일 이전 날짜도 계산)")
    void dayBefore2000() {
        assertEquals("갑술", calc.dayPillar(LocalDate.of(1900, 1, 1)).korean());
    }

    @Test
    @DisplayName("일주는 60일마다 반복된다")
    void dayCycle() {
        LocalDate d = LocalDate.of(1994, 8, 17);
        assertEquals(calc.dayPillar(d), calc.dayPillar(d.plusDays(60)));
    }

    // ───────── 시주 ─────────

    @Test
    @DisplayName("무일 14:30 출생 → 기미시")
    void hourPillar() {
        assertEquals("기미", calc.hourPillar(Stem.MU, LocalTime.of(14, 30)).korean());
    }

    @Test
    @DisplayName("시간 보정: 13:29는 오시, 13:30은 미시")
    void hourBoundary() {
        assertEquals(Branch.O, calc.hourPillar(Stem.GAP, LocalTime.of(13, 29)).branch());
        assertEquals(Branch.MI, calc.hourPillar(Stem.GAP, LocalTime.of(13, 30)).branch());
    }

    @Test
    @DisplayName("23:29 출생은 당일 일주, 23:30 출생은 다음날 일주 + 자시")
    void lateNight() {
        SajuChart before = calc.calculate(LocalDate.of(2000, 1, 1), LocalTime.of(23, 29));
        SajuChart after = calc.calculate(LocalDate.of(2000, 1, 1), LocalTime.of(23, 30));

        assertEquals("무오", before.day().korean());
        assertEquals("기미", after.day().korean());
        assertEquals(Branch.JA, after.hour().branch());
        // 기일 자시 → 갑자시
        assertEquals("갑자", after.hour().korean());
    }

    @Test
    @DisplayName("시간을 모르면 시주는 없다")
    void unknownHour() {
        SajuChart chart = calc.calculate(LocalDate.of(1994, 8, 17), null);
        assertFalse(chart.hasHour());
        assertNull(chart.hour());
    }

    // ───────── 오행 비율 ─────────

    @Test
    @DisplayName("오행 비율의 합은 항상 100")
    void percentSum() {
        for (int i = 0; i < 400; i++) {
            LocalDate d = LocalDate.of(1960, 1, 1).plusDays(i * 97L);
            SajuChart withHour = calc.calculate(d, LocalTime.of(i % 24, (i * 7) % 60));
            SajuChart noHour = calc.calculate(d, null);
            assertEquals(100, sum(withHour.elementPercent()), d.toString());
            assertEquals(100, sum(noHour.elementPercent()), d.toString());
        }
    }

    @Test
    @DisplayName("월지에는 가중치 3이 적용된다")
    void monthWeight() {
        // 갑자년 병인월 갑자일 갑자시를 직접 넣어 계산
        // 목: 갑(년간) + 인(월지 x3) + 갑(일간) + 갑(시간) = 6
        // 화: 병(월간) = 1 / 수: 자 x3 = 3 → 총 10점
        Pillar y = new Pillar(Stem.GAP, Branch.JA);
        Pillar m = new Pillar(Stem.BYEONG, Branch.IN);
        Pillar d = new Pillar(Stem.GAP, Branch.JA);
        Pillar h = new Pillar(Stem.GAP, Branch.JA);

        Map<Element, Integer> p = calc.elementPercent(y, m, d, h);
        assertEquals(60, (int) p.get(Element.WOOD));
        assertEquals(10, (int) p.get(Element.FIRE));
        assertEquals(30, (int) p.get(Element.WATER));
        assertEquals(0, (int) p.get(Element.EARTH));
        assertEquals(0, (int) p.get(Element.METAL));
    }

    // ───────── 올해와의 관계 ─────────

    @Test
    @DisplayName("오행 관계: 목 일간 기준")
    void relation() {
        assertEquals(Relation.PEER, Relation.between(Element.WOOD, Element.WOOD));
        assertEquals(Relation.OUTPUT, Relation.between(Element.WOOD, Element.FIRE));
        assertEquals(Relation.WEALTH, Relation.between(Element.WOOD, Element.EARTH));
        assertEquals(Relation.OFFICER, Relation.between(Element.WOOD, Element.METAL));
        assertEquals(Relation.RESOURCE, Relation.between(Element.WOOD, Element.WATER));
    }

    private static int sum(Map<Element, Integer> m) {
        return m.values().stream().mapToInt(Integer::intValue).sum();
    }
}
