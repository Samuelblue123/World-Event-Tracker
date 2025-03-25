package org.wet.world_event_tracker.handlers.server;

import org.json.JSONObject;
import org.wet.world_event_tracker.World_event_tracker;
import org.wet.world_event_tracker.components.Handler;
import org.wet.world_event_tracker.components.Managers;
import org.wet.world_event_tracker.components.Models;
import org.wet.world_event_tracker.handlers.server.event.S2CServerEvents;
import org.wet.world_event_tracker.net.SocketIOClient;

public class ServerMessageHandler extends Handler {

    @Override
    public void init() {
        SocketIOClient socketIOClient = Managers.Net.socket;
        socketIOClient.addServerListener("serverMessage", this::onServerMessage);
    }

    private void onServerMessage(Object[] args) {

                World_event_tracker.LOGGER.info("received discord {}", args[0].toString());
                S2CServerEvents.MESSAGE.invoker().interact(args[0]);

    }
}