package com.byeolgyeol.saju.profile;

import com.byeolgyeol.saju.calc.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional(readOnly = true)
public class ReflectionService {

    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");

    private final ReflectionRepository repository;
    private final ProfileRepository profileRepository;
    private final SajuCalculator calculator = new SajuCalculator();
    private final QuestionBank bank = QuestionBank.load();

    public ReflectionService(ReflectionRepository repository, ProfileRepository profileRepository) {
        this.repository = repository;
        this.profileRepository = profileRepository;
    }

    /**
     * 오늘의 성찰 질문.
     * core: 내 일간 오행 질문 + 공통 질문 / year: 올해 천간과 나의 관계 질문
     * career: 직장운 질문 / marriage: 결혼운 질문
     */
    public ReflectionDto.Question question(String clientId, Long profileId, String type, int offset) {
        Profile p = profile(clientId, profileId);
        List<QuestionBank.Question> pool = pool(p, type);
        long seed = QuestionBank.dailySeed(LocalDate.now(KOREA).toEpochDay(), p.getId());
        QuestionBank.Question q = QuestionBank.pick(pool, seed, offset);
        return new ReflectionDto.Question(q.code(), type, q.text(), Math.floorMod(offset, pool.size()), pool.size());
    }

    public List<ReflectionDto.Response> findAll(String clientId, Long profileId) {
        List<Reflection> list = (profileId == null)
                ? repository.findByClientIdOrderByCreatedAtDesc(clientId)
                : repository.findByClientIdAndProfileIdOrderByCreatedAtDesc(clientId, profileId);
        return list.stream().map(ReflectionDto.Response::of).toList();
    }

    @Transactional
    public ReflectionDto.Response create(String clientId, ReflectionDto.CreateRequest req) {
        Profile p = profile(clientId, req.profileId());

        // 질문 문구는 화면에서 보낸 값을 믿지 않고, 서버의 질문 데이터에서 다시 찾는다
        QuestionBank.Question q = bank.find(req.questionCode())
                .filter(found -> pool(p, req.type()).contains(found))
                .orElseThrow(() -> new IllegalArgumentException("질문 정보가 올바르지 않아요."));

        Reflection saved = repository.save(new Reflection(clientId, p.getId(), req.type(),
                q.code(), q.text(), req.answer().strip()));
        return ReflectionDto.Response.of(saved);
    }

    @Transactional
    public ReflectionDto.Response update(String clientId, Long id, ReflectionDto.UpdateRequest req) {
        Reflection r = get(clientId, id);
        r.updateAnswer(req.answer().strip());
        return ReflectionDto.Response.of(r);
    }

    @Transactional
    public void delete(String clientId, Long id) {
        repository.delete(get(clientId, id));
    }

    /** 내 프로필인지 확인 */
    private Profile profile(String clientId, Long profileId) {
        if (profileId == null) throw new NoSuchElementException("사주 프로필을 찾을 수 없어요.");
        return profileRepository.findByIdAndClientId(profileId, clientId)
                .orElseThrow(() -> new NoSuchElementException("사주 프로필을 찾을 수 없어요. 목록에서 다시 선택해주세요."));
    }

    private Reflection get(String clientId, Long id) {
        return repository.findByIdAndClientId(id, clientId)
                .orElseThrow(() -> new NoSuchElementException("성찰 일기를 찾을 수 없어요."));
    }

    private List<QuestionBank.Question> pool(Profile p, String type) {
        if ("career".equals(type)) return bank.careerPool();
        if ("marriage".equals(type)) return bank.marriagePool();
        SajuChart chart = calculator.calculate(p.getBirthDate(), p.getBirthTime());
        Element me = chart.dayMaster().element();
        if ("year".equals(type)) {
            int thisYear = calculator.sajuYear(LocalDate.now(KOREA));
            Element yearElement = calculator.yearPillarOf(thisYear).stem().element();
            return bank.yearPool(Relation.between(me, yearElement));
        }
        return bank.corePool(me);
    }
}
