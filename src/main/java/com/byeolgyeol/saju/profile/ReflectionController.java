package com.byeolgyeol.saju.profile;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 성찰 일기 REST API
 *
 * GET    /api/reflections/question?profileId=&type=core|year|career|marriage&offset=0   오늘의 성찰 질문
 * GET    /api/reflections?profileId=                                    성찰 일기 목록 (profileId 없으면 전체)
 * POST   /api/reflections                                               성찰 일기 저장
 * PUT    /api/reflections/{id}                                          답 수정
 * DELETE /api/reflections/{id}                                          삭제
 */
@RestController
@RequestMapping("/api/reflections")
public class ReflectionController {

    private static final String CLIENT_HEADER = "X-Client-Id";

    private final ReflectionService service;

    public ReflectionController(ReflectionService service) {
        this.service = service;
    }

    @GetMapping("/question")
    public ReflectionDto.Question question(@RequestHeader(CLIENT_HEADER) String clientId,
                                           @RequestParam("profileId") Long profileId,
                                           @RequestParam(value = "type", defaultValue = "core") String type,
                                           @RequestParam(value = "offset", defaultValue = "0") int offset) {
        if (!ReflectionDto.TYPES.contains(type)) {
            throw new IllegalArgumentException("질문 종류가 올바르지 않아요.");
        }
        return service.question(ProfileController.ClientId.check(clientId), profileId, type, offset);
    }

    @GetMapping
    public List<ReflectionDto.Response> list(@RequestHeader(CLIENT_HEADER) String clientId,
                                             @RequestParam(value = "profileId", required = false) Long profileId) {
        return service.findAll(ProfileController.ClientId.check(clientId), profileId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReflectionDto.Response create(@RequestHeader(CLIENT_HEADER) String clientId,
                                         @Valid @RequestBody ReflectionDto.CreateRequest request) {
        return service.create(ProfileController.ClientId.check(clientId), request);
    }

    @PutMapping("/{id}")
    public ReflectionDto.Response update(@RequestHeader(CLIENT_HEADER) String clientId, @PathVariable("id") Long id,
                                         @Valid @RequestBody ReflectionDto.UpdateRequest request) {
        return service.update(ProfileController.ClientId.check(clientId), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestHeader(CLIENT_HEADER) String clientId, @PathVariable("id") Long id) {
        service.delete(ProfileController.ClientId.check(clientId), id);
    }
}
