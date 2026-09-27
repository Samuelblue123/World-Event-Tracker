package org.wet.world_event_tracker;

import com.google.gson.*;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.wet.world_event_tracker.components.Handlers;
import org.wet.world_event_tracker.components.Managers;
import org.wet.world_event_tracker.components.Models;
import org.wet.world_event_tracker.utils.FileUtils;
import org.wet.world_event_tracker.utils.McUtils;
import org.wet.world_event_tracker.utils.type.Prepend;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class World_event_tracker implements ClientModInitializer {
    public static final String MOD_ID = "world_event_tracker";
    public static final Logger LOGGER = LoggerFactory.getLogger("weTracker");
    public static FileUtils fileUtils = new FileUtils();
    public static String fileName = "config/WET/tracked.json";
    public static JsonObject fileData;
    public static File configFile;
    public static ModContainer MOD_CONTAINER;
    public static String MOD_VERSION;
    public static JsonObject secrets;
    public static LiteralArgumentBuilder<FabricClientCommandSource> BASE_COMMAND = ClientCommandManager.literal("wet")
            .executes((context) -> {
                McUtils.sendLocalMessage(Text.of("§a§lWorld Event Tracker §r§av" + MOD_VERSION + " by §lSamuelblue123§r§a.\n§fType /wet help for a list of commands. If you encounter any problems please let Samuelblue123 know on discord."), Prepend.DEFAULT.get(), false);
                return Command.SINGLE_SUCCESS;
            });
    private static boolean development;

    public static boolean isDevelopment() {
        return development;
    }

    public static boolean isTesting() {
        return false;
    }

    @Override
    public void onInitializeClient() {
        development = FabricLoader.getInstance().isDevelopmentEnvironment();
        if (FabricLoader.getInstance().getModContainer(MOD_ID).isPresent()) {
            MOD_CONTAINER = FabricLoader.getInstance().getModContainer(MOD_ID).get();
            MOD_VERSION = MOD_CONTAINER.getMetadata().getVersion().getFriendlyString();
        }

        if ((configFile = new File(fileName)).exists()) {
            try {
                String configs = fileUtils.readFile(configFile);
                fileData = new JsonParser().parse(configs).getAsJsonObject();
                if (!fileData.has("settings")){
                    JsonObject settings = new JsonObject();
                    settings.add("sound", new JsonPrimitive(true));
                    fileData.add("settings", settings);
                }
                fileUtils.writeFile(configFile, fileData.toString());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        else {
            try {
                configFile = fileUtils.createFile(fileName);
                var obj = new JsonObject();
                JsonArray initEvents = new JsonArray();
                JsonObject initSettings = new JsonObject();
                initSettings.add("sound", new JsonPrimitive(true));
                obj.add("events", initEvents);
                obj.add("settings", initSettings);
                fileUtils.writeFile(configFile,obj.toString());
                fileData = obj;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            final LiteralCommandNode<FabricClientCommandSource> baseCommandNode = dispatcher.register(BASE_COMMAND);
            dispatcher.register(ClientCommandManager.literal("wet").executes(baseCommandNode.getCommand())
                    .redirect(baseCommandNode));
        });
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                    World_event_tracker.BASE_COMMAND.then(
                            ClientCommandManager.literal("credits")
                                    .executes(context -> {
                                        McUtils.sendLocalMessage(Text.literal("§aMod made by Samuelblue123 and JustCactus."), Prepend.DEFAULT.get(), false);
                                        return Command.SINGLE_SUCCESS;
                                    })
                    )
            );
        });
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                    World_event_tracker.BASE_COMMAND.then(
                            ClientCommandManager.literal("list")
                                    .executes(context -> {
                                        JsonArray eventList = fileData.get("events").getAsJsonArray();
                                        StringBuilder eventString = new StringBuilder();
                                        for (JsonElement event : eventList) {
                                            eventString.append(event.getAsString()).append(", ");
                                        }
                                        if (!eventString.toString().isEmpty()) {
                                            eventString.setLength(eventString.length() - 2);
                                            McUtils.sendLocalMessage(Text.literal("§aYou are tracking: " + eventString), Prepend.DEFAULT.get(), false);
                                            return Command.SINGLE_SUCCESS;
                                        }
                                        McUtils.sendLocalMessage(Text.literal("§aYou are not tracking anything, add some events to your list and get tracking!"), Prepend.DEFAULT.get(), false);
                                        return Command.SINGLE_SUCCESS;
                                    })));
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->{
            dispatcher.register(
                    World_event_tracker.BASE_COMMAND.then(
                            ClientCommandManager.literal("toggle").then(
                                    ClientCommandManager.literal("sound")
                                    .executes(context -> {
                                        JsonObject settings = fileData.get("settings").getAsJsonObject();
                                        if (settings.get("sound") == null) {
                                            try {
                                                settings.add("sound", new JsonPrimitive(true));
                                                fileData.remove("settings");
                                                fileData.add("settings", settings);
                                                fileUtils.writeFile(configFile, fileData.toString());
                                                McUtils.sendLocalMessage(Text.literal("§aSound effects set to ON"), Prepend.DEFAULT.get(), false);
                                            } catch (IOException e) {
                                                throw new RuntimeException(e);
                                            }
                                            return Command.SINGLE_SUCCESS;
                                        }
                                        if(settings.get("sound").getAsBoolean()){
                                            try {
                                                settings.remove("sound");
                                                settings.add("sound", new JsonPrimitive(false));
                                                fileData.remove("settings");
                                                fileData.add("settings", settings);
                                                fileUtils.writeFile(configFile, fileData.toString());
                                                McUtils.sendLocalMessage(Text.literal("§aSound effects set to OFF"), Prepend.DEFAULT.get(), false);
                                            } catch (IOException e) {
                                                throw new RuntimeException(e);
                                            }
                                            return Command.SINGLE_SUCCESS;
                                        }
                                        else {
                                            try {
                                                settings.remove("sound");
                                                settings.add("sound", new JsonPrimitive(true));
                                                fileData.remove("settings");
                                                fileData.add("settings", settings);
                                                fileUtils.writeFile(configFile, fileData.toString());
                                                McUtils.sendLocalMessage(Text.literal("§aSound effects set to ON"), Prepend.DEFAULT.get(), false);
                                            } catch (IOException e) {
                                                throw new RuntimeException(e);
                                            }
                                            return Command.SINGLE_SUCCESS;
                                        }
                                    })
                            )
                    )
            );
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {

            // World event options
            String[] allowedEvents = new String[]{
                    "HaywireDefender", "ApproachingRaid", "SkitteringSpiders", "OvertakenFarm", "ArachnidAmbush", "EncroachingBlaze",
                    "DarkDeacons", "EncroachingDestruction", "CorruptedSpring", "NecromanticSite", "RisenReturn", "EncroachingMisery",
                    "TaintedShoreline", "AeonOrigin", "BowelsoftheRoots", "EncroachingReanimation", "ImproperBurialRites",
                    "Blood-EncrustedMastaba", "EncroachingConflagration", "FailedHunt", "CanineAmbush", "BlazingCombustion", "LonelyIslet",
                    "EncroachingAblation", "RogueWyrmling", "SlimySchism", "SwashbucklingBrawl", "DesperateAmbush",
                    "ABurningMemory", "EncroachingExtinction", "PeculiarGrotto", "LightEmissaries", "UnsettlingEncounters",
                    "VisitfromBeyond", "AbandonedSentinels", "RealmicAntigen", "TerritorialTrolls", "ColossiIngrain", "EnragedEagle",
                    "DespermechOccupation", "DecommissionedWarMachines", "BubblingTerrace", "InfernalCaldera",
                    "MaarAshpit", "ShatteredRoosts", "AhmsMonuments", "IncomprehensibleCynosure", "ShapesintheDark", "AllEyesonMe",
                    "MonumenttoLoss", "PestilentialDownpour", "OtherworldlyExhibition", "SwamplandSquabble", "AutumnPoachers",
                    "StackpeakPinnacle", "KaroshiUnion", "SteelSkirmish", "BiohazardousBloom", "Tree-TopCradle", "ApiaryHive", "FossilFighters",
                    "GlacialTraining", "PatrollingSoldiers", "MoleMeet-Up", "CitadelBarracks", "RoyalAlchemists", "PalaceGuards",
                    "RestingGrounds"
            };
            dispatcher.register(World_event_tracker.BASE_COMMAND.then(ClientCommandManager.literal("untrack").then(ClientCommandManager.literal("all").executes(context -> {
                var obj = new JsonObject();
                JsonArray initFile = new JsonArray();
                fileData.remove("events");
                fileData.add("events", initFile);
                try {
                    fileUtils.writeFile(configFile,fileData.toString());
                    McUtils.sendLocalMessage(Text.literal("§aSuccessfully deregistered from ALL events."), Prepend.DEFAULT.get(), false);
                } catch (IOException e) {
                    McUtils.sendLocalMessage(Text.literal("§cUnable to be deregistered from ALL events."), Prepend.DEFAULT.get(), false);
                    throw new RuntimeException(e);
                }
                return Command.SINGLE_SUCCESS;
            }))));
            dispatcher.register(World_event_tracker.BASE_COMMAND.then(ClientCommandManager.literal("track").then(ClientCommandManager.literal("all").executes(context -> {
                JsonArray eventList = new JsonArray();
                for (String event : allowedEvents){
                    eventList.add(event);
                }
                fileData.remove("events");
                fileData.add("events", eventList);
                try {
                    fileUtils.writeFile(configFile, fileData.toString());
                    McUtils.sendLocalMessage(Text.literal("§aSuccessfully registered for ALL events."), Prepend.DEFAULT.get(), false);
                } catch (IOException e) {
                    McUtils.sendLocalMessage(Text.literal("§cUnable to init for ALL events."), Prepend.DEFAULT.get(), false);
                    throw new RuntimeException(e);
                }
                return Command.SINGLE_SUCCESS;
            }))));

            SuggestionProvider worldEventSuggestions = (context, builder) -> CommandSource.suggestMatching(allowedEvents, builder);

            LiteralArgumentBuilder<FabricClientCommandSource> trackCommand = ClientCommandManager.literal("track")
                    .then(ClientCommandManager.argument("world_event", StringArgumentType.string())
                            .suggests(worldEventSuggestions)
                            .executes(context -> {
                                String event = StringArgumentType.getString(context, "world_event");
                                if (event.equals("RuffTumble")) event = "Ruff&Tumble";
                                JsonElement jsonEvent = new JsonParser().parse(event);
                                if (fileData.getAsJsonArray("events").contains(jsonEvent)) {
                                    McUtils.sendLocalMessage(Text.literal("§cYou were already registered for the " + event + "."), Prepend.DEFAULT.get(), false);
                                    return Command.SINGLE_SUCCESS;
                                }
                                fileData.getAsJsonArray("events").add(event); //errors maybe?
                                try {
                                    fileUtils.writeFile(configFile, fileData.toString());
                                    McUtils.sendLocalMessage(Text.literal("§aSuccessfully registered for the " + event + "."), Prepend.DEFAULT.get(), false);
                                } catch (IOException e) {
                                    McUtils.sendLocalMessage(Text.literal("§cUnable to init for the " + event + "."), Prepend.DEFAULT.get(), false);
                                    throw new RuntimeException(e);
                                }
                                return Command.SINGLE_SUCCESS;
                            }));

            LiteralArgumentBuilder<FabricClientCommandSource> untrackCommand = ClientCommandManager.literal("untrack")
                    .then(ClientCommandManager.argument("world_event", StringArgumentType.string())
                            .suggests(worldEventSuggestions)
                            .executes(context -> {
                                String event = StringArgumentType.getString(context, "world_event");
                                if (event.equals("RuffTumble")) event = "Ruff&Tumble";
                                JsonElement jsonEvent = new JsonParser().parse(event);
                                fileData.getAsJsonArray("events").remove(jsonEvent); //errors maybe?
                                try {
                                    fileUtils.writeFile(configFile, fileData.toString());
                                    McUtils.sendLocalMessage(Text.literal("§aSuccessfully deregistered for the " + event + "."), Prepend.DEFAULT.get(), false);
                                } catch (IOException e) {
                                    McUtils.sendLocalMessage(Text.literal("§cUnable to deregister for the " + event + "."), Prepend.DEFAULT.get(), false);
                                    throw new RuntimeException(e);
                                }
                                return Command.SINGLE_SUCCESS;
                            }));

            dispatcher.register(World_event_tracker.BASE_COMMAND.then(trackCommand).then(untrackCommand));
        });


            try {
                InputStream inputStream = getClass().getClassLoader().getResourceAsStream("WEsecrets.json");
                if (inputStream == null) {
                    throw new IOException("Secret file not found");
                }
                secrets = JsonParser.parseReader(new InputStreamReader(inputStream)).getAsJsonObject();
                World_event_tracker.LOGGER.info("Secrets loaded successfully.");
            } catch (Exception e) {
                World_event_tracker.LOGGER.error("Failed to load or parse secrets configuration: ", e);
                secrets = new JsonObject();
                MinecraftClient.getInstance().execute( () -> {
                    McUtils.sendLocalMessage(
                            Text.literal("§cConfiguration corruption detected. Please reinstall or reset your mod configuration."),
                            Prepend.DEFAULT.get(), true);
                });
            }
        Handlers.ServerMessage.init();
        Managers.Connection.init();
        Managers.Net.init();
        Managers.Feature.init();
        Managers.ChatMessageInit.init();
        Models.WorldState.init();
    }
}