package com.byeolgyeol.saju.profile;

import com.byeolgyeol.saju.common.LockedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * 구매 내역 · 유료 리포트 권한 확인.
 * 토스 결제 승인(PaymentService)이나 모의 결제가 끝나면 구매 기록을 남겨서 심화 리포트를 연다.
 */
@Service
@Transactional(readOnly = true)
public class PurchaseService {

    private final PurchaseRepository repository;
    private final ProfileRepository profileRepository;

    public PurchaseService(PurchaseRepository repository, ProfileRepository profileRepository) {
        this.repository = repository;
        this.profileRepository = profileRepository;
    }

    public List<PurchaseDto.ProductResponse> products() {
        return Arrays.stream(Product.values()).map(PurchaseDto.ProductResponse::of).toList();
    }

    public List<PurchaseDto.Response> findAll(String clientId) {
        // 궁합 상대는 "내 사주 목록"에 없어서, 화면에 이름을 보여주려고 함께 내려준다
        return repository.findByClientIdOrderByCreatedAtDesc(clientId).stream()
                .map(p -> PurchaseDto.Response.of(p, p.getPartnerId() == null ? null
                        : profileRepository.findByIdAndClientId(p.getPartnerId(), clientId).map(Profile::getName).orElse(null)))
                .toList();
    }

    /**
     * 이 프로필로 열려 있는 유료 리포트.
     * 궁합(COMPAT)은 상대마다 따로 사기 때문에 여기에 넣지 않는다 (묶음에도 궁합은 없음).
     */
    public Set<Product> owned(String clientId, Long profileId) {
        return Product.owned(repository.findByClientIdAndProfileId(clientId, profileId).stream()
                .filter(Purchase::isActive)
                .map(Purchase::getProduct).filter(p -> !p.perPartner()).toList());
    }

    /** 상대마다 사는 상품(궁합 · 재회운)으로 열린 상대 목록 */
    public List<Long> partnersFor(String clientId, Long profileId, Product product) {
        return repository.findByClientIdAndProfileId(clientId, profileId).stream()
                .filter(p -> p.isActive() && p.getProduct() == product && p.getPartnerId() != null)
                .map(Purchase::getPartnerId).distinct().toList();
    }

    public List<Long> compatPartners(String clientId, Long profileId) {
        return partnersFor(clientId, profileId, Product.COMPAT);
    }

    /** 이 상대로 구매한 기록이 있는지 */
    public boolean ownsPartner(String clientId, Long profileId, Product product, Long partnerId) {
        return partnerId != null && partnersFor(clientId, profileId, product).contains(partnerId);
    }

    public boolean ownsCompat(String clientId, Long profileId, Long partnerId) {
        return ownsPartner(clientId, profileId, Product.COMPAT, partnerId);
    }

    /**
     * 결제와 엮인 사주인지 (내 사주로 산 리포트가 있거나, 궁합 상대로 쓰였는지).
     * 이런 사주는 생년월일 · 시간 · 성별을 바꿀 수 없다.
     * → 한 번 산 리포트를 생일만 바꿔 다른 사람 것으로 다시 보는 것을 막는다.
     */
    public boolean isPaidProfile(String clientId, Long profileId) {
        return repository.existsByClientIdAndProfileId(clientId, profileId)
                || repository.existsByClientIdAndPartnerId(clientId, profileId);
    }

    /** 유료 리포트를 볼 수 있는지 확인 (못 보면 403) */
    public void requireOwned(String clientId, Long profileId, Product report) {
        if (report != null && !owned(clientId, profileId).contains(report)) {
            throw new LockedException(report.title() + "를 구매하면 이용할 수 있어요.");
        }
    }

    public void requirePartner(String clientId, Long profileId, Product product, Long partnerId) {
        if (!ownsPartner(clientId, profileId, product, partnerId)) {
            throw new LockedException("이 상대와의 " + product.title() + "를 구매하면 볼 수 있어요.");
        }
    }

    /** 구매 요청 확인: 내 프로필인지, 있는 상품인지, 새로 열리는 리포트가 있는지 */
    public Product check(String clientId, Long profileId, String productCode, Long partnerId) {
        Profile p = profileRepository.findByIdAndClientId(profileId, clientId)
                .orElseThrow(() -> new NoSuchElementException("사주 프로필을 찾을 수 없어요."));
        Product product;
        try {
            product = Product.valueOf(productCode);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("없는 상품이에요.");
        }
        if (product.perPartner()) {
            // 궁합 · 재회운은 상대가 꼭 있어야 하고, 이 사주에 그 종류로 입력한 상대여야 한다
            if (partnerId == null) throw new IllegalArgumentException("상대를 먼저 입력해주세요.");
            Profile partner = profileRepository.findByIdAndClientId(partnerId, clientId)
                    .orElseThrow(() -> new NoSuchElementException("상대 정보를 찾을 수 없어요."));
            if (!p.getId().equals(partner.getPartnerOf()) || !product.name().equals(partner.getPartnerKind())) {
                throw new NoSuchElementException("상대 정보를 찾을 수 없어요.");
            }
            if (ownsPartner(clientId, p.getId(), product, partnerId)) throw new IllegalArgumentException("이미 구매한 리포트예요.");
            return product;
        }
        if (!product.addsSomething(owned(clientId, p.getId()))) {
            throw new IllegalArgumentException("이미 구매한 리포트예요.");
        }
        return product;
    }

    /**
     * 심화 리포트를 열어봤다고 기록 (열어본 리포트는 환불 불가).
     * 이 리포트를 연 구매(단품 · 묶음 · 이 상대와의 궁합)에 모두 표시한다.
     */
    @Transactional
    public void markOpened(String clientId, Long profileId, Product report, Long partnerId) {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        for (Purchase p : repository.findByClientIdAndProfileId(clientId, profileId)) {
            if (!p.isActive() || p.getOpenedAt() != null) continue;
            Product bought = p.getProduct();
            boolean covers = report.perPartner()
                    ? bought == report && java.util.Objects.equals(p.getPartnerId(), partnerId)
                    : bought == report || (bought == Product.BUNDLE && Product.BUNDLE_REPORTS.contains(report));
            if (covers) {
                p.markOpened(now);
                repository.save(p);
            }
        }
    }

    /** 환불할 구매를 확인 (내 것 · 사용 중 · 열어보지 않음 · 7일 이내) */
    public Purchase refundable(String clientId, Long purchaseId) {
        Purchase p = repository.findByIdAndClientId(purchaseId, clientId)
                .orElseThrow(() -> new NoSuchElementException("구매 내역을 찾을 수 없어요."));
        String blocked = p.refundBlockedReason(java.time.LocalDateTime.now());
        if (blocked != null) throw new IllegalArgumentException(blocked);
        return p;
    }

    @Transactional
    public PurchaseDto.Response markRefunded(Purchase p) {
        p.refund(java.time.LocalDateTime.now());
        return PurchaseDto.Response.of(repository.save(p));
    }

    /** 구매 기록 저장 (결제 승인이 끝난 뒤, 또는 모의 결제) */
    @Transactional
    public PurchaseDto.Response grant(String clientId, Long profileId, Product product, Long partnerId) {
        return PurchaseDto.Response.of(repository.save(new Purchase(clientId, profileId, product, partnerId)));
    }

    /** 모의 결제: 확인 후 바로 저장 */
    @Transactional
    public PurchaseDto.Response create(String clientId, PurchaseDto.CreateRequest req) {
        Product product = check(clientId, req.profileId(), req.productCode(), req.partnerId());
        return grant(clientId, req.profileId(), product, req.partnerId());
    }
}
