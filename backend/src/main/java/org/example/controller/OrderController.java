package org.example.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.example.dto.OrderDTO;
import org.example.dto.PaymentStatusResponse;
import org.example.dto.PlaceOrderRequest;
import org.example.dto.UpdateOrderStatusRequest;
import org.example.dto.UpdatePaymentStatusRequest;
import org.example.dto.UpdateTrackingRequest;
import org.example.service.OrderService;
import org.example.service.QrCodeService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final QrCodeService qrCodeService;

    public OrderController(OrderService orderService, QrCodeService qrCodeService) {
        this.orderService = orderService;
        this.qrCodeService = qrCodeService;
    }

    private String resolveSessionId(String headerSession, HttpSession session) {
        return (headerSession != null && !headerSession.isBlank())
                ? headerSession
                : session.getId();
    }

    // Публичный — пользователь не авторизован при оформлении заказа
    @PostMapping("/place")
    public OrderDTO placeOrder(@Valid @RequestBody PlaceOrderRequest request,
                               @RequestHeader(value = "X-Session-Id", required = false) String headerSession,
                               HttpSession session) {
        return orderService.placeOrder(resolveSessionId(headerSession, session), request);
    }

    // Только ADMIN — закрыто в SecurityConfig
    @PatchMapping("/{id}/payment-status")
    public OrderDTO updatePaymentStatus(@PathVariable Long id,
                                        @Valid @RequestBody UpdatePaymentStatusRequest request) {
        return orderService.updatePaymentStatus(id, request.getPaymentStatus());
    }


    @GetMapping("/{id}/payment-status")
    public ResponseEntity<PaymentStatusResponse> getPaymentStatus(@PathVariable Long id) {
        OrderDTO order = orderService.getOrder(id);
        // Возвращаем только публично безопасные поля
        PaymentStatusResponse response = new PaymentStatusResponse(
                order.getId(),
                order.getPaymentStatus(),
                null,   // receiptNumber — только для владельца/админа
                null    // trackNumber   — только для владельца/админа
        );
        return ResponseEntity.ok(response);
    }

    // Публичный — нужен для перехода пользователя по QR-ссылке
    @GetMapping(value = "/{id}/sbp-qr", produces = "image/svg+xml")
    public ResponseEntity<byte[]> getSbpQr(@PathVariable Long id, HttpServletRequest request) {
        String base = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
        String paymentLink = base + "/api/orders/" + id + "/pay";
        String svg = qrCodeService.generateSvg(paymentLink, 280);
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("image/svg+xml"))
                .body(svg.getBytes(StandardCharsets.UTF_8));
    }

    // Публичный — страница ожидания оплаты, открывается по QR
    @GetMapping(value = "/{id}/pay", produces = MediaType.TEXT_HTML_VALUE)
    public String payPage(@PathVariable Long id) {
        OrderDTO order = orderService.getOrder(id);

        // [FIX #16] Метод safe() удалён — он нигде не использовался
        String html = "<!doctype html>" +
                "<html lang=\"ru\">" +
                "<head>" +
                "<meta charset=\"UTF-8\">" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "<title>Ожидание оплаты</title>" +
                "<style>" +
                "body{font-family:Arial,sans-serif;background:#f5f5f5;display:grid;place-items:center;min-height:100vh;margin:0}" +
                ".card{background:#fff;padding:32px;border-radius:18px;max-width:420px;box-shadow:0 18px 50px rgba(0,0,0,.12);text-align:center}" +
                ".icon{font-size:56px;line-height:1;color:#ca8a04;margin-bottom:12px}" +
                "h1{font-size:28px;margin:0 0 10px}" +
                "p{color:#555;line-height:1.5;margin:8px 0}" +
                ".meta{margin-top:16px;padding:14px;background:#faf7f3;border-radius:12px;text-align:left}" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class=\"card\">" +
                "<div class=\"icon\">⏳</div>" +
                "<h1>Ожидание оплаты</h1>" +
                "<p>Оплатите заказ через СБП. Статус обновится автоматически после подтверждения от банка.</p>" +
                "<div class=\"meta\">" +
                "<p><b>Заказ:</b> #" + order.getId() + "</p>" +
                "<p><b>Статус:</b> " + order.getPaymentStatus() + "</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";

        return html;
    }

    // Только ADMIN — закрыто в SecurityConfig
    @GetMapping
    public List<OrderDTO> getAllOrders() {
        return orderService.getAllOrders();
    }

    // Только ADMIN — закрыто в SecurityConfig
    @PatchMapping("/{id}/status")
    public OrderDTO updateStatus(@PathVariable Long id,
                                 @Valid @RequestBody UpdateOrderStatusRequest request) {
        return orderService.updateOrderStatus(id, request.getStatus());
    }

    // Только ADMIN — закрыто в SecurityConfig
    @PatchMapping("/{id}/tracking")
    public OrderDTO updateTracking(@PathVariable Long id,
                                   @Valid @RequestBody UpdateTrackingRequest request) {
        return orderService.updateTracking(id, request.getTrackNumber());
    }

    // Только ADMIN — закрыто в SecurityConfig
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }
}