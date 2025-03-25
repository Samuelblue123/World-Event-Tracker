package org.wet.world_event_tracker.components;

import org.wet.world_event_tracker.handlers.chat.ChatHandler;
import org.wet.world_event_tracker.handlers.server.ServerMessageHandler;

public final class Handlers {
    public static final ChatHandler Chat = new ChatHandler();
    public static final ServerMessageHandler ServerMessage = new ServerMessageHandler();
    public static void init() {
        Chat.init();
        ServerMessage.init();
    }
}