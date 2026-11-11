package com.byeolgyeol.saju.feedback;

import com.byeolgyeol.saju.auth.Login;
import com.byeolgyeol.saju.profile.ProfileController;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** POST /api/feedback  건의사항 보내기 (로그인 없이 가능) */
@RestController
public class FeedbackController {

    private final FeedbackService service;

    public FeedbackController(FeedbackService service) {
        this.service = service;
    }

    @PostMapping("/api/feedback")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> create(@RequestHeader("X-Client-Id") String clientId,
                                      @Valid @RequestBody FeedbackDto.CreateRequest req, HttpServletRequest http) {
        service.create(ProfileController.ClientId.check(clientId), Login.username(http), req);
        return Map.of("message", "의견을 보냈어요. 고마워요!");
    }
}
