package com.byeolgyeol.saju.calc;

/**
 * 오행 (목·화·토·금·수)
 * 상생: 목 → 화 → 토 → 금 → 수 → 목
 * 상극: 목 → 토 → 수 → 화 → 금 → 목
 */
public enum Element {
    WOOD("목", "木", "나무"),
    FIRE("화", "火", "불"),
    EARTH("토", "土", "흙"),
    METAL("금", "金", "금속"),
    WATER("수", "水", "물");

    private final String korean;
    private final String hanja;
    private final String meaning;

    Element(String korean, String hanja, String meaning) {
        this.korean = korean;
        this.hanja = hanja;
        this.meaning = meaning;
    }

    public String korean() { return korean; }
    public String hanja() { return hanja; }
    public String meaning() { return meaning; }

    /** 내가 생(生)하는 오행. 예: 목 → 화 */
    public Element generates() {
        return values()[(ordinal() + 1) % 5];
    }

    /** 내가 극(剋)하는 오행. 예: 목 → 토 */
    public Element controls() {
        return values()[(ordinal() + 2) % 5];
    }
}
