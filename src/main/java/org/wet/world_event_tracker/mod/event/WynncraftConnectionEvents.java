package org.wet.world_event_tracker.mod.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public interface WynncraftConnectionEvents {
    Event<WynncraftConnectionEvents> JOIN = EventFactory.createArrayBacked(WynncraftConnectionEvents.class,
            (listeners) -> () -> {
                for (WynncraftConnectionEvents listener : listeners) {
                    listener.interact();
                }
            });
    Event<WynncraftConnectionEvents> LEAVE = EventFactory.createArrayBacked(WynncraftConnectionEvents.class,
            (listeners) -> () -> {
                for (WynncraftConnectionEvents listener : listeners) {
                    listener.interact();
                }
            });
    Event<WynncraftConnectionEvents> CHANGE = EventFactory.createArrayBacked(WynncraftConnectionEvents.class,
            (listeners) -> () -> {
                for (WynncraftConnectionEvents listener : listeners) {
                    listener.interact();
                }
            });

    void interact();
}
