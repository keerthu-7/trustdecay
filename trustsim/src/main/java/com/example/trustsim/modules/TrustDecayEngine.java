package com.example.trustsim.modules;

import com.example.trustsim.Config;
import com.example.trustsim.MathUtil;
import com.example.trustsim.model.DataObject;

public final class TrustDecayEngine {
    public void updateTrust(final DataObject obj, final int now) {
        final int dt = Math.max(0, now - obj.lastAccessTime);
        final double inactivityFactor = Math.min(1.0, ((double) dt) / Config.HALF_LIFE);

        final double accessLegitRate = obj.accessStats.legitRate();
        final double risk = obj.riskStats.risk;
        final double anomaly = obj.riskStats.anomalyScore;

        final double updated = obj.trust
            - Config.DECAY_RATE * inactivityFactor
            + Config.REINFORCEMENT_RATE * accessLegitRate
            - Config.RISK_PENALTY_WEIGHT * risk
            - Config.ANOMALY_PENALTY_WEIGHT * anomaly;

        obj.trust = MathUtil.clamp(updated, 0.0, 1.0);
    }
}

