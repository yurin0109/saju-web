package com.byeolgyeol.saju.calc;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class QuestionBankTest {

    private final QuestionBank bank = QuestionBank.load();

    @Test
    @DisplayName("성찰 질문은 600개 이상")
    void enoughQuestions() {
        assertTrue(bank.size() >= 600, "현재 " + bank.size() + "개");
    }

    @Test
    @DisplayName("오행 5종 · 공통 · 올해 관계 5종 · 직장 · 결혼 분류가 모두 있다")
    void allCategories() {
        for (Element e : Element.values()) {
            assertTrue(bank.category(e.name()).size() >= 60, e.name());
        }
        assertTrue(bank.category(QuestionBank.COMMON).size() >= 60);
        for (Relation r : Relation.values()) {
            assertTrue(bank.category("YEAR_" + r.name()).size() >= 30, r.name());
        }
        assertTrue(bank.careerPool().size() >= 50);
        assertTrue(bank.marriagePool().size() >= 50);
    }

    @Test
    @DisplayName("같은 질문이 두 번 들어가 있지 않다")
    void noDuplicateText() {
        Set<String> seen = new HashSet<>();
        for (String c : bank.categories()) {
            for (QuestionBank.Question q : bank.category(c)) {
                assertTrue(seen.add(q.text()), "중복: " + q.text());
            }
        }
    }

    @Test
    @DisplayName("추가한 질문은 기존 질문 뒤에 번호가 이어진다 (기존 코드 유지)")
    void appendedKeepsCodes() {
        assertEquals("요즘 내가 가장 키워가고 싶은 것은 무엇인가요?", bank.find("WOOD-1").orElseThrow().text());
        assertEquals("요즘 나를 가장 많이 자라게 해주는 사람은 누구인가요?", bank.find("WOOD-41").orElseThrow().text());
        assertEquals("CAREER-1", bank.careerPool().get(0).code());
    }

    @Test
    @DisplayName("질문 코드로 다시 찾을 수 있다")
    void findByCode() {
        QuestionBank.Question q = bank.category("WOOD").get(0);
        assertEquals("WOOD-1", q.code());
        assertEquals(q, bank.find("WOOD-1").orElseThrow());
        assertTrue(bank.find("WOOD-9999").isEmpty());
    }

    @Test
    @DisplayName("같은 날 같은 프로필이면 같은 질문, 다른 질문 보기를 누르면 바뀐다")
    void dailyPick() {
        List<QuestionBank.Question> pool = bank.corePool(Element.FIRE);
        long seed = QuestionBank.dailySeed(20_000, 3);
        assertEquals(QuestionBank.pick(pool, seed, 0), QuestionBank.pick(pool, seed, 0));
        assertNotEquals(QuestionBank.pick(pool, seed, 0), QuestionBank.pick(pool, seed, 1));
        // 한 바퀴 돌면 처음 질문으로 돌아온다
        assertEquals(QuestionBank.pick(pool, seed, 0), QuestionBank.pick(pool, seed, pool.size()));
    }

    @Test
    @DisplayName("결과 탭 질문은 내 오행 + 공통에서, 운세 탭 질문은 올해 관계에서 나온다")
    void poolsMatchCategory() {
        for (int i = 0; i < 100; i++) {
            QuestionBank.Question core = QuestionBank.pick(bank.corePool(Element.WATER), i * 13L, 0);
            assertTrue(core.category().equals("WATER") || core.category().equals(QuestionBank.COMMON));
            QuestionBank.Question year = QuestionBank.pick(bank.yearPool(Relation.OUTPUT), i * 13L, 0);
            assertEquals("YEAR_OUTPUT", year.category());
        }
    }
}
