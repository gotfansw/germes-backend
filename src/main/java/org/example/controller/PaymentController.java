package org.example.controller;

import org.example.model.PaymentStatus;
import org.example.service.OrderService;
import org.example.service.YookassaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final YookassaService yookassaService;
    private final OrderService orderService;

    public PaymentController(YookassaService yookassaService, OrderService orderService) {
        this.yookassaService = yookassaService;
        this.orderService = orderService;
    }

    /**
     * Вебхук от ЮКассы — вызывается автоматически при смене статуса платежа.
     * Настрой URL в личном кабинете ЮКассы: https://your-domain.ru/api/payments/webhook
     */
    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(@RequestBody String rawBody) {
        try {
            YookassaService.WebhookResult result = yookassaService.parseWebhook(rawBody);

            if (result.orderId() == null) {
                return ResponseEntity.ok().build(); // нет orderId — игнорируем
            }

            switch (result.event()) {
                case "payment.succeeded" -> orderService.updatePaymentStatus(result.orderId(), PaymentStatus.PAID);
                case "payment.canceled"  -> orderService.updatePaymentStatus(result.orderId(), PaymentStatus.CANCELED);
                // payment.waiting_for_capture, refund.succeeded — при необходимости добавить
            }

        } catch (Exception e) {
            // Всегда возвращаем 200, иначе ЮКасса будет ретраить
            System.err.println("Webhook error: " + e.getMessage());
        }

        return ResponseEntity.ok().build();
    }
}