package org.wet.world_event_tracker.mc.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.text.Text;


public interface WynnChatMessage {
    Event<WynnChatMessage> EVENT = EventFactory.createArrayBacked(WynnChatMessage.class, (listeners) -> (message) -> {
        for (WynnChatMessage listener : listeners) {
            listener.interact(message);
        }
    });

    void interact(Text message);
}