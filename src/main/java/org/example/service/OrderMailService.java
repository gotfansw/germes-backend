package org.example.service;

import org.example.model.Order;
import org.example.model.PaymentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class OrderMailService {

    private static final Logger log = LoggerFactory.getLogger(OrderMailService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${mail.from:no-reply@germes74.local}")
    private String from;

    public OrderMailService(ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.mailSenderProvider = mailSenderProvider;
    }

    public void sendPaymentMail(Order order) {
        if (order.getCustomerEmail() == null || order.getCustomerEmail().isBlank()) return;

        String subject = switch (order.getPaymentStatus()) {
            case PENDING -> "Ожидание оплаты заказа #" + order.getId();
            case PAID -> "Спасибо за оплату заказа #" + order.getId();
            case FAILED -> "Оплата заказа #" + order.getId() + " отклонена";
            case CANCELED -> "Оплата заказа #" + order.getId() + " отменена";
        };

        String body = buildBody(order);
        send(order.getCustomerEmail(), subject, body);
    }

    private void send(String to, String subject, String body) {
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null) {
            log.info("Mail sender not configured. To: {} Subject: {} Body:\n{}", to, subject, body);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            sender.send(message);
        } catch (Exception ex) {
            log.warn("Failed to send email to {}: {}", to, ex.getMessage());
            log.info("Email body fallback:\n{}", body);
        }
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
        return marker(current, PaymentStatus.PENDING) + " Ожидание оплаты\n" +
                marker(current, PaymentStatus.PAID) + " Оплачено\n" +
                marker(current, PaymentStatus.FAILED) + " Отказано\n" +
                marker(current, PaymentStatus.CANCELED) + " Отменено\n";
    }

    private String marker(PaymentStatus current, PaymentStatus status) {
        return current == status ? "[x]" : "[ ]";
    }

    private String translatePaymentStatus(PaymentStatus status) {
        return switch (status) {
            case PENDING -> "Ожидание оплаты";
            case PAID -> "Оплачено";
            case FAILED -> "Отказано";
            case CANCELED -> "Отменено";
        };
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }
}
