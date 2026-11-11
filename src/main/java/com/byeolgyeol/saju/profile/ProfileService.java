package com.byeolgyeol.saju.profile;

import com.byeolgyeol.saju.calc.LunarCalendar;
import com.byeolgyeol.saju.calc.Pillar;
import com.byeolgyeol.saju.calc.Report;
import com.byeolgyeol.saju.calc.Reports;
import com.byeolgyeol.saju.calc.SajuCalculator;
import com.byeolgyeol.saju.calc.SajuChart;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class ProfileService {

    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");
    static final int MAX_PROFILES = 30;

    private final ProfileRepository repository;
    private final ReflectionRepository reflectionRepository;
    private final PurchaseRepository purchaseRepository;
    private final PurchaseService purchaseService;
    private final SajuCalculator calculator = new SajuCalculator();

    public ProfileService(ProfileRepository repository, ReflectionRepository reflectionRepository,
                          PurchaseRepository purchaseRepository, PurchaseService purchaseService) {
        this.repository = repository;
        this.reflectionRepository = reflectionRepository;
        this.purchaseRepository = purchaseRepository;
        this.purchaseService = purchaseService;
    }

    public List<ProfileResponse> findAll(String clientId) {
        return repository.findByClientIdOrderByCreatedAtDesc(clientId).stream()
                .map(this::toResponse)
                .toList();
    }

    public ProfileResponse findOne(String clientId, Long id) {
        return toResponse(get(clientId, id));
    }

    @Transactional
    public ProfileResponse create(String clientId, ProfileRequest req) {
        // 한 브라우저(사용자)당 저장할 수 있는 사주 수 제한: DB를 쓰레기 데이터로 채우는 것 방지
        if (repository.countByClientId(clientId) >= MAX_PROFILES) {
            throw new IllegalArgumentException("사주는 " + MAX_PROFILES + "개까지 저장할 수 있어요. 안 쓰는 사주를 지운 뒤 추가해주세요.");
        }
        LocalDate solar = solarDate(req);
        Profile p = new Profile(clientId, req.name().trim(), solar,
                req.birthTime(), req.gender(), blankToNull(req.birthPlace()));
        apply(p, req);
        return toResponse(repository.save(p));
    }

    @Transactional
    public ProfileResponse update(String clientId, Long id, ProfileRequest req) {
        Profile p = get(clientId, id);
        LocalDate solar = solarDate(req);
        boolean sajuChanged = !solar.equals(p.getBirthDate())
                || !java.util.Objects.equals(req.birthTime(), p.getBirthTime())
                || !req.gender().equals(p.getGender());
        if (sajuChanged && purchaseService.isPaidProfile(clientId, p.getId())) {
            throw new IllegalArgumentException(
                    "결제한 리포트와 연결된 사주는 생년월일 · 태어난 시간 · 성별을 바꿀 수 없어요. 새 사주로 추가해주세요.");
        }
        p.update(req.name().trim(), solar, req.birthTime(), req.gender(),
                blankToNull(req.birthPlace()));
        apply(p, req);
        return toResponse(p);
    }

    /** 유료 심화 리포트. 구매하지 않았으면 LockedException(403) */
    @Transactional   // 열어본 기록(opened_at)을 저장해야 해서 읽기 전용이 아님
    public Report premium(String clientId, Long id, String code, Long partnerId) {
        Profile p = get(clientId, id);
        Product product;
        try {
            product = Product.valueOf(code);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("리포트 종류가 올바르지 않아요.");
        }
        if (product == Product.BUNDLE) throw new IllegalArgumentException("리포트 종류가 올바르지 않아요.");
        if (product == Product.COMPAT) purchaseService.requireCompat(clientId, p.getId(), partnerId);
        else purchaseService.requireOwned(clientId, p.getId(), product);
        purchaseService.markOpened(clientId, p.getId(), product, partnerId);   // 열어보면 환불 불가

        SajuChart chart = chart(p);
        int year = calculator.sajuYear(LocalDate.now(KOREA));
        return switch (product) {
            case YEAR -> Reports.yearPremium(p.getName(), chart, year);
            case CAREER -> Reports.careerPremium(p.getName(), chart, year);
            case MARRIAGE -> Reports.marriagePremium(p.getName(), p.getGender(), chart, year);
            case REUNION -> Reports.reunionPremium(p.getName(), chart, year);
            case LOVE -> Reports.lovePremium(p.getName(), p.getGender(), chart, year);
            case FAMILY -> Reports.familyPremium(p.getName(), chart, year);
            case STUDY -> Reports.studyPremium(p.getName(), chart, year);
            case COMPAT -> {
                Profile partner = partner(clientId, p, partnerId);
                yield Reports.compatPremium(p.getName(), chart, partner.getName(), chart(partner));
            }
            case BUNDLE -> throw new IllegalStateException();
        };
    }

    /** 궁합 기본 풀이 (무료). 상대는 내가 저장한 다른 사주 프로필 */
    public Report compat(String clientId, Long id, Long partnerId) {
        Profile p = get(clientId, id);
        Profile partner = partner(clientId, p, partnerId);
        return Reports.compatBasic(p.getName(), chart(p), partner.getName(), chart(partner));
    }

    private Profile partner(String clientId, Profile me, Long partnerId) {
        if (partnerId == null) throw new IllegalArgumentException("궁합을 볼 상대를 선택해주세요.");
        if (partnerId.equals(me.getId())) throw new IllegalArgumentException("나와 다른 사람을 선택해주세요.");
        return get(clientId, partnerId);
    }

    private SajuChart chart(Profile p) {
        return calculator.calculate(p.getBirthDate(), p.getBirthTime());
    }

    /** 입력한 날짜 → 사주 계산에 쓸 양력 날짜 (음력이면 변환) */
    static LocalDate solarDate(ProfileRequest req) {
        String[] d = req.birthDate().split("-");
        int y = Integer.parseInt(d[0]), m = Integer.parseInt(d[1]), day = Integer.parseInt(d[2]);
        LocalDate solar;
        if (req.lunar()) {
            solar = LunarCalendar.toSolar(y, m, day, req.leap());
        } else {
            try {
                solar = LocalDate.of(y, m, day);
            } catch (java.time.DateTimeException e) {
                throw new IllegalArgumentException("없는 날짜예요. 생년월일을 다시 확인해주세요.");
            }
        }
        if (solar.getYear() < 1900) throw new IllegalArgumentException("1900년 이후 생년월일만 계산할 수 있어요.");
        if (solar.isAfter(LocalDate.now(KOREA))) throw new IllegalArgumentException("생년월일은 오늘 이전이어야 해요.");
        return solar;
    }

    private static void apply(Profile p, ProfileRequest req) {
        if (req.lunar()) p.setCalendar("LUNAR", req.birthDate(), req.leap());
        else p.setCalendar("SOLAR", null, null);
        String text = blankToNull(req.concernText());
        p.setConcern(req.concernType(), text);
    }

    @Transactional
    public void delete(String clientId, Long id) {
        Profile p = get(clientId, id);
        // 프로필을 지우면 그 프로필로 쓴 성찰 일기도 함께 지운다
        reflectionRepository.deleteByClientIdAndProfileId(clientId, p.getId());
        purchaseRepository.deleteByClientIdAndProfileId(clientId, p.getId());
        repository.delete(p);
    }

    private Profile get(String clientId, Long id) {
        return repository.findByIdAndClientId(id, clientId)
                .orElseThrow(() -> new NoSuchElementException("사주 프로필을 찾을 수 없어요."));
    }

    /** 사주는 DB에 저장하지 않고 조회할 때마다 계산한다 (계산이 가볍고, 로직을 고쳐도 바로 반영됨) */
    private ProfileResponse toResponse(Profile p) {
        SajuChart chart = calculator.calculate(p.getBirthDate(), p.getBirthTime());

        // 올해 = 오늘 날짜 기준 사주상 연도 (입춘 전이면 작년)
        int thisYear = calculator.sajuYear(LocalDate.now(KOREA));
        Pillar yearPillar = calculator.yearPillarOf(thisYear);

        // 이 프로필로 구매한 유료 리포트
        Set<Product> owned = purchaseService.owned(p.getClientId(), p.getId());

        return ProfileResponse.of(p, chart, thisYear, yearPillar, owned,
                purchaseService.compatPartners(p.getClientId(), p.getId()));
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
