package com.example.trustsim.metrics;

import com.example.trustsim.model.DataObject;
import com.example.trustsim.model.Sensitivity;
import com.example.trustsim.model.Tier;

import java.util.List;

public final class MetricsComputer {
    private final int numObjects;
    private final int duration;

    private double baselineStorageCost;
    private double actualStorageCost;

    private double privacyRiskExposure;
    private long complianceViolationIncidents;

    public MetricsComputer(final int numObjects, final int duration) {
        this.numObjects = numObjects;
        this.duration = duration;
    }

    public void onTickAfterDecision(final List<DataObject> objects) {
        // baseline: all objects in HOT tier for all ticks (even if we'd delete them)
        baselineStorageCost += numObjects * 1.0;

        for (final DataObject obj : objects) {
            actualStorageCost += tierCost(obj.tier);

            if (obj.sensitivity != Sensitivity.NON_SENSITIVE && obj.tier != Tier.DELETED) {
                privacyRiskExposure += obj.riskStats.risk;
            }

            if (obj.riskStats.highRiskFlag && obj.tier == Tier.HOT && !obj.anonymized) {
                complianceViolationIncidents++;
            }
        }
    }

    private static double tierCost(final Tier tier) {
        switch (tier) {
            case HOT:
                return 1.0;
            case COLD:
                return 0.2;
            case DELETED:
                return 0.0;
            default:
                return 1.0;
        }
    }

    public Summary summarize(final List<DataObject> objects) {
        long keepTrue = 0;
        long keepTrueDeleted = 0;
        long keepFalse = 0;
        long keepFalseRetainedHot = 0;
        long keepFalseArchivedOrDeleted = 0;

        long convergedCount = 0;
        long convergedTimeSum = 0;

        for (final DataObject obj : objects) {
            if (obj.keepLabelGroundTruth) {
                keepTrue++;
                if (obj.tier == Tier.DELETED) keepTrueDeleted++;
            } else {
                keepFalse++;
                if (obj.tier == Tier.HOT) keepFalseRetainedHot++;
                if (obj.tier == Tier.COLD || obj.tier == Tier.DELETED) keepFalseArchivedOrDeleted++;
            }

            if (obj.trustConvergenceTime >= 0) {
                convergedCount++;
                convergedTimeSum += obj.trustConvergenceTime;
            }
        }

        final double storageCostReduction = baselineStorageCost <= 0
            ? 0.0
            : Math.max(0.0, 1.0 - (actualStorageCost / baselineStorageCost));

        final double falseDeletionRate = keepTrue <= 0 ? 0.0 : ((double) keepTrueDeleted) / keepTrue;
        final double retentionEfficiency = keepFalse <= 0 ? 0.0 : ((double) keepFalseArchivedOrDeleted) / keepFalse;

        final double avgConvergenceTime = convergedCount <= 0 ? -1.0 : ((double) convergedTimeSum) / convergedCount;

        return new Summary(
            storageCostReduction,
            privacyRiskExposure,
            complianceViolationIncidents,
            avgConvergenceTime,
            falseDeletionRate,
            retentionEfficiency,
            (int) convergedCount,
            duration
        );
    }

    public static final class Summary {
        public final double storageCostReduction;
        public final double privacyRiskExposure;
        public final long complianceViolationIncidents;
        public final double avgTrustConvergenceTime;
        public final double falseDeletionRate;
        public final double retentionEfficiency;
        public final int convergedObjects;
        public final int duration;

        public Summary(
            final double storageCostReduction,
            final double privacyRiskExposure,
            final long complianceViolationIncidents,
            final double avgTrustConvergenceTime,
            final double falseDeletionRate,
            final double retentionEfficiency,
            final int convergedObjects,
            final int duration
        ) {
            this.storageCostReduction = storageCostReduction;
            this.privacyRiskExposure = privacyRiskExposure;
            this.complianceViolationIncidents = complianceViolationIncidents;
            this.avgTrustConvergenceTime = avgTrustConvergenceTime;
            this.falseDeletionRate = falseDeletionRate;
            this.retentionEfficiency = retentionEfficiency;
            this.convergedObjects = convergedObjects;
            this.duration = duration;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append("Simulation Metrics Summary").append('\n');
            sb.append(" - Duration (ticks): ").append(duration).append('\n');
            sb.append(" - Storage cost reduction: ").append(String.format(java.util.Locale.ROOT, "%.2f%%", 100.0 * storageCostReduction)).append('\n');
            sb.append(" - Privacy risk exposure (sum): ").append(String.format(java.util.Locale.ROOT, "%.2f", privacyRiskExposure)).append('\n');
            sb.append(" - Compliance violation incidents: ").append(complianceViolationIncidents).append('\n');
            sb.append(" - Trust convergence (avg tick, -1 if none): ").append(String.format(java.util.Locale.ROOT, "%.2f", avgTrustConvergenceTime)).append('\n');
            sb.append(" - Trust converged objects: ").append(convergedObjects).append('\n');
            sb.append(" - False deletion rate: ").append(String.format(java.util.Locale.ROOT, "%.4f", falseDeletionRate)).append('\n');
            sb.append(" - Retention efficiency: ").append(String.format(java.util.Locale.ROOT, "%.4f", retentionEfficiency)).append('\n');
            return sb.toString();
        }
    }
}

