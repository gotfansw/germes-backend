package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    @Value("${yookassa.shop-id}")
    private String shopId;

    @Value("${yookassa.secret-key}")
    private String secretKey;

    @Value("${yookassa.return-url}")
    private String returnUrl;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Создаёт платёж СБП в ЮКассе и возвращает URL для редиректа.
     */
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

            if (response.statusCode() != 200 && response.statusCode() != 201) {
                throw new RuntimeException("ЮКасса вернула ошибку: " + response.statusCode() + " " + response.body());
            }

            JsonNode json = objectMapper.readTree(response.body());
            return json.path("confirmation").path("confirmation_url").asText();

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Ошибка создания платежа: " + e.getMessage(), e);
        }
    }

    /**
     * Извлекает orderId из вебхука ЮКассы и возвращает статус платежа.
     */
    public WebhookResult parseWebhook(String rawBody) {
        try {
            JsonNode root = objectMapper.readTree(rawBody);
            String event = root.path("event").asText();
            JsonNode obj = root.path("object");

            String orderIdStr = obj.path("metadata").path("orderId").asText(null);
            Long orderId = (orderIdStr != null && !orderIdStr.isBlank()) ? Long.parseLong(orderIdStr) : null;
            String status = obj.path("status").asText();

            return new WebhookResult(orderId, event, status);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка разбора вебхука: " + e.getMessage(), e);
        }
    }

    public record WebhookResult(Long orderId, String event, String paymentStatus) {}
}