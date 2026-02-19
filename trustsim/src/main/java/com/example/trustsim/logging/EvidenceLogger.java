package com.example.trustsim.logging;

import com.example.trustsim.Config;
import com.example.trustsim.model.Action;
import com.example.trustsim.model.DataObject;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class EvidenceLogger implements Closeable {
    private final BufferedWriter out;
    private final boolean logChangedOnly;
    private final Action[] lastActionById;

    public EvidenceLogger(final String path, final int numObjects, final boolean logChangedOnly) throws IOException {
        this.out = new BufferedWriter(new FileWriter(path, StandardCharsets.UTF_8));
        this.logChangedOnly = logChangedOnly;
        this.lastActionById = new Action[numObjects];
        writeHeader();
    }

    public static EvidenceLogger createDefault(final int numObjects) throws IOException {
        return new EvidenceLogger(Config.DEFAULT_CSV_PATH, numObjects, Config.LOG_CHANGED_ONLY);
    }

    private void writeHeader() throws IOException {
        out.write("time,dataId,sensitivity,trust,accessRate,legitRate,suspiciousRate,risk,anomalyScore,predictedRelevance,action,tier,anonymized,reasonCode");
        out.newLine();
        out.flush();
    }

    public void logTick(
        final int time,
        final DataObject obj,
        final double predictedRelevance,
        final Action action,
        final String reasonCode
    ) throws IOException {
        if (logChangedOnly) {
            final Action last = lastActionById[obj.id];
            if (last != null && last == action) {
                return;
            }
            lastActionById[obj.id] = action;
        }

        out.write(Integer.toString(time));
        out.write(',');
        out.write(Integer.toString(obj.id));
        out.write(',');
        out.write(obj.sensitivity.name());
        out.write(',');
        out.write(String.format(java.util.Locale.ROOT, "%.4f", obj.trust));
        out.write(',');
        out.write(String.format(java.util.Locale.ROOT, "%.4f", obj.accessStats.accessRate()));
        out.write(',');
        out.write(String.format(java.util.Locale.ROOT, "%.4f", obj.accessStats.legitRate()));
        out.write(',');
        out.write(String.format(java.util.Locale.ROOT, "%.4f", obj.accessStats.suspiciousRate()));
        out.write(',');
        out.write(String.format(java.util.Locale.ROOT, "%.4f", obj.riskStats.risk));
        out.write(',');
        out.write(String.format(java.util.Locale.ROOT, "%.4f", obj.riskStats.anomalyScore));
        out.write(',');
        out.write(String.format(java.util.Locale.ROOT, "%.4f", predictedRelevance));
        out.write(',');
        out.write(action.name());
        out.write(',');
        out.write(obj.tier.name());
        out.write(',');
        out.write(Boolean.toString(obj.anonymized));
        out.write(',');
        out.write(reasonCode == null ? "" : reasonCode);
        out.newLine();
    }

    public void flush() throws IOException {
        out.flush();
    }

    @Override
    public void close() throws IOException {
        out.flush();
        out.close();
    }
}

