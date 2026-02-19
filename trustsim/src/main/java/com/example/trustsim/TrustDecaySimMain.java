package com.example.trustsim;

import com.example.trustsim.logging.EvidenceLogger;
import com.example.trustsim.metrics.MetricsComputer;
import com.example.trustsim.ml.MLRelevancePredictor;
import com.example.trustsim.model.DataObject;
import com.example.trustsim.model.Sensitivity;
import com.example.trustsim.model.Tier;
import com.example.trustsim.modules.DataAccessMonitoringModule;
import com.example.trustsim.modules.RetentionDecisionController;
import com.example.trustsim.modules.RiskAnalyzer;
import com.example.trustsim.modules.TrustDecayEngine;
import com.example.trustsim.sim.TrustSimControllerEntity;
import com.example.trustsim.workload.WorkloadGenerator;
import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.datacenters.DatacenterSimple;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.hosts.HostSimple;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.resources.PeSimple;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.vms.VmSimple;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Main entrypoint for "Trust-Decaying Data Retention Management in Cloud Applications using ML".
 */
public final class TrustDecaySimMain {
    public static void main(final String[] args) throws IOException {
        // Reduce CloudSim Plus log noise (slf4j-simple)
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "warn");

        final int numObjects = Config.NUM_OBJECTS;
        final int duration = Config.SIM_DURATION;

        // CloudSim simulation
        final CloudSim simulation = new CloudSim();
        createMinimalDatacenter(simulation);
        simulation.terminateAt(duration);

        // Minimal broker/VM to satisfy CloudSim structure
        final DatacenterBrokerSimple broker = new DatacenterBrokerSimple(simulation);
        final Vm vm = new VmSimple(1000, 1)
            .setRam(1024).setBw(1000).setSize(10_000);
        broker.submitVm(vm);

        // Create data objects
        final Random rnd = new Random(7);
        final WorkloadGenerator.Profile[] profiles = WorkloadGenerator.assignProfiles(rnd, numObjects);
        final List<DataObject> objects = createObjects(numObjects, rnd, profiles);

        // Train ML model before sim starts
        final MLRelevancePredictor ml = new MLRelevancePredictor();
        ml.trainSynthetic();

        // Generate workload
        final WorkloadGenerator workload = new WorkloadGenerator(new Random(99), duration, objects, profiles);
        final var accessEventsByTime = workload.generate();

        // Modules
        final DataAccessMonitoringModule monitoring = new DataAccessMonitoringModule(0.55);
        final RiskAnalyzer riskAnalyzer = new RiskAnalyzer();
        final TrustDecayEngine trustEngine = new TrustDecayEngine();
        final RetentionDecisionController decisionController = new RetentionDecisionController();
        final EvidenceLogger logger = EvidenceLogger.createDefault(numObjects);
        // Metrics use the effective evaluation window (post-grace)
        final MetricsComputer metrics = new MetricsComputer(numObjects, duration - Config.GRACE_PERIOD);

        // Controller entity: schedules evaluation tick every 1 time unit
        new TrustSimControllerEntity(
            simulation,
            duration,
            objects,
            accessEventsByTime,
            monitoring,
            riskAnalyzer,
            trustEngine,
            ml,
            decisionController,
            logger,
            metrics
        );

        simulation.start();

        final MetricsComputer.Summary summary = metrics.summarize(objects);
        System.out.println(summary);
        System.out.println("CSV audit trail written to: " + Config.DEFAULT_CSV_PATH);
    }

    private static Datacenter createMinimalDatacenter(final CloudSim simulation) {
        final List<Pe> peList = List.of(new PeSimple(1000));
        final Host host = new HostSimple(8192, 10_000, 1_000_000, peList);
        final List<Host> hosts = List.of(host);
        return new DatacenterSimple(simulation, hosts);
    }

    private static List<DataObject> createObjects(
        final int numObjects,
        final Random rnd,
        final WorkloadGenerator.Profile[] profiles
    ) {
        final List<DataObject> list = new ArrayList<>(numObjects);
        for (int id = 0; id < numObjects; id++) {
            final Sensitivity sensitivity = sampleSensitivity(rnd);
            final double businessValue = rnd.nextDouble();

            final boolean hotLike = profiles[id] == WorkloadGenerator.Profile.HOT;
            final boolean keepLabelGroundTruth = (businessValue > 0.60) || hotLike;

            final double initialTrust = MathUtil.clamp(0.55 + 0.25 * rnd.nextDouble(), 0.0, 1.0);
            final DataObject obj = new DataObject(
                id,
                sensitivity,
                initialTrust,
                Tier.HOT,
                false,
                businessValue,
                0,
                0,
                keepLabelGroundTruth
            );
            list.add(obj);
        }
        return list;
    }

    private static Sensitivity sampleSensitivity(final Random rnd) {
        final double u = rnd.nextDouble();
        if (u < 0.55) return Sensitivity.NON_SENSITIVE;
        if (u < 0.80) return Sensitivity.PII;
        if (u < 0.92) return Sensitivity.FINANCIAL;
        return Sensitivity.HEALTH;
    }
}

