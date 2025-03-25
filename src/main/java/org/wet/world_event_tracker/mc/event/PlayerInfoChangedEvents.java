package org.wet.world_event_tracker.mc.event;


import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.text.Text;
import org.wet.world_event_tracker.World_event_tracker;

import java.util.UUID;

public class PlayerInfoChangedEvents {
    public static final Event<Footer> FOOTER = EventFactory.createArrayBacked(Footer.class, (listeners) -> (footer) -> {
        for (Footer listener : listeners) {
            listener.footerChanged(footer);
        }
    });
    public static final Event<Display> DISPLAY = EventFactory.createArrayBacked(Display.class, (listeners) -> (uuid, display) -> {
        for (Display listener : listeners) {
            listener.displayChanged(uuid, display);
        }
    });

    public interface Footer {
        void footerChanged(Text footer);
    }

    public interface Display {
        void displayChanged(UUID uuid, Text display);
    }
}
