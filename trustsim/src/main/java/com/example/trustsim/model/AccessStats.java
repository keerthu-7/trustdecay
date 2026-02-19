package com.example.trustsim.model;

import com.example.trustsim.Config;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Sliding window W=20 of recent access events.
 */
public final class AccessStats {
    private static final class Entry {
        final int time;
        final boolean legit;
        final boolean suspicious;

        Entry(final int time, final boolean legit, final boolean suspicious) {
            this.time = time;
            this.legit = legit;
            this.suspicious = suspicious;
        }
    }

    private final int windowSize;
    private final Deque<Entry> window;

    private int totalCount;
    private int legitCount;
    private int suspiciousCount;

    public AccessStats() {
        this(Config.ACCESS_WINDOW_W);
    }

    public AccessStats(final int windowSize) {
        this.windowSize = windowSize;
        this.window = new ArrayDeque<>(windowSize + 1);
        this.totalCount = 0;
        this.legitCount = 0;
        this.suspiciousCount = 0;
    }

    public void add(final int time, final boolean legit, final boolean suspicious) {
        window.addLast(new Entry(time, legit, suspicious));
        totalCount++;
        if (legit) legitCount++;
        if (suspicious) suspiciousCount++;

        while (window.size() > windowSize) {
            final Entry e = window.removeFirst();
            totalCount--;
            if (e.legit) legitCount--;
            if (e.suspicious) suspiciousCount--;
        }
    }

    public int getTotalCount() {
        return totalCount;
    }

    public int getLegitCount() {
        return legitCount;
    }

    public int getSuspiciousCount() {
        return suspiciousCount;
    }

    public double accessRate() {
        return ((double) totalCount) / windowSize;
    }

    public double legitRate() {
        return ((double) legitCount) / windowSize;
    }

    public double suspiciousRate() {
        return ((double) suspiciousCount) / windowSize;
    }

    /**
     * Burst detection: >=5 suspicious within 3 time units.
     */
    public boolean burstDetected(final int now) {
        int cnt = 0;
        for (final Entry e : window) {
            if (!e.suspicious) continue;
            if (now - e.time <= 3) cnt++;
        }
        return cnt >= 5;
    }
}

