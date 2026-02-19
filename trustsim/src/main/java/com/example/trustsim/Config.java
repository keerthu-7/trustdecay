package com.example.trustsim;

/**
 * Centralized configuration constants for the trust-decay retention simulation.
 */
public final class Config {
    private Config() {}

    // AccessStats sliding window
    public static final int ACCESS_WINDOW_W = 20;

    // Grace period where objects cannot be deleted/archived
    public static final int GRACE_PERIOD = 5;

    // Cold-start window during which never-accessed objects are held in HOT
    public static final int COLD_START_WINDOW = 20;

    // Trust decay parameters
    public static final int HALF_LIFE = 30;
    public static final double DECAY_RATE = 0.03;
    public static final double REINFORCEMENT_RATE = 0.10;
    public static final double RISK_PENALTY_WEIGHT = 0.12;
    public static final double ANOMALY_PENALTY_WEIGHT = 0.20;

    // Retention decision thresholds
    public static final double T_HIGH = 0.75;
    public static final double T_MID = 0.40;
    public static final double R_HIGH = 0.70;
    public static final double R_MID = 0.50;
    public static final double P_LOW = 0.30;
    public static final double P_MID = 0.50;

    // Workload generation defaults
    public static final double HOT_FRACTION = 0.15;
    public static final double WARM_FRACTION = 0.25;
    public static final double COLD_FRACTION = 0.60;

    // ML training
    public static final int ML_TRAIN_SAMPLES = 50_000;
    public static final int ML_EPOCHS = 200;
    public static final double ML_LEARNING_RATE = 0.01;
    public static final double ML_L2 = 1e-4;

    // Logging
    public static final boolean LOG_CHANGED_ONLY = false;
    public static final String DEFAULT_CSV_PATH = "trustsim_audit.csv";

    // Simulation
    public static final int NUM_OBJECTS = 10_000;
    public static final int SIM_DURATION = 300;
    public static final int TICK_INTERVAL = 1;
}

