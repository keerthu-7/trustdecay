package com.example.trustsim;

public final class MathUtil {
    private MathUtil() {}

    public static double clamp(final double v, final double min, final double max) {
        if (v < min) return min;
        if (v > max) return max;
        return v;
    }

    public static double sigmoid(final double z) {
        // numerically stable sigmoid
        if (z >= 0) {
            final double ez = Math.exp(-z);
            return 1.0 / (1.0 + ez);
        } else {
            final double ez = Math.exp(z);
            return ez / (1.0 + ez);
        }
    }
}

