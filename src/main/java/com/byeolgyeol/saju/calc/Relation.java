package com.byeolgyeol.saju.calc;

/**
 * 일간(나)과 다른 오행의 관계 = 십성의 큰 분류 5가지.
 * 올해 운세는 "올해 천간의 오행"이 나와 어떤 관계인지로 풀이한다.
 */
public enum Relation {
    PEER("비겁", "나와 같은 기운"),
    OUTPUT("식상", "내가 생하는 기운"),
    WEALTH("재성", "내가 극하는 기운"),
    OFFICER("관성", "나를 극하는 기운"),
    RESOURCE("인성", "나를 생하는 기운");

    private final String name;
    private final String description;

    Relation(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String koreanName() { return name; }
    public String description() { return description; }

    /** me(일간 오행) 기준으로 other 오행과의 관계 */
    public static Relation between(Element me, Element other) {
        if (me == other) return PEER;
        if (me.generates() == other) return OUTPUT;
        if (me.controls() == other) return WEALTH;
        if (other.controls() == me) return OFFICER;
        return RESOURCE; // other.generates() == me
    }
}
