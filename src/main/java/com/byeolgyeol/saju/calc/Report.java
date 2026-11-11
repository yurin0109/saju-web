package com.byeolgyeol.saju.calc;

import java.util.ArrayList;
import java.util.List;

/**
 * 화면에 그대로 그리는 리포트 구조.
 * 리포트 = 여러 개의 섹션, 섹션 = 문단 여러 개 + (선택) 항목 목록.
 * 무료 리포트(재회운 · 가족운 · 궁합 기본)와 유료 심화 리포트가 모두 이 모양을 쓴다.
 */
public record Report(String code, String title, String subtitle, List<Section> sections) {

    /** 항목 한 줄. label 은 굵게, tag 는 작은 꼬리표(없으면 null), score 는 1 ~ 5 별점(없으면 null) */
    public record Item(String label, String tag, String text, Integer score) {
        public Item(String label, String tag, String text) {
            this(label, tag, text, null);
        }
    }

    public record Section(String title, String lead, List<String> paragraphs, List<Item> items) {}

    /** 섹션을 차례로 쌓는 도우미 */
    public static final class Builder {
        private final String code, title, subtitle;
        private final List<Section> sections = new ArrayList<>();

        public Builder(String code, String title, String subtitle) {
            this.code = code;
            this.title = title;
            this.subtitle = subtitle;
        }

        public Builder text(String title, String lead, List<String> paragraphs) {
            sections.add(new Section(title, lead, List.copyOf(paragraphs), List.of()));
            return this;
        }

        public Builder items(String title, String lead, List<String> paragraphs, List<Item> items) {
            sections.add(new Section(title, lead, List.copyOf(paragraphs), List.copyOf(items)));
            return this;
        }

        public Report build() {
            return new Report(code, title, subtitle, List.copyOf(sections));
        }
    }

    /** 문단 전체 글자 수 (테스트에서 "기본 풀이보다 3배 이상 긴지" 확인용) */
    public int length() {
        int n = 0;
        for (Section s : sections) {
            for (String p : s.paragraphs()) n += p.length();
            for (Item i : s.items()) n += i.text().length();
        }
        return n;
    }
}
