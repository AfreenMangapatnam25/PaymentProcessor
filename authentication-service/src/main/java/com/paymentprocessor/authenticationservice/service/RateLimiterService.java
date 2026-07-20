package com.paymentprocessor.authenticationservice.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.paymentprocessor.authenticationservice.config.AuthProperties;
import com.paymentprocessor.authenticationservice.exception.TooManyRequestsException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory fixed-window rate limiter. Suitable for a single instance; for a
 * horizontally-scaled deployment back this with a shared store (e.g. Redis).
 */
@Service
public class RateLimiterService {

    private final Cache<String, AtomicInteger> loginBuckets =
            Caffeine.newBuilder().expireAfterWrite(Duration.ofMinutes(1)).build();
    private final Cache<String, AtomicInteger> resetBuckets =
            Caffeine.newBuilder().expireAfterWrite(Duration.ofHours(1)).build();

    private final int loginLimit;
    private final int resetLimit;

    public RateLimiterService(AuthProperties props) {
        this.loginLimit = props.getRatelimit().getLoginAttemptsPerMinute();
        this.resetLimit = props.getRatelimit().getResetRequestsPerHour();
    }

    public void checkLogin(String key) {
        AtomicInteger counter = loginBuckets.get(key, k -> new AtomicInteger(0));
        if (counter.incrementAndGet() > loginLimit) {
            throw new TooManyRequestsException("Too many login attempts, try again shortly");
        }
    }

    public void checkPasswordReset(String key) {
        AtomicInteger counter = resetBuckets.get(key, k -> new AtomicInteger(0));
        if (counter.incrementAndGet() > resetLimit) {
            throw new TooManyRequestsException("Too many reset requests, try again later");
        }
    }
}
