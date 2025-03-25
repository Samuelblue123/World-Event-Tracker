package org.wet.world_event_tracker.net.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import org.wet.world_event_tracker.World_event_tracker;
import org.wet.world_event_tracker.net.type.Api;

public interface NetEvents {
    Event<NetEvents> LOADED = EventFactory.createArrayBacked(NetEvents.class, (listeners) -> (api) -> {
        for (NetEvents listener : listeners) {
            listener.interact(api);
        }
    });
    Event<NetEvents> DISABLED = EventFactory.createArrayBacked(NetEvents.class, (listeners) -> (api) -> {
        for (NetEvents listener : listeners) {
            listener.interact(api);
        }
    });

    void interact(Api api);

}
