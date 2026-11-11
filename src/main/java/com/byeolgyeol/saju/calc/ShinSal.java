package com.byeolgyeol.saju.calc;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 신살(神殺) 찾기: 사주 여덟 글자 안에 특별한 글자 조합이 있는지 본다.
 *
 * 기준 (가장 널리 쓰는 방식)
 * - 역마 · 도화 · 화개: 년지 또는 일지가 속한 삼합(申子辰 / 寅午戌 / 巳酉丑 / 亥卯未)을 기준으로,
 *   다른 기둥의 지지에 해당 글자가 있으면 있다고 본다.
 * - 천을귀인 · 문창귀인 · 양인: 일간(나)을 기준으로 네 기둥의 지지를 본다.
 * - 괴강: 일주가 庚辰 · 庚戌 · 壬辰 · 壬戌 · 戊戌 중 하나일 때.
 * - 월덕귀인: 월지의 삼합을 기준으로 정해진 천간이 사주에 있을 때.
 * 태어난 시간을 모르면 시주는 빼고 본다.
 */
public final class ShinSal {

    private ShinSal() {}

    public enum Kind { NOBLE, STAR }   // 귀인(도움) / 살(기운이 센 글자)

    /** 신살 하나: 이름, 한자, 종류, 한 줄 요약, 풀이, 잘 쓰는 법, 있는 자리 */
    public record Found(String code, String name, String hanja, Kind kind,
                        String summary, String meaning, String tip, List<String> where) {}

    /** 설명 (신살 목록 안내용) */
    public record Info(String code, String name, String hanja, Kind kind, String summary, String meaning, String tip) {}

    private static final String[] PILLAR_NAME = {"년주", "월주", "일주", "시주"};

