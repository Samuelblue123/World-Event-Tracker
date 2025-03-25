package org.wet.world_event_tracker.consumers;

import org.wet.world_event_tracker.World_event_tracker;
import org.wet.world_event_tracker.components.Feature;
//import org.wet.world_event_tracker.features.AutoUpdateFeature;
import org.wet.world_event_tracker.features.CommandHelpFeature;
import org.wet.world_event_tracker.features.TestCommandHelpFeature;
import org.wet.world_event_tracker.features.server.ServerBridgeFeature;

public class FeatureManager {
//    private static final Map<Feature, FeatureState> FEATURES = new LinkedHashMap<>();

    public void init() {
        registerFeature(new CommandHelpFeature());
        if (World_event_tracker.isTesting()) registerFeature(new TestCommandHelpFeature());
        registerFeature(new ServerBridgeFeature());
//        registerFeature(new AutoUpdateFeature());
    }

    private void registerFeature(Feature feature) {
//        FEATURES.put(feature, FeatureState.ENABLED);
        initializeFeature(feature);
    }

    private void initializeFeature(Feature feature) {
        feature.init();
    }
}
