package com.grupo5e.morapack.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuración para ejecución asíncrona de tareas
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "simulacionExecutor")
    public Executor simulacionExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("simulacion-");
        executor.initialize();
        return executor;
    }
}

