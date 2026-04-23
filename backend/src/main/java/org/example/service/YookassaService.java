package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

@Service
public class YookassaService {

    private static final Logger log = LoggerFactory.getLogger(YookassaService.class);

    @Value("${yookassa.shop-id}")
    private String shopId;

    @Value("${yookassa.secret-key}")
    private String secretKey;

    @Value("${yookassa.return-url}")
    private String returnUrl;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String createSbpPayment(Long orderId, java.math.BigDecimal amount, String description) {
        try {
            String idempotenceKey = UUID.randomUUID().toString();

            String body = objectMapper.writeValueAsString(java.util.Map.of(
                    "amount", java.util.Map.of(
                            "value", String.format("%.2f", amount),
                            "currency", "RUB"
                    ),
                    "payment_method_data", java.util.Map.of(
                            "type", "sbp"
                    ),
                    "confirmation", java.util.Map.of(
                            "type", "redirect",
                            "return_url", returnUrl + "?orderId=" + orderId
                    ),
                    "description", description,
                    "metadata", java.util.Map.of(
                            "orderId", String.valueOf(orderId)
                    ),
                    "capture", true
            ));

            log.info("ЮКасса запрос для заказа #{}: {}", orderId, body);

            String credentials = Base64.getEncoder().encodeToString(
                    (shopId + ":" + secretKey).getBytes(StandardCharsets.UTF_8)
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.yookassa.ru/v3/payments"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Basic " + credentials)
                    .header("Idempotence-Key", idempotenceKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            log.info("ЮКасса статус ответа: {}", response.statusCode());
            log.info("ЮКасса тело ответа: {}", response.body());

            if (response.statusCode() != 200 && response.statusCode() != 201) {
                log.error("ЮКасса вернула ошибку для заказа #{}: {} {}", orderId, response.statusCode(), response.body());
                throw new RuntimeException("ЮКасса вернула ошибку: " + response.statusCode() + " " + response.body());
            }

            JsonNode json = objectMapper.readTree(response.body());
            String confirmationUrl = json.path("confirmation").path("confirmation_url").asText();

            if (confirmationUrl == null || confirmationUrl.isBlank()) {
                log.error("ЮКасса не вернула confirmation_url для заказа #{}: {}", orderId, response.body());
                throw new RuntimeException("ЮКасса не вернула ссылку для оплаты");
            }

            log.info("ЮКасса платёж создан для заказа #{}, url: {}", orderId, confirmationUrl);
            return confirmationUrl;

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Неожиданная ошибка при создании платежа для заказа #{}: {}", orderId, e.getMessage(), e);
            throw new RuntimeException("Ошибка создания платежа: " + e.getMessage(), e);
        }
    }

    public WebhookResult parseWebhook(String rawBody) {
        try {
            log.info("ЮКасса вебхук получен: {}", rawBody);

            JsonNode root = objectMapper.readTree(rawBody);
            String event = root.path("event").asText();
            JsonNode obj = root.path("object");

            String orderIdStr = obj.path("metadata").path("orderId").asText(null);
            Long orderId = (orderIdStr != null && !orderIdStr.isBlank()) ? Long.parseLong(orderIdStr) : null;
            String status = obj.path("status").asText();

            log.info("ЮКасса вебхук: event={}, orderId={}, status={}", event, orderId, status);

            return new WebhookResult(orderId, event, status);
        } catch (Exception e) {
            log.error("Ошибка разбора вебхука: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка разбора вебхука: " + e.getMessage(), e);
        }
    }

    public record WebhookResult(Long orderId, String event, String paymentStatus) {}
}