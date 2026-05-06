package org.example.service;

import org.example.model.Order;
import org.example.model.PaymentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class OrderMailService {

    private static final Logger log = LoggerFactory.getLogger(OrderMailService.class);


    private static final int MAX_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 2_000L;

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${mail.from:no-reply@germes74.local}")
    private String from;

    public OrderMailService(ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.mailSenderProvider = mailSenderProvider;
    }

    public void sendPaymentMail(Order order) {
        if (order.getCustomerEmail() == null || order.getCustomerEmail().isBlank()) return;

        String subject = switch (order.getPaymentStatus()) {
            case PENDING        -> "Ожидание оплаты заказа #" + order.getId();
            case PAID           -> "Спасибо за оплату заказа #" + order.getId();
            case FAILED         -> "Оплата заказа #" + order.getId() + " отклонена";
            case CANCELED       -> "Оплата заказа #" + order.getId() + " отменена";
            case PAYMENT_FAILED -> "Оплата заказа #" + order.getId() + " не прошла";
        };

        String body = buildBody(order);
        sendWithRetry(order.getCustomerEmail(), subject, body, order.getId());
    }


    private void sendWithRetry(String to, String subject, String body, Long orderId) {
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null) {
            log.info("[Mail] Sender не настроен. Заказ #{} | To: {} | Subject: {} | Body:\n{}",
                    orderId, to, subject, body);
            return;
        }

        MailException lastException = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(from);
                message.setTo(to);
                message.setSubject(subject);
                message.setText(body);
                sender.send(message);

                if (attempt > 1) {
                    log.info("[Mail] Письмо для заказа #{} отправлено с попытки {}", orderId, attempt);
                }
                return; // успех — выходим

            } catch (MailException ex) {
                lastException = ex;
                log.warn("[Mail] Попытка {}/{} для заказа #{} неудачна: {}",
                        attempt, MAX_ATTEMPTS, orderId, ex.getMessage());

                // Не повторяем при постоянных ошибках (неверный адрес, отказ сервера 5xx)
                if (isNonRecoverable(ex)) {
                    log.error("[Mail] Неустранимая ошибка для заказа #{}, retry прерван: {}",
                            orderId, ex.getMessage());
                    break;
                }

                if (attempt < MAX_ATTEMPTS) {
                    long delay = RETRY_DELAY_MS * (1L << (attempt - 1)); // 2s, 4s, 8s...
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.warn("[Mail] Retry прерван для заказа #{}", orderId);
                        break;
                    }
                }
            }
        }

        // Все попытки исчерпаны — логируем письмо полностью для ручного восстановления
        log.error("[Mail] Не удалось отправить письмо для заказа #{} после {} попыток. " +
                        "Последняя ошибка: {}. " +
                        "To: {} | Subject: {} | Body:\n{}",
                orderId, MAX_ATTEMPTS,
                lastException != null ? lastException.getMessage() : "unknown",
                to, subject, body);
    }

    /**
     * Постоянные ошибки SMTP, при которых retry бессмысленен:
     * 5xx — постоянный отказ сервера, неверный адрес получателя и т.п.
     */
    private boolean isNonRecoverable(MailException ex) {
        String msg = ex.getMessage();
        if (msg == null) return false;
        // 550 — mailbox not found / user unknown
        // 553 — address not allowed
        // 554 — transaction failed
        return msg.contains("550") || msg.contains("553") || msg.contains("554")
                || msg.toLowerCase().contains("user unknown")
                || msg.toLowerCase().contains("invalid address");
    }

    private String buildBody(Order order) {
        return "Здравствуйте!\n\n" +
                "Заказ №" + order.getId() + "\n" +
                "Статус оплаты: " + translatePaymentStatus(order.getPaymentStatus()) + "\n" +
                "Квитанция: " + safe(order.getReceiptNumber()) + "\n" +
                "Трек-номер: " + safe(order.getTrackNumber()) + "\n\n" +
                "Статусы оплаты:\n" +
                formatPaymentStatuses(order.getPaymentStatus()) +
                "\nС уважением, Germes74";
    }

    private String formatPaymentStatuses(PaymentStatus current) {
        return marker(current, PaymentStatus.PENDING)        + " Ожидание оплаты\n" +
                marker(current, PaymentStatus.PAID)           + " Оплачено\n"         +
                marker(current, PaymentStatus.FAILED)         + " Отказано\n"         +
                marker(current, PaymentStatus.CANCELED)       + " Отменено\n"         +
                marker(current, PaymentStatus.PAYMENT_FAILED) + " Платёж не прошёл\n";
    }

    private String marker(PaymentStatus current, PaymentStatus status) {
        return current == status ? "[x]" : "[ ]";
    }

    private String translatePaymentStatus(PaymentStatus status) {
        return switch (status) {
            case PENDING        -> "Ожидание оплаты";
            case PAID           -> "Оплачено";
            case FAILED         -> "Отказано";
            case CANCELED       -> "Отменено";
            case PAYMENT_FAILED -> "Платёж не прошёл";
        };
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }
}