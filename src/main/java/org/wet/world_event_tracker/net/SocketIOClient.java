package org.wet.world_event_tracker.net;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.socket.client.IO;
import io.socket.client.Socket;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;
import org.json.JSONObject;
import org.wet.world_event_tracker.World_event_tracker;
import org.wet.world_event_tracker.components.Managers;
import org.wet.world_event_tracker.components.Models;
import org.wet.world_event_tracker.models.worldState.event.WorldStateEvents;
import org.wet.world_event_tracker.models.worldState.type.WorldState;
import org.wet.world_event_tracker.net.type.Api;
import org.wet.world_event_tracker.utils.ColourUtils;
import org.wet.world_event_tracker.utils.McUtils;
import org.wet.world_event_tracker.utils.type.Prepend;

import java.net.URI;
import java.util.*;
import java.util.function.Consumer;


public class SocketIOClient extends Api {
    private static SocketIOClient instance;
    private final ArrayList<Pair<String, Consumer<Object[]>>> listeners = new ArrayList<>();
    public Socket serverSocket;
    private boolean firstConnect = true;
    private int connectAttempt = 0;
    private World_event_trackerClient user;
    public String guildId;
    private boolean queuedReconnect = false;
    private final IO.Options options = IO.Options.builder()
            .setExtraHeaders(new HashMap<>(Map.of("user" +
                    "-agent", Collections.singletonList(World_event_tracker.MOD_ID + "/" + World_event_tracker.MOD_VERSION))))
            .setTimeout(60000)
            .setReconnection(false)
            .build();

    // TODO if add multiple sockets, create wrapper class for each socket with add listeners, etc.
    public SocketIOClient() {
        super("socket", List.of(World_event_trackerClient.class));
        instance = this;
    }

    public void emit(Socket socket, String event, Object data) {
        if (socket != null && socket.connected()) {
            World_event_tracker.LOGGER.info("emitting, {}", data);
            socket.emit(event, data);
        } else {
            World_event_tracker.LOGGER.warn("skipped event because of missing or inactive socket");
        }
    }

    public static SocketIOClient getInstance() {
        return instance;
    }

    @Override
    protected void ready() {

        user = Managers.Net.user;
        options.extraHeaders.put("from", Collections.singletonList(McUtils.playerName()));
        boolean reloadSocket = false;
        if (!user.uuid.equals(guildId)) {
            guildId = user.uuid;
            reloadSocket = true;
        }

        initSocket(reloadSocket);
        super.enable();

    }
    public void reconnectSocket() {
        if (serverSocket != null && !serverSocket.connected()) {
            World_event_tracker.LOGGER.info("Attempting to reconnect the socket...");
            McUtils.sendLocalMessage(Text.literal("§eReconnecting to chat server..."),
                    Prepend.WE.getWithStyle(ColourUtils.YELLOW), true);

            // Reconnect logic
            connectAttempt = 1;
            serverSocket.connect();
        } else if (serverSocket != null && serverSocket.connected()) {
            World_event_tracker.LOGGER.info("Socket is already connected.");
        }
    }

