package org.example.audit;

import java.time.LocalDateTime;


public class AuditEvent {

    public enum Action {
        ORDER_CREATED,
        PAYMENT_STATUS_CHANGED,
        ORDER_STATUS_CHANGED,
        ORDER_TRACKING_UPDATED,
        ORDER_DELETED,
        ADMIN_ACTION
    }

    private final Action action;
    private final Long orderId;
    private final String details;
    private final LocalDateTime occurredAt;

    public AuditEvent(Action action, Long orderId, String details) {
        this.action = action;
        this.orderId = orderId;
        this.details = details;
        this.occurredAt = LocalDateTime.now();
    }

    public Action getAction()             { return action; }
    public Long getOrderId()              { return orderId; }
    public String getDetails()            { return details; }
    public LocalDateTime getOccurredAt()  { return occurredAt; }

    @Override
    public String toString() {
        return "[AUDIT] " + action + " | orderId=" + orderId +
                " | " + details + " | at=" + occurredAt;
    }
}