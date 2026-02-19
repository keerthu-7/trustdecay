package com.example.trustsim.modules;

import com.example.trustsim.model.AccessEvent;
import com.example.trustsim.model.DataObject;
import com.example.trustsim.model.Role;
import com.example.trustsim.model.Sensitivity;

/**
 * Updates last access time and sliding window stats; marks suspicious based on
 * role-sensitivity legitimacy, requestScore threshold, and burst behavior (via AccessStats).
 */
public final class DataAccessMonitoringModule {
    // conservative threshold: low-scored requests are suspicious even if role allowed
    private final double requestScoreThreshold;

    public DataAccessMonitoringModule(final double requestScoreThreshold) {
        this.requestScoreThreshold = requestScoreThreshold;
    }

    public MonitoringResult onAccess(final DataObject obj, final AccessEvent event) {
        obj.lastAccessTime = event.time;
        obj.totalAccessCountAllTime++;

        final boolean allowedByRole = isRoleLegitimate(event.role, obj);
        final boolean scoreOk = event.requestScore >= requestScoreThreshold;
        final boolean legit = allowedByRole && scoreOk;
        final boolean suspicious = !legit;

        obj.accessStats.add(event.time, legit, suspicious);

        final boolean burst = obj.accessStats.burstDetected(event.time);
        return new MonitoringResult(legit, suspicious, burst);
    }

    public boolean isRoleLegitimate(final Role role, final DataObject obj) {
        final Sensitivity s = obj.sensitivity;
        switch (role) {
            case Admin:
                return true;
            case Analyst:
                return s == Sensitivity.NON_SENSITIVE || obj.anonymized;
            case User:
                return s == Sensitivity.NON_SENSITIVE;
            case Service:
                // For Service, the requestScore check is applied separately.
                return s == Sensitivity.NON_SENSITIVE || s == Sensitivity.PII;
            default:
                return false;
        }
    }

    public static final class MonitoringResult {
        public final boolean legitimate;
        public final boolean suspicious;
        public final boolean burstDetected;

        public MonitoringResult(final boolean legitimate, final boolean suspicious, final boolean burstDetected) {
            this.legitimate = legitimate;
            this.suspicious = suspicious;
            this.burstDetected = burstDetected;
        }
    }
}

