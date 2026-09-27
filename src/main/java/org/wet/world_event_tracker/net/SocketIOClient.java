package org.wet.world_event_tracker.net;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.socket.client.IO;
import io.socket.client.Socket;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;
import org.wet.world_event_tracker.World_event_tracker;
import org.wet.world_event_tracker.components.Models;
import org.wet.world_event_tracker.handlers.server.event.S2CServerEvents;
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
    private boolean queuedReconnect = false;
    private final IO.Options options = IO.Options.builder()
            .setExtraHeaders(new HashMap<>(Map.of("user" +
                    "-agent", Collections.singletonList(World_event_tracker.MOD_ID + "/" + World_event_tracker.MOD_VERSION))))
            .setTimeout(60000)
            .setReconnection(false)
            .build();

    public SocketIOClient() {
        super("socket", List.of(SocketIOClient.class));
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

    @Override
    protected void ready() {
        boolean reloadSocket = false;
        initSocket(reloadSocket);
        super.enable();

    }
    public void reconnectSocket() {
        if (serverSocket != null && !serverSocket.connected()) {
            McUtils.sendLocalMessage(Text.literal("§eReconnecting to chat server..."),
                    Prepend.WE.getWithStyle(ColourUtils.YELLOW), true);
            connectAttempt = 1;
            serverSocket.connect();
        } else if (serverSocket != null && serverSocket.connected()) {
            World_event_tracker.LOGGER.info("Socket is already connected.");
        }
    }

    private void initSocket(boolean reloadSocket) {
        if (reloadSocket) {
            firstConnect = true;
            for (Pair<String, Consumer<Object[]>> listener : listeners) {
                registerServerListener(listener.getLeft(), listener.getRight());
            }
        }
        if (World_event_tracker.isDevelopment() || Models.WorldState.onWorld()) {
            serverSocket.connect();
            World_event_tracker.LOGGER.info("Socket connecting.");
        }

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
        if (state == WorldState.WORLD) {
            this.enable();
            World_event_tracker.LOGGER.info("WorldState is WORLD, attempting to connect socket.");
            McUtils.sendLocalMessage(Text.literal("§eConnecting to chat server..."),
                    Prepend.WE.getWithStyle(ColourUtils.YELLOW), true);
            serverSocket.connect();
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
        String queryString = String.format("username=%s&modVersion=%s",
                McUtils.playerName(), World_event_tracker.MOD_VERSION);
        options.query = queryString;
        serverSocket = IO.socket(URI.create(World_event_tracker.secrets.get("url").getAsString()), options);
        serverSocket.on(Socket.EVENT_CONNECT, args -> {
            MinecraftClient.getInstance().execute(() -> {
                McUtils.sendLocalMessage(Text.literal("§aSuccessfully connected to chat server."),
                        Prepend.WE.getWithStyle(Style.EMPTY.withColor(Formatting.GREEN)), true);
            });
        });

        serverSocket.on(Socket.EVENT_CONNECT_ERROR, args -> {
            MinecraftClient.getInstance().execute(() -> {
                McUtils.sendLocalMessage(Text.literal("§cUnable to connect to chat server. Type /wet reconnect to try again."),
                        Prepend.WE.getWithStyle(Style.EMPTY.withColor(Formatting.RED)), true);
            });
        });

        serverSocket.on(Socket.EVENT_DISCONNECT, args -> {
            MinecraftClient.getInstance().execute(() -> {
                McUtils.sendLocalMessage(Text.literal("§cDisconnected from chat server, attempting to reconnect."),
                        Prepend.WE.getWithStyle(Style.EMPTY.withColor(Formatting.RED)), true);
            });
            reconnectSocket();
        });
        serverSocket.on("serverMessage", args -> {
            World_event_tracker.LOGGER.info("received notif {}", args[0].toString());

            S2CServerEvents.MESSAGE.invoker().interact(args[0]);
        });
        WorldStateEvents.CHANGE.register(this::worldStateChanged);
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