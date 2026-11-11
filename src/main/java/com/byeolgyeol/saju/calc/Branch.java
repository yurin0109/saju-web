package com.byeolgyeol.saju.calc;

/** 지지 12개 (자축인묘진사오미신유술해) */
public enum Branch {
    JA("자", "子", Element.WATER),
    CHUK("축", "丑", Element.EARTH),
    IN("인", "寅", Element.WOOD),
    MYO("묘", "卯", Element.WOOD),
    JIN("진", "辰", Element.EARTH),
    SA("사", "巳", Element.FIRE),
    O("오", "午", Element.FIRE),
    MI("미", "未", Element.EARTH),
    SHIN("신", "申", Element.METAL),
    YU("유", "酉", Element.METAL),
    SUL("술", "戌", Element.EARTH),
    HAE("해", "亥", Element.WATER);

    private final String korean;
    private final String hanja;
    private final Element element;

    Branch(String korean, String hanja, Element element) {
        this.korean = korean;
        this.hanja = hanja;
        this.element = element;
    }

    public static Branch of(int index) {
        return values()[Math.floorMod(index, 12)];
    }

    public String korean() { return korean; }
    public String hanja() { return hanja; }
    public Element element() { return element; }
}
