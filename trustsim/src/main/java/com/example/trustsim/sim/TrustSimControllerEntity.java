package com.example.trustsim.sim;

import com.example.trustsim.Config;
import com.example.trustsim.logging.EvidenceLogger;
import com.example.trustsim.metrics.MetricsComputer;
import com.example.trustsim.ml.MLRelevancePredictor;
import com.example.trustsim.model.AccessEvent;
import com.example.trustsim.model.Action;
import com.example.trustsim.model.DataObject;
import com.example.trustsim.modules.DataAccessMonitoringModule;
import com.example.trustsim.modules.RetentionDecisionController;
import com.example.trustsim.modules.RiskAnalyzer;
import com.example.trustsim.modules.TrustDecayEngine;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimEntity;
import org.cloudbus.cloudsim.core.CloudSimTag;
import org.cloudbus.cloudsim.core.events.SimEvent;

import java.io.IOException;
import java.util.List;

/**
 * CloudSim Plus entity that drives the simulation via periodic tick events.
 */
public final class TrustSimControllerEntity extends CloudSimEntity {
    private enum LocalMsg { TICK }

    private final int duration;
    private final List<DataObject> objects;
    private final List<AccessEvent>[] accessEventsByTime;

    private final DataAccessMonitoringModule monitoring;
    private final RiskAnalyzer riskAnalyzer;
    private final TrustDecayEngine trustEngine;
    private final MLRelevancePredictor ml;
    private final RetentionDecisionController decisionController;
    private final EvidenceLogger logger;
    private final MetricsComputer metrics;

    public TrustSimControllerEntity(
        final CloudSim simulation,
        final int duration,
        final List<DataObject> objects,
        final List<AccessEvent>[] accessEventsByTime,
        final DataAccessMonitoringModule monitoring,
        final RiskAnalyzer riskAnalyzer,
        final TrustDecayEngine trustEngine,
        final MLRelevancePredictor ml,
        final RetentionDecisionController decisionController,
        final EvidenceLogger logger,
        final MetricsComputer metrics
    ) {
        super(simulation);
        this.duration = duration;
        this.objects = objects;
        this.accessEventsByTime = accessEventsByTime;
        this.monitoring = monitoring;
        this.riskAnalyzer = riskAnalyzer;
        this.trustEngine = trustEngine;
        this.ml = ml;
        this.decisionController = decisionController;
        this.logger = logger;
        this.metrics = metrics;
    }

    @Override
    protected void startInternal() {
        // First evaluation tick after grace period
        schedule(Config.GRACE_PERIOD, CloudSimTag.NONE, LocalMsg.TICK);
    }

    @Override
    public void processEvent(final SimEvent evt) {
        if (evt.getTag() == CloudSimTag.SIMULATION_END) {
            try {
                logger.close();
            } catch (final IOException e) {
                throw new RuntimeException("Failed to close CSV logger", e);
            }
            return;
        }

        if (evt.getTag() != CloudSimTag.NONE || evt.getData() != LocalMsg.TICK) return;

        final int now = (int) Math.floor(getSimulation().clock());
        if (now >= duration) return;

        try {
            tick(now);
        } catch (final IOException e) {
            throw new RuntimeException("Failed during tick logging", e);
        }

        if (now + Config.TICK_INTERVAL < duration) {
            schedule(Config.TICK_INTERVAL, CloudSimTag.NONE, LocalMsg.TICK);
        }
    }

    private void tick(final int now) throws IOException {
        // Process access events scheduled for this time.
        final List<AccessEvent> events = (now >= 0 && now < accessEventsByTime.length) ? accessEventsByTime[now] : null;
        if (events != null && !events.isEmpty()) {
            for (final AccessEvent e : events) {
                if (e.dataId < 0 || e.dataId >= objects.size()) continue;
                final DataObject obj = objects.get(e.dataId);
                monitoring.onAccess(obj, e);
            }
        }

        // Evaluate each object once per tick.
        for (final DataObject obj : objects) {
            double predicted = 0.0;
            RetentionDecisionController.Decision d;

            if (!obj.isDeleted()) {
                riskAnalyzer.updateRisk(obj, now);
                trustEngine.updateTrust(obj, now);
                obj.updateTrustConvergence(now);
                predicted = ml.predict(obj);
                d = decisionController.decide(obj, predicted, now);
            } else {
                d = decisionController.decide(obj, 0.0, now);
            }

            final Action action = d.action;
            logger.logTick(now, obj, predicted, action, d.reasonCode);
        }

        metrics.onTickAfterDecision(objects);

        if (now % 10 == 0) logger.flush();
    }
}

