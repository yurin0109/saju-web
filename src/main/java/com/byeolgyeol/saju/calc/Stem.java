package com.byeolgyeol.saju.calc;

/** 천간 10개 (갑을병정무기경신임계) */
public enum Stem {
    GAP("갑", "甲", Element.WOOD),
    EUL("을", "乙", Element.WOOD),
    BYEONG("병", "丙", Element.FIRE),
    JEONG("정", "丁", Element.FIRE),
    MU("무", "戊", Element.EARTH),
    GI("기", "己", Element.EARTH),
    GYEONG("경", "庚", Element.METAL),
    SIN("신", "辛", Element.METAL),
    IM("임", "壬", Element.WATER),
    GYE("계", "癸", Element.WATER);

    private final String korean;
    private final String hanja;
    private final Element element;

    Stem(String korean, String hanja, Element element) {
        this.korean = korean;
        this.hanja = hanja;
        this.element = element;
    }

    public static Stem of(int index) {
        return values()[Math.floorMod(index, 10)];
    }

    public String korean() { return korean; }
    public String hanja() { return hanja; }
    public Element element() { return element; }
}
