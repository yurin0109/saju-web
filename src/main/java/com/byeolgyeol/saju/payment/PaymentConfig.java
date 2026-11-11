package com.byeolgyeol.saju.payment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 결제 설정.
 * 키는 코드에 적지 않고 환경변수로 받는다. (GitHub 에 올라가면 안 되는 값)
 *   TOSS_CLIENT_KEY : 화면에서 결제창을 띄울 때 쓰는 키 (test_ck_ 로 시작)
 *   TOSS_SECRET_KEY : 서버가 결제를 승인할 때 쓰는 비밀 키 (test_sk_ 로 시작, 절대 화면에 보내지 않음)
 * 두 키가 모두 있으면 토스 결제창, 하나라도 없으면 모의 결제로 동작한다.
 */
@Component
public class PaymentConfig {

    private final String clientKey;
    private final String secretKey;

    public PaymentConfig(@Value("${toss.client-key:}") String clientKey,
                         @Value("${toss.secret-key:}") String secretKey) {
        this.clientKey = clientKey == null ? "" : clientKey.trim();
        this.secretKey = secretKey == null ? "" : secretKey.trim();
    }

    public boolean toss() {
        return !clientKey.isEmpty() && !secretKey.isEmpty();
    }

    public boolean mock() {
        return !toss();
    }

    /** 테스트 키인지 (test_ 로 시작) */
    public boolean testMode() {
        return clientKey.startsWith("test_");
    }

    public String clientKey() { return clientKey; }

    String secretKey() { return secretKey; }
}
