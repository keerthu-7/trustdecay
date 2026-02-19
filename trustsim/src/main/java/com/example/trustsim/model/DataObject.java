package com.example.trustsim.model;

import com.example.trustsim.MathUtil;

public final class DataObject {
    public final int id;
    public final Sensitivity sensitivity;

    public double trust; // 0..1
    public Tier tier;
    public boolean anonymized;

    public final double baseBusinessValue; // 0..1
    public final int createdAt;
    public int lastAccessTime;
    public int totalAccessCountAllTime;

    public final AccessStats accessStats;
    public final RiskStats riskStats;

    public final boolean keepLabelGroundTruth;
    public int deletedAtTime; // -1 if not deleted

    // metrics helpers
    public int trustConvergenceTime = -1;
    private final double[] lastTrusts = new double[10];
    private int trustHistSize = 0;
    private int trustHistPos = 0;

    public DataObject(
        final int id,
        final Sensitivity sensitivity,
        final double initialTrust,
        final Tier tier,
        final boolean anonymized,
        final double baseBusinessValue,
        final int createdAt,
        final int lastAccessTime,
        final boolean keepLabelGroundTruth
    ) {
        this.id = id;
        this.sensitivity = sensitivity;
        this.trust = MathUtil.clamp(initialTrust, 0.0, 1.0);
        this.tier = tier;
        this.anonymized = anonymized;
        this.baseBusinessValue = MathUtil.clamp(baseBusinessValue, 0.0, 1.0);
        this.createdAt = createdAt;
        this.lastAccessTime = lastAccessTime;
        this.accessStats = new AccessStats();
        this.riskStats = new RiskStats();
        this.keepLabelGroundTruth = keepLabelGroundTruth;
        this.deletedAtTime = -1;
        this.totalAccessCountAllTime = 0;
    }

    public boolean isDeleted() {
        return tier == Tier.DELETED;
    }

    public void markDeleted(final int now) {
        this.tier = Tier.DELETED;
        this.deletedAtTime = now;
    }

    public void updateTrustConvergence(final int now) {
        lastTrusts[trustHistPos] = trust;
        trustHistPos = (trustHistPos + 1) % lastTrusts.length;
        if (trustHistSize < lastTrusts.length) trustHistSize++;

        if (trustConvergenceTime >= 0) return;
        if (trustHistSize < lastTrusts.length) return;

        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < lastTrusts.length; i++) {
            min = Math.min(min, lastTrusts[i]);
            max = Math.max(max, lastTrusts[i]);
        }
        if (max - min <= 0.04) { // ±0.02 stability band
            trustConvergenceTime = now;
        }
    }
}

