package com.hcaptcha.sdk.journeylitics;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * In-memory sink that keeps the last 50 events for user journey tracking
 */
public class InMemorySink implements JLSink {
    private static final int MAX_EVENTS = 50;
    private final List<JLEvent> events = new ArrayList<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    @Override
    public void emit(JLEvent event) {
        lock.writeLock().lock();
        try {
            events.add(event);
            // Keep only the last MAX_EVENTS events
            if (events.size() > MAX_EVENTS) {
                events.remove(0);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Gets a snapshot of all current events and clears the list
     * @return List of events (may be empty, never null)
     */
    public List<JLEvent> getAndClearEvents() {
        lock.writeLock().lock();
        try {
            final List<JLEvent> snapshot = new ArrayList<>(events);
            events.clear();
            return snapshot;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Gets a snapshot of all current events without clearing the list.
     * @return List of events (may be empty, never null)
     */
    public List<JLEvent> getEvents() {
        lock.writeLock().lock();
        try {
            return new ArrayList<>(events);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Clears all buffered events.
     */
    public void clearEvents() {
        lock.writeLock().lock();
        try {
            events.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

}
