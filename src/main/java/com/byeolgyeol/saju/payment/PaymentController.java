package com.byeolgyeol.saju.payment;

import com.byeolgyeol.saju.profile.ProfileController;
import com.byeolgyeol.saju.profile.PurchaseDto;
import com.byeolgyeol.saju.auth.Login;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private static final String CLIENT_HEADER = "X-Client-Id";

    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }

    /** 화면이 토스 결제창을 쓸지, 모의 결제를 쓸지 확인 */
    @GetMapping("/config")
    public PaymentDto.ConfigResponse config() {
        return service.config();
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentDto.OrderResponse order(@RequestHeader(CLIENT_HEADER) String clientId,
                                          @Valid @RequestBody PaymentDto.OrderRequest req,
                                          HttpServletRequest request) {
        Login.require(request);   // 결제는 로그인한 회원만
        return service.createOrder(ProfileController.ClientId.check(clientId), req);
    }

    @PostMapping("/confirm")
    public PurchaseDto.Response confirm(@RequestHeader(CLIENT_HEADER) String clientId,
                                        @Valid @RequestBody PaymentDto.ConfirmRequest req,
                                        HttpServletRequest request) {
        Login.require(request);
        return service.confirm(ProfileController.ClientId.check(clientId), req);
    }
}
