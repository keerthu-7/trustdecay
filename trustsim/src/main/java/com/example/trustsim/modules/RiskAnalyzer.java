package com.example.trustsim.modules;

import com.example.trustsim.MathUtil;
import com.example.trustsim.model.DataObject;
import com.example.trustsim.model.Sensitivity;

public final class RiskAnalyzer {
    public void updateRisk(final DataObject obj, final int now) {
        final double baseRisk = baseRisk(obj.sensitivity);
        final double suspiciousRate = obj.accessStats.suspiciousRate();
        final boolean burst = obj.accessStats.burstDetected(now);

        double addOns = 0.0;
        if (suspiciousRate > 0.2) addOns += 0.15;
        if (burst) addOns += 0.15;

        final double burstFlag = burst ? 1.0 : 0.0;
        final double anomalyScore = MathUtil.clamp(0.5 * suspiciousRate + 0.5 * burstFlag, 0.0, 1.0);

        final double risk = MathUtil.clamp(baseRisk + addOns, 0.0, 1.0);
        obj.riskStats.anomalyScore = anomalyScore;
        obj.riskStats.risk = risk;
        obj.riskStats.highRiskFlag = risk >= 0.7;
    }

    private static double baseRisk(final Sensitivity s) {
        switch (s) {
            case NON_SENSITIVE:
                return 0.1;
            case PII:
                return 0.6;
            case FINANCIAL:
                return 0.7;
            case HEALTH:
                return 0.8;
            default:
                return 0.5;
        }
    }
}

