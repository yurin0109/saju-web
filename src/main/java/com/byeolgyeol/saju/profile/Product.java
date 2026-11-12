package com.byeolgyeol.saju.profile;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

/**
 * 판매 상품.
 * 리포트는 하나씩 살 수도 있고, 궁합 · 재회운을 뺀 7종은 묶음(BUNDLE)으로 한 번에 살 수도 있다.
 * 궁합(COMPAT) · 재회운(REUNION)은 상대 1명마다 따로 산다.
 * 각 탭의 기본 풀이(내 결과 · 올해 운세 · 직장운 · 결혼운 · 궁합)는 무료이고,
 * 상품은 그 아래 붙는 "심화 리포트"다.
 */
public enum Product {

    WEALTH("재물운 심화 리포트", 4_900, "한눈에 보는 재물운, 재물의 별, 나에게 맞는 돈 버는 방식, 재물이 들어오기 좋은 달 TOP 3, 지출을 조심할 달, 3년 재물 흐름, 재물운 체크리스트"),
    LOVE("연애운 심화 리포트", 4_900, "연애 스타일 심화, 이성 인연의 별, 나의 매력 포인트, 인연이 들어오기 좋은 달 TOP 3, 3년 연애 흐름, 연애운 체크리스트"),
    YEAR("올해 운세 심화 리포트", 4_900, "이번 달부터 12개월 월별 흐름, 일 · 재물 · 관계 · 컨디션 영역별 풀이, 행운 포인트, 분기별 실천 플랜"),
    CAREER("직장운 심화 리포트", 4_900, "일하는 스타일 심화, 강점 · 약점, 직장 내 관계, 3년 커리어 흐름, 잘 맞는 일 TOP 4"),
    MARRIAGE("결혼운 심화 리포트", 4_900, "나의 배우자상, 6년 결혼 시기 흐름, 결혼 생활 스타일, 가정 경제 · 역할 분담, 부부 갈등 해결"),
    COMPAT("궁합 심화 리포트", 4_900, "상대 1명 기준 · 두 사람의 기운 심화, 생활 리듬, 서로 채워주는 오행, 갈등 포인트와 대화법"),
    REUNION("재회운 심화 리포트", 4_900, "상대 1명 기준 · 다시 이어지기 위한 조건, 반복되는 패턴, 서로 채워주던 기운, 3년 재회 흐름, 연락 전 체크 · 마음 정리 가이드"),
    STUDY("학업운 심화 리포트", 4_900, "나에게 맞는 공부법, 시험 · 자격증 운, 3년 학업 흐름, 잘 맞는 공부 분야, 슬럼프 관리"),
    FAMILY("가족운 심화 리포트", 4_900, "부모 · 형제 · 자녀 자리 풀이, 가족 안에서의 나의 역할, 가족과의 거리 두기, 3년 흐름"),
    BUNDLE("프리미엄 리포트 7종 묶음", 19_900, "올해 운세 · 직장운 · 재물운 · 학업운 · 연애운 · 결혼운 · 가족운 심화 (궁합 · 재회운은 상대마다 따로 구매)");

    /** 하나씩 열 수 있는 유료 리포트 */
    public static final Set<Product> REPORTS = EnumSet.of(YEAR, CAREER, MARRIAGE, COMPAT, REUNION, FAMILY, STUDY, LOVE, WEALTH);

    /**
     * 묶음으로 열리는 리포트 7종.
     * 궁합 · 재회운은 "상대 1명 = 1번 구매"라서 묶음에 넣지 않는다 (한 번 결제로 여러 사람과 보는 것 방지).
     */
    public static final Set<Product> BUNDLE_REPORTS = EnumSet.of(YEAR, CAREER, MARRIAGE, FAMILY, STUDY, LOVE, WEALTH);

    /** 상대 1명마다 따로 사는 상품 (궁합 · 재회운) */
    public boolean perPartner() { return this == COMPAT || this == REUNION; }

    private final String title;
    private final int price;
    private final String includes;

    Product(String title, int price, String includes) {
        this.title = title;
        this.price = price;
        this.includes = includes;
    }

    public String title() { return title; }
    public int price() { return price; }
    public String includes() { return includes; }

    /** 이 상품을 사면 열리는 리포트 */
    public Set<Product> unlocks() {
        return this == BUNDLE ? EnumSet.copyOf(BUNDLE_REPORTS) : EnumSet.of(this);
    }

    /** 구매한 상품 목록 → 열려 있는 리포트 */
    public static Set<Product> owned(Collection<Product> purchased) {
        Set<Product> set = EnumSet.noneOf(Product.class);
        for (Product p : purchased) set.addAll(p.unlocks());
        return set;
    }

    /** 이미 가진 리포트를 기준으로, 이 상품을 사도 새로 열리는 게 있는지 */
    public boolean addsSomething(Set<Product> owned) {
        return !owned.containsAll(unlocks());
    }

}
