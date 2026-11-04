package com.byeolgyeol.saju.calc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 성찰 질문 은행.
 * resources/reflection-questions.txt 를 읽어서 분류별로 들고 있는다.
 *
 * 질문 코드 = "분류-순번" (예: WOOD-1)
 * 오늘의 질문은 날짜와 프로필 번호로 정해지므로 같은 날에는 같은 질문이 나오고,
 * offset 을 바꾸면("다른 질문 보기") 다음 질문으로 넘어간다.
 */
public final class QuestionBank {

    public static final String RESOURCE = "/reflection-questions.txt";
    public static final String COMMON = "COMMON";
    public static final String CAREER = "CAREER";
    public static final String MARRIAGE = "MARRIAGE";

    public record Question(String code, String category, String text) {}

    private final Map<String, List<Question>> byCategory;
    private final Map<String, Question> byCode;

    private QuestionBank(Map<String, List<Question>> byCategory) {
        this.byCategory = byCategory;
        Map<String, Question> codes = new HashMap<>();
        byCategory.values().forEach(list -> list.forEach(q -> codes.put(q.code(), q)));
        this.byCode = codes;
    }

    public static QuestionBank load() {
        try (InputStream in = QuestionBank.class.getResourceAsStream(RESOURCE)) {
            if (in == null) throw new IllegalStateException(RESOURCE + " 파일을 찾을 수 없어요.");
            return parse(new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static QuestionBank parse(BufferedReader reader) throws IOException {
        Map<String, List<Question>> map = new LinkedHashMap<>();
        String category = null;
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.strip();
            if (line.isEmpty() || line.startsWith("#")) continue;
            if (line.startsWith("[") && line.endsWith("]")) {
                category = line.substring(1, line.length() - 1).strip();
                map.putIfAbsent(category, new ArrayList<>());
                continue;
            }
            if (category == null) throw new IllegalStateException("분류([...]) 없이 시작한 질문: " + line);
            List<Question> list = map.get(category);
            list.add(new Question(category + "-" + (list.size() + 1), category, line));
        }
        map.replaceAll((k, v) -> List.copyOf(v));
        return new QuestionBank(Collections.unmodifiableMap(map));
    }

    /** 내 결과 탭용: 일간 오행 질문 + 공통 질문 */
    public List<Question> corePool(Element dayMaster) {
        return concat(dayMaster.name(), COMMON);
    }

    /** 올해 운세 탭용: 올해와 나의 관계별 질문 */
    public List<Question> yearPool(Relation relation) {
        return concat("YEAR_" + relation.name());
    }

    /** 직장운 탭용 */
    public List<Question> careerPool() {
        return concat(CAREER);
    }

    /** 결혼운 탭용 */
    public List<Question> marriagePool() {
        return concat(MARRIAGE);
    }

    /**
     * 질문 하나 고르기.
     * @param seed   같은 값이면 같은 질문 (날짜 + 프로필로 만든다)
     * @param offset "다른 질문 보기"를 누른 횟수
     */
    public static Question pick(List<Question> pool, long seed, int offset) {
        if (pool.isEmpty()) throw new IllegalStateException("질문이 비어 있어요.");
        return pool.get((int) Math.floorMod(seed + offset, (long) pool.size()));
    }

    /** 날짜(epochDay)와 프로필 번호로 그날의 기준값 만들기 */
    public static long dailySeed(long epochDay, long profileId) {
        return epochDay * 31 + profileId * 7;
    }

    public Optional<Question> find(String code) {
        return Optional.ofNullable(byCode.get(code));
    }

    public List<Question> category(String name) {
        return byCategory.getOrDefault(name, List.of());
    }

    public Set<String> categories() {
        return byCategory.keySet();
    }

    public int size() {
        return byCode.size();
    }

    private List<Question> concat(String... names) {
        List<Question> all = new ArrayList<>();
        for (String n : names) all.addAll(category(n));
        return all;
    }
}
