package org.wet.world_event_tracker.handlers.server.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import org.json.JSONObject;

public class S2CServerEvents {
    public static Event<Message> MESSAGE = EventFactory.createArrayBacked(Message.class, (listeners) -> (message) -> {
        for (Message listener : listeners) {
            String[] splitMessage = message.toString().split(":");
            listener.interact("The "+splitMessage[0]+" World Event is starting soon ("+splitMessage[1]+")!");
        }
    });

    public interface Message {
        void interact(Object message);
    }
}