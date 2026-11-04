package com.byeolgyeol.saju.calc;

import java.util.EnumMap;
import java.util.Map;

/**
 * 계산된 사주 원국.
 * hour 는 태어난 시간을 모를 때 null.
 */
public record SajuChart(
        Pillar year,
        Pillar month,
        Pillar day,
        Pillar hour,
        Map<Element, Integer> elementPercent
) {
    /** 일간 = 나를 나타내는 글자 (일주의 천간) */
    public Stem dayMaster() {
        return day.stem();
    }

    public boolean hasHour() {
        return hour != null;
    }

    /** 비율이 가장 높은 오행 (동점이면 목→수 순서상 앞쪽) */
    public Element strongest() {
        Element best = Element.WOOD;
        for (Element e : Element.values()) {
            if (elementPercent.get(e) > elementPercent.get(best)) best = e;
        }
        return best;
    }

    /** 비율이 가장 낮은 오행 */
    public Element weakest() {
        Element worst = Element.WOOD;
        for (Element e : Element.values()) {
            if (elementPercent.get(e) < elementPercent.get(worst)) worst = e;
        }
        return worst;
    }

    public static Map<Element, Integer> emptyElementMap() {
        Map<Element, Integer> map = new EnumMap<>(Element.class);
        for (Element e : Element.values()) map.put(e, 0);
        return map;
    }
}
