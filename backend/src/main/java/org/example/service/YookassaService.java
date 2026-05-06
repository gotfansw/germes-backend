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
import java.util.Set;
import java.util.UUID;

@Service
public class YookassaService {

    private static final Logger log = LoggerFactory.getLogger(YookassaService.class);


    private static final Set<String> YOOKASSA_IPS = Set.of(
            "185.71.76.0/27",
            "185.71.77.0/27",
            "77.75.153.0/25",
            "77.75.156.11",
            "77.75.156.35",
            "77.75.154.128/25",
            "2a02:5180::/32"
    );

    // Конкретные IPv4 без CIDR для простой проверки (без сторонних библиотек)
    private static final Set<String> YOOKASSA_EXACT_IPS = Set.of(
            "77.75.156.11",
            "77.75.156.35"
    );
    // Префиксы подсетей ЮКассы (первые октеты)
    private static final Set<String> YOOKASSA_IP_PREFIXES = Set.of(
            "185.71.76.",
            "185.71.77.",
            "77.75.153.",
            "77.75.154."
    );

    @Value("${yookassa.shop-id}")
    private String shopId;

    @Value("${yookassa.secret-key}")
    private String secretKey;

    @Value("${yookassa.return-url}")
    private String returnUrl;


    @Value("${yookassa.webhook.verify-ip:true}")
    private boolean verifyIp;

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

            // Не логируем тело целиком в проде — может содержать чувствительные данные
            log.info("ЮКасса: создаём платёж для заказа #{}", orderId);

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

            log.info("ЮКасса статус ответа для заказа #{}: {}", orderId, response.statusCode());

            if (response.statusCode() != 200 && response.statusCode() != 201) {
                log.error("ЮКасса вернула ошибку для заказа #{}: {} {}", orderId, response.statusCode(), response.body());
                throw new RuntimeException("ЮКасса вернула ошибку: " + response.statusCode() + " " + response.body());
            }

            JsonNode json = objectMapper.readTree(response.body());
            String confirmationUrl = json.path("confirmation").path("confirmation_url").asText();

            if (confirmationUrl == null || confirmationUrl.isBlank()) {
                log.error("ЮКасса не вернула confirmation_url для заказа #{}", orderId);
                throw new RuntimeException("ЮКасса не вернула ссылку для оплаты");
            }

            log.info("ЮКасса: платёж создан для заказа #{}", orderId);
            return confirmationUrl;

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Неожиданная ошибка при создании платежа для заказа #{}: {}", orderId, e.getMessage(), e);
            throw new RuntimeException("Ошибка создания платежа: " + e.getMessage(), e);
        }
    }


    public void verifyWebhookIp(String remoteAddr) {
        if (!verifyIp) {
            log.warn("Проверка IP вебхука ЮКассы отключена (yookassa.webhook.verify-ip=false)");
            return;
        }

        if (remoteAddr == null || remoteAddr.isBlank()) {
            log.warn("Вебхук ЮКассы: пустой IP-адрес, отклоняем");
            throw new SecurityException("Вебхук отклонён: IP не определён");
        }

        // Точные адреса
        if (YOOKASSA_EXACT_IPS.contains(remoteAddr)) {
            return;
        }

        // Проверка по префиксу подсети
        for (String prefix : YOOKASSA_IP_PREFIXES) {
            if (remoteAddr.startsWith(prefix)) {
                return;
            }
        }

        log.warn("Вебхук ЮКассы с неизвестного IP: {} — отклоняем", remoteAddr);
        throw new SecurityException("Вебхук отклонён: неизвестный IP " + remoteAddr);
    }

    public WebhookResult parseWebhook(String rawBody) {
        try {
            log.info("ЮКасса: разбираем вебхук");

            JsonNode root = objectMapper.readTree(rawBody);
            String event = root.path("event").asText();
            JsonNode obj = root.path("object");

            String orderIdStr = obj.path("metadata").path("orderId").asText(null);
            Long orderId = (orderIdStr != null && !orderIdStr.isBlank()) ? Long.parseLong(orderIdStr) : null;
            String status = obj.path("status").asText();

            log.info("ЮКасса вебхук: event={}, orderId={}, status={}", event, orderId, status);

            return new WebhookResult(orderId, event, status);
        } catch (Exception e) {
            log.error("Ошибка разбора вебхука ЮКассы: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка разбора вебхука: " + e.getMessage(), e);
        }
    }

    public record WebhookResult(Long orderId, String event, String paymentStatus) {}
}