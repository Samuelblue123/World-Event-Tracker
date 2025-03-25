package org.wet.world_event_tracker.features;

import com.mojang.brigadier.Command;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Pair;
import org.wet.world_event_tracker.World_event_tracker;
import org.wet.world_event_tracker.components.Feature;
import org.wet.world_event_tracker.utils.McUtils;
import org.wet.world_event_tracker.utils.type.Prepend;

import java.util.List;

public class CommandHelpFeature extends Feature {
    private final List<Pair<String, String>> commands = List.of(
            new Pair<>("/wet help", "Displays this list of commands."),
            new Pair<>("/wet credits", "Displays the credits for this mod."),

            new Pair<>("\n","Mandatory Commands:"),

            new Pair<>("/wet register", "Registers user on the database (required for mod functionality)."),

            new Pair<>("\n", "Bridge:"),

            new Pair<>("/wet reconnect", "Tries to connect to the chat server if it isn't already connected."),

            new Pair<>("\n", "Tracking Commands:"),

            new Pair<>("/wet track <World Event>", "Tracks the specified world event."),
            new Pair<>("/wet untrack <World Event>", "Untracks the specified world event."),
            new Pair<>("/wet list", "Lists all tracked world events."),

            new Pair<>("\n","Self-Shoutout"),
            new Pair<>("Made by Opus Maximus.","If you enjoy the mod, consider joining the guild!")

    );

    private MutableText helpMessage;

    @Override
    public void init() {
        helpMessage = Text.literal("§aCommands:\n");
        for (int i = 0; i < commands.size(); i++) {
            Pair<String, String> entry = commands.get(i);
            String delimiter = entry.getLeft().isBlank() ? "":" - ";
            helpMessage.append(entry.getLeft() + delimiter + entry.getRight());
            if (i != commands.size() - 1)
                helpMessage.append("\n");
        }
        ClientCommandRegistrationCallback.EVENT.register(((dispatcher, registryAccess) -> {
            dispatcher.register(World_event_tracker.BASE_COMMAND.then(ClientCommandManager.literal("help").executes((context) -> {
                McUtils.sendLocalMessage(helpMessage, Prepend.DEFAULT.get(), false);
                return Command.SINGLE_SUCCESS;
            })));
        }));
    }
}
