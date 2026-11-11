package com.byeolgyeol.saju.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

/**
 * 토스페이먼츠 결제 승인 API 호출.
 * POST https://api.tosspayments.com/v1/payments/confirm
 * 인증: Basic base64("시크릿키:")  ← 시크릿 키 뒤에 콜론
 */
@Component
public class TossClient {

    private static final String CONFIRM_URL = "https://api.tosspayments.com/v1/payments/confirm";

    private final PaymentConfig config;
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public TossClient(PaymentConfig config) {
        this.config = config;
    }

    public record Result(boolean ok, String method, String code, String message) {}

    /**
     * 결제 취소(환불): POST /v1/payments/{paymentKey}/cancel
     * Idempotency-Key: 같은 환불 요청이 두 번 가도 한 번만 처리되게
     */
    public Result cancel(String paymentKey, String reason, String idempotencyKey) {
        try {
            String body = mapper.writeValueAsString(Map.of("cancelReason", reason));
            String auth = Base64.getEncoder().encodeToString((config.secretKey() + ":").getBytes(StandardCharsets.UTF_8));
            String url = "https://api.tosspayments.com/v1/payments/"
                    + java.net.URLEncoder.encode(paymentKey, StandardCharsets.UTF_8) + "/cancel";
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Basic " + auth)
                    .header("Content-Type", "application/json")
                    .header("Idempotency-Key", idempotencyKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode json = mapper.readTree(res.body());
            if (res.statusCode() == 200) return new Result(true, null, null, null);
            return new Result(false, null, json.path("code").asText("UNKNOWN"), json.path("message").asText("환불 처리에 실패했어요."));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Result(false, null, "NETWORK", "결제 서버와 연결하지 못했어요.");
        } catch (Exception e) {
            return new Result(false, null, "NETWORK", "결제 서버와 연결하지 못했어요.");
        }
    }

    public Result confirm(String paymentKey, String orderId, int amount) {
        try {
            String body = mapper.writeValueAsString(Map.of("paymentKey", paymentKey, "orderId", orderId, "amount", amount));
            String auth = Base64.getEncoder().encodeToString((config.secretKey() + ":").getBytes(StandardCharsets.UTF_8));
            HttpRequest req = HttpRequest.newBuilder(URI.create(CONFIRM_URL))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Basic " + auth)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode json = mapper.readTree(res.body());
            if (res.statusCode() == 200) {
                return new Result(true, json.path("method").asText(null), null, null);
            }
            return new Result(false, null, json.path("code").asText("UNKNOWN"), json.path("message").asText("결제 승인에 실패했어요."));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Result(false, null, "NETWORK", "결제 서버와 연결하지 못했어요.");
        } catch (Exception e) {
            return new Result(false, null, "NETWORK", "결제 서버와 연결하지 못했어요.");
        }
    }
}
