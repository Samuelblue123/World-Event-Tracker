package org.wet.world_event_tracker.net;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.wet.world_event_tracker.World_event_tracker;
import org.wet.world_event_tracker.models.worldState.event.WorldStateEvents;
import org.wet.world_event_tracker.models.worldState.type.WorldState;
import org.wet.world_event_tracker.net.event.NetEvents;
import org.wet.world_event_tracker.net.type.Api;
import org.wet.world_event_tracker.utils.ColourUtils;
import org.wet.world_event_tracker.utils.JsonUtils;
import org.wet.world_event_tracker.utils.McUtils;
import org.wet.world_event_tracker.utils.NetUtils;
import org.wet.world_event_tracker.utils.type.Prepend;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class AutoUpdateApi extends Api {
    private static AutoUpdateApi instance;

    public AutoUpdateApi() {
        super("update", List.of(AutoUpdateApi.class));
        instance = this;
    }

    @Override
    protected void ready() {
        super.enable();
    }

    @Override
    public void init() {
        WorldStateEvents.CHANGE.register(this::worldStateChanged);
    }

    private void worldStateChanged(WorldState state) {
        if (state == WorldState.WORLD) {
            this.enable();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(World_event_tracker.secrets.get("url").getAsString() + "api/v2/mod/update"))
                    .header("Accept", "application/json")
                    .version(HttpClient.Version.HTTP_1_1)
                    .build();
            HttpClient client = HttpClient.newHttpClient();
            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        String latest = JsonUtils.toJsonObject(response.body()).get("versionNumber").getAsString();
                        if (!(latest.equals(World_event_tracker.MOD_VERSION))){
                            World_event_tracker.LOGGER.warn("outdated version: {}", World_event_tracker.MOD_VERSION);
                            MinecraftClient.getInstance().execute( ()-> {
                                McUtils.sendLocalMessage(Text.literal("§a[World Event Tracker] You are running build v" + World_event_tracker.MOD_VERSION + ", but the latest version is " + latest + "." +
                                        " " +
                                        "Please consider updating through modrinth."), Prepend.EMPTY.get(), false);
                            });
                        }
                    })
                    .exceptionally(err -> {
                        World_event_tracker.LOGGER.error("Update check failed", err);
                        return null;
                    });
        }
        else {
            this.disable();
        }
    }
}