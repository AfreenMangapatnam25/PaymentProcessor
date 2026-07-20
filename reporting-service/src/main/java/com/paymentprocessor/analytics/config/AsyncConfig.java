package com.paymentprocessor.analytics.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/** Bounded pool for CPU/IO-heavy report generation; scheduling for the dispatcher. */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

    public static final String REPORT_EXECUTOR = "reportExecutor";

    @Bean(name = REPORT_EXECUTOR)
    public Executor reportExecutor(AnalyticsProperties props) {
        AnalyticsProperties.Reports.Executor cfg = props.getReports().getExecutor();
        ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
        exec.setCorePoolSize(cfg.getCorePoolSize());
        exec.setMaxPoolSize(cfg.getMaxPoolSize());
        exec.setQueueCapacity(cfg.getQueueCapacity());
        exec.setThreadNamePrefix("report-gen-");
        // Back-pressure: when saturated, run on the caller instead of dropping work.
        exec.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        exec.setWaitForTasksToCompleteOnShutdown(true);
        exec.setAwaitTerminationSeconds(60);
        exec.initialize();
        return exec;
    }
}
