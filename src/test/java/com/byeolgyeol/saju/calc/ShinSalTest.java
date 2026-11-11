package com.byeolgyeol.saju.calc;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class ShinSalTest {

    private static Map<String, List<String>> codes(SajuChart c) {
        return ShinSal.find(c).stream().collect(Collectors.toMap(ShinSal.Found::code, ShinSal.Found::where));
    }

    @Test
    @DisplayName("년지 子 기준 역마(寅) · 화개(辰), 일주 庚辰 괴강, 월지 寅 기준 월덕(丙)을 찾는다")
    void findsStars() {
        SajuChart c = new SajuChart(new Pillar(Stem.GAP, Branch.JA), new Pillar(Stem.BYEONG, Branch.IN),
                new Pillar(Stem.GYEONG, Branch.JIN), new Pillar(Stem.MU, Branch.IN), SajuChart.emptyElementMap());
        Map<String, List<String>> m = codes(c);
        assertEquals(List.of("월주", "시주"), m.get("YEOKMA"));
        assertEquals(List.of("일주"), m.get("HWAGAE"));
        assertEquals(List.of("일주"), m.get("GOEGANG"));
        assertEquals(List.of("월주"), m.get("WOLDEOK"));
        assertFalse(m.containsKey("DOHWA"));
        assertFalse(m.containsKey("CHEONEUL"), "庚 일간의 천을귀인은 丑 · 未");
    }

    @Test
    @DisplayName("일간 기준 천을귀인 · 문창귀인 · 양인을 찾고, 시간을 모르면 시주는 뺀다")
    void findsNobles() {
        // 일간 甲: 천을 丑未, 문창 巳, 양인 卯
        SajuChart c = new SajuChart(new Pillar(Stem.EUL, Branch.CHUK), new Pillar(Stem.GI, Branch.MYO),
                new Pillar(Stem.GAP, Branch.SA), null, SajuChart.emptyElementMap());
        Map<String, List<String>> m = codes(c);
        assertEquals(List.of("년주"), m.get("CHEONEUL"));
        assertEquals(List.of("일주"), m.get("MUNCHANG"));
        assertEquals(List.of("월주"), m.get("YANGIN"));
        assertEquals(8, ShinSal.all().size());
    }
}
