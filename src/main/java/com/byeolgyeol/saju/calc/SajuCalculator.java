package com.byeolgyeol.saju.calc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * 사주팔자 계산기.
 * 스프링에 의존하지 않는 순수 자바 클래스라서 JUnit으로 바로 테스트할 수 있다.
 *
 * [계산 정책]
 * 1. 년주: 입춘(2/4) 기준으로 해가 바뀐다.
 * 2. 월주: 12절기 기준으로 달이 바뀐다. (SolarTerms 참고)
 * 3. 일주: 2000-01-01 = 무오일을 기준으로 날짜 차이를 60으로 나눈 나머지.
 * 4. 시주: 한국 표준시와 실제 경도 차이(약 30분)를 반영해
 *          자시 = 23:30 ~ 01:29, 축시 = 01:30 ~ 03:29 ... 로 본다.
 *          23:30 이후 출생은 다음 날의 일주로 계산한다.
 * 5. 오행 비율: 월지(태어난 계절)는 영향력이 커서 3점, 나머지 글자는 1점.
 */
public class SajuCalculator {

    /** 2000-01-01 은 60갑자 중 54번째(무오) 날 */
    private static final LocalDate BASE_DATE = LocalDate.of(2000, 1, 1);
    private static final int BASE_DAY_INDEX = 54;

    /** 월지 가중치 */
    public static final int MONTH_BRANCH_WEIGHT = 3;

    /** 시간 보정: 자시는 23:30 에 시작 */
    private static final int JASI_START_MINUTES = 23 * 60 + 30;

    public SajuChart calculate(LocalDate birthDate, LocalTime birthTime) {
        Pillar year = yearPillar(birthDate);
        Pillar month = monthPillar(birthDate);

        // 23:30 이후 출생이면 다음 날 일주
        LocalDate dayBase = birthDate;
        if (birthTime != null && toMinutes(birthTime) >= JASI_START_MINUTES) {
            dayBase = birthDate.plusDays(1);
        }
        Pillar day = dayPillar(dayBase);
        Pillar hour = (birthTime == null) ? null : hourPillar(day.stem(), birthTime);

        Map<Element, Integer> percent = elementPercent(year, month, day, hour);
        return new SajuChart(year, month, day, hour, percent);
    }

    /** 사주상 연도 (입춘 전이면 전년도) */
    public int sajuYear(LocalDate date) {
        MonthDay md = MonthDay.from(date);
        return md.isBefore(SolarTerms.ipchun()) ? date.getYear() - 1 : date.getYear();
    }

    /** 년주: 서기 4년이 갑자년이라 (연도 - 4) 를 60으로 나눈 나머지 */
    public Pillar yearPillar(LocalDate date) {
        return yearPillarOf(sajuYear(date));
    }

    public Pillar yearPillarOf(int sajuYear) {
        return Pillar.ofSexagenary(sajuYear - 4);
    }

    /**
     * 월주
     * 월지: 인월부터 순서대로 (인=2번 지지)
     * 월간: 년간에 따라 인월의 천간이 정해진다 (갑·기년 → 병인월 ...)
     */
    public Pillar monthPillar(LocalDate date) {
        int order = SolarTerms.monthOrder(date);
        Stem yearStem = yearPillar(date).stem();

        int firstMonthStem = (yearStem.ordinal() % 5) * 2 + 2;
        Stem stem = Stem.of(firstMonthStem + order);
        Branch branch = Branch.of(order + 2);
        return new Pillar(stem, branch);
    }

    /** 일주 */
    public Pillar dayPillar(LocalDate date) {
        long days = ChronoUnit.DAYS.between(BASE_DATE, date);
        return Pillar.ofSexagenary((int) Math.floorMod(BASE_DAY_INDEX + days, 60L));
    }

    /**
     * 시주
     * 시지: 2시간 단위 (자시부터)
     * 시간: 일간에 따라 자시의 천간이 정해진다 (갑·기일 → 갑자시 ...)
     */
    public Pillar hourPillar(Stem dayStem, LocalTime time) {
        int fromJasi = Math.floorMod(toMinutes(time) - JASI_START_MINUTES, 24 * 60);
        int branchIndex = fromJasi / 120;

        int firstHourStem = (dayStem.ordinal() % 5) * 2;
        return new Pillar(Stem.of(firstHourStem + branchIndex), Branch.of(branchIndex));
    }

    /** 오행 비율 (%) — 합계가 정확히 100이 되도록 반올림 보정 */
    public Map<Element, Integer> elementPercent(Pillar year, Pillar month, Pillar day, Pillar hour) {
        Map<Element, Integer> score = SajuChart.emptyElementMap();

        add(score, year.stem().element(), 1);
        add(score, year.branch().element(), 1);
        add(score, month.stem().element(), 1);
        add(score, month.branch().element(), MONTH_BRANCH_WEIGHT);
        add(score, day.stem().element(), 1);
        add(score, day.branch().element(), 1);
        if (hour != null) {
            add(score, hour.stem().element(), 1);
            add(score, hour.branch().element(), 1);
        }

        int total = score.values().stream().mapToInt(Integer::intValue).sum();

        // 최대 잔여 방식: 내림한 뒤 남은 %p를 소수점이 큰 순서대로 1씩 배분
        Map<Element, Integer> percent = SajuChart.emptyElementMap();
        double[] remainders = new double[5];
        int used = 0;
        for (Element e : Element.values()) {
            double exact = score.get(e) * 100.0 / total;
            int floor = (int) Math.floor(exact);
            percent.put(e, floor);
            remainders[e.ordinal()] = exact - floor;
            used += floor;
        }
        for (int left = 100 - used; left > 0; left--) {
            int best = 0;
            for (int i = 1; i < 5; i++) {
                if (remainders[i] > remainders[best]) best = i;
            }
            Element e = Element.values()[best];
            percent.put(e, percent.get(e) + 1);
            remainders[best] = -1;
        }
        return percent;
    }

    private static void add(Map<Element, Integer> score, Element e, int point) {
        score.put(e, score.get(e) + point);
    }

    private static int toMinutes(LocalTime t) {
        return t.getHour() * 60 + t.getMinute();
    }
}
