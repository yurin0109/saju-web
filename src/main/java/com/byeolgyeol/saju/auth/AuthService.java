package com.byeolgyeol.saju.auth;

import com.byeolgyeol.saju.payment.PaymentOrderRepository;
import com.byeolgyeol.saju.profile.ProfileRepository;
import com.byeolgyeol.saju.profile.PurchaseRepository;
import com.byeolgyeol.saju.profile.ReflectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 회원가입 · 로그인.
 * 로그인(가입)에 성공하면 로그인 전에 이 브라우저로 저장한 사주 · 성찰 일기 등을 회원 계정으로 옮긴다.
 */
@Service
@Transactional(readOnly = true)
public class AuthService {

    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");
    /** 아이디가 없을 때와 비밀번호가 틀렸을 때 같은 문구 (어떤 아이디가 있는지 알려주지 않기) */
    static final String WRONG = "아이디 또는 비밀번호가 맞지 않아요.";

    private final MemberRepository members;
    private final ProfileRepository profiles;
    private final PurchaseRepository purchases;
    private final ReflectionRepository reflections;
    private final PaymentOrderRepository orders;

    public AuthService(MemberRepository members, ProfileRepository profiles, PurchaseRepository purchases,
                       ReflectionRepository reflections, PaymentOrderRepository orders) {
        this.members = members;
        this.profiles = profiles;
        this.purchases = purchases;
        this.reflections = reflections;
        this.orders = orders;
    }

    /** 가입 결과: 회원 + 한 번만 보여줄 복구 코드 */
    public record Signed(Member member, String recoveryCode) {}

    @Transactional
    public Signed signup(AuthDto.SignupRequest req) {
        String username = req.username().trim();
        if (members.existsByUsername(username)) throw new IllegalArgumentException("이미 사용 중인 아이디예요.");
        checkPassword(username, req.password());
        Member m = new Member(username, PasswordHasher.hash(req.password()));
        String code = RecoveryCode.generate();
        m.changeRecovery(RecoveryCode.hash(code));
        m.loginSucceeded(java.time.LocalDateTime.now());   // 가입하면 바로 로그인되므로 마지막 로그인도 기록
        return new Signed(members.save(m), code);
    }

    private static void checkPassword(String username, String password) {
        if (password.toLowerCase().contains(username.toLowerCase())) {
            throw new IllegalArgumentException("비밀번호에 아이디를 넣지 말아주세요.");
        }
    }

    /** 아이디 찾기: 복구 코드로 */
    public String findId(AuthDto.FindIdRequest req) {
        return members.findByRecoveryHash(RecoveryCode.hash(req.recoveryCode()))
                .map(Member::getUsername)
                .orElseThrow(() -> new IllegalArgumentException("복구 코드가 맞지 않아요. 가입할 때 받은 코드를 확인해주세요."));
    }

    /** 비밀번호 재설정: 아이디 + 복구 코드가 맞으면 새 비밀번호로. 복구 코드도 새로 발급 (한 번 쓴 코드는 버림) */
    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public String resetPassword(AuthDto.ResetPasswordRequest req) {
        LocalDateTime now = LocalDateTime.now(KOREA);
        String wrong = "아이디 또는 복구 코드가 맞지 않아요.";
        Member m = members.findByUsername(req.username().trim()).orElseThrow(() -> new IllegalArgumentException(wrong));
        if (m.isLocked(now)) throw new IllegalArgumentException(lockedMessage());
        String given = RecoveryCode.hash(req.recoveryCode());
        if (m.getRecoveryHash() == null || !java.security.MessageDigest.isEqual(
                given.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                m.getRecoveryHash().getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
            m.loginFailed(now);
            members.save(m);
            throw new IllegalArgumentException(wrong);
        }
        checkPassword(m.getUsername(), req.newPassword());
        m.changePassword(PasswordHasher.hash(req.newPassword()));
        String code = RecoveryCode.generate();
        m.changeRecovery(RecoveryCode.hash(code));
        m.loginSucceeded(now);
        members.save(m);
        return code;
    }

    /** 비밀번호 변경 (로그인한 상태, 지금 비밀번호 확인) */
    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public void changePassword(Long memberId, AuthDto.ChangePasswordRequest req) {
        Member m = verified(memberId, req.currentPassword());
        if (req.currentPassword().equals(req.newPassword())) throw new IllegalArgumentException("지금과 다른 비밀번호로 정해주세요.");
        checkPassword(m.getUsername(), req.newPassword());
        m.changePassword(PasswordHasher.hash(req.newPassword()));
        members.save(m);
    }

    /** 복구 코드 다시 받기 (비밀번호 확인 후). 예전 코드는 더 이상 못 씀 */
    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public String reissueRecovery(Long memberId, String password) {
        Member m = verified(memberId, password);
        String code = RecoveryCode.generate();
        m.changeRecovery(RecoveryCode.hash(code));
        members.save(m);
        return code;
    }

    /** 로그인한 회원의 비밀번호 다시 확인 (틀리면 틀린 횟수 증가) */
    private Member verified(Long memberId, String password) {
        LocalDateTime now = LocalDateTime.now(KOREA);
        Member m = members.findById(memberId).orElseThrow(() -> new LoginRequiredException("다시 로그인해주세요."));
        if (m.isLocked(now)) throw new IllegalArgumentException(lockedMessage());
        if (!PasswordHasher.matches(password, m.getPasswordHash())) {
            m.loginFailed(now);
            members.save(m);
            throw new IllegalArgumentException("비밀번호가 맞지 않아요.");
        }
        return m;
    }

    private static String lockedMessage() {
        return "비밀번호(복구 코드)를 " + Member.MAX_FAILS + "번 틀려서 잠시 잠겼어요. " + Member.LOCK_MINUTES + "분 뒤에 다시 시도해주세요.";
    }

    /** noRollbackFor: 비밀번호가 틀려도 "틀린 횟수"는 저장되어야 하므로 */
    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public Member login(AuthDto.LoginRequest req) {
        LocalDateTime now = LocalDateTime.now(KOREA);
        Member m = members.findByUsername(req.username().trim()).orElse(null);
        if (m == null) {
            PasswordHasher.matches(req.password(), PasswordHasher.DUMMY);   // 걸리는 시간을 맞춘다
            throw new IllegalArgumentException(WRONG);
        }
        if (m.isLocked(now)) throw new IllegalArgumentException(lockedMessage());
        if (!PasswordHasher.matches(req.password(), m.getPasswordHash())) {
            m.loginFailed(now);
            members.save(m);
            throw new IllegalArgumentException(WRONG);
        }
        m.loginSucceeded(now);
        members.save(m);
        return m;
    }

    /** 로그인 전 브라우저 ID로 저장한 데이터를 회원 열쇠로 옮긴다. 옮긴 사주 수를 돌려준다 */
    @Transactional
    public int moveBrowserData(String browserClientId, String ownerKey) {
        if (browserClientId == null || browserClientId.equals(ownerKey)) return 0;
        int moved = profiles.moveOwner(browserClientId, ownerKey);
        purchases.moveOwner(browserClientId, ownerKey);
        reflections.moveOwner(browserClientId, ownerKey);
        orders.moveOwner(browserClientId, ownerKey);
        return moved;
    }
}