    public static final Map<String, Info> INFO = new LinkedHashMap<>();
    static {
        put("CHEONEUL", "천을귀인", "天乙貴人", Kind.NOBLE,
                "어려울 때 도와주는 사람이 나타나는 가장 좋은 귀인",
                "신살 중에서 가장 좋은 기운으로 꼽혀요. 막다른 길에 섰을 때 뜻밖의 사람이 손을 내밀어 주거나, "
                        + "일이 꼬여도 결국 풀리는 쪽으로 흘러가는 힘이에요. 윗사람 · 선배 · 선생님에게 예쁨을 받기 쉽고, "
                        + "사람 사이에서 신뢰를 얻는 분위기가 있어요.",
                "도움을 받았을 때 고마움을 꼭 표현하고 인연을 이어가세요. 귀인은 관계를 소중히 할수록 더 자주 찾아와요.");
        put("MUNCHANG", "문창귀인", "文昌貴人", Kind.NOBLE,
                "글 · 공부 · 시험에 강한 총명함의 별",
                "머리가 맑고 배운 것을 빨리 이해하는 기운이에요. 글을 쓰거나 말로 설명하는 데 재능이 있고, "
                        + "시험 · 자격증처럼 공부로 결과를 내는 일과 잘 맞아요. 기획서 · 보고서 · 발표처럼 "
                        + "생각을 정리해 전달하는 일에서 빛나요.",
                "배운 것을 글로 정리하는 습관을 들이면 이 기운이 실력으로 바뀌어요. 시험 준비는 요약 노트가 잘 맞아요.");
        put("WOLDEOK", "월덕귀인", "月德貴人", Kind.NOBLE,
                "큰 탈 없이 지켜주는 덕의 기운",
                "위험한 일이 생겨도 크게 다치지 않고 지나가게 해 주는 보호의 기운이에요. 마음씨가 너그럽고 "
                        + "남을 돕는 일에서 보람을 느끼는 편이라, 베푼 만큼 돌아오는 흐름이 있어요.",
                "봉사 · 나눔처럼 덕을 쌓는 일을 꾸준히 하면 이 기운이 더 커져요.");
        put("YEOKMA", "역마살", "驛馬殺", Kind.STAR,
                "이동 · 여행 · 변화가 많은 활동적인 기운",
                "역마는 옛날 역참의 말처럼 늘 움직이는 기운이에요. 이사 · 출장 · 여행 · 해외와 인연이 많고, "
                        + "한자리에 오래 머물면 답답함을 느끼기 쉬워요. 예전에는 떠돌이 기운이라 꺼렸지만, "
                        + "요즘은 영업 · 무역 · 여행 · 항공 · 외근 · 해외 업무처럼 움직임이 곧 능력이 되는 시대라 좋은 기운으로 봐요.",
                "가만히 있으면 불안해지는 기운이라, 여행 · 운동 · 새로운 장소처럼 몸을 움직일 출구를 만들어 두세요. "
                        + "이직이나 이사는 충동이 아니라 계획으로 결정하면 좋아요.");
        put("DOHWA", "도화살", "桃花殺", Kind.STAR,
                "사람을 끄는 매력과 인기의 기운",
                "복숭아꽃처럼 눈에 띄고 사람을 끄는 매력이 있어요. 첫인상이 좋고 주변에 사람이 잘 모이며, "
                        + "연애 기회도 많은 편이에요. 예전에는 이성 문제로 걱정했지만, 지금은 방송 · 서비스 · 영업 · "
                        + "디자인 · SNS처럼 사람의 마음을 끄는 일에서 큰 장점이 돼요.",
                "매력이 오해를 부르지 않게 관계의 선을 분명히 해 두세요. 이 기운을 일(발표 · 콘텐츠 · 고객 응대)에 쓰면 성과로 이어져요.");
        put("HWAGAE", "화개살", "華蓋殺", Kind.STAR,
                "혼자 깊이 파고드는 예술 · 학문 · 종교의 기운",
                "화려한 덮개라는 뜻으로, 겉으로 드러나기보다 안으로 깊어지는 기운이에요. 혼자 있는 시간에 "
                        + "생각이 깊어지고, 예술 · 연구 · 철학 · 종교 · 상담처럼 한 분야를 깊이 파는 일과 잘 맞아요. "
                        + "가끔 외로움을 타거나 사람들과 거리를 두고 싶을 때가 있어요.",
                "혼자만의 시간을 죄책감 없이 확보하세요. 그 시간에 만든 결과물을 사람들과 나누면 외로움 대신 인정이 따라와요.");
        put("YANGIN", "양인살", "羊刃殺", Kind.STAR,
                "강한 추진력과 승부욕, 날카로운 칼 같은 기운",
                "나를 나타내는 글자의 힘이 아주 강해지는 자리예요. 결단력 · 추진력 · 승부욕이 강해서 "
                        + "경쟁이 있는 일에서 앞서가요. 대신 성격이 급해지거나 고집이 세 보일 수 있고, "
                        + "몸을 쓰는 일에서 다치지 않게 조심하라는 뜻도 있어요.",
                "강한 힘은 방향이 중요해요. 목표를 분명히 정하고 운동처럼 에너지를 쏟을 곳을 두면 큰 성과로 바뀌어요.");
        put("GOEGANG", "괴강살", "魁罡殺", Kind.STAR,
                "우두머리 기질, 큰 그릇의 강한 기운",
                "괴강은 북두칠성의 머리를 뜻해요. 카리스마와 리더십이 강하고, 맡은 일은 끝까지 밀어붙이는 힘이 있어요. "
                        + "평범하게 사는 것보다 크게 성공하거나 크게 부딪히는 극적인 흐름을 타기 쉬워요.",
                "혼자 다 하려 하기보다 사람을 이끄는 자리에서 힘을 쓰세요. 말투를 한 번 부드럽게 다듬으면 따르는 사람이 많아져요.");
    }

    private static void put(String code, String name, String hanja, Kind kind, String summary, String meaning, String tip) {
        INFO.put(code, new Info(code, name, hanja, kind, summary, meaning, tip));
    }

    /** 삼합 그룹의 대표: 申子辰=水, 寅午戌=火, 巳酉丑=金, 亥卯未=木 */
    private static int group(Branch b) {
        return switch (b) {
            case SHIN, JA, JIN -> 0;
            case IN, O, SUL -> 1;
            case SA, YU, CHUK -> 2;
            case HAE, MYO, MI -> 3;
        };
    }

    private static final Branch[] YEOKMA = {Branch.IN, Branch.SHIN, Branch.HAE, Branch.SA};
    private static final Branch[] DOHWA = {Branch.YU, Branch.MYO, Branch.O, Branch.JA};
    private static final Branch[] HWAGAE = {Branch.JIN, Branch.SUL, Branch.CHUK, Branch.MI};
    /** 월덕귀인: 월지 삼합 → 천간 (申子辰=壬, 寅午戌=丙, 巳酉丑=庚, 亥卯未=甲) */
    private static final Stem[] WOLDEOK = {Stem.IM, Stem.BYEONG, Stem.GYEONG, Stem.GAP};

