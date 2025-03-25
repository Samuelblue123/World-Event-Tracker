package org.wet.world_event_tracker.utils;


import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.util.ChatMessages;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.wet.world_event_tracker.World_event_tracker;
import org.wet.world_event_tracker.mc.mixin.accessors.ChatHudAccessorInvoker;
import org.wet.world_event_tracker.utils.text.TextUtils;
import org.wet.world_event_tracker.utils.type.Prepend;

public class McUtils {
    public static String devName = "Samuelblue123";

    public static String playerName() {
        if (World_event_tracker.isDevelopment()) return devName;
        return player().getName().getString();
    }

    public static String playerUUID() {
        if (World_event_tracker.isDevelopment()) return "39365bd4-5c78-41de-8901-c7dc5b7c64c4";
        if (player() != null)
            return player().getUuidAsString();
        return null;
    }

    public static PlayerEntity player() {
        return mc().player;
    }

    public static MinecraftClient mc() {
        return MinecraftClient.getInstance();
    }

    public static void sendLocalMessage(Text message, MutableText prepend, boolean wynncraftStyle) {
        if (player() == null) {
            World_event_tracker.LOGGER.error("Tried to send local message but player was null.");
            return;
        }
        ChatHud chatHud = MinecraftClient.getInstance().inGameHud.getChatHud();
        ChatHudAccessorInvoker chatHudAccessorInvoker = (ChatHudAccessorInvoker) chatHud;
        Text withPrepend = Text.empty().append(prepend).append(message);
        if (wynncraftStyle) withPrepend = TextUtils.toBlockMessage(withPrepend, prepend.getStyle());
        Prepend.linesSinceBadge += ChatMessages.breakRenderedChatMessageLines(withPrepend, chatHudAccessorInvoker.invokeGetWidth(), MinecraftClient.getInstance().textRenderer)
                .size();
        player().sendMessage(withPrepend, false);
    }
}
