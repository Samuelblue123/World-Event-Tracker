package org.wet.world_event_tracker.components;

import org.wet.world_event_tracker.consumers.FeatureManager;
import org.wet.world_event_tracker.mod.ConnectionManager;
import org.wet.world_event_tracker.net.NetManager;

public final class Managers {
    public static final ConnectionManager Connection = new ConnectionManager();
    public static final NetManager Net = new NetManager();

    public static final FeatureManager Feature = new FeatureManager();
}