package com.example.trustsim.model;

public final class RiskStats {
    public double anomalyScore; // 0..1
    public double risk;         // 0..1
    public boolean highRiskFlag;

    public RiskStats() {
        this.anomalyScore = 0.0;
        this.risk = 0.0;
        this.highRiskFlag = false;
    }
}

