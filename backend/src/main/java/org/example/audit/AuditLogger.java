package org.example.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;


@Component
public class AuditLogger {

    // Отдельный logger "audit" — можно настроить в logback.xml отдельно от основного лога
    private static final Logger audit = LoggerFactory.getLogger("audit");

    @EventListener
    @Async
    public void onAuditEvent(AuditEvent event) {
        audit.info("{}", event);
    }
}