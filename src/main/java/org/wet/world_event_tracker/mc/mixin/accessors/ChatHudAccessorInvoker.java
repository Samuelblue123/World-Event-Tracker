package org.wet.world_event_tracker.mc.mixin.accessors;


import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(ChatHud.class)
public interface ChatHudAccessorInvoker {
    @Accessor("messages")
    List<ChatHudLine> getMessages();

    @Accessor("scrolledLines")
    int getScrolledLines();

    @Invoker("getWidth")
    int invokeGetWidth();

    @Invoker("getLineHeight")
    int invokeGetLineHeight();

}
