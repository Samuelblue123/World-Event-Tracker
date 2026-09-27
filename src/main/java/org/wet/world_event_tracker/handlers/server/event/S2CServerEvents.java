package org.wet.world_event_tracker.handlers.server.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.text.Text;
import org.wet.world_event_tracker.World_event_tracker;
import org.wet.world_event_tracker.utils.FileUtils;
import org.wet.world_event_tracker.utils.McUtils;
import org.wet.world_event_tracker.utils.type.Prepend;

import java.io.IOException;

public class S2CServerEvents {
    private static final FileUtils fileUtils = new FileUtils();
    public static Event<Message> MESSAGE = EventFactory.createArrayBacked(Message.class, (listeners) -> (message) -> {
        for (Message listener : listeners) {
            try {
                String events = fileUtils.readFile(World_event_tracker.configFile);
                String[] splitMessage = message.toString().split(":");
                String[] messageParts = splitMessage[0].split(" ");
                String event = "";
                if (splitMessage[1].toString().contains("§")){
                        splitMessage[1] = splitMessage[1].toString().replace("§0§0", " ");
                }
                for (String part : messageParts) {
                    event += part;
                }
                if (events.contains(event)) {
                    listener.interact("The "+splitMessage[0]+" World Event is starting soon ("+splitMessage[1]+")!");

                }
            } catch (IOException e) {
                World_event_tracker.LOGGER.error("Error with tracked events file, please try deleting the 'tracked.json' in 'config/WET'.");
                McUtils.sendLocalMessage(Text.of("§cError with tracked events file, please try deleting the 'tracked.json' in 'config/WET'."), Prepend.DEFAULT.get(), true);
                throw new RuntimeException(e);
            }

        }
    });

    public interface Message {
        void interact(Object message);
    }
}