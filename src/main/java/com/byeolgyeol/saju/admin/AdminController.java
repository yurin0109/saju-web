package com.byeolgyeol.saju.admin;

import com.byeolgyeol.saju.auth.Member;
import com.byeolgyeol.saju.auth.MemberRepository;
import com.byeolgyeol.saju.feedback.FeedbackDto;
import com.byeolgyeol.saju.feedback.FeedbackService;
import com.byeolgyeol.saju.profile.ProfileRepository;
import com.byeolgyeol.saju.profile.Purchase;
import com.byeolgyeol.saju.profile.PurchaseRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import com.byeolgyeol.saju.profile.Profile;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * 관리자 API (모든 요청에 X-Admin-Key 헤더 = ADMIN_PASSWORD 필요)
 *
 * GET    /api/admin/summary               회원 · 사주 · 구매 · 매출 · 새 의견 수
 * GET    /api/admin/feedback              건의사항 목록 (최근 300개)
 * POST   /api/admin/feedback/{id}/done    처리 완료 표시 (?done=false 로 되돌리기)
 * DELETE /api/admin/feedback/{id}         건의사항 삭제
 * GET    /api/admin/members               회원 목록 (아이디 · 가입일 · 마지막 로그인 · 잠김 여부 · 사주/구매 수, 비밀번호는 볼 수 없음)
 * GET    /api/admin/profiles              저장된 사주 목록 (별명 · 태어난 해 · 고민, 생년월일 전체는 보여주지 않음)
 * GET    /api/admin/purchases             구매 목록 (상품 · 금액 · 상태 · 누가 · 어떤 사주)
 * POST   /api/admin/members/{id}/unlock   잠긴 계정 풀기
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final String KEY = "X-Admin-Key";

    private final AdminGuard guard;
    private final FeedbackService feedback;
    private final MemberRepository members;
    private final ProfileRepository profiles;
    private final PurchaseRepository purchases;

    public AdminController(AdminGuard guard, FeedbackService feedback, MemberRepository members,
                           ProfileRepository profiles, PurchaseRepository purchases) {
        this.guard = guard;
        this.feedback = feedback;
        this.members = members;
        this.profiles = profiles;
        this.purchases = purchases;
    }

    private static final int LIMIT = 500;

    public record MemberRow(Long id, String username, LocalDateTime createdAt, LocalDateTime lastLoginAt, boolean locked,
                            long profiles, long purchases, int spent) {}

    /** 저장된 사주 한 줄. 생년월일 전체 대신 태어난 해만 (관리자 화면에서도 개인정보는 최소로) */
    public record ProfileRow(Long id, String name, String owner, String gender, Integer birthYear, String calendar,
                             boolean timeKnown, String concernType, String concernText, String partnerKind, String partnerOfName,
                             long purchases, LocalDateTime createdAt) {}

    public record PurchaseRow(Long id, String productCode, String product, int price, String owner, String profileName,
                              String partnerName, String status, LocalDateTime createdAt, LocalDateTime openedAt,
                              LocalDateTime refundedAt) {}

    /** ownerKey(회원 열쇠) → 아이디. 브라우저 손님이면 없음 */
    private Map<String, String> usernames() {
        Map<String, String> m = new HashMap<>();
        for (Member x : members.findAll()) if (x.getOwnerKey() != null) m.put(x.getOwnerKey(), x.getUsername());
        return m;
    }

    private static String owner(Map<String, String> names, String clientId) {
        String u = names.get(clientId);
        return u != null ? u : "손님";
    }

    @GetMapping("/summary")
    @Transactional(readOnly = true)
    public Map<String, Object> summary(@RequestHeader(value = KEY, required = false) String key) {
        guard.check(key);
        List<Purchase> all = purchases.findAll();
        long active = all.stream().filter(Purchase::isActive).count();
        int revenue = all.stream().filter(Purchase::isActive).mapToInt(Purchase::getPrice).sum();
        return Map.of("members", members.count(), "profiles", profiles.count(),
                "purchases", active, "refunded", all.size() - active, "revenue", revenue,
                "feedbackNew", feedback.countNew());
    }

    @GetMapping("/feedback")
    public List<FeedbackDto.Response> feedback(@RequestHeader(value = KEY, required = false) String key) {
        guard.check(key);
        return feedback.recent();
    }

    @PostMapping("/feedback/{id}/done")
    public Map<String, String> done(@RequestHeader(value = KEY, required = false) String key, @PathVariable Long id,
                                    @RequestParam(value = "done", defaultValue = "true") boolean done) {
        guard.check(key);
        feedback.markDone(id, done);
        return Map.of("message", "저장했어요.");
    }

    @DeleteMapping("/feedback/{id}")
    public Map<String, String> deleteFeedback(@RequestHeader(value = KEY, required = false) String key, @PathVariable Long id) {
        guard.check(key);
        feedback.delete(id);
        return Map.of("message", "삭제했어요.");
    }

    @GetMapping("/members")
    @Transactional(readOnly = true)
    public List<MemberRow> members(@RequestHeader(value = KEY, required = false) String key) {
        guard.check(key);
        LocalDateTime now = LocalDateTime.now();
        Map<String, Long> pc = profiles.findAll().stream().filter(p -> p.getPartnerOf() == null)
                .collect(Collectors.groupingBy(Profile::getClientId, Collectors.counting()));
        List<Purchase> buys = purchases.findAll().stream().filter(Purchase::isActive).toList();
        Map<String, Long> bc = buys.stream().collect(Collectors.groupingBy(Purchase::getClientId, Collectors.counting()));
        Map<String, Integer> spent = buys.stream().collect(Collectors.groupingBy(Purchase::getClientId, Collectors.summingInt(Purchase::getPrice)));
        return members.findTop300ByOrderByCreatedAtDesc().stream()
                .map(m -> new MemberRow(m.getId(), m.getUsername(), m.getCreatedAt(), m.getLastLoginAt(), m.isLocked(now),
                        pc.getOrDefault(m.getOwnerKey(), 0L), bc.getOrDefault(m.getOwnerKey(), 0L), spent.getOrDefault(m.getOwnerKey(), 0)))
                .toList();
    }

    @GetMapping("/profiles")
    @Transactional(readOnly = true)
    public List<ProfileRow> profileList(@RequestHeader(value = KEY, required = false) String key) {
        guard.check(key);
        Map<String, String> names = usernames();
        List<Profile> all = profiles.findAll();
        Map<Long, String> nameById = all.stream().collect(Collectors.toMap(Profile::getId, Profile::getName, (a, b) -> a));
        Map<Long, Long> buys = purchases.findAll().stream().filter(Purchase::isActive)
                .collect(Collectors.groupingBy(Purchase::getProfileId, Collectors.counting()));
        return all.stream()
                .sorted(Comparator.comparing(Profile::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(LIMIT)
                .map(p -> new ProfileRow(p.getId(), p.getName(), owner(names, p.getClientId()), p.getGender(),
                        p.getBirthDate() == null ? null : p.getBirthDate().getYear(), p.getCalendarType(), p.getBirthTime() != null,
                        String.join(",", p.getConcernTypes()), p.getConcernText(), p.getPartnerKind(),
                        p.getPartnerOf() == null ? null : nameById.get(p.getPartnerOf()),
                        buys.getOrDefault(p.getId(), 0L), p.getCreatedAt()))
                .toList();
    }

    @GetMapping("/purchases")
    @Transactional(readOnly = true)
    public List<PurchaseRow> purchaseList(@RequestHeader(value = KEY, required = false) String key) {
        guard.check(key);
        Map<String, String> names = usernames();
        Map<Long, String> nameById = profiles.findAll().stream()
                .collect(Collectors.toMap(Profile::getId, Profile::getName, (a, b) -> a));
        return purchases.findAll().stream()
                .sorted(Comparator.comparing(Purchase::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(LIMIT)
                .map(b -> new PurchaseRow(b.getId(), b.getProductCode(), title(b.getProductCode()), b.getPrice(),
                        owner(names, b.getClientId()), nameById.get(b.getProfileId()),
                        b.getPartnerId() == null ? null : nameById.get(b.getPartnerId()),
                        !b.isActive() ? "REFUNDED" : b.getOpenedAt() != null ? "OPENED" : "PAID",
                        b.getCreatedAt(), b.getOpenedAt(), b.getRefundedAt()))
                .toList();
    }

    private static String title(String code) {
        try { return com.byeolgyeol.saju.profile.Product.valueOf(code).title(); } catch (RuntimeException e) { return code; }
    }

    @PostMapping("/members/{id}/unlock")
    @Transactional
    public Map<String, String> unlock(@RequestHeader(value = KEY, required = false) String key, @PathVariable Long id) {
        guard.check(key);
        Member m = members.findById(id).orElseThrow(() -> new NoSuchElementException("회원을 찾을 수 없어요."));
        m.unlock();
        members.save(m);
        return Map.of("message", "잠금을 풀었어요.");
    }
}
