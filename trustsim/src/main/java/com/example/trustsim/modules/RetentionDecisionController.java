package com.example.trustsim.modules;

import com.example.trustsim.Config;
import com.example.trustsim.MathUtil;
import com.example.trustsim.model.Action;
import com.example.trustsim.model.DataObject;
import com.example.trustsim.model.Tier;

public final class RetentionDecisionController {
    public Decision decide(final DataObject obj, final double predictedRelevance, final int now) {
        // a) already deleted
        if (obj.tier == Tier.DELETED) {
            return new Decision(Action.DELETE, "already_deleted");
        }

        // b) global grace period: always retain newly created objects
        if (now - obj.createdAt < Config.GRACE_PERIOD) {
            obj.tier = Tier.HOT;
            return new Decision(Action.RETAIN, "grace_period");
        }

        final double trust = obj.trust;
        final double risk = obj.riskStats.risk;

        // c) anonymize but keep value for high-risk/high-relevance
        if (risk >= Config.R_HIGH && predictedRelevance >= Config.P_MID) {
            obj.anonymized = true;
            obj.riskStats.risk = MathUtil.clamp(risk - 0.2, 0.0, 1.0);
            obj.riskStats.highRiskFlag = obj.riskStats.risk >= 0.7;
            return new Decision(Action.ANONYMIZE, "high_risk_keep_value");
        }

        // d) cold-start: never-seen objects stay HOT for a while
        if (obj.totalAccessCountAllTime == 0 && (now - obj.createdAt < Config.COLD_START_WINDOW)) {
            obj.tier = Tier.HOT;
            return new Decision(Action.RETAIN, "cold_start_hold");
        }

        // e) clearly low trust and low business value
        if (trust < Config.T_MID && predictedRelevance < Config.P_LOW) {
            obj.markDeleted(now);
            return new Decision(Action.DELETE, "low_trust_low_value");
        }

        // f) clearly high trust and high predicted value, with moderate risk
        if (trust >= Config.T_HIGH && predictedRelevance >= Config.P_MID && risk < Config.R_MID) {
            obj.tier = Tier.HOT;
            return new Decision(Action.RETAIN, "high_trust_high_value");
        }

        // g) everything else -> archive (COLD)
        obj.tier = Tier.COLD;
        return new Decision(Action.ARCHIVE, "mid_zone");
    }

    public static final class Decision {
        public final Action action;
        public final String reasonCode;

        public Decision(final Action action, final String reasonCode) {
            this.action = action;
            this.reasonCode = reasonCode;
        }
    }
}

