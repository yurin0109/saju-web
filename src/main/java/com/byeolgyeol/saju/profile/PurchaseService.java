package com.byeolgyeol.saju.profile;

import com.byeolgyeol.saju.common.LockedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * 모의 결제 · 구매 내역.
 * 실제 결제(PG) 연동은 하지 않고, 구매 기록만 남겨서 유료 리포트를 연다.
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
        return repository.findByClientIdOrderByCreatedAtDesc(clientId).stream()
                .map(PurchaseDto.Response::of).toList();
    }

    /** 이 프로필로 열려 있는 유료 리포트 */
    public Set<Product> owned(String clientId, Long profileId) {
        return Product.owned(repository.findByClientIdAndProfileId(clientId, profileId).stream()
                .map(Purchase::getProduct).toList());
    }

    /** 유료 리포트를 볼 수 있는지 확인 (못 보면 403) */
    public void requireOwned(String clientId, Long profileId, Product report) {
        if (report != null && !owned(clientId, profileId).contains(report)) {
            throw new LockedException(report.title() + "를 구매하면 이용할 수 있어요.");
        }
    }

    /** 구매 요청 확인: 내 프로필인지, 있는 상품인지, 새로 열리는 리포트가 있는지 */
    public Product check(String clientId, Long profileId, String productCode) {
        Profile p = profileRepository.findByIdAndClientId(profileId, clientId)
                .orElseThrow(() -> new NoSuchElementException("사주 프로필을 찾을 수 없어요."));
        Product product;
        try {
            product = Product.valueOf(productCode);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("없는 상품이에요.");
        }
        if (!product.addsSomething(owned(clientId, p.getId()))) {
            throw new IllegalArgumentException("이미 구매한 리포트예요.");
        }
        return product;
    }

    /** 구매 기록 저장 (결제 승인이 끝난 뒤, 또는 모의 결제) */
    @Transactional
    public PurchaseDto.Response grant(String clientId, Long profileId, Product product) {
        return PurchaseDto.Response.of(repository.save(new Purchase(clientId, profileId, product)));
    }

    /** 모의 결제: 확인 후 바로 저장 */
    @Transactional
    public PurchaseDto.Response create(String clientId, PurchaseDto.CreateRequest req) {
        Product product = check(clientId, req.profileId(), req.productCode());
        return grant(clientId, req.profileId(), product);
    }
}
