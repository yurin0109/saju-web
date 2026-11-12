package com.byeolgyeol.saju.calc;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
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
    @DisplayName("학업운 심화는 무료 학업운보다 3배 이상 길다")
    void studyLonger() {
        for (LocalDate d = LocalDate.of(1985, 1, 1); d.getYear() < 1995; d = d.plusDays(31)) {
            SajuChart c = calc.calculate(d, null);
            int free = Reports.studyBasic("가", c, 2026).length(), premium = Reports.studyPremium("가", c, 2026).length();
            assertTrue(premium >= free * 3, "학업운 " + premium + " < 3 × " + free);
        }
    }

    @Test
    @DisplayName("연애운 심화는 무료 연애운보다 3배 이상 길고, 좋은 달 3개 · 3년 흐름 · 체크리스트가 있다")
    void loveLonger() {
        for (LocalDate d = LocalDate.of(1985, 1, 1); d.getYear() < 1995; d = d.plusDays(31)) {
            SajuChart c = calc.calculate(d, null);
            for (String g : new String[]{"M", "F"}) {
                int free = Reports.loveBasic("가", g, c, 2026).length();
                Report r = Reports.lovePremium("가", g, c, 2026, LocalDate.of(2026, 11, 6));
                assertTrue(r.length() >= free * 3, "연애운 " + r.length() + " < 3 × " + free);
                assertEquals(3, r.sections().stream().filter(s -> s.title().startsWith("인연이 들어오기")).findFirst().orElseThrow().items().size());
                assertEquals(3, r.sections().stream().filter(s -> s.title().startsWith("앞으로 3년")).findFirst().orElseThrow().items().size());
                assertTrue(r.sections().stream().anyMatch(s -> s.items().stream().anyMatch(i -> i.label().equals("☐"))));
            }
        }
    }

    @Test
    @DisplayName("고민 분류를 여러 개 고르면 분류마다 답이 하나씩, 글과 가장 맞는 분류가 먼저, 인용은 첫 답에만")
    void concernsMulti() {
        SajuChart c = calc.calculate(LocalDate.of(1995, 7, 12), null);
        List<Reports.ConcernDto> list = Reports.concerns(List.of("LOVE", "CAREER"), "회사를 그만두고 이직할까 고민돼요", "가", c, 2026);
        assertEquals(2, list.size());
        assertEquals("CAREER", list.get(0).type());
        assertEquals("LOVE", list.get(1).type());
        assertTrue(list.get(0).paragraphs().get(0).startsWith("“"));
        assertFalse(list.get(1).paragraphs().get(0).startsWith("“"));
        assertFalse(list.get(0).topic().equals(list.get(1).topic()));
        assertEquals(1, Reports.concerns(List.of("SELF"), "요즘 너무 지쳐요", "가", c, 2026).size());
        assertTrue(Reports.concerns(List.of(), "x", "가", c, 2026).isEmpty());
    }

    @Test
    @DisplayName("재물운 심화는 무료 재물운보다 3배 이상 길고, 좋은 달 3개 · 3년 흐름 · 체크리스트가 있다")
    void wealthLonger() {
        for (LocalDate d = LocalDate.of(1985, 1, 1); d.getYear() < 1995; d = d.plusDays(31)) {
            SajuChart c = calc.calculate(d, null);
            int free = Reports.wealthBasic("가", c, 2026).length();
            Report r = Reports.wealthPremium("가", c, 2026, LocalDate.of(2026, 11, 6));
            assertTrue(r.length() >= free * 3, "재물운 " + r.length() + " < 3 × " + free);
            assertEquals(3, r.sections().stream().filter(s -> s.title().startsWith("재물이 들어오기")).findFirst().orElseThrow().items().size());
            assertEquals(3, r.sections().stream().filter(s -> s.title().startsWith("앞으로 3년")).findFirst().orElseThrow().items().size());
            assertTrue(r.sections().stream().anyMatch(s -> s.items().stream().anyMatch(i -> i.label().equals("☐"))));
        }
    }

    @Test
    @DisplayName("두 사람 재회운: 점수는 35~95점, 심화는 기본보다 3배 이상 길다")
    void reunionPair() {
        SajuChart other = calc.calculate(LocalDate.of(1993, 3, 5), null);
        for (LocalDate d = LocalDate.of(1985, 1, 1); d.getYear() < 1995; d = d.plusDays(47)) {
            SajuChart c = calc.calculate(d, null);
            Report basic = Reports.reunionBasic("가", c, "나", other, 2026);
            int score = Integer.parseInt(basic.subtitle().split("점")[0]);
            assertTrue(score >= 35 && score <= 95, "점수 " + score);
            assertTrue(Reports.reunionPremium("가", c, "나", other, 2026).length() >= basic.length() * 3);
        }
    }

    @Test
    @DisplayName("고민 문장에서 세부 주제를 찾아 맞춤 답(공감 · 해볼 일 3가지 · 질문)을 만든다")
    void concernTopic() {
        SajuChart c = calc.calculate(LocalDate.of(1994, 8, 17), null);
        assertEquals("이별 · 재회", Reports.concern("LOVE", "전남친이랑 다시 만날 수 있을까요", "가", c, 2026).topic());
        assertEquals("이직 · 퇴사", Reports.concern("CAREER", "회사를 그만두고 이직할까 고민돼요", "가", c, 2026).topic());
        assertEquals("시험 · 자격증", Reports.concern("STUDY", "ADsP 시험이 걱정돼요", "가", c, 2026).topic());
        assertEquals("연애 · 결혼", Reports.concern("LOVE", null, "가", c, 2026).topic());
        Reports.ConcernDto d = Reports.concern("MONEY", "돈이 안 모여요", "가", c, 2026);
        assertEquals("저축 · 돈 모으기", d.topic());
        assertEquals(3, d.steps().size());
        assertTrue(d.paragraphs().get(0).startsWith("“돈이 안 모여요”"));
        assertNotNull(d.question());
        // 단어를 더 많이 설정한 뒤: 줄임말 · 다른 표현 · 부정문 · 다른 분류 이야기
        assertEquals("짝사랑 · 고백", Reports.concern("LOVE", "썸타는 사람이 읽씹해요", "가", c, 2026).topic());
        assertEquals("연인과의 갈등", Reports.concern("LOVE", "남친이랑 연락 문제로 자꾸 싸워요", "가", c, 2026).topic());
        assertEquals("신뢰 · 바람", Reports.concern("LOVE", "애인이 바람피우는 것 같아요", "가", c, 2026).topic());
        assertEquals("취업 · 면접", Reports.concern("CAREER", "이직은 안 할 거고 첫 직장 면접이 걱정돼요", "가", c, 2026).topic());
        assertEquals("이직 · 퇴사", Reports.concern("CAREER", "이직할까 아니면 버틸까", "가", c, 2026).topic());
        assertEquals("창업 · 부업", Reports.concern("CAREER", "회사 다니면서 스마트스토어 부업 해볼까", "가", c, 2026).topic());
        assertEquals("투자", Reports.concern("MONEY", "코인에 물렸어요", "가", c, 2026).topic());
        assertEquals("가족 관계", Reports.concern("RELATION", "엄마랑 자꾸 부딪혀요", "가", c, 2026).topic());
        assertEquals("불안 · 무기력", Reports.concern("SELF", "요즘 잠이 안 오고 불안해요", "가", c, 2026).topic());
        assertEquals("이직 · 퇴사", Reports.concern("LOVE", "퇴사하고 싶어요", "가", c, 2026).topic(), "분류가 달라도 내용으로 찾기");
        assertEquals("앞으로의 방향", Reports.concern("FUTURE", "그냥 이것저것", "가", c, 2026).topic(), "안 맞으면 기본 주제");
        for (var e : ConcernText.TOPICS.entrySet())
            for (var t : e.getValue()) { assertEquals(3, t.steps().size(), t.label()); assertFalse(t.question().isBlank()); }
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
                    Reports.compatBasic("가", c, "나", other), Reports.compatPremium("가", c, "나", other),
                    Reports.studyBasic("가", c, 2026), Reports.studyPremium("가", c, 2026),
                    Reports.loveBasic("가", "F", c, 2026), Reports.lovePremium("가", "M", c, 2026),
                    Reports.wealthBasic("가", c, 2026), Reports.wealthPremium("가", c, 2026),
                    Reports.reunionBasic("가", c, "나", other, 2026), Reports.reunionPremium("가", c, "나", other, 2026)}) {
                for (Report.Section s : r.sections()) {
                    assertFalse(s.paragraphs().isEmpty() && s.items().isEmpty(), r.code() + " " + s.title());
                    for (String p : s.paragraphs()) assertNotNull(p, r.code() + " " + s.title());
                    for (Report.Item i : s.items()) assertNotNull(i.text(), r.code() + " " + s.title());
                }
            }
            for (String type : new String[]{"LOVE", "CAREER", "STUDY", "MONEY", "RELATION", "SELF", "FUTURE"}) {
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
