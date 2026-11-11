package com.byeolgyeol.saju.calc;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 리포트 조립.
 * 계산 결과(일간 · 십성 · 올해와의 관계 · 다른 사람의 사주)를 보고 문구 템플릿을 골라 섹션으로 쌓는다.
 * 무료: 고민 맞춤 풀이, 궁합 기본, 재회운, 가족운 / 유료: 각 심화 리포트
 */
public final class Reports {

    private Reports() {}

    private static final SajuCalculator CALC = new SajuCalculator();

    // ════════════════════ 공통 도우미 ════════════════════

    private static Relation yearRelation(SajuChart chart, int sajuYear) {
        return Relation.between(chart.dayMaster().element(), CALC.yearPillarOf(sajuYear).stem().element());
    }

    private static final Map<String, String> LEVEL_KO = Map.of("NONE", "원국에 없음", "BALANCED", "알맞음", "MANY", "많음");

    private static String elementName(Element e) {
        return e.korean() + "(" + e.hanja() + ")";
    }

    /** 육합: 자축 · 인해 · 묘술 · 진유 · 사신 · 오미 */
    static boolean hap(Branch a, Branch b) {
        return (a.ordinal() + b.ordinal()) % 12 == 1;
    }

    /** 충: 서로 정반대(6칸 차이) */
    static boolean chung(Branch a, Branch b) {
        return Math.abs(a.ordinal() - b.ordinal()) == 6;
    }

    /** 천간합: 갑기 · 을경 · 병신 · 정임 · 무계 */
    static boolean stemHap(Stem a, Stem b) {
        return Math.abs(a.ordinal() - b.ordinal()) == 5;
    }

    private static String branchState(Branch a, Branch b) {
        if (hap(a, b)) return "HAP";
        if (chung(a, b)) return "CHUNG";
        return "NORMAL";
    }

    private static Relation least(Map<Relation, Integer> counts) {
        Relation min = Relation.PEER;
        for (Relation r : Relation.values()) if (counts.get(r) < counts.get(min)) min = r;
        return min;
    }

    // ════════════════════ 고민 맞춤 풀이 (무료) ════════════════════

    public record ConcernDto(String type, String label, String text, List<String> paragraphs,
                             String product, String pitch) {}

    public static ConcernDto concern(String type, String text, String name, SajuChart chart, int sajuYear) {
        if (type == null) return null;
        MoreText.Concern c = MoreText.CONCERN.get(type);
        if (c == null) return null;
        Element me = chart.dayMaster().element();
        Relation rel = yearRelation(chart, sajuYear);
        List<String> ps = new ArrayList<>();
        if (text != null && !text.isBlank()) {
            ps.add("“" + text.strip() + "”라는 고민을 적어주셨어요. " + name + " 님의 사주로 이 고민을 함께 들여다볼게요.");
        }
        ps.add(c.intro());
        ps.add(MoreText.CONCERN_APPROACH.get(me));
        ps.add(sajuYear + "년의 흐름으로 보면, " + MoreText.CONCERN_TIMING.get(rel));
        return new ConcernDto(type, c.label(), text, ps, c.product(), c.pitch());
    }

    /** 내 결과: 일간 더 읽기 (관계에서 · 일에서 · 지칠 때) */
    public static List<String> dayMasterMore(Stem dm) {
        return MoreText.DAY_MASTER_MORE.get(dm);
    }

    // ════════════════════ 올해 운세 심화 (유료) ════════════════════

    public static Report yearPremium(String name, SajuChart chart, int sajuYear) {
        return yearPremium(name, chart, sajuYear, LocalDate.now(java.time.ZoneId.of("Asia/Seoul")));
    }

