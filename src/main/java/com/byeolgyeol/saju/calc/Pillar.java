package com.byeolgyeol.saju.calc;

/** 기둥 하나 = 천간 + 지지 (예: 병오) */
public record Pillar(Stem stem, Branch branch) {

    /** 60갑자 번호(0=갑자 ~ 59=계해)로 기둥 만들기 */
    public static Pillar ofSexagenary(int index60) {
        int i = Math.floorMod(index60, 60);
        return new Pillar(Stem.of(i), Branch.of(i));
    }

    public String korean() {
        return stem.korean() + branch.korean();
    }

    public String hanja() {
        return stem.hanja() + branch.hanja();
    }
}
