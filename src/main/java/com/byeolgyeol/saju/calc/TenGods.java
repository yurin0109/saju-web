package com.byeolgyeol.saju.calc;

import java.util.EnumMap;
import java.util.Map;

/**
 * 십성(비겁·식상·재성·관성·인성) 분포 계산.
 *
 * 일간(나)을 뺀 나머지 글자의 오행이 나와 어떤 관계인지 센다.
 * 태어난 계절을 나타내는 월지는 영향력이 커서 2점, 나머지는 1점.
 * (오행 비율의 월지 가중치 3과는 별개의 기준)
 */
public final class TenGods {

    public static final int MONTH_BRANCH_WEIGHT = 2;

    private TenGods() {}

    public static Map<Relation, Integer> count(SajuChart chart) {
        Element me = chart.dayMaster().element();
        Map<Relation, Integer> map = new EnumMap<>(Relation.class);
        for (Relation r : Relation.values()) map.put(r, 0);

        add(map, me, chart.year().stem().element(), 1);
        add(map, me, chart.year().branch().element(), 1);
        add(map, me, chart.month().stem().element(), 1);
        add(map, me, chart.month().branch().element(), MONTH_BRANCH_WEIGHT);
        add(map, me, chart.day().branch().element(), 1);
        if (chart.hasHour()) {
            add(map, me, chart.hour().stem().element(), 1);
            add(map, me, chart.hour().branch().element(), 1);
        }
        return map;
    }

    /** 가장 많은 십성 (동점이면 비겁→식상→재성→관성→인성 순서상 앞쪽) */
    public static Relation dominant(Map<Relation, Integer> counts) {
        Relation best = Relation.PEER;
        for (Relation r : Relation.values()) {
            if (counts.get(r) > counts.get(best)) best = r;
        }
        return best;
    }

    public static int total(Map<Relation, Integer> counts) {
        return counts.values().stream().mapToInt(Integer::intValue).sum();
    }

    /**
     * 배우자를 나타내는 십성 (전통 명리 기준)
     * 여성은 나를 극하는 기운(관성), 남성은 내가 극하는 기운(재성)
     */
    public static Relation spouseStar(String gender) {
        return "F".equals(gender) ? Relation.OFFICER : Relation.WEALTH;
    }

    /** 배우자 자리(일지)와 나의 관계 */
    public static Relation spousePalace(SajuChart chart) {
        return Relation.between(chart.dayMaster().element(), chart.day().branch().element());
    }

    /** 배우자 별 점수 → 없음 / 적당 / 많음 */
    public static String level(int score) {
        if (score == 0) return "NONE";
        if (score <= 2) return "BALANCED";
        return "MANY";
    }

    private static void add(Map<Relation, Integer> map, Element me, Element other, int point) {
        Relation r = Relation.between(me, other);
        map.put(r, map.get(r) + point);
    }
}
