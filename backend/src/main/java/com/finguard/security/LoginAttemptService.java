package com.finguard.security;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory brute-force guard: after 5 failed logins for a username,
 * that username is locked out for 60 seconds. No external dependency needed --
 * a real production system would use Redis or a proper rate-limiting gateway,
 * but this demonstrates the same principle safely for a single-instance app.
 */
@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCKOUT_SECONDS = 60;

    private static class Attempts {
        int count = 0;
        Instant lockedUntil = null;
    }

    private final ConcurrentHashMap<String, Attempts> attemptsByUsername = new ConcurrentHashMap<>();

    public boolean isLocked(String username) {
        Attempts a = attemptsByUsername.get(username);
        if (a == null || a.lockedUntil == null) {
            return false;
        }
        if (Instant.now().isAfter(a.lockedUntil)) {
            attemptsByUsername.remove(username);
            return false;
        }
        return true;
    }

    public void recordFailure(String username) {
        Attempts a = attemptsByUsername.computeIfAbsent(username, k -> new Attempts());
        a.count++;
        if (a.count >= MAX_ATTEMPTS) {
            a.lockedUntil = Instant.now().plusSeconds(LOCKOUT_SECONDS);
        }
    }

    public void recordSuccess(String username) {
        attemptsByUsername.remove(username);
    }
}
