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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * 관리자 API (모든 요청에 X-Admin-Key 헤더 = ADMIN_PASSWORD 필요)
 *
 * GET    /api/admin/summary               회원 · 사주 · 구매 · 매출 · 새 의견 수
 * GET    /api/admin/feedback              건의사항 목록 (최근 300개)
 * POST   /api/admin/feedback/{id}/done    처리 완료 표시 (?done=false 로 되돌리기)
 * DELETE /api/admin/feedback/{id}         건의사항 삭제
 * GET    /api/admin/members               회원 목록 (아이디 · 가입일 · 마지막 로그인 · 잠김 여부, 비밀번호는 볼 수 없음)
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

    public record MemberRow(Long id, String username, LocalDateTime createdAt, LocalDateTime lastLoginAt, boolean locked) {}

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
        return members.findTop300ByOrderByCreatedAtDesc().stream()
                .map(m -> new MemberRow(m.getId(), m.getUsername(), m.getCreatedAt(), m.getLastLoginAt(), m.isLocked(now)))
                .toList();
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
