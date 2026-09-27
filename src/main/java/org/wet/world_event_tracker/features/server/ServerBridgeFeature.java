package org.wet.world_event_tracker.features.server;

import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvents;
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.wet.world_event_tracker.World_event_tracker.fileData;


public class ServerBridgeFeature extends Feature {
    private final Pattern WE_PATTERN = Pattern.compile("^§0((\uDAFF\uDFFC\uE00D\uDAFF\uDFFF\uE002\uDAFF\uDFFE)|(\uDAFF\uDFFC\uE001\uDB00\uDC06))§0 §0The (?<worldevent>.+)+ World Event starts in (?<time>.+)+!");
    private final Pattern ANNIE_PATTERN = Pattern.compile("^§0\uDAFF\uDFFC\uE001\uDB00\uDC06§0 §cPrepare to defend the province at the Corruption Portal in (?<time>.+)+!");
    private SocketIOClient socketIOClient;

    @Override
    public void init() {
        socketIOClient = Managers.Net.socket;
        ChatMessageReceived.EVENT.register(this::onWynnMessage);
        S2CServerEvents.MESSAGE.register(this::onServerMessage);
    }

    //TODO: blood encrusted mastaba has formatting between the minutes and seconds causing it to return the seconds part as
    // black color code formatted, try fix this before next release.

    private void onWynnMessage(Text message) {
        String m = TextUtils.parseStyled(message, TextParseOptions.DEFAULT.withExtractUsernames(true));
        if (World_event_tracker.isDevelopment()){
            m = m.replaceFirst("&", "§");
            m = m.replaceFirst("&", "§");
            m = m.replaceFirst("&", "§");
        }
        Matcher weMatcher = WE_PATTERN.matcher(m);
        Matcher annieMatcher = ANNIE_PATTERN.matcher(m);
        if (!m.contains("\uE003") && weMatcher.find()) {
                socketIOClient.emit(socketIOClient.serverSocket, "wynnMessage", weMatcher.group("worldevent")+":"+weMatcher.group("time"));
        }
        if (!m.contains("\uE004") && annieMatcher.find()) {
            socketIOClient.emit(socketIOClient.serverSocket,"annieMessage", annieMatcher.group("time"));
        }
    }

    private void onServerMessage(Object message) {
        MinecraftClient.getInstance().execute(() -> {
            try {
                McUtils.sendLocalMessage(
                        Text.empty()
                                .append(FontUtils.BannerPillFont.parseStringWithFill("server")
                                        .fillStyle(Style.EMPTY.withColor(Formatting.DARK_AQUA)))
                                .append(" ")
                                .append(Text.literal(message.toString())
                                        .fillStyle(Style.EMPTY.withColor(Formatting.BLUE))),
                        Prepend.WE.getWithStyle(Style.EMPTY.withColor(Formatting.DARK_BLUE)),
                        true
                );

                MinecraftClient client = MinecraftClient.getInstance();

                if (client.player != null && fileData.get("settings").getAsJsonObject().get("sound").getAsBoolean()) {
                    client.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP);
                }
            } catch (Exception e) {
                World_event_tracker.LOGGER.info("server message error: {} {}", e, e.getMessage());
            }
        });
    }


}