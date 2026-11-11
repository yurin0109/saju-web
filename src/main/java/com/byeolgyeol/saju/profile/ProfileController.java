package com.byeolgyeol.saju.profile;

import com.byeolgyeol.saju.calc.Report;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 사주 프로필 REST API
 *
 * GET    /api/profiles        내 사주 목록
 * GET    /api/profiles/{id}   사주 결과 보기
 * POST   /api/profiles        사주 등록
 * PUT    /api/profiles/{id}   사주 정보 수정
 * DELETE /api/profiles/{id}   사주 삭제
 *
 * 모든 요청은 X-Client-Id 헤더(브라우저별 고유 ID)로 내 데이터만 다룬다.
 */
@RestController
@RequestMapping("/api/profiles")
public class ProfileController {

    private static final String CLIENT_HEADER = "X-Client-Id";

    private final ProfileService service;

    public ProfileController(ProfileService service) {
        this.service = service;
    }

    @GetMapping
    public List<ProfileResponse> list(@RequestHeader(CLIENT_HEADER) String clientId) {
        return service.findAll(ClientId.check(clientId));
    }

    @GetMapping("/{id}")
    public ProfileResponse detail(@RequestHeader(CLIENT_HEADER) String clientId, @PathVariable("id") Long id) {
        return service.findOne(ClientId.check(clientId), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProfileResponse create(@RequestHeader(CLIENT_HEADER) String clientId,
                                  @Valid @RequestBody ProfileRequest request) {
        return service.create(ClientId.check(clientId), request);
    }

    @PutMapping("/{id}")
    public ProfileResponse update(@RequestHeader(CLIENT_HEADER) String clientId, @PathVariable("id") Long id,
                                  @Valid @RequestBody ProfileRequest request) {
        return service.update(ClientId.check(clientId), id, request);
    }

    /** 궁합 기본 풀이 (무료) */
    @GetMapping("/{id}/compat")
    public Report compat(@RequestHeader(CLIENT_HEADER) String clientId, @PathVariable("id") Long id,
                         @RequestParam(value = "partnerId") Long partnerId) {
        return service.compat(ClientId.check(clientId), id, partnerId);
    }

    /** 유료 심화 리포트 (구매한 경우만, 아니면 403) */
    @GetMapping("/{id}/premium/{code}")
    public Report premium(@RequestHeader(CLIENT_HEADER) String clientId, @PathVariable("id") Long id,
                          @PathVariable("code") String code,
                          @RequestParam(value = "partnerId", required = false) Long partnerId) {
        return service.premium(ClientId.check(clientId), id, code, partnerId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestHeader(CLIENT_HEADER) String clientId, @PathVariable("id") Long id) {
        service.delete(ClientId.check(clientId), id);
    }

    /** 클라이언트 ID 형식 검사 (UUID) */
    public static final class ClientId {
        private static final String UUID_PATTERN =
                "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";

        public static String check(String id) {
            if (id == null || !id.matches(UUID_PATTERN)) {
                throw new IllegalArgumentException("잘못된 요청이에요. 페이지를 새로고침해주세요.");
            }
            return id;
        }
    }
}
