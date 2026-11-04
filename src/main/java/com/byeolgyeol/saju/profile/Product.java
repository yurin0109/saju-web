package com.byeolgyeol.saju.profile;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

/**
 * 판매 상품.
 * 리포트(YEAR · CAREER · MARRIAGE)는 하나씩 살 수도 있고, 묶음(BUNDLE)으로 한 번에 살 수도 있다.
 * 각 탭의 기본 풀이(내 결과 · 올해 운세 · 직장운 · 결혼운 · 궁합)는 무료이고,
 * 상품은 그 아래 붙는 "심화 리포트"다.
 */
public enum Product {

    YEAR("올해 운세 심화 리포트", 4_900, "이번 달부터 12개월 월별 흐름, 일 · 재물 · 관계 · 컨디션 영역별 풀이, 행운 포인트, 분기별 실천 플랜"),
    CAREER("직장운 심화 리포트", 4_900, "일하는 스타일 심화, 강점 · 약점, 직장 내 관계, 3년 커리어 흐름, 잘 맞는 일 TOP 4"),
    MARRIAGE("결혼운 심화 리포트", 4_900, "배우자 별 · 배우자 자리 심화, 연애 패턴과 갈등 해결, 잘 맞는 상대, 6년 인연 흐름"),
    COMPAT("궁합 심화 리포트", 4_900, "두 사람의 기운 심화, 생활 리듬, 서로 채워주는 오행, 갈등 포인트와 대화법"),
    REUNION("재회운 심화 리포트", 4_900, "이별을 대하는 나의 방식, 다시 이어지는 관계의 조건, 3년 흐름, 마음 정리 가이드"),
    FAMILY("가족운 심화 리포트", 4_900, "부모 · 형제 · 자녀 자리 풀이, 가족 안에서의 나의 역할, 가족과의 거리 두기, 3년 흐름"),
    BUNDLE("프리미엄 리포트 전체 6종", 19_900, "올해 운세 · 직장운 · 결혼운 · 궁합 · 재회운 · 가족운 심화 리포트를 한 번에");

    /** 하나씩 열 수 있는 유료 리포트 */
    public static final Set<Product> REPORTS = EnumSet.of(YEAR, CAREER, MARRIAGE, COMPAT, REUNION, FAMILY);

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
        return this == BUNDLE ? EnumSet.copyOf(REPORTS) : EnumSet.of(this);
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
