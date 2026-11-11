package com.byeolgyeol.saju.feedback;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    List<Feedback> findTop300ByOrderByCreatedAtDesc();

    long countByClientIdAndCreatedAtAfter(String clientId, LocalDateTime after);

    long countByDoneFalse();
}
