package org.wet.world_event_tracker.models.worldState.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import org.wet.world_event_tracker.models.worldState.type.WorldState;

public interface WorldStateEvents {
    Event<WorldStateEvents> CHANGE = EventFactory.createArrayBacked(WorldStateEvents.class, (listeners) -> (newState) -> {
        for (WorldStateEvents listener : listeners) {
            listener.changed(newState);
        }
    });

    void changed(WorldState newState);
}
