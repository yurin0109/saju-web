package com.byeolgyeol.saju.profile;

import com.byeolgyeol.saju.calc.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 화면에 내려주는 응답.
 * 프로필 정보 + 사주 원국 + 핵심 성향 + 오행 + 올해 운세 + 직장운 + 결혼운을 한 번에 담는다.
 *
 * 여기 담긴 풀이는 모두 무료다.
 * 유료 "심화 리포트"는 /api/profiles/{id}/premium/{code} 에서 구매한 경우에만 내려준다.
 */
public record ProfileResponse(
        Long id,
        String name,
        LocalDate birthDate,
        /** SOLAR / LUNAR */
        String calendar,
        /** 음력으로 입력했으면 입력한 음력 날짜 (양력이면 null) */
        String lunarDate,
        boolean leapMonth,
        LocalTime birthTime,
        String gender,
        String birthPlace,
        String concernType,
        String concernText,
        Saju saju
) {

    public record PillarDto(String label, String korean, String hanja,
                            String stemElement, String branchElement) {
        static PillarDto of(String label, Pillar p) {
            if (p == null) return new PillarDto(label, null, null, null, null);
            return new PillarDto(label, p.korean(), p.hanja(),
                    p.stem().element().name(), p.branch().element().name());
        }
    }

    public record ElementDto(String code, String korean, String hanja, String meaning,
                             String trait, int percent) {}

    public record CoreDto(String dayMaster, String dayMasterHanja, String dayMasterElement,
                          String headline, String description, List<String> keywords,
                          List<String> more) {}

    public record BalanceDto(String strongest, String strongText,
                             String weakest, String weakTip, String reflection) {}

    public record YearDto(int year, String pillar, String pillarHanja, String yearElement,
                          String relation, String relationDesc, String title,
                          String keywords, String caution, String body, String action) {}

    /** 십성 하나의 분포 */
    public record TenGodDto(String code, String name, String desc, int score, int percent) {}

    public record CareerDto(String dominant, String dominantDesc, String type,
                            String keywords, String caution, String body,
                            List<String> fields, String action, String thisYear, String thisYearRelation,
                            List<TenGodDto> tenGods) {}

    public record MarriageDto(String starName, String starDesc, int starScore, String level,
                              String title, String keywords, String caution, String body,
                              String palace, String palaceHanja, String palaceElement,
                              String palaceRelation, String palaceText, String action) {}

    /** 올해 간지 (무료로 보여주는 기본 정보) */
    public record YearInfo(int year, String pillar, String pillarHanja, String element) {}

    public record Saju(List<PillarDto> pillars, List<ElementDto> elements,
                       CoreDto core, BalanceDto balance, YearInfo year,
                       List<String> owned,
                       /** 궁합 심화를 산 상대 프로필 id (궁합은 상대마다 따로 구매) */
                       List<Long> compatPartners,
                       YearDto thisYear, CareerDto career, MarriageDto marriage,
                       Report reunion, Report family, Report study, Reports.ConcernDto concern,
                       /** 내 사주에 있는 신살 (역마살 · 도화살 · 천을귀인 …) + 전체 신살 설명 */
                       List<ShinSal.Found> shinsal, List<ShinSal.Info> shinsalGuide) {}

    public static ProfileResponse of(Profile p, SajuChart chart, int currentSajuYear, Pillar yearPillar,
                                     Set<Product> owned, List<Long> compatPartners) {
        // 기둥 (시 · 일 · 월 · 년 순서가 전통 표기)
        List<PillarDto> pillars = List.of(
                PillarDto.of("시주", chart.hour()),
                PillarDto.of("일주", chart.day()),
                PillarDto.of("월주", chart.month()),
                PillarDto.of("년주", chart.year()));

        // 오행
        List<ElementDto> elements = new ArrayList<>();
        for (Element e : Element.values()) {
            elements.add(new ElementDto(e.name(), e.korean(), e.hanja(), e.meaning(),
                    Interpretation.ELEMENT.get(e).trait(), chart.elementPercent().get(e)));
        }

        // 핵심 성향 (일간)
        Stem dm = chart.dayMaster();
        Interpretation.DayMasterText dt = Interpretation.DAY_MASTER.get(dm);
        CoreDto core = new CoreDto(dm.korean(), dm.hanja(), dm.element().name(),
                dt.headline(), dt.description(), dt.keywords(), Reports.dayMasterMore(dm));

        // 강한 / 약한 오행
        Element strong = chart.strongest();
        Element weak = chart.weakest();
        BalanceDto balance = new BalanceDto(
                strong.name(), Interpretation.ELEMENT.get(strong).strong(),
                weak.name(), Interpretation.ELEMENT.get(weak).weakTip(),
                Interpretation.REFLECTION.get(dm.element()));

        // 올해 운세
        Element yearElement = yearPillar.stem().element();
        Relation rel = Relation.between(dm.element(), yearElement);
        Interpretation.YearText yt = Interpretation.YEAR.get(rel);
        YearDto thisYear = new YearDto(currentSajuYear, yearPillar.korean(), yearPillar.hanja(),
                yearElement.name(), rel.koreanName(), rel.description(),
                yt.title(), yt.keywords(), yt.caution(), yt.body(), yt.action());

        // 십성 분포
        Map<Relation, Integer> counts = TenGods.count(chart);
        int total = TenGods.total(counts);
        List<TenGodDto> tenGods = new ArrayList<>();
        for (Relation r : Relation.values()) {
            int score = counts.get(r);
            tenGods.add(new TenGodDto(r.name(), r.koreanName(), r.description(), score,
                    total == 0 ? 0 : Math.round(score * 100f / total)));
        }

        // 직장운
        Relation dom = TenGods.dominant(counts);
        Interpretation.CareerText ct = Interpretation.CAREER.get(dom);
        CareerDto career = new CareerDto(dom.koreanName(), dom.description(), ct.type(),
                ct.keywords(), ct.caution(), ct.body(), ct.fields(), ct.action(),
                Interpretation.CAREER_YEAR.get(rel), rel.koreanName(), tenGods);

        // 결혼운
        Relation star = TenGods.spouseStar(p.getGender());
        int starScore = counts.get(star);
        String level = TenGods.level(starScore);
        Interpretation.MarriageText mt = Interpretation.MARRIAGE_STAR.get(level);
        Relation palace = TenGods.spousePalace(chart);
        Branch palaceBranch = chart.day().branch();
        MarriageDto marriage = new MarriageDto(star.koreanName(), star.description(), starScore, level,
                mt.title(), mt.keywords(), Interpretation.MARRIAGE_CAUTION.get(palace), mt.body(),
                palaceBranch.korean(), palaceBranch.hanja(), palaceBranch.element().name(),
                palace.koreanName(), Interpretation.MARRIAGE_PALACE.get(palace),
                Interpretation.MARRIAGE_ACTION.get(palace));

        return new ProfileResponse(p.getId(), p.getName(), p.getBirthDate(),
                p.getCalendarType(), p.getLunarDate(), p.isLeapMonth(), p.getBirthTime(),
                p.getGender(), p.getBirthPlace(), p.getConcernType(), p.getConcernText(),
                new Saju(pillars, elements, core, balance,
                        new YearInfo(currentSajuYear, yearPillar.korean(), yearPillar.hanja(), yearElement.name()),
                        owned.stream().map(Enum::name).sorted().toList(),
                        compatPartners,
                        thisYear, career, marriage,
                        Reports.reunionBasic(p.getName(), chart, currentSajuYear),
                        Reports.familyBasic(p.getName(), chart),
                        Reports.studyBasic(p.getName(), chart, currentSajuYear),
                        Reports.concern(p.getConcernType(), p.getConcernText(), p.getName(), chart, currentSajuYear),
                        ShinSal.find(chart), ShinSal.all()));
    }
}
