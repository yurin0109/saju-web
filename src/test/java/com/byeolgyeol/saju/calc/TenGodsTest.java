package com.byeolgyeol.saju.calc;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TenGodsTest {

    private final SajuCalculator calc = new SajuCalculator();

    /** 계미시 · 을해일 · 임신월 · 갑술년 (1994-08-17 14:30) */
    private SajuChart sample() {
        return calc.calculate(LocalDate.of(1994, 8, 17), LocalTime.of(14, 30));
    }

    @Test
    @DisplayName("십성 분포: 을목 일간 기준으로 7글자를 센다 (월지 2점)")
    void countSample() {
        // 을(목) 기준: 년간 갑목=비겁, 년지 술토=재성, 월간 임수=인성, 월지 신금=관성(x2),
        //              일지 해수=인성, 시간 계수=인성, 시지 미토=재성
        Map<Relation, Integer> c = TenGods.count(sample());
        assertEquals(1, (int) c.get(Relation.PEER));
        assertEquals(0, (int) c.get(Relation.OUTPUT));
        assertEquals(2, (int) c.get(Relation.WEALTH));
        assertEquals(2, (int) c.get(Relation.OFFICER));
        assertEquals(3, (int) c.get(Relation.RESOURCE));
        assertEquals(8, TenGods.total(c));
        assertEquals(Relation.RESOURCE, TenGods.dominant(c));
    }

    @Test
    @DisplayName("시간을 모르면 시주 두 글자를 빼고 센다")
    void countWithoutHour() {
        SajuChart chart = calc.calculate(LocalDate.of(1994, 8, 17), null);
        assertEquals(6, TenGods.total(TenGods.count(chart)));
    }

    @Test
    @DisplayName("배우자 별: 여성은 관성, 남성은 재성")
    void spouseStar() {
        assertEquals(Relation.OFFICER, TenGods.spouseStar("F"));
        assertEquals(Relation.WEALTH, TenGods.spouseStar("M"));
    }

    @Test
    @DisplayName("배우자 자리: 일지(해수)와 을목의 관계는 인성")
    void spousePalace() {
        assertEquals(Relation.RESOURCE, TenGods.spousePalace(sample()));
    }

    @Test
    @DisplayName("배우자 별 점수 구간: 0 없음, 1~2 적당, 3 이상 많음")
    void level() {
        assertEquals("NONE", TenGods.level(0));
        assertEquals("BALANCED", TenGods.level(1));
        assertEquals("BALANCED", TenGods.level(2));
        assertEquals("MANY", TenGods.level(3));
    }

    @Test
    @DisplayName("모든 십성에 직장운 · 결혼운 문구가 준비돼 있다")
    void textsExist() {
        for (Relation r : Relation.values()) {
            assertTrue(Interpretation.CAREER.containsKey(r));
            assertTrue(Interpretation.CAREER_YEAR.containsKey(r));
            assertTrue(Interpretation.MARRIAGE_PALACE.containsKey(r));
            assertTrue(Interpretation.MARRIAGE_CAUTION.containsKey(r));
            assertTrue(Interpretation.MARRIAGE_ACTION.containsKey(r));
        }
        for (String lv : new String[]{"NONE", "BALANCED", "MANY"}) {
            assertTrue(Interpretation.MARRIAGE_STAR.containsKey(lv));
        }
    }
}
