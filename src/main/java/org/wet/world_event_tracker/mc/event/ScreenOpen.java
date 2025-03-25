package org.wet.world_event_tracker.mc.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.text.Text;


public interface ScreenOpen {
    Event<ScreenOpen> EVENT = EventFactory.createArrayBacked(ScreenOpen.class, (listeners) -> (type, name) -> {
        for (ScreenOpen listener : listeners) {
            listener.interact(type, name);
        }
    });

    void interact(ScreenHandlerType<?> type, Text name);
}
