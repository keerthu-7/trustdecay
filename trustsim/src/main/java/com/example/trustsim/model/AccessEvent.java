package com.example.trustsim.model;

public final class AccessEvent {
    public final int time;
    public final int dataId;
    public final Role role;
    public final boolean legitimate;
    public final double requestScore; // 0..1

    public AccessEvent(final int time, final int dataId, final Role role, final boolean legitimate, final double requestScore) {
        this.time = time;
        this.dataId = dataId;
        this.role = role;
        this.legitimate = legitimate;
        this.requestScore = requestScore;
    }
}

