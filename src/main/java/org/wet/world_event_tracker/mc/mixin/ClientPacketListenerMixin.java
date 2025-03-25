package org.wet.world_event_tracker.mc.mixin;


import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListHeaderS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.wet.world_event_tracker.components.Managers;
import org.wet.world_event_tracker.mc.event.PlayerInfoChangedEvents;
import org.wet.world_event_tracker.mc.event.WynnChatMessage;
import org.wet.world_event_tracker.utils.type.Prepend;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPacketListenerMixin {
    @Inject(method = "onGameMessage", at = @At("HEAD"))
    private void onGameMessage(GameMessageS2CPacket packet, CallbackInfo ci) {
        if (!MinecraftClient.getInstance().isOnThread()) return;
        if (!packet.overlay() && Managers.Connection.onWynncraft()) {
            Prepend.lastBadge = "";
            WynnChatMessage.EVENT.invoker().interact(packet.content());
        }
    }

    // for world
    @Inject(method = "onPlayerList", at = @At("HEAD"))
    private void onPlayerList(PlayerListS2CPacket packet, CallbackInfo ci) {
        if (!MinecraftClient.getInstance().isOnThread()) return;
        if (!Managers.Connection.onWynncraft()) return;
        for (PlayerListS2CPacket.Entry entry : packet.getEntries()) {
            for (PlayerListS2CPacket.Action action : packet.getActions()) {
                if (action == PlayerListS2CPacket.Action.UPDATE_DISPLAY_NAME) {
                    if (entry.displayName() == null) continue;
                    PlayerInfoChangedEvents.DISPLAY.invoker().displayChanged(entry.profileId(), entry.displayName());
                }
            }
        }
    }

    // for hub
    @Inject(method = "onPlayerListHeader", at = @At("HEAD"))
    private void onPlayerListHeader(PlayerListHeaderS2CPacket packet, CallbackInfo ci) {
        if (!MinecraftClient.getInstance().isOnThread()) return;
        PlayerInfoChangedEvents.FOOTER.invoker().footerChanged(packet.footer());
    }
}
