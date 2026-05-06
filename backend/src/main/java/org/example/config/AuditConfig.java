package org.example.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;


@Configuration
@EnableAsync
public class AuditConfig {
    // Spring создаст пул потоков по умолчанию (SimpleAsyncTaskExecutor).
    // Для продакшена замени на ThreadPoolTaskExecutor с явными настройками:
    //
    // @Bean
    // public Executor auditExecutor() {
    //     ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
    //     exec.setCorePoolSize(2);
    //     exec.setMaxPoolSize(4);
    //     exec.setQueueCapacity(100);
    //     exec.setThreadNamePrefix("audit-");
    //     exec.initialize();
    //     return exec;
    // }
}