package com.byeolgyeol.saju.feedback;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional(readOnly = true)
public class FeedbackService {

    /** 같은 사람이 하루에 보낼 수 있는 의견 수 (도배 방지) */
    static final int DAILY_LIMIT = 10;

    private final FeedbackRepository repository;

    public FeedbackService(FeedbackRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void create(String clientId, String username, FeedbackDto.CreateRequest req) {
        if (repository.countByClientIdAndCreatedAtAfter(clientId, LocalDateTime.now().minusDays(1)) >= DAILY_LIMIT) {
            throw new IllegalArgumentException("의견은 하루에 " + DAILY_LIMIT + "개까지 보낼 수 있어요. 소중한 의견 고마워요!");
        }
        repository.save(new Feedback(clientId, Feedback.Category.valueOf(req.category()), req.message().strip(),
                req.page() == null || req.page().isBlank() ? null : req.page(), username));
    }

    public List<FeedbackDto.Response> recent() {
        return repository.findTop300ByOrderByCreatedAtDesc().stream().map(FeedbackDto.Response::of).toList();
    }

    public long countNew() {
        return repository.countByDoneFalse();
    }

    @Transactional
    public void markDone(Long id, boolean done) {
        Feedback f = repository.findById(id).orElseThrow(() -> new NoSuchElementException("의견을 찾을 수 없어요."));
        f.markDone(done);
        repository.save(f);
    }

    @Transactional
    public void delete(Long id) {
        Feedback f = repository.findById(id).orElseThrow(() -> new NoSuchElementException("의견을 찾을 수 없어요."));
        repository.delete(f);
    }
}
