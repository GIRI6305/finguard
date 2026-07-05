package com.finguard.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory sliding-window velocity checker.
 * Tracks how many transactions a card has made in the last 60 seconds
 * using a thread-safe map of timestamp lists -- no external cache needed.
 */
@Service
public class VelocityCheckService {

    private static final long WINDOW_SECONDS = 60;

    private final Map<String, CopyOnWriteArrayList<Instant>> history = new ConcurrentHashMap<>();

    /** Records this transaction's timestamp and returns the count within the last 60 seconds. */
    public long recordAndCount(String cardNumber) {
        Instant now = Instant.now();
        CopyOnWriteArrayList<Instant> timestamps =
                history.computeIfAbsent(cardNumber, k -> new CopyOnWriteArrayList<>());

        timestamps.add(now);
        timestamps.removeIf(t -> t.isBefore(now.minusSeconds(WINDOW_SECONDS)));

        return timestamps.size();
    }
}
