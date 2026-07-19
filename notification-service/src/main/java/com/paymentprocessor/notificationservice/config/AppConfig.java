package com.paymentprocessor.notificationservice.config;

import com.paymentprocessor.notificationservice.security.ApiKeyAuthFilter;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@EnableScheduling
@ConfigurationPropertiesScan("com.paymentprocessor.notificationservice.config")
public class AppConfig {

    /** Shared HTTP client used for webhook delivery attempts. */
    @Bean
    public HttpClient webhookHttpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    /** Backs both the declarative @Scheduled jobs (RetentionService) and DispatcherScheduler's programmatic poll. */
    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("notification-scheduler-");
        scheduler.initialize();
        return scheduler;
    }

    @Bean
    public FilterRegistrationBean<ApiKeyAuthFilter> apiKeyAuthFilter(SecurityProperties securityProperties) {
        FilterRegistrationBean<ApiKeyAuthFilter> registration =
                new FilterRegistrationBean<>(new ApiKeyAuthFilter(securityProperties));
        registration.addUrlPatterns("/api/*");
        registration.setOrder(1);
        return registration;
    }
}
