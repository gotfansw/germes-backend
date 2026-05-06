package org.example.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.model.PaymentStatus;
import org.example.service.OrderService;
import org.example.service.YookassaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final YookassaService yookassaService;
    private final OrderService orderService;

    public PaymentController(YookassaService yookassaService, OrderService orderService) {
        this.yookassaService = yookassaService;
        this.orderService = orderService;
    }


    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(@RequestBody String rawBody,
                                        HttpServletRequest httpRequest) {
        // [FIX #5] Получаем реальный IP с учётом reverse proxy (nginx/Vercel)
        String remoteIp = getClientIp(httpRequest);

        try {
            // Бросает SecurityException если IP не из белого списка ЮКассы
            yookassaService.verifyWebhookIp(remoteIp);

            YookassaService.WebhookResult result = yookassaService.parseWebhook(rawBody);

            if (result.orderId() == null) {
                log.warn("Вебхук ЮКассы: orderId отсутствует в metadata, игнорируем. Event: {}", result.event());
                return ResponseEntity.ok().build();
            }

            switch (result.event()) {
                case "payment.succeeded" -> {
                    log.info("Вебхук ЮКассы: payment.succeeded для заказа #{}", result.orderId());
                    orderService.updatePaymentStatus(result.orderId(), PaymentStatus.PAID);
                }
                case "payment.canceled" -> {
                    log.info("Вебхук ЮКассы: payment.canceled для заказа #{}", result.orderId());
                    orderService.updatePaymentStatus(result.orderId(), PaymentStatus.CANCELED);
                }
                default -> log.info("Вебхук ЮКассы: неизвестное событие '{}', игнорируем", result.event());
            }

        } catch (SecurityException e) {
            // [FIX #10] log.warn вместо System.err
            log.warn("Вебхук отклонён (IP {}): {}", remoteIp, e.getMessage());
            // Возвращаем 200 даже при отклонении — не даём злоумышленнику
            // понять, что запрос заблокирован (и не провоцируем ретраи ЮКассы)
        } catch (Exception e) {
            // [FIX #10] log.error вместо System.err
            log.error("Ошибка обработки вебхука ЮКассы: {}", e.getMessage(), e);
        }

        return ResponseEntity.ok().build();
    }


    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            // X-Forwarded-For может содержать цепочку: "client, proxy1, proxy2"
            // Берём первый (самый левый) — это реальный клиент
            return xff.split(",")[0].trim();
        }
        String xri = request.getHeader("X-Real-IP");
        if (xri != null && !xri.isBlank()) {
            return xri.trim();
        }
        return request.getRemoteAddr();
    }
}