    private void initSocket(boolean reloadSocket) {
        String queryString = String.format("username=%s&modVersion=%s&uuid=%s",
                McUtils.playerName(), World_event_tracker.MOD_VERSION, user.uuid);
        options.query = queryString;
        if (reloadSocket) {
            firstConnect = true;
            options.extraHeaders.put("Authorization", Collections.singletonList("Bearer " + World_event_tracker.secrets.get("password").getAsString()));
            serverSocket = IO.socket(URI.create(World_event_tracker.secrets.get("url")+"?"+queryString), options);

            for (Pair<String, Consumer<Object[]>> listener : listeners) {
                registerServerListener(listener.getLeft(), listener.getRight());
            }

            registerServerListener(Socket.EVENT_DISCONNECT, (reason) -> {
                World_event_tracker.LOGGER.info("{} disconnected", reason);
                McUtils.sendLocalMessage(Text.literal("§cDisconnected from chat server."),
                        Prepend.WE.getWithStyle(Style.EMPTY.withColor(Formatting.RED)), true);
                if (reason[0].equals("io client disconnect") || reason[0].equals("forced close")) {
                    World_event_tracker.LOGGER.info("{} skip", reason);
                    return;
                }
                connectAttempt = 0;

                try {
                    Thread.sleep(1000);
                    serverSocket.connect();
                } catch (InterruptedException e) {
                    World_event_tracker.LOGGER.error("thread sleep error: {} {}", e, e.getMessage());
                }
            });

            registerServerListener(Socket.EVENT_CONNECT_ERROR, (err) -> {
                if (connectAttempt % 5 == 0) {
                    if (firstConnect) McUtils.sendLocalMessage(Text.literal("§eConnecting to chat server..."),
                            Prepend.WE.getWithStyle(ColourUtils.YELLOW), true);
                    else McUtils.sendLocalMessage(Text.literal("§eReconnecting..."),
                            Prepend.WE.getWithStyle(ColourUtils.YELLOW), true);
                }
                World_event_tracker.LOGGER.info("{} reconnect error", err);

                if (err[0] instanceof JSONObject error) {
                    try {
                        String message = error.getString("message");
                        if (message.equals("Invalid token provided") || message.equals("No token provided"))
                            options.extraHeaders.put("Authorization", Collections.singletonList("Bearer " + World_event_tracker.secrets.get("password").getAsString()));
                    } catch (Exception e) {
                        World_event_tracker.LOGGER.error("connect error: {} {}", e, e.getMessage());
                    }
                }
                try {
                    Thread.sleep(1000);
                    if (++connectAttempt < 10) {
                        serverSocket.disconnect();
                        serverSocket.connect();
                    } else
                        McUtils.sendLocalMessage(Text.literal("§cCould not connect to chat server. Type /reconnect to try again."),
                                Prepend.WE.getWithStyle(ColourUtils.RED), true);
                } catch (Exception e) {
                    World_event_tracker.LOGGER.error("reconnect server error: {} {}", e, e.getMessage());
                }
            });

            registerServerListener(Socket.EVENT_CONNECT, (args) -> {
                McUtils.sendLocalMessage(Text.literal("§aSuccessfully connected to chat server."),
                        Prepend.WE.getWithStyle(Style.EMPTY.withColor(Formatting.GREEN)), true);
                queuedReconnect = false;
                firstConnect = false;
            });
        }
        if (World_event_tracker.isDevelopment() || Models.WorldState.onWorld()) {
            serverSocket.connect();
            World_event_tracker.LOGGER.info("sockets connecting");
        }
        WorldStateEvents.CHANGE.register(this::worldStateChanged);
    }

    public void addServerListener(String name, Consumer<Object[]> listener) {
        listeners.add(new Pair<>(name, listener));
        registerServerListener(name, listener);
    }

    public void registerServerListener(String name, Consumer<Object[]> listener) {
        if (serverSocket != null)
            serverSocket.on(name, listener::accept);
    }

    private void worldStateChanged(WorldState state) {
        World_event_tracker.LOGGER.info("worldStateChanged called with state: " + state);
        if (state == WorldState.WORLD) {
            this.enable();
            World_event_tracker.LOGGER.info("WorldState is WORLD, attempting to connect socket.");
            reconnectSocket();
            World_event_tracker.LOGGER.info("server socket on");
        } else {
            this.disable();
            connectAttempt = 999;
            if (serverSocket.connected()) {
                serverSocket.disconnect();
                World_event_tracker.LOGGER.info("server socket off");
            }
        }
    }

    @Override
    public void init() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("wet").then(ClientCommandManager.literal("reconnect")
                    .executes((context) -> {
                if (isDisabled() && !World_event_tracker.isDevelopment()) {
                    queuedReconnect = true;
                    McUtils.sendLocalMessage(Text.literal("§eYou are not currently in a world. Chat server reconnect queued."),
                            Prepend.WE.getWithStyle(ColourUtils.YELLOW), true);
                    return Command.SINGLE_SUCCESS;
                }
                if (serverSocket == null) {
                    McUtils.sendLocalMessage(Text.literal("§cCould not find chat server."), Prepend.WE.getWithStyle(ColourUtils.RED), true);
                    return 0;
                }
                if (!serverSocket.connected()) {
                    McUtils.sendLocalMessage(Text.literal("§eConnecting to chat server..."),
                            Prepend.WE.getWithStyle(ColourUtils.YELLOW), true);
                    connectAttempt = 1;
                    serverSocket.connect();
                    return Command.SINGLE_SUCCESS;
                } else {
                    McUtils.sendLocalMessage(Text.literal("§aYou are already connected to the chat server!"),
                            Prepend.WE.getWithStyle(ColourUtils.GREEN), true);
                    return 0;
                }
            })));
            if (World_event_tracker.isTesting()) {
                dispatcher.register(ClientCommandManager.literal("testmessage")
                        .then(ClientCommandManager.argument("message", StringArgumentType.greedyString())
                                .executes((context) -> {
                                    emit(serverSocket, "wynnMessage", StringArgumentType.getString(context, "message")
                                            .replaceAll("&", "§"));
                                    return Command.SINGLE_SUCCESS;
                                })));
            }
        });
    }

    @Override
    protected void unready() {
        super.unready();
        if (serverSocket != null)
            serverSocket.disconnect();
        options.extraHeaders.clear();
        options.extraHeaders.put("user-agent", Collections.singletonList(World_event_tracker.MOD_ID + "/" + World_event_tracker.MOD_VERSION));
        firstConnect = true;
        connectAttempt = 0;

    }
}