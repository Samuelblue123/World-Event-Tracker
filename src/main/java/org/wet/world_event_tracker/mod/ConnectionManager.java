package org.wet.world_event_tracker.mod;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.wet.world_event_tracker.World_event_tracker;
import org.wet.world_event_tracker.mod.event.WynncraftConnectionEvents;

import java.net.InetSocketAddress;
import java.util.regex.Pattern;

public class ConnectionManager {
    private static final Pattern WYNNCRAFT_SERVER_PATTERN =
            Pattern.compile("^(?:(.*)\\.)?wynncraft\\.(?:com|net|org)\\.?$");
    private boolean isConnected = false;

    public boolean onWynncraft() {
        return isConnected;
    }

    public void init() {
        ClientPlayConnectionEvents.JOIN.register(this::onConnected);
        ClientPlayConnectionEvents.DISCONNECT.register(this::onDisconnected);
    }

    public void onConnected(ClientPlayNetworkHandler handler, PacketSender sender, MinecraftClient client) {
        if (World_event_tracker.isDevelopment()) {
            connect();
            return;
        }
        if (handler.getConnection().getAddress() instanceof InetSocketAddress address) {
            World_event_tracker.LOGGER.info("ip: {}", address.getHostName());
            if (!isConnected && WYNNCRAFT_SERVER_PATTERN.matcher(address.getHostName()).matches()) {
                connect();
            } else if (WYNNCRAFT_SERVER_PATTERN.matcher(address.getHostName()).matches()) {
                World_event_tracker.LOGGER.info("server change");
                WynncraftConnectionEvents.CHANGE.invoker().interact();
            }
        }

    }

    private void onDisconnected(ClientPlayNetworkHandler clientPlayNetworkHandler, MinecraftClient minecraftClient) {
        if (isConnected)
            disconnect();
    }

    private void connect() {
        isConnected = true;
        World_event_tracker.LOGGER.info("on wynn");
        WynncraftConnectionEvents.JOIN.invoker().interact();
    }

    private void disconnect() {
        isConnected = false;
        World_event_tracker.LOGGER.info("off wynn");
        WynncraftConnectionEvents.LEAVE.invoker().interact();
    }

}
