package org.wet.world_event_tracker.features.server;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.socket.client.Ack;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundCategory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.wet.world_event_tracker.World_event_tracker;
import org.wet.world_event_tracker.components.Feature;
import org.wet.world_event_tracker.components.Managers;
import org.wet.world_event_tracker.handlers.chat.event.ChatMessageReceived;
import org.wet.world_event_tracker.handlers.server.event.S2CServerEvents;
import org.wet.world_event_tracker.net.SocketIOClient;
import org.wet.world_event_tracker.utils.McUtils;
import org.wet.world_event_tracker.utils.text.FontUtils;
import org.wet.world_event_tracker.utils.text.TextUtils;
import org.wet.world_event_tracker.utils.text.type.TextParseOptions;
import org.wet.world_event_tracker.utils.type.Prepend;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ServerBridgeFeature extends Feature {
    private final Pattern WE_PATTERN = Pattern.compile("^§0((\uDAFF\uDFFC\uE00D\uDAFF\uDFFF\uE002\uDAFF\uDFFE)|(\uDAFF\uDFFC\uE001\uDB00\uDC06))§0 §0The (?<worldevent>.+)+ World Event starts in (?<time>.+)+!");
    private SocketIOClient socketIOClient;

    @Override
    public void init() {
        socketIOClient = Managers.Net.socket;

//        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
//            dispatcher.register(ClientCommandManager.literal("server")
//                    .then(ClientCommandManager.argument("message", StringArgumentType.greedyString())
//                            .executes((context) -> {
//                                String message = StringArgumentType.getString(context, "message");
//                                message = message.replaceAll("[\u200C\uE087\uE013\u2064\uE071\uE012\uE000\uE089\uE088\uE07F\uE08B\uE07E\uE080ÁÀ֎]", "");
//                                if (message.isBlank()) return 0;
//                                if (socketIOClient == null || socketIOClient.serverSocket == null) {
//                                    McUtils.sendLocalMessage(Text.literal("Still connecting to chat server...")
//                                            .setStyle(Style.EMPTY.withColor(Formatting.YELLOW)), Prepend.DEFAULT.get(), false);
//                                    return 0;
//                                }
//                                if (!socketIOClient.serverSocket.connected()) {
//                                    McUtils.sendLocalMessage(Text.literal("Chat server not connected. Type /reconnect to try to connect.")
//                                            .setStyle(Style.EMPTY.withColor(Formatting.RED)), Prepend.DEFAULT.get(), false);
//                                    return 0;
//
//                                }
//                                socketIOClient.emit(socketIOClient.serverSocket, "serverOnlyWynnMessage", McUtils.playerName() + ": " + message);
//                                socketIOClient.emit(socketIOClient.serverSocket, "serverMessage", Map.of("Author", McUtils.playerName(), "Content", message, "WynnGuildId", Managers.Net.user.uuid));
//                                return Command.SINGLE_SUCCESS;
//                            })));
//            dispatcher.register(ClientCommandManager.literal("dc").redirect(dispatcher.getRoot().getChild("server")));
//
//            dispatcher.register(ClientCommandManager.literal("online").executes((context) -> {
//                if (socketIOClient == null || socketIOClient.serverSocket == null) {
//                    McUtils.sendLocalMessage(Text.literal("Still connecting to chat server...")
//                            .setStyle(Style.EMPTY.withColor(Formatting.YELLOW)), Prepend.DEFAULT.get(), false);
//                    return 0;
//                }
//                socketIOClient.emit(socketIOClient.serverSocket, "listOnline", (Ack) args -> {
//                    if (args[0] instanceof JSONArray data) {
//                        try {
//                            MutableText message = Text.literal("Online mod users: ");
//                            for (int i = 0; i < data.length(); i++) {
//                                message.append(data.getString(i));
//                                if (i != data.length() - 1) message.append(", ");
//                            }
//                            message.setStyle(Style.EMPTY.withColor(Formatting.GREEN));
//                            McUtils.sendLocalMessage(message, Prepend.WE.getWithStyle(Style.EMPTY.withColor(Formatting.GREEN)), true);
//                        } catch (Exception e) {
//                            World_event_tracker.LOGGER.error("error parsing online users: {} {}", e, e.getMessage());
//                        }
//                    }
//                });
//                return Command.SINGLE_SUCCESS;
//            }));
//        });

        ChatMessageReceived.EVENT.register(this::onWynnMessage);
        S2CServerEvents.MESSAGE.register(this::onServerMessage);
    }

    private void onWynnMessage(Text message) {
        String m = TextUtils.parseStyled(message, TextParseOptions.DEFAULT.withExtractUsernames(true));
        if (World_event_tracker.isDevelopment()) m = m.replaceAll("&", "§");
        World_event_tracker.LOGGER.info("received: {}", m);
        Matcher weMatcher = WE_PATTERN.matcher(m);
        if (!m.contains("\uE003") && weMatcher.find()) {
            World_event_tracker.LOGGER.info(weMatcher.group("worldevent"));
                socketIOClient.emit(socketIOClient.serverSocket, "wynnMessage", weMatcher.group("worldevent")+":"+weMatcher.group("time"));
        }
    }

    private void onServerMessage(Object message) {
        try {
            McUtils.sendLocalMessage(Text.empty().append(FontUtils.BannerPillFont.parseStringWithFill("server")
                            .fillStyle(Style.EMPTY.withColor(Formatting.DARK_AQUA))).append(" ")
                    .append(Text.literal(message.toString())
                            .fillStyle(Style.EMPTY.withColor(Formatting.BLUE))), Prepend.WE.getWithStyle(Style.EMPTY.withColor(Formatting.DARK_BLUE)), true);


            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                client.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP);
            }
        } catch (Exception e) {
            World_event_tracker.LOGGER.info("server message error: {} {}", e, e.getMessage());
        }
    }


}