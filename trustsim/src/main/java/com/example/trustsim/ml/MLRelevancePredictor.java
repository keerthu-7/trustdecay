package com.example.trustsim.ml;

import com.example.trustsim.Config;
import com.example.trustsim.MathUtil;
import com.example.trustsim.model.DataObject;
import com.example.trustsim.model.Sensitivity;

import java.util.Random;

/**
 * Manual Logistic Regression (no external ML libs).
 *
 * Features:
 *  [1,
 *   businessValue,
 *   accessRate, legitRate, suspiciousRate,
 *   trust,
 *   sensitivityNumeric,
 *   anomalyScore, risk]
 *
 * Predicts "future relevant" probability.
 */
public final class MLRelevancePredictor {
    private static final int DIM = 9;
    private final double[] w = new double[DIM];

    public MLRelevancePredictor() {
        // small random init
        final Random r = new Random(42);
        for (int i = 0; i < w.length; i++) {
            w[i] = (r.nextDouble() - 0.5) * 0.02;
        }
    }

    public void trainSynthetic() {
        final Random rnd = new Random(123);
        final int n = Config.ML_TRAIN_SAMPLES;
        final double lr = Config.ML_LEARNING_RATE;
        final double l2 = Config.ML_L2;
        final double maxStep = 0.1; // clip per-weight update magnitude

        final double[][] x = new double[n][DIM];
        final int[] y = new int[n];

        for (int i = 0; i < n; i++) {
            final double businessValue = rnd.nextDouble(); // 0..1

            // access patterns: mixture for variety
            final double mix = rnd.nextDouble();
            final double accessRate = mix < 0.2 ? 0.6 + 0.4 * rnd.nextDouble()
                : mix < 0.7 ? 0.2 + 0.5 * rnd.nextDouble()
                : 0.0 + 0.25 * rnd.nextDouble();

            final double legitRate = MathUtil.clamp(accessRate * (0.6 + 0.4 * rnd.nextDouble()), 0.0, 1.0);
            final double suspiciousRate = MathUtil.clamp(accessRate - legitRate + 0.2 * rnd.nextDouble() * (1.0 - legitRate), 0.0, 1.0);
            final double trust = MathUtil.clamp(0.5 + 0.4 * legitRate - 0.6 * suspiciousRate + 0.1 * (rnd.nextDouble() - 0.5), 0.0, 1.0);

            final Sensitivity s = sampleSensitivity(rnd);
            final double sensitivityNumeric = sensitivityNumeric(s);

            final double baseRisk = baseRisk(s);
            final double burstFlag = rnd.nextDouble() < (suspiciousRate > 0.25 ? 0.25 : 0.05) ? 1.0 : 0.0;
            final double anomalyScore = MathUtil.clamp(0.5 * suspiciousRate + 0.5 * burstFlag, 0.0, 1.0);
            double risk = baseRisk;
            if (suspiciousRate > 0.2) risk += 0.15;
            if (burstFlag > 0.5) risk += 0.15;
            risk = MathUtil.clamp(risk, 0.0, 1.0);

            x[i][0] = 1.0;
            x[i][1] = businessValue;
            x[i][2] = accessRate;
            x[i][3] = legitRate;
            x[i][4] = suspiciousRate;
            x[i][5] = trust;
            x[i][6] = sensitivityNumeric;
            x[i][7] = anomalyScore;
            x[i][8] = risk;

            // normalize non-bias features explicitly
            normalizeFeatures(x[i]);

            final boolean hotLike = accessRate > 0.55;
            boolean relevant = (businessValue > 0.65) || (accessRate > 0.50) || hotLike;
            if (risk > 0.85 && suspiciousRate > 0.35) relevant = false;
            y[i] = relevant ? 1 : 0;
        }

        for (int epoch = 0; epoch < Config.ML_EPOCHS; epoch++) {
            final double[] grad = new double[DIM];
            for (int i = 0; i < n; i++) {
                final double p = predictFromFeatures(x[i]);
                final double err = (p - y[i]); // derivative of logloss
                for (int j = 0; j < DIM; j++) {
                    grad[j] += err * x[i][j];
                }
            }
            for (int j = 0; j < DIM; j++) {
                grad[j] = grad[j] / n + l2 * w[j];
                double delta = lr * grad[j];
                if (delta > maxStep) delta = maxStep;
                else if (delta < -maxStep) delta = -maxStep;
                w[j] -= delta;
            }
        }
    }

    public double predict(final DataObject obj) {
        final double[] f = features(obj);
        return predictFromFeatures(f);
    }

    private double predictFromFeatures(final double[] f) {
        double z = 0.0;
        for (int j = 0; j < DIM; j++) z += w[j] * f[j];
        final double p = MathUtil.sigmoid(z);
        // avoid extreme 0 or 1 probabilities
        return MathUtil.clamp(p, 0.01, 0.99);
    }

    private static double[] features(final DataObject obj) {
        final double[] f = new double[DIM];
        f[0] = 1.0;
        f[1] = obj.baseBusinessValue;
        f[2] = obj.accessStats.accessRate();
        f[3] = obj.accessStats.legitRate();
        f[4] = obj.accessStats.suspiciousRate();
        f[5] = obj.trust;
        f[6] = sensitivityNumeric(obj.sensitivity);
        f[7] = obj.riskStats.anomalyScore;
        f[8] = obj.riskStats.risk;
        normalizeFeatures(f);
        return f;
    }

    /**
     * Explicit feature normalization (in-place) for indices 1..8.
     * Current synthetic features are already in [0,1], so we simply
     * shift/scale them to [-1,1] to improve conditioning.
     */
    private static void normalizeFeatures(final double[] f) {
        for (int j = 1; j < DIM; j++) {
            final double v = f[j];
            // map [0,1] -> [-1,1]
            f[j] = (v - 0.5) * 2.0;
        }
    }

    private static Sensitivity sampleSensitivity(final Random rnd) {
        final double u = rnd.nextDouble();
        if (u < 0.55) return Sensitivity.NON_SENSITIVE;
        if (u < 0.80) return Sensitivity.PII;
        if (u < 0.92) return Sensitivity.FINANCIAL;
        return Sensitivity.HEALTH;
    }

    private static double sensitivityNumeric(final Sensitivity s) {
        switch (s) {
            case NON_SENSITIVE:
                return 0.0;
            case PII:
                return 0.4;
            case FINANCIAL:
                return 0.7;
            case HEALTH:
                return 1.0;
            default:
                return 0.5;
        }
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