    /** today: 월별 흐름을 시작할 달 (테스트에서 날짜를 고정하려고 따로 받음) */
    public static Report yearPremium(String name, SajuChart chart, int sajuYear, LocalDate today) {
        Element me = chart.dayMaster().element();
        Pillar yp = CALC.yearPillarOf(sajuYear);
        Element ye = yp.stem().element();
        Relation rel = Relation.between(me, ye);

        Report.Builder b = new Report.Builder("YEAR", sajuYear + "년 운세 심화 리포트",
                name + " 님의 " + sajuYear + "년 " + yp.korean() + "(" + yp.hanja() + ")년과 앞으로 12개월을 영역별로 자세히 읽어요.");

        b.text("올해 총론", rel.koreanName() + " · " + rel.description() + "이 들어오는 해", YearPremiumText.OVERVIEW.get(rel));

        // 월별 흐름: 이번 달부터 12개월. 매달 15일의 월주(절기 기준)로 그달 기운을 본다
        List<Report.Item> months = new ArrayList<>();
        LocalDate first = today.withDayOfMonth(15);
        for (int i = 0; i < 12; i++) {
            LocalDate d = first.plusMonths(i);
            Pillar mp = CALC.monthPillar(d);
            Relation mr = Relation.between(me, mp.stem().element());
            String text = YearPremiumText.MONTH.get(mr).get(i % 2);
            String label = (i == 0 ? "이번 달 · " : "") + (d.getMonthValue() == 1 || i == 0 ? d.getYear() + "년 " : "") + d.getMonthValue() + "월";
            months.add(new Report.Item(label, mp.korean() + "월 · " + mr.koreanName(), text));
        }
        b.items("앞으로 12개월 월별 흐름", "이번 달부터 12개월이에요. 사주의 달은 절기(매달 4 ~ 8일쯤)에 바뀌어요.", List.of(), months);

        // 분기별 실천 플랜: 월별 흐름과 같은 12개월을 3개월씩 나눈다
        List<String> q = YearPremiumText.QUARTERS.get(rel);
        List<Report.Item> quarters = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            LocalDate from = first.plusMonths(i * 3L), to = from.plusMonths(2);
            quarters.add(new Report.Item((i + 1) + "분기", from.getMonthValue() + " ~ " + to.getMonthValue() + "월", q.get(i)));
        }
        b.items("분기별 실천 플랜", "위 12개월을 3개월씩 나눈 실천 계획이에요.", List.of(), quarters);

        List<String> areas = YearPremiumText.AREAS.get(rel);
        List<Integer> scores = YearPremiumText.AREA_SCORE.get(rel);
        List<String> keys = YearPremiumText.AREA_KEY.get(rel);
        String[] names = {"일 · 공부", "재물", "관계", "컨디션"};
        List<Report.Item> areaItems = new ArrayList<>();
        for (int i = 0; i < 4; i++) areaItems.add(new Report.Item(names[i], keys.get(i), areas.get(i), scores.get(i)));
        b.items("영역별 풀이", "올해 " + rel.koreanName() + "의 기운이 네 가지 영역에 미치는 영향이에요. (별 5개 만점)", List.of(), areaItems);

        // 나의 오행 균형으로 본 올해
        Element strong = chart.strongest(), weak = chart.weakest();
        List<String> balance = new ArrayList<>();
        if (ye == weak) {
            balance.add("올해 들어오는 " + elementName(ye) + "의 기운은 " + name + " 님 사주에서 가장 부족한 기운이에요. 부족한 것을 채워주는 해라서, 평소 어렵게 느껴지던 일이 생각보다 수월하게 풀릴 수 있어요.");
            balance.add("이런 해에는 새로운 습관을 들이기 좋아요. 올해 채운 기운은 사주의 균형을 맞춰주는 든든한 바탕이 되니, 이 기운과 관련된 활동(아래 행운 포인트)을 꾸준히 해보세요.");
        } else if (ye == strong) {
            balance.add("올해 들어오는 " + elementName(ye) + "의 기운은 " + name + " 님 사주에서 이미 가장 강한 기운이에요. 장점이 더 커지는 해지만, 넘치는 기운은 지나침이 되기도 해요.");
            balance.add("잘하는 것을 더 잘하게 되는 대신, 같은 방식만 고집하기 쉬워요. 부족한 " + elementName(weak) + "의 기운을 의식적으로 채우면 한 해의 균형이 맞춰져요.");
        } else {
            balance.add("올해 들어오는 " + elementName(ye) + "의 기운은 " + name + " 님 사주에서 넘치지도 부족하지도 않은 기운이에요. 기존의 균형을 크게 흔들지 않아서, 계획한 일을 차분히 이어가기 좋아요.");
            balance.add("사주에서 가장 강한 " + elementName(strong) + "의 기운은 올해도 나의 무기가 되고, 가장 부족한 " + elementName(weak) + "의 기운은 올해 채워가면 좋을 숙제예요.");
        }
        b.text("나의 오행으로 본 올해", null, balance);