    /** 천을귀인: 일간 → 지지 두 개 */
    private static Set<Branch> cheoneul(Stem s) {
        return switch (s) {
            case GAP, MU, GYEONG -> Set.of(Branch.CHUK, Branch.MI);
            case EUL, GI -> Set.of(Branch.JA, Branch.SHIN);
            case BYEONG, JEONG -> Set.of(Branch.HAE, Branch.YU);
            case SIN -> Set.of(Branch.IN, Branch.O);
            case IM, GYE -> Set.of(Branch.SA, Branch.MYO);
        };
    }

    /** 문창귀인: 일간 → 지지 */
    private static Branch munchang(Stem s) {
        return switch (s) {
            case GAP -> Branch.SA;
            case EUL -> Branch.O;
            case BYEONG, MU -> Branch.SHIN;
            case JEONG, GI -> Branch.YU;
            case GYEONG -> Branch.HAE;
            case SIN -> Branch.JA;
            case IM -> Branch.IN;
            case GYE -> Branch.MYO;
        };
    }

    /** 양인: 양간(甲丙戊庚壬)만 본다 */
    private static Branch yangin(Stem s) {
        return switch (s) {
            case GAP -> Branch.MYO;
            case BYEONG, MU -> Branch.O;
            case GYEONG -> Branch.YU;
            case IM -> Branch.JA;
            default -> null;
        };
    }

    public static List<Found> find(SajuChart chart) {
        Pillar[] pillars = {chart.year(), chart.month(), chart.day(), chart.hour()};
        Map<String, Set<String>> hits = new LinkedHashMap<>();
        for (String code : INFO.keySet()) hits.put(code, new LinkedHashSet<>());

        // 년지 · 일지 기준 역마 · 도화 · 화개
        for (int base : new int[]{0, 2}) {
            int g = group(pillars[base].branch());
            for (int i = 0; i < 4; i++) {
                if (i == base || pillars[i] == null) continue;
                Branch b = pillars[i].branch();
                if (b == YEOKMA[g]) hits.get("YEOKMA").add(PILLAR_NAME[i]);
                if (b == DOHWA[g]) hits.get("DOHWA").add(PILLAR_NAME[i]);
                if (b == HWAGAE[g]) hits.get("HWAGAE").add(PILLAR_NAME[i]);
            }
        }

        // 일간 기준 귀인 · 양인
        Stem me = chart.dayMaster();
        Set<Branch> noble = cheoneul(me);
        Branch mc = munchang(me), yi = yangin(me);
        for (int i = 0; i < 4; i++) {
            if (pillars[i] == null) continue;
            Branch b = pillars[i].branch();
            if (noble.contains(b)) hits.get("CHEONEUL").add(PILLAR_NAME[i]);
            if (b == mc) hits.get("MUNCHANG").add(PILLAR_NAME[i]);
            if (b == yi) hits.get("YANGIN").add(PILLAR_NAME[i]);
        }

        // 월덕귀인: 월지 삼합의 천간이 사주 천간에 있으면
        Stem wd = WOLDEOK[group(chart.month().branch())];
        for (int i = 0; i < 4; i++) {
            if (pillars[i] != null && pillars[i].stem() == wd) hits.get("WOLDEOK").add(PILLAR_NAME[i]);
        }

        // 괴강: 일주 자체
        Pillar d = chart.day();
        boolean goegang = (d.stem() == Stem.GYEONG && (d.branch() == Branch.JIN || d.branch() == Branch.SUL))
                || (d.stem() == Stem.IM && (d.branch() == Branch.JIN || d.branch() == Branch.SUL))
                || (d.stem() == Stem.MU && d.branch() == Branch.SUL);
        if (goegang) hits.get("GOEGANG").add("일주");

        List<Found> out = new ArrayList<>();
        hits.forEach((code, where) -> {
            if (where.isEmpty()) return;
            Info i = INFO.get(code);
            out.add(new Found(code, i.name(), i.hanja(), i.kind(), i.summary(), i.meaning(), i.tip(), List.copyOf(where)));
        });
        return out;
    }

    /** 사주에 없는 신살도 "이런 게 있어요" 하고 보여줄 수 있게 전체 설명 목록 */
    public static List<Info> all() {
        return List.copyOf(INFO.values());
    }
}
