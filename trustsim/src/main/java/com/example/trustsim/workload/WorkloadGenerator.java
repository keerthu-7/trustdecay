package com.example.trustsim.workload;

import com.example.trustsim.Config;
import com.example.trustsim.model.AccessEvent;
import com.example.trustsim.model.DataObject;
import com.example.trustsim.model.Role;
import com.example.trustsim.model.Sensitivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Generates access events across simulation time.
 *
 * - 15% HOT objects: frequent legitimate accesses
 * - 25% WARM: periodic
 * - 60% COLD: rare
 * - Attack bursts: every ~30 time units pick some sensitive objects and generate suspicious burst events
 */
public final class WorkloadGenerator {
    public enum Profile { HOT, WARM, COLD }

    private final Random rnd;
    private final int duration;
    private final List<DataObject> objects;
    private final Profile[] profileById;

    public WorkloadGenerator(final Random rnd, final int duration, final List<DataObject> objects, final Profile[] profileById) {
        this.rnd = rnd;
        this.duration = duration;
        this.objects = objects;
        this.profileById = profileById;
    }

    @SuppressWarnings("unchecked")
    public List<AccessEvent>[] generate() {
        final List<AccessEvent>[] byTime = new List[duration + 1];
        for (int t = 0; t <= duration; t++) byTime[t] = new ArrayList<>();

        // Precompute sensitive IDs for attack selection
        final int[] sensitiveIds = objects.stream()
            .filter(o -> o.sensitivity != Sensitivity.NON_SENSITIVE)
            .mapToInt(o -> o.id)
            .toArray();

        for (int t = 0; t < duration; t++) {
            // baseline workload
            for (final DataObject obj : objects) {
                final Profile p = profileById[obj.id];
                final double u = rnd.nextDouble();
                if (p == Profile.HOT) {
                    if (u < 0.25) byTime[t].add(makeLegitEvent(t, obj));
                } else if (p == Profile.WARM) {
                    if (t % 5 == 0 && u < 0.18) byTime[t].add(makeLegitEvent(t, obj));
                } else {
                    if (u < 0.01) byTime[t].add(makeLegitEvent(t, obj));
                }

                // small background noise of suspicious attempts
                if (rnd.nextDouble() < 0.0015 && obj.sensitivity != Sensitivity.NON_SENSITIVE) {
                    byTime[t].add(makeSuspiciousEvent(t, obj));
                }
            }

            // attack bursts every ~30 time units
            if (t > 0 && t % 30 == 0 && sensitiveIds.length > 0) {
                final int burstTargets = Math.min(40, sensitiveIds.length);
                for (int k = 0; k < burstTargets; k++) {
                    final int id = sensitiveIds[rnd.nextInt(sensitiveIds.length)];
                    final DataObject obj = objects.get(id);
                    // generate >=5 suspicious within 3 time units -> 6 events over t..t+2
                    for (int dt = 0; dt <= 2; dt++) {
                        final int tt = t + dt;
                        if (tt >= duration) continue;
                        byTime[tt].add(makeSuspiciousEvent(tt, obj));
                        byTime[tt].add(makeSuspiciousEvent(tt, obj));
                    }
                }
            }
        }

        return byTime;
    }

    private AccessEvent makeLegitEvent(final int time, final DataObject obj) {
        final Role role = pickLegitRole(obj);
        final double requestScore = 0.70 + 0.30 * rnd.nextDouble();
        final boolean legitimate = isLegitimate(role, obj, requestScore);
        return new AccessEvent(time, obj.id, role, legitimate, requestScore);
    }

    private AccessEvent makeSuspiciousEvent(final int time, final DataObject obj) {
        final Role role = pickSuspiciousRole(obj);
        final double requestScore = 0.05 + 0.45 * rnd.nextDouble();
        final boolean legitimate = isLegitimate(role, obj, requestScore);
        return new AccessEvent(time, obj.id, role, legitimate, requestScore);
    }

    private Role pickLegitRole(final DataObject obj) {
        // ensure role is usually compatible with sensitivity rules
        if (obj.sensitivity == Sensitivity.NON_SENSITIVE) {
            final double u = rnd.nextDouble();
            if (u < 0.55) return Role.User;
            if (u < 0.75) return Role.Analyst;
            if (u < 0.92) return Role.Service;
            return Role.Admin;
        }
        if (obj.sensitivity == Sensitivity.PII) {
            return rnd.nextDouble() < 0.80 ? Role.Service : Role.Admin;
        }
        return Role.Admin;
    }

    private Role pickSuspiciousRole(final DataObject obj) {
        // prefer roles that violate policy for sensitive objects
        if (obj.sensitivity == Sensitivity.NON_SENSITIVE) {
            return rnd.nextDouble() < 0.7 ? Role.Service : Role.User;
        }
        final double u = rnd.nextDouble();
        if (u < 0.50) return Role.User;
        if (u < 0.85) return Role.Analyst;
        return Role.Service;
    }

    private static boolean isLegitimate(final Role role, final DataObject obj, final double requestScore) {
        switch (role) {
            case Admin:
                return true;
            case Analyst:
                return obj.sensitivity == Sensitivity.NON_SENSITIVE || obj.anonymized;
            case User:
                return obj.sensitivity == Sensitivity.NON_SENSITIVE;
            case Service:
                // Service legitimate for NON_SENSITIVE + PII if requestScore high
                return (obj.sensitivity == Sensitivity.NON_SENSITIVE || obj.sensitivity == Sensitivity.PII) && requestScore >= 0.65;
            default:
                return false;
        }
    }

    public static Profile[] assignProfiles(final Random rnd, final int numObjects) {
        final Profile[] p = new Profile[numObjects];
        for (int i = 0; i < numObjects; i++) {
            final double u = rnd.nextDouble();
            if (u < Config.HOT_FRACTION) p[i] = Profile.HOT;
            else if (u < Config.HOT_FRACTION + Config.WARM_FRACTION) p[i] = Profile.WARM;
            else p[i] = Profile.COLD;
        }
        return p;
    }
}