        List<String> lucky = YearPremiumText.LUCKY.get(weak);
        b.items("올해의 행운 포인트", "부족한 " + elementName(weak) + "의 기운을 채워주는 것들이에요.", List.of(), List.of(
                new Report.Item("색", null, lucky.get(0)),
                new Report.Item("방향", null, lucky.get(1)),
                new Report.Item("시간", null, lucky.get(2)),
                new Report.Item("활동", null, lucky.get(3)),
                new Report.Item("물건", null, lucky.get(4))));

        return b.build();
    }

    // ════════════════════ 직장운 심화 (유료) ════════════════════

    public static Report careerPremium(String name, SajuChart chart, int sajuYear) {
        Map<Relation, Integer> counts = TenGods.count(chart);
        Relation dom = TenGods.dominant(counts);
        Relation lack = least(counts);
        Interpretation.CareerText ct = Interpretation.CAREER.get(dom);

        Report.Builder b = new Report.Builder("CAREER", "직장운 심화 리포트",
                name + " 님의 '" + ct.type() + "' 일하는 방식을 강점 · 관계 · 3년 흐름까지 자세히 읽어요.");

        b.text("일하는 스타일 심화", "가장 많은 기운 " + dom.koreanName() + " · " + dom.description(), CareerPremiumText.STYLE.get(dom));

        int total = TenGods.total(counts);
        List<Report.Item> dist = new ArrayList<>();
        for (Relation r : Relation.values()) {
            int pct = total == 0 ? 0 : Math.round(counts.get(r) * 100f / total);
            dist.add(new Report.Item(r.koreanName(), pct + "%", CareerPremiumText.ROLE.get(r)));
        }
        b.items("십성 분포로 본 일의 힘", "월지는 2배로 반영했어요.", List.of(), dist);

        List<Report.Item> sw = new ArrayList<>();
        for (String s : CareerPremiumText.STRENGTHS.get(dom)) {
            String[] p = s.split(": ", 2);
            sw.add(new Report.Item(p[0], "강점", p[1]));
        }
        for (String s : CareerPremiumText.WEAKNESSES.get(dom)) {
            String[] p = s.split(": ", 2);
            sw.add(new Report.Item(p[0], "약점", p[1]));
        }
        // 보완하면 좋은 기운(가장 적은 십성)도 같은 섹션 안에
        sw.add(new Report.Item(lack.koreanName() + " 채우기", "보완", CareerPremiumText.LACK.get(lack)));
        b.items("강점과 약점", "가장 많은 기운 " + dom.koreanName() + " · 가장 적은 기운 " + lack.koreanName(), List.of(), sw);

        List<String> people = CareerPremiumText.PEOPLE.get(dom);
        List<String> say = CareerPremiumText.PEOPLE_SAY.get(dom);
        List<Integer> chem = CareerPremiumText.PEOPLE_SCORE.get(dom);
        b.items("직장 안에서의 관계", "회사에서의 나는 " + CareerPremiumText.OFFICE_CHARACTER.get(dom), List.of(), List.of(
                new Report.Item("상사와", say.get(0), people.get(0), chem.get(0)),
                new Report.Item("동료와", say.get(1), people.get(1), chem.get(1)),
                new Report.Item("후배와", say.get(2), people.get(2), chem.get(2))));

        List<Report.Item> years = new ArrayList<>();
        for (int y = sajuYear; y < sajuYear + 3; y++) {
            Relation r = yearRelation(chart, y);
            Pillar p = CALC.yearPillarOf(y);
            years.add(new Report.Item(y + "년", p.korean() + "년 · " + r.koreanName(), CareerPremiumText.YEAR_FLOW.get(r),
                    CareerPremiumText.YEAR_SCORE.get(r)));
        }
        b.items("앞으로 3년 일의 흐름", "해마다 들어오는 기운과 나의 관계로 본 커리어 흐름이에요.", List.of(), years);

        List<Report.Item> fields = new ArrayList<>();
        for (String f : CareerPremiumText.FIELDS.get(dom)) {
            String[] p = f.split(": ", 2);
            fields.add(new Report.Item(p[0], null, p[1]));
        }
        b.items("잘 맞는 일 TOP 4", "사주의 기운으로 본 적합도 순위예요.", List.of(), fields);

        return b.build();
    }

    // ════════════════════ 결혼운 심화 (유료) ════════════════════

    public static Report marriagePremium(String name, String gender, SajuChart chart, int sajuYear) {
        Map<Relation, Integer> counts = TenGods.count(chart);
        Relation star = TenGods.spouseStar(gender);
        String level = TenGods.level(counts.get(star));
        Relation palace = TenGods.spousePalace(chart);
        Branch dayBranch = chart.day().branch();

        Report.Builder b = new Report.Builder("MARRIAGE", "결혼운 심화 리포트",
                name + " 님의 배우자상 · 결혼 시기 · 결혼 생활을 자세히 읽어요.");

        b.text("한눈에 보는 결혼운", null, List.of(
                "종합하면 " + name + " 님은 '" + Interpretation.MARRIAGE_STAR.get(level).title() + "'의 사주에, 배우자 자리에 " + palace.description()
                        + "(" + palace.koreanName() + ")이 있는 사람이에요. 어떤 배우자를 언제 만나는지는 배우자 별이, 결혼 후 어떤 가정을 꾸리는지는 배우자 자리가 보여줘요.",
                "결혼 생활에서 특히 조심할 점은 '" + Interpretation.MARRIAGE_CAUTION.get(palace) + "'이고, 지금 해보면 좋은 준비는 '"
                        + Interpretation.MARRIAGE_ACTION.get(palace) + "'예요."));

        b.text("나의 배우자상과 결혼 시기", "배우자 별 " + star.koreanName() + " (" + ("F".equals(gender) ? "여성" : "남성") + " 기준) · " + LEVEL_KO.get(level),
                LovePremiumText.STAR.get(level));

        List<Report.Item> years = new ArrayList<>();
        for (int y = sajuYear; y < sajuYear + 6; y++) {
            Pillar p = CALC.yearPillarOf(y);
            Relation r = yearRelation(chart, y);
            String tag = p.korean() + "년";
            String text = LovePremiumText.LOVE_YEAR.get(r);
            if (r == star) { tag += " · ★ 배우자 별"; text = "배우자 별이 들어오는 해예요. 결혼 이야기가 구체적으로 진행되기 쉬워요. " + text; }
            if (hap(p.branch(), dayBranch)) { tag += " · 일지 합"; text += " 배우자 자리와 합을 이루어 결혼이나 새 가정을 이루기 좋은 해예요."; }
            if (chung(p.branch(), dayBranch)) { tag += " · 일지 충"; text += " 배우자 자리와 충을 이루어 이사나 생활 변화가 생기기 쉬우니 부부 대화를 늘려주세요."; }
            years.add(new Report.Item(y + "년", tag, text));
        }
        b.items("앞으로 6년 결혼의 흐름", "★ 표시는 결혼 인연이 특히 강하게 들어오는 해예요.", List.of(), years);

        b.text("결혼 생활 스타일", "배우자 자리 " + dayBranch.korean() + "(" + dayBranch.hanja() + ") · " + palace.koreanName(),
                LovePremiumText.PALACE.get(palace));

        b.items("결혼할 사람인지 알아보는 신호", null, List.of(), LovePremiumText.SIGNS.get(level).stream()
                .map(t -> new Report.Item("✦", null, t)).toList());

        b.text("나와 잘 맞는 배우자의 기운", "나에게 부족한 " + elementName(chart.weakest()) + "을 채워주는 사람",
                List.of(LovePremiumText.MATCH.get(chart.weakest()),
                        "오행이 서로를 채워주는 부부는 다툼이 생겨도 회복이 빠르고, 함께 살수록 각자의 부족함이 자연스럽게 메워져요. 사랑운 › 궁합에서 상대의 사주를 저장하면, 그 사람이 이 기운을 가졌는지 바로 확인할 수 있어요."));

        Relation dom = TenGods.dominant(counts);
        b.text("가정 경제와 역할 분담", "가장 많은 기운 " + dom.koreanName(), LovePremiumText.LIFE.get(dom));

        b.items("부부 갈등을 줄이는 법", null, List.of(), LovePremiumText.CONFLICT_TIPS.get(palace).stream()
                .map(t -> new Report.Item("✦", null, t)).toList());

        b.items("결혼 준비 체크리스트", null, List.of(), LovePremiumText.MARRIAGE_PLAN.get(palace).stream()
                .map(t -> new Report.Item("☐", null, t)).toList());
        return b.build();
    }

    // ════════════════════ 궁합 ════════════════════

    public record CompatScore(int score, String grade, Relation stemRelation, boolean stemHap,
                              String branchState, boolean meFilled, boolean partnerFilled) {}

    public static CompatScore compatScore(SajuChart me, SajuChart partner) {
        Relation r = Relation.between(me.dayMaster().element(), partner.dayMaster().element());
        boolean sh = stemHap(me.dayMaster(), partner.dayMaster());
        String bs = branchState(me.day().branch(), partner.day().branch());
        boolean meFilled = partner.strongest() == me.weakest();      // 상대가 내 부족함을 채움
        boolean partnerFilled = me.strongest() == partner.weakest(); // 내가 상대의 부족함을 채움

        int score = 62;
        score += switch (r) {
            case RESOURCE, OUTPUT -> 10;   // 서로 생해주는 관계
            case PEER -> 6;
            case WEALTH, OFFICER -> 2;     // 극하는 관계 (긴장도 있지만 끌림도 있음)
        };
        if (sh) score += 10;
        if ("HAP".equals(bs)) score += 10;
        if ("CHUNG".equals(bs)) score -= 8;
        if (meFilled) score += 6;
        if (partnerFilled) score += 6;
        score = Math.max(45, Math.min(98, score));
        String grade = score >= 88 ? "천생연분" : score >= 78 ? "아주 좋은 궁합" : score >= 68 ? "좋은 궁합" : score >= 58 ? "노력하면 깊어지는 궁합" : "서로 배워가는 궁합";
        return new CompatScore(score, grade, r, sh, bs, meFilled, partnerFilled);
    }

    public static Report compatBasic(String myName, SajuChart me, String partnerName, SajuChart partner) {
        CompatScore s = compatScore(me, partner);
        Report.Builder b = new Report.Builder("COMPAT", myName + " ♥ " + partnerName,
                s.score() + "점 · " + s.grade());
        List<String> ps = new ArrayList<>();
        ps.add(LovePremiumText.COMPAT_SUMMARY.get(s.stemRelation()));
        if (s.stemHap()) ps.add("두 사람의 일간은 '천간합'을 이루어요. 서로에게 자연스럽게 끌리는 강한 인연의 표시예요.");
        ps.add(LovePremiumText.BRANCH.get(s.branchState()).get(0));
        b.text("두 사람의 기본 궁합", myName + " 님(" + me.dayMaster().korean() + me.dayMaster().element().korean()
                + ") → " + partnerName + " 님(" + partner.dayMaster().korean() + partner.dayMaster().element().korean() + ") · "
                + s.stemRelation().koreanName(), ps);

        List<Report.Item> items = new ArrayList<>();
        items.add(new Report.Item("일간 관계", s.stemRelation().koreanName(), s.stemRelation().description() + (s.stemHap() ? " + 천간합" : "")));
        items.add(new Report.Item("일지 관계", switch (s.branchState()) { case "HAP" -> "합"; case "CHUNG" -> "충"; default -> "보통"; },
                me.day().branch().korean() + " · " + partner.day().branch().korean()));
        items.add(new Report.Item("오행 보완", s.meFilled() || s.partnerFilled() ? "있음" : "보통",
                (s.meFilled() ? partnerName + " 님이 내 부족한 " + elementName(me.weakest()) + "을 채워줘요. " : "")
                        + (s.partnerFilled() ? "내가 " + partnerName + " 님의 부족한 " + elementName(partner.weakest()) + "을 채워줘요." : "")
                        + (!s.meFilled() && !s.partnerFilled() ? "서로의 부족한 기운이 겹치지 않아 각자의 색이 분명한 관계예요." : "")));
        b.items("궁합 점수 근거", null, List.of(), items);
        return b.build();
    }

    public static Report compatPremium(String myName, SajuChart me, String partnerName, SajuChart partner) {
        CompatScore s = compatScore(me, partner);
        Report.Builder b = new Report.Builder("COMPAT", "궁합 심화 리포트",
                myName + " 님과 " + partnerName + " 님의 관계를 기운 · 생활 리듬 · 대화법까지 자세히 읽어요.");
        List<String> deep = new ArrayList<>(LovePremiumText.COMPAT_DEEP.get(s.stemRelation()));
        if (s.stemHap()) deep.add("여기에 두 사람의 일간이 천간합을 이루어요. 천간합은 서로 다른 두 기운이 만나 새로운 기운을 만들어내는 관계라, 함께할 때 각자일 때와는 다른 모습이 나와요. 처음 만났을 때 묘하게 끌렸다면 이 합의 영향일 수 있어요.");
        b.text("두 사람의 기운 심화", s.stemRelation().koreanName() + " · " + s.stemRelation().description(), deep);
        b.text("생활 리듬 (배우자 자리)", me.day().branch().korean() + "(" + me.day().branch().hanja() + ") · "
                + partner.day().branch().korean() + "(" + partner.day().branch().hanja() + ")", LovePremiumText.BRANCH.get(s.branchState()));

        List<String> fill = new ArrayList<>();
        fill.add(myName + " 님에게 가장 강한 기운은 " + elementName(me.strongest()) + ", 가장 부족한 기운은 " + elementName(me.weakest()) + "이에요. "
                + partnerName + " 님에게 가장 강한 기운은 " + elementName(partner.strongest()) + ", 가장 부족한 기운은 " + elementName(partner.weakest()) + "이에요.");
        if (s.meFilled()) fill.add(partnerName + " 님의 넘치는 기운이 " + myName + " 님의 빈자리를 채워줘요. " + LovePremiumText.MATCH.get(me.weakest()));
        if (s.partnerFilled()) fill.add(myName + " 님의 넘치는 기운이 " + partnerName + " 님의 빈자리를 채워줘요. 상대가 힘들어하는 부분에서 내가 자연스럽게 힘이 되어줄 수 있어요.");
        if (!s.meFilled() && !s.partnerFilled()) fill.add("두 사람은 서로의 부족한 기운을 직접 채워주는 관계는 아니에요. 대신 각자의 색이 분명해서, 서로의 영역을 존중할 때 편안한 관계가 돼요. 부족한 기운은 함께하는 활동으로 채워보세요.");
        if (me.weakest() == partner.weakest()) fill.add("두 사람 모두 " + elementName(me.weakest()) + "의 기운이 부족해요. 같은 부분에서 함께 약해질 수 있으니, 이 기운을 채우는 활동을 둘이 함께 해보세요: " + YearPremiumText.LUCKY.get(me.weakest()).get(3) + ".");
        b.text("서로 채워주는 오행", null, fill);

        b.items("갈등을 줄이는 대화법", null, List.of(), LovePremiumText.COMPAT_TALK.get(s.stemRelation()).stream()
                .map(t -> new Report.Item("✦", null, t)).toList());
        b.items("함께하면 좋은 활동", null, List.of(), List.of(
                new Report.Item(myName + " 님을 위해", null, YearPremiumText.LUCKY.get(me.weakest()).get(3)),
                new Report.Item(partnerName + " 님을 위해", null, YearPremiumText.LUCKY.get(partner.weakest()).get(3))));
        return b.build();
    }

    // ════════════════════ 재회운 ════════════════════

    public static Report reunionBasic(String name, SajuChart chart, int sajuYear) {
        Element me = chart.dayMaster().element();
        Relation rel = yearRelation(chart, sajuYear);
        return new Report.Builder("REUNION", "재회운", "이별을 대하는 나의 방식과 올해의 흐름")
                .text("이별을 대하는 나의 방식", elementName(me) + "의 기운", List.of(LovePremiumText.REUNION_STYLE.get(me)))
                .text(sajuYear + "년의 재회 흐름", rel.koreanName() + "의 해", List.of(LovePremiumText.REUNION_YEAR.get(rel)))
                .build();
    }

    public static Report reunionPremium(String name, SajuChart chart, int sajuYear) {
        Relation palace = TenGods.spousePalace(chart);
        Branch dayBranch = chart.day().branch();
        Report.Builder b = new Report.Builder("REUNION", "재회운 심화 리포트",
                name + " 님의 관계가 다시 이어지기 위한 조건과 3년의 흐름을 읽어요.");
        b.text("다시 이어지는 관계의 조건", "배우자 자리 " + dayBranch.korean() + "(" + dayBranch.hanja() + ") · " + palace.koreanName(),
                LovePremiumText.REUNION_DEEP.get(palace));
        b.text("관계에서 반복되기 쉬운 패턴", null, List.of(LovePremiumText.PALACE.get(palace).get(1),
                "같은 이유로 다시 멀어지지 않으려면, 이 패턴을 '상대의 문제'가 아니라 '두 사람 사이에서 생기는 흐름'으로 보는 것이 중요해요. 패턴을 알아차리는 순간 다르게 행동할 수 있어요."));

        List<Report.Item> years = new ArrayList<>();
        for (int y = sajuYear; y < sajuYear + 3; y++) {
            Pillar p = CALC.yearPillarOf(y);
            Relation r = yearRelation(chart, y);
            String tag = p.korean() + "년 · " + r.koreanName();
            String text = LovePremiumText.REUNION_YEAR.get(r);
            if (hap(p.branch(), dayBranch)) { tag += " · 일지 합"; text += " 배우자 자리와 합을 이루어 끊어졌던 인연이 다시 닿기 쉬운 해예요."; }
            if (chung(p.branch(), dayBranch)) { tag += " · 일지 충"; text += " 배우자 자리와 충을 이루어 관계를 정리하거나 새로 시작하는 기운이 강한 해예요."; }
            years.add(new Report.Item(y + "년", tag, text));
        }
        b.items("앞으로 3년 재회의 흐름", null, List.of(), years);
        b.items("마음 정리 가이드", "다시 만나든, 떠나보내든 나를 지키는 다섯 단계", List.of(),
                java.util.stream.IntStream.range(0, LovePremiumText.REUNION_GUIDE.size())
                        .mapToObj(i -> new Report.Item(String.valueOf(i + 1), null, LovePremiumText.REUNION_GUIDE.get(i))).toList());
        return b.build();
    }

    // ════════════════════ 가족운 ════════════════════

    public static Report familyBasic(String name, SajuChart chart) {
        Element me = chart.dayMaster().element();
        Relation parents = Relation.between(me, chart.month().stem().element());
        return new Report.Builder("FAMILY", "가족운", "가족 안에서의 나와 부모 · 형제 자리")
                .text("가족 안에서의 나", elementName(me) + "의 기운", List.of(MoreText.FAMILY_ROLE.get(me)))
                .text("부모 · 형제 자리", "월주 " + chart.month().korean() + " · " + parents.koreanName(),
                        List.of(MoreText.PARENTS.get(parents).get(0)))
                .build();
    }

    public static Report familyPremium(String name, SajuChart chart, int sajuYear) {
        Element me = chart.dayMaster().element();
        Map<Relation, Integer> counts = TenGods.count(chart);
        Relation parents = Relation.between(me, chart.month().stem().element());
        Relation childhood = Relation.between(me, chart.year().stem().element());

        Report.Builder b = new Report.Builder("FAMILY", "가족운 심화 리포트",
                name + " 님의 부모 · 형제 · 자녀 자리와 가족 안에서의 역할을 자세히 읽어요.");
        b.text("어린 시절 (년주)", "년주 " + chart.year().korean() + " · " + childhood.koreanName(), List.of(MoreText.CHILDHOOD.get(childhood)));
        b.text("부모 · 형제 자리 심화 (월주)", "월주 " + chart.month().korean() + " · " + parents.koreanName(), MoreText.PARENTS.get(parents));
        b.text("형제 · 동료의 기운", "비겁 " + counts.get(Relation.PEER) + "점", List.of(MoreText.SIBLINGS.get(TenGods.level(counts.get(Relation.PEER)))));
        String child = chart.hasHour() ? Relation.between(me, chart.hour().stem().element()).name() : "UNKNOWN";
        b.text("자녀 자리 (시주)", chart.hasHour() ? "시주 " + chart.hour().korean() : "태어난 시간 모름", List.of(MoreText.CHILDREN.get(child)));
        b.text("가족과 건강한 거리 두기", null, List.of(MoreText.DISTANCE.get(me)));

        List<Report.Item> years = new ArrayList<>();
        for (int y = sajuYear; y < sajuYear + 3; y++) {
            Relation r = yearRelation(chart, y);
            years.add(new Report.Item(y + "년", CALC.yearPillarOf(y).korean() + "년 · " + r.koreanName(), MoreText.FAMILY_YEAR.get(r)));
        }
        b.items("앞으로 3년 가족의 흐름", null, List.of(), years);
        b.items("가족을 위한 실천 체크리스트", null, List.of(), MoreText.FAMILY_PLAN.get(me).stream()
                .map(t -> new Report.Item("☐", null, t)).toList());
        return b.build();
    }

    // ════════════════════ 학업운 ════════════════════

    private static String examType(Map<Relation, Integer> counts) {
        boolean off = counts.get(Relation.OFFICER) > 0, res = counts.get(Relation.RESOURCE) > 0;
        return off && res ? "BOTH" : off ? "OFFICER" : res ? "RESOURCE" : "NEITHER";
    }

    public static Report studyBasic(String name, SajuChart chart, int sajuYear) {
        Element me = chart.dayMaster().element();
        Map<Relation, Integer> counts = TenGods.count(chart);
        Relation rel = yearRelation(chart, sajuYear);
        return new Report.Builder("STUDY", "학업운", "공부 스타일 · 배움의 기운 · 올해 학업 흐름")
                .text("나의 공부 스타일", elementName(me) + "의 기운", List.of(StudyText.STYLE.get(me).get(0)))
                .text("배움의 기운 (인성)", "인성 " + counts.get(Relation.RESOURCE) + "점", List.of(StudyText.RESOURCE_LEVEL.get(TenGods.level(counts.get(Relation.RESOURCE)))))
                .text(sajuYear + "년 학업 흐름", rel.koreanName() + "의 해", List.of(StudyText.YEAR.get(rel)))
                .build();
    }

    public static Report studyPremium(String name, SajuChart chart, int sajuYear) {
        Element me = chart.dayMaster().element();
        Map<Relation, Integer> counts = TenGods.count(chart);
        Relation dom = TenGods.dominant(counts);
        String exam = examType(counts);
        Report.Builder b = new Report.Builder("STUDY", "학업운 심화 리포트",
                name + " 님의 공부 스타일 · 시험운 · 3년 학업 흐름을 자세히 읽어요.");

        b.text("한눈에 보는 학업운", null, List.of(
                name + " 님은 " + elementName(me) + "의 기운을 가진 사람이라 공부할 때 " + switch (me) {
                    case WOOD -> "기초부터 쌓아 올리는";
                    case FIRE -> "짧고 강하게 몰입하는";
                    case EARTH -> "매일 꾸준히 해내는";
                    case METAL -> "핵심을 정리하는";
                    case WATER -> "원리를 깊이 이해하는";
                } + " 방식에서 가장 큰 힘이 나요. 시험운은 " + switch (exam) {
                    case "BOTH" -> "관성과 인성이 함께 있어 공부한 만큼 합격으로 이어지기 좋은 구조예요.";
                    case "OFFICER" -> "목표와 마감이 있을 때 강해지는 타입이에요.";
                    case "RESOURCE" -> "배우는 힘은 크지만 실전 연습이 필요한 타입이에요.";
                    default -> "시험보다 경험으로 배우는 공부가 더 잘 맞는 타입이에요.";
                }));

        b.text("공부 스타일 심화", elementName(me) + "의 기운", StudyText.STYLE.get(me));

        List<String> m = StudyText.METHOD.get(me);
        b.items("나에게 맞는 공부법", null, List.of(), List.of(
                new Report.Item("집중 시간대", null, m.get(0)),
                new Report.Item("공부 장소", null, m.get(1)),
                new Report.Item("공부 방법", null, m.get(2)),
                new Report.Item("쉬는 방법", null, m.get(3))));

        b.text("시험 · 자격증 운", "관성 " + counts.get(Relation.OFFICER) + "점 · 인성 " + counts.get(Relation.RESOURCE) + "점", StudyText.EXAM.get(exam));

        List<Report.Item> years = new ArrayList<>();
        for (int y = sajuYear; y < sajuYear + 3; y++) {
            Relation r = yearRelation(chart, y);
            years.add(new Report.Item(y + "년", CALC.yearPillarOf(y).korean() + "년 · " + r.koreanName(),
                    StudyText.YEAR_FLOW.get(r), StudyText.YEAR_SCORE.get(r)));
        }
        b.items("앞으로 3년 학업의 흐름", "해마다 들어오는 기운과 나의 관계로 본 학업 흐름이에요.", List.of(), years);

        List<Report.Item> fields = new ArrayList<>();
        for (String f : StudyText.FIELDS.get(dom)) {
            String[] p = f.split(": ", 2);
            fields.add(new Report.Item(p[0], null, p[1]));
        }
        b.items("잘 맞는 공부 분야 TOP 4", "가장 많은 기운 " + dom.koreanName() + "으로 본 순위예요.", List.of(), fields);

        b.text("집중력 · 슬럼프 관리", "부족한 " + elementName(chart.weakest()) + "의 기운 채우기", List.of(StudyText.SLUMP.get(chart.weakest())));

        b.items("학업 체크리스트", null, List.of(), StudyText.PLAN.get(me).stream()
                .map(t -> new Report.Item("☐", null, t)).toList());
        return b.build();
    }
}
