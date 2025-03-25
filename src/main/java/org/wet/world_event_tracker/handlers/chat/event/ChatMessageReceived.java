package org.wet.world_event_tracker.handlers.chat.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.text.Text;

public interface ChatMessageReceived {
    Event<ChatMessageReceived> EVENT = EventFactory.createArrayBacked(ChatMessageReceived.class, (listeners) -> (message) -> {
        for (ChatMessageReceived listener : listeners) {
            listener.interact(message);
        }
    });

    void interact(Text message);
}
