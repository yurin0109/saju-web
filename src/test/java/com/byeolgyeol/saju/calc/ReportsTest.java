package com.byeolgyeol.saju.calc;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class ReportsTest {

    private final SajuCalculator calc = new SajuCalculator();

    @Test
    @DisplayName("심화 리포트는 같은 탭의 무료 풀이보다 3배 이상 길다")
    void premiumIsLonger() {
        for (Relation r : Relation.values()) {
            SajuChart chart = chartWithYearRelation(r);
            Interpretation.YearText yt = Interpretation.YEAR.get(r);
            int freeYear = yt.body().length() + yt.action().length();
            Report year = Reports.yearPremium("테스트", chart, 2026);
            assertTrue(year.length() >= freeYear * 3, r + " 올해 운세 " + year.length() + " < 3 × " + freeYear);
        }
        for (Relation r : Relation.values()) {
            Interpretation.CareerText ct = Interpretation.CAREER.get(r);
            int free = ct.body().length() + ct.action().length();
            int premium = minCareer(r);
            assertTrue(premium >= free * 3, r + " 직장운 " + premium + " < 3 × " + free);
        }
        for (LocalDate d = LocalDate.of(1985, 1, 1); d.getYear() < 1995; d = d.plusDays(29)) {
            SajuChart c = calc.calculate(d, null);
            for (String g : new String[]{"M", "F"}) {
                String level = TenGods.level(TenGods.count(c).get(TenGods.spouseStar(g)));
                Relation palace = TenGods.spousePalace(c);
                int free = Interpretation.MARRIAGE_STAR.get(level).body().length() + Interpretation.MARRIAGE_PALACE.get(palace).length();
                int premium = Reports.marriagePremium("가", g, c, 2026).length();
                assertTrue(premium >= free * 3, "결혼운 " + premium + " < 3 × " + free);
            }
        }
    }

    @Test
    @DisplayName("1950 ~ 2010년 여러 생일로 모든 리포트를 만들어도 빈 문구가 없다")
    void noMissingText() {
        SajuChart other = calc.calculate(LocalDate.of(1994, 8, 17), LocalTime.of(14, 30));
        for (LocalDate d = LocalDate.of(1950, 1, 3); d.getYear() < 2010; d = d.plusDays(37)) {
            SajuChart c = calc.calculate(d, d.getDayOfMonth() % 3 == 0 ? null : LocalTime.of(d.getDayOfMonth() % 24, 0));
            for (Report r : new Report[]{
                    Reports.yearPremium("가", c, 2026), Reports.careerPremium("가", c, 2026),
                    Reports.marriagePremium("가", d.getDayOfMonth() % 2 == 0 ? "M" : "F", c, 2026),
                    Reports.reunionBasic("가", c, 2026), Reports.reunionPremium("가", c, 2026),
                    Reports.familyBasic("가", c), Reports.familyPremium("가", c, 2026),
                    Reports.compatBasic("가", c, "나", other), Reports.compatPremium("가", c, "나", other)}) {
                for (Report.Section s : r.sections()) {
                    assertFalse(s.paragraphs().isEmpty() && s.items().isEmpty(), r.code() + " " + s.title());
                    for (String p : s.paragraphs()) assertNotNull(p, r.code() + " " + s.title());
                    for (Report.Item i : s.items()) assertNotNull(i.text(), r.code() + " " + s.title());
                }
            }
            for (String type : new String[]{"LOVE", "CAREER", "MONEY", "RELATION", "SELF", "FUTURE"}) {
                Reports.ConcernDto cd = Reports.concern(type, "고민", "가", c, 2026);
                for (String p : cd.paragraphs()) assertNotNull(p);
            }
        }
    }

    @Test
    @DisplayName("올해 운세 심화의 월별 흐름은 이번 달부터 12개월")
    void twelveMonths() {
        Report r = Reports.yearPremium("가", calc.calculate(LocalDate.of(1994, 8, 17), null), 2026, LocalDate.of(2026, 11, 3));
        Report.Section months = r.sections().stream().filter(s -> s.title().contains("월별")).findFirst().orElseThrow();
        assertEquals(12, months.items().size());
        assertEquals("이번 달 · 2026년 11월", months.items().get(0).label());
        assertEquals("2027년 1월", months.items().get(2).label());
        assertEquals("10월", months.items().get(11).label());
        assertTrue(months.items().get(0).tag().startsWith("기해월"), months.items().get(0).tag());
        // 분기별 플랜은 월별 흐름 바로 다음, 같은 12개월 기준
        int at = r.sections().indexOf(months);
        Report.Section quarters = r.sections().get(at + 1);
        assertEquals("분기별 실천 플랜", quarters.title());
        assertEquals("11 ~ 1월", quarters.items().get(0).tag());
        assertEquals("8 ~ 10월", quarters.items().get(3).tag());
    }

    @Test
    @DisplayName("궁합: 합 · 충 · 천간합 판정과 점수 범위")
    void compat() {
        assertTrue(Reports.hap(Branch.JA, Branch.CHUK));
        assertTrue(Reports.hap(Branch.O, Branch.MI));
        assertTrue(Reports.chung(Branch.JA, Branch.O));
        assertFalse(Reports.chung(Branch.JA, Branch.CHUK));
        assertTrue(Reports.stemHap(Stem.GAP, Stem.GI));
        for (LocalDate d = LocalDate.of(1980, 1, 1); d.getYear() < 2000; d = d.plusDays(53)) {
            int s = Reports.compatScore(calc.calculate(d, null), calc.calculate(d.plusDays(400), null)).score();
            assertTrue(s >= 45 && s <= 98);
        }
    }

    @Test
    @DisplayName("고민을 적지 않으면 고민 풀이는 없다")
    void noConcern() {
        assertNull(Reports.concern(null, null, "가", calc.calculate(LocalDate.of(1994, 8, 17), null), 2026));
    }

    /** 2026년(병화)과 원하는 관계가 되는 일간을 가진 사주를 찾는다 */
    private SajuChart chartWithYearRelation(Relation want) {
        for (LocalDate d = LocalDate.of(1990, 1, 1); ; d = d.plusDays(1)) {
            SajuChart c = calc.calculate(d, null);
            if (Relation.between(c.dayMaster().element(), Element.FIRE) == want) return c;
        }
    }

    private int minCareer(Relation dominant) {
        int min = Integer.MAX_VALUE;
        for (LocalDate d = LocalDate.of(1980, 1, 1); d.getYear() < 2000; d = d.plusDays(11)) {
            SajuChart c = calc.calculate(d, null);
            if (TenGods.dominant(TenGods.count(c)) == dominant) min = Math.min(min, Reports.careerPremium("가", c, 2026).length());
        }
        return min;
    }
}
