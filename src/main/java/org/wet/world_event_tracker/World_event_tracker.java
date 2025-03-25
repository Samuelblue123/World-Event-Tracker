package org.wet.world_event_tracker;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.wet.world_event_tracker.components.Handlers;
import org.wet.world_event_tracker.components.Managers;
import org.wet.world_event_tracker.components.Models;
import org.wet.world_event_tracker.net.NetManager;
import org.wet.world_event_tracker.net.SocketIOClient;
import org.wet.world_event_tracker.net.World_event_trackerClient;
import org.wet.world_event_tracker.utils.JsonUtils;
import org.wet.world_event_tracker.utils.McUtils;
import org.wet.world_event_tracker.utils.NetUtils;
import org.wet.world_event_tracker.utils.type.Prepend;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;


public class World_event_tracker implements ClientModInitializer {
    public static JsonUtils jsonUtils = new JsonUtils();
    public static final String MOD_ID = "world_event_tracker";
    public static final String MOD_STORAGE_ROOT = "weTracker";
    public static final Logger LOGGER = LoggerFactory.getLogger("weTracker");
    public static ModContainer MOD_CONTAINER;
    public static String MOD_VERSION;
    public static JsonObject secrets;
    public static LiteralArgumentBuilder<FabricClientCommandSource> BASE_COMMAND = ClientCommandManager.literal("wet")
            .executes((context) -> {
                McUtils.sendLocalMessage(Text.of("§a§lWorld Event Tracker §r§av" + MOD_VERSION + " by §lSamuelblue123§r§a.\n§fType /wet help for a list of commands. If you encounter any problems please let Samuelblue123 know on discord."), Prepend.DEFAULT.get(), false);
                return Command.SINGLE_SUCCESS;
            });
    private static boolean development;
    private World_event_trackerClient user;
    private static SocketIOClient socket;

    public static File getModStorageDir(String dirName) {
        return new File(MOD_STORAGE_ROOT, dirName);
    }

    public static boolean isDevelopment() {
        return development;
    }

    public static boolean isTesting() {
        return false;
    }

    @Override
    public void onInitializeClient() {
        development = FabricLoader.getInstance().isDevelopmentEnvironment();
        World_event_tracker.LOGGER.info(""+development);
        if (FabricLoader.getInstance().getModContainer(MOD_ID).isPresent()) {
            MOD_CONTAINER = FabricLoader.getInstance().getModContainer(MOD_ID).get();
            MOD_VERSION = MOD_CONTAINER.getMetadata().getVersion().getFriendlyString();
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
                                        HttpRequest getRequest = HttpRequest.newBuilder()
                                                .uri(URI.create(World_event_tracker.secrets.get("url").getAsString() + "api/v2/user/wes"))
                                                .header("authorization", "Bearer " + World_event_tracker.secrets.get("password").getAsString())
                                                .header("Content-Type", "application/json")
                                                .header("uuid", Managers.Net.wynn.wynnPlayerInfo.get("uuid").getAsString())
                                                //                              .version(HttpClient.Version.HTTP_1_1) // remove in final version
                                                .GET()
                                                .build();
                                        NetManager.HTTP_CLIENT.sendAsync(getRequest, HttpResponse.BodyHandlers.ofString()).whenCompleteAsync((stringHttpResponse, throwable) -> {
                                        McUtils.sendLocalMessage(Text.literal("§aYou are tracking:"+stringHttpResponse.body()), Prepend.DEFAULT.get(), false);
                                        });

                                        return Command.SINGLE_SUCCESS;
                                    })));
        });
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(World_event_tracker.BASE_COMMAND.then(ClientCommandManager.literal("register")
                    .executes((context) -> {
                        JsonObject requestbody=new JsonObject();
                        HttpRequest getRequest = HttpRequest.newBuilder()
                                .uri(URI.create(World_event_tracker.secrets.get("url").getAsString() + "api/v2/user/wes"))
                                .header("authorization", "Bearer " + World_event_tracker.secrets.get("password").getAsString())
                                .header("Content-Type", "application/json")
                                .header("uuid", Managers.Net.wynn.wynnPlayerInfo.get("uuid").getAsString())
  //                              .version(HttpClient.Version.HTTP_1_1) // remove in final version
                                .GET()
                                .build();
                        NetManager.HTTP_CLIENT.sendAsync(getRequest, HttpResponse.BodyHandlers.ofString()).whenCompleteAsync((stringHttpResponse, throwable) -> {
                            if(!(stringHttpResponse.statusCode()/100==2)) {
                                HttpRequest request = HttpRequest.newBuilder()
                                        .uri(URI.create(World_event_tracker.secrets.get("url").getAsString() + "api/v2/user/"))
                                        .header("Content-Type", "application/json")
                                        .header("Authorization", "Bearer " + World_event_tracker.secrets.get("password").getAsString())
                                        .header("uuid", Managers.Net.wynn.wynnPlayerInfo.get("uuid").getAsString())
//                                        .version(HttpClient.Version.HTTP_1_1) // remove in final version
                                        .POST(HttpRequest.BodyPublishers.ofString(requestbody.toString()))
                                        .build();
                                World_event_tracker.LOGGER.info(requestbody.toString());
                                NetManager.HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString()).whenCompleteAsync((httpResponse, throwable2) -> {
                                    World_event_tracker.LOGGER.info("" + httpResponse.body());

                                    World_event_tracker.LOGGER.info("Registered User");
                                    McUtils.sendLocalMessage(Text.literal("§aRegistered user. Please restart minecraft to connect to the server."), Prepend.DEFAULT.get(), false);
                                });
                            }
                            else{
                                McUtils.sendLocalMessage(Text.literal("§dUser already registered."), Prepend.DEFAULT.get(), false);

                            }
                        });
                        return Command.SINGLE_SUCCESS;
                    })));
        });
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {

            // World event options
            String[] allowedEvents = new String[]{
                    "HaywireDefender", "ApproachingRaid", "SkitteringSpiders", "OvertakenFarm", "ArachnidAmbush", "EncroachingBlaze",
                    "DarkDeacons", "EncroachingDestruction", "CorruptedSpring", "NecromanticSite", "RisenReturn", "EncroachingMisery",
                    "TaintedShoreline", "AeonOrigin", "BowelsoftheRoots", "EncroachingReanimation", "ImproperBurialRites",
                    "Blood-EncrustedMastaba", "EncroachingConflagration", "FailedHunt", "CanineAmbush", "BlazingCombustion",
                    "EncroachingAblation", "RogueWyrmling", "SlimySchism", "SwashbucklingBrawl", "DesperateAmbush",
                    "ABurningMemory", "EncroachingExtinction", "PeculiarGrotto", "LightEmissaries", "UnsettlingEncounters",
                    "VisitfromBeyond", "AbandonedSentinels", "RealmicAntigen", "TerritorialTrolls", "ColossiIngrain", "EnragedEagle",
                    "DespermechOccupation", "DecommissionedWarMachines", "BubblingTerrace", "InfernalCaldera",
                    "MaarAshpit", "ShatteredRoosts", "AhmsMonuments", "IncomprehensibleCynosure", "ShapesintheDark", "AllEyesonMe",
                    "MonumenttoLoss", "PestilentialDownpour", "OtherworldlyExhibition"
            };
            dispatcher.register(World_event_tracker.BASE_COMMAND.then(ClientCommandManager.literal("untrack").then(ClientCommandManager.literal("all").executes(context -> {
                String uuid = Managers.Net.wynn.wynnPlayerInfo.get("uuid").getAsString();

                JsonObject requestBody=new JsonObject();
                try {
                    String updatedList;
                    updatedList = "[]";
                    requestBody.add("uuid", JsonUtils.toJsonElement(uuid));
                    requestBody.add("worldevents", JsonUtils.toJsonElement(updatedList));
                    World_event_tracker.LOGGER.info(requestBody.toString());
                }
                catch (Exception e) {
                    throw new RuntimeException("Failed to fetch world event list", e);

                }
                HttpRequest putRequest = HttpRequest.newBuilder()
                        .uri(URI.create(World_event_tracker.secrets.get("url").getAsString() + "api/v2/user"))
                        .header("authorization", "Bearer " + World_event_tracker.secrets.get("password").getAsString())
                        .header("Content-Type", "application/json")
                        .PUT(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                        .build();
                NetManager.HTTP_CLIENT.sendAsync(putRequest, HttpResponse.BodyHandlers.ofString())
                        .whenCompleteAsync((res, throwable) -> {
                            try {
                                NetUtils.applyDefaultCallback(res, throwable,
                                        (ok) -> McUtils.sendLocalMessage(Text.literal("§aSuccessfully deregistered for ALL events."), Prepend.DEFAULT.get(), false),
                                        (err) -> McUtils.sendLocalMessage(Text.literal("§cYou were already registered for that event."), Prepend.DEFAULT.get(), false));
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        });
                return Command.SINGLE_SUCCESS;
            }))));
            dispatcher.register(World_event_tracker.BASE_COMMAND.then(ClientCommandManager.literal("untrack")
                    .then(ClientCommandManager.literal("RuffTumble")
                            .executes(context -> {
                                String event= "Ruff&Tumble";
                                String uuid = Managers.Net.wynn.wynnPlayerInfo.get("uuid").getAsString();
                                HttpRequest getRequest = HttpRequest.newBuilder()
                                        .uri(URI.create(World_event_tracker.secrets.get("url").getAsString() + "api/v2/user/wes"))
                                        .header("authorization", "Bearer " + World_event_tracker.secrets.get("password").getAsString())
                                        .header("uuid", uuid)
                                        .header("Content-Type", "application/json")
                                        .GET()
                                        .build();

                                JsonObject requestBody = new JsonObject();
                                try {
                                    NetManager.HTTP_CLIENT.sendAsync(getRequest, HttpResponse.BodyHandlers.ofString())
                                            .whenCompleteAsync((res, throwable) -> {
                                                String updatedList="";
                                                String response=res.body();
                                                if (!response.isEmpty() && response.startsWith("[")) {
                                                    updatedList = response.replaceAll("\\s", "").replace("]", "") + ",\"" + event + "\"]";
                                                }
                                                World_event_tracker.LOGGER.info(response);
                                                requestBody.add("uuid", JsonUtils.toJsonElement(uuid));
                                                requestBody.add("worldevents", JsonUtils.toJsonElement(updatedList));
                                                HttpRequest putRequest = HttpRequest.newBuilder()
                                                        .uri(URI.create(World_event_tracker.secrets.get("url").getAsString() + "api/v2/user/ut"))
                                                        .header("authorization", "Bearer " + World_event_tracker.secrets.get("password").getAsString())
                                                        .header("Content-Type", "application/json")
                                                        .PUT(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                                                        .build();

                                                NetManager.HTTP_CLIENT.sendAsync(putRequest, HttpResponse.BodyHandlers.ofString())
                                                        .whenCompleteAsync((res1, throwable1) -> {
                                                            try {
                                                                NetUtils.applyDefaultCallback(res1, throwable1,
                                                                        (ok) -> McUtils.sendLocalMessage(Text.literal("§aSuccessfully unregistered from the event."), Prepend.DEFAULT.get(), false),
                                                                        (err) -> McUtils.sendLocalMessage(Text.literal("§cYou were not registered for that event."), Prepend.DEFAULT.get(), false));
                                                            } catch (Exception e) {
                                                                throw new RuntimeException(e);
                                                            }
                                                        });
                                            });

                                } catch (Exception e) {
                                    throw new RuntimeException("Failed to fetch or modify world event list", e);
                                }



                                return Command.SINGLE_SUCCESS;
                            }))));
            dispatcher.register(World_event_tracker.BASE_COMMAND.then(ClientCommandManager.literal("track").then(ClientCommandManager.literal("all").executes(context -> {
                String uuid = Managers.Net.wynn.wynnPlayerInfo.get("uuid").getAsString();

                JsonObject requestBody=new JsonObject();
                try {
                    String updatedList;
                    updatedList = "[";
                    for (String event : allowedEvents){
                        updatedList+="\""+event+"\",";
                    }
                    updatedList= updatedList.substring(0, updatedList.length()-1)+"]";
                    requestBody.add("uuid", JsonUtils.toJsonElement(uuid));
                    requestBody.add("worldevents", JsonUtils.toJsonElement(updatedList));

                }
                catch (Exception e) {
                    throw new RuntimeException("Failed to fetch world event list", e);

                }
                HttpRequest putRequest = HttpRequest.newBuilder()
                        .uri(URI.create(World_event_tracker.secrets.get("url").getAsString() + "api/v2/user"))
                        .header("authorization", "Bearer " + World_event_tracker.secrets.get("password").getAsString())
                        .header("Content-Type", "application/json")
                        .PUT(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                        .build();
                NetManager.HTTP_CLIENT.sendAsync(putRequest, HttpResponse.BodyHandlers.ofString())
                        .whenCompleteAsync((res, throwable) -> {
                            try {
                                NetUtils.applyDefaultCallback(res, throwable,
                                        (ok) -> McUtils.sendLocalMessage(Text.literal("§aSuccessfully registered for ALL events."), Prepend.DEFAULT.get(), false),
                                        (err) -> McUtils.sendLocalMessage(Text.literal("§cYou were already registered for that event."), Prepend.DEFAULT.get(), false));
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        });
                return Command.SINGLE_SUCCESS;
            }))));
            dispatcher.register(World_event_tracker.BASE_COMMAND.then(ClientCommandManager.literal("track")
                    .then(ClientCommandManager.literal("RuffTumble")
                            .executes(context -> {
                                String event = "Ruff&Tumble";
                                String uuid = Managers.Net.wynn.wynnPlayerInfo.get("uuid").getAsString();

                                // Fetch existing tracked events
                                HttpRequest getRequest = HttpRequest.newBuilder()
                                        .uri(URI.create(World_event_tracker.secrets.get("url").getAsString() + "api/v2/user/wes"))
                                        .header("authorization", "Bearer " + World_event_tracker.secrets.get("password").getAsString())
                                        .header("uuid", uuid)
                                        .GET()
                                        .build();

                                JsonObject requestBody = new JsonObject();
                                try {
                                    NetManager.HTTP_CLIENT.sendAsync(getRequest, HttpResponse.BodyHandlers.ofString()).whenCompleteAsync((stringHttpResponse, throwable) -> {
                                        String response = stringHttpResponse.body();
                                        String updatedList;
                                        if (!response.isEmpty() && response.startsWith("[") && !response.equals("[]")) {
                                            updatedList = response.replaceAll("\\s", "").replace("]", "") + ",\"" + event + "\"]";
                                        } else {
                                            updatedList = "[\"" + event + "\"]";
                                        }
                                        requestBody.add("uuid", JsonUtils.toJsonElement(uuid));
                                        requestBody.add("worldevents", JsonUtils.toJsonElement(updatedList));
                                        // Send update
                                        HttpRequest putRequest = HttpRequest.newBuilder()
                                                .uri(URI.create(World_event_tracker.secrets.get("url").getAsString() + "api/v2/user"))
                                                .header("authorization", "Bearer " + World_event_tracker.secrets.get("password").getAsString())
                                                .header("Content-Type", "application/json")
                                                .PUT(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                                                .build();

                                        NetManager.HTTP_CLIENT.sendAsync(putRequest, HttpResponse.BodyHandlers.ofString())
                                                .whenCompleteAsync((res, throwable1) -> {
                                                    try {
                                                        NetUtils.applyDefaultCallback(res, throwable1,
                                                                (ok) -> McUtils.sendLocalMessage(Text.literal("§aSuccessfully registered for the event."), Prepend.DEFAULT.get(), false),
                                                                (err) -> McUtils.sendLocalMessage(Text.literal("§cYou were already registered for that event."), Prepend.DEFAULT.get(), false));
                                                    } catch (Exception e) {
                                                        throw new RuntimeException(e);
                                                    }
                                                });
                                    });

                                } catch (Exception e) {
                                    throw new RuntimeException("Failed to fetch world event list", e);
                                }



                                return Command.SINGLE_SUCCESS;
                            }))));

            // Suggestion provider
            SuggestionProvider worldEventSuggestions = (context, builder) -> CommandSource.suggestMatching(allowedEvents, builder);

            // Register track command
            LiteralArgumentBuilder<FabricClientCommandSource> trackCommand = ClientCommandManager.literal("track")
                    .then(ClientCommandManager.argument("world_event", StringArgumentType.string())
                            .suggests(worldEventSuggestions)
                            .executes(context -> {
                                String event = StringArgumentType.getString(context, "world_event");
                                String uuid = Managers.Net.wynn.wynnPlayerInfo.get("uuid").getAsString();
                                boolean valid = false;
                                for(String currentevent:allowedEvents){
                                    if(event.equals(currentevent)){
                                        valid = true;
                                    }
                                }
                                if(!valid){
                                    McUtils.sendLocalMessage(Text.literal("§cInvalid event."), Prepend.DEFAULT.get(), false);
                                    return Command.SINGLE_SUCCESS;
                                }
                                // Fetch existing tracked events
                                HttpRequest getRequest = HttpRequest.newBuilder()
                                        .uri(URI.create(World_event_tracker.secrets.get("url").getAsString() + "api/v2/user/wes"))
                                        .header("authorization", "Bearer " + World_event_tracker.secrets.get("password").getAsString())
                                        .header("uuid", uuid)
                                        .GET()
                                        .build();

                                JsonObject requestBody = new JsonObject();
                                try {
                                    NetManager.HTTP_CLIENT.sendAsync(getRequest, HttpResponse.BodyHandlers.ofString()).whenCompleteAsync((res, throwable) -> {
                                        String response=res.body();
                                        String updatedList;
                                        if (!response.isEmpty() && response.startsWith("[")&&!response.equals("[]")) {
                                            updatedList = response.replaceAll("\\s", "").replace("]", "") + ",\"" + event + "\"]";
                                        } else {
                                            updatedList = "[\"" + event + "\"]";
                                        }
                                        requestBody.add("uuid", JsonUtils.toJsonElement(uuid));
                                        requestBody.add("worldevents", JsonUtils.toJsonElement(updatedList));
                                        // Send update
                                        HttpRequest putRequest = HttpRequest.newBuilder()
                                                .uri(URI.create(World_event_tracker.secrets.get("url").getAsString() + "api/v2/user"))
                                                .header("authorization", "Bearer " + World_event_tracker.secrets.get("password").getAsString())
                                                .header("Content-Type", "application/json")
                                                .PUT(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                                                .build();

                                        NetManager.HTTP_CLIENT.sendAsync(putRequest, HttpResponse.BodyHandlers.ofString())
                                                .whenCompleteAsync((res1, throwable1) -> {
                                                    try {
                                                        NetUtils.applyDefaultCallback(res1, throwable1,
                                                                (ok) -> McUtils.sendLocalMessage(Text.literal("§aSuccessfully registered for the event."), Prepend.DEFAULT.get(), false),
                                                                (err) -> McUtils.sendLocalMessage(Text.literal("§cYou were already registered for that event."), Prepend.DEFAULT.get(), false));
                                                    } catch (Exception e) {
                                                        throw new RuntimeException(e);
                                                    }
                                                });
                                    });

                                } catch (Exception e) {
                                    throw new RuntimeException("Failed to fetch world event list", e);
                                }



                                return Command.SINGLE_SUCCESS;
                            }));

            // Register untrack command
            LiteralArgumentBuilder<FabricClientCommandSource> untrackCommand = ClientCommandManager.literal("untrack")
                    .then(ClientCommandManager.argument("world_event", StringArgumentType.string())
                            .suggests(worldEventSuggestions)
                            .executes(context -> {
                                String event = StringArgumentType.getString(context, "world_event");
                                String uuid = Managers.Net.wynn.wynnPlayerInfo.get("uuid").getAsString();
                                boolean valid = false;
                                for(String currentevent:allowedEvents){
                                    if(event.equals(currentevent)){
                                        valid = true;
                                    }
                                }
                                if(!valid){
                                    McUtils.sendLocalMessage(Text.literal("§cInvalid event."), Prepend.DEFAULT.get(), false);
                                    return Command.SINGLE_SUCCESS;
                                }
                                HttpRequest getRequest = HttpRequest.newBuilder()
                                        .uri(URI.create(World_event_tracker.secrets.get("url").getAsString() + "api/v2/user/wes"))
                                        .header("authorization", "Bearer " + World_event_tracker.secrets.get("password").getAsString())
                                        .header("uuid", uuid)
                                        .header("Content-Type", "application/json")
                                        .GET()
                                        .build();

                                JsonObject requestBody = new JsonObject();
                                try {
                                    NetManager.HTTP_CLIENT.sendAsync(getRequest, HttpResponse.BodyHandlers.ofString()).whenCompleteAsync((stringHttpResponse, throwable) -> {
                                        String response=stringHttpResponse.body();
                                        String updatedList="";
                                        if (!response.isEmpty() && response.startsWith("[")) {
                                            updatedList = response.replaceAll("\\s", "").replace("]", "") + ",\"" + event + "\"]";
                                        }
                                        World_event_tracker.LOGGER.info(response);
                                        requestBody.add("uuid", JsonUtils.toJsonElement(uuid));
                                        requestBody.add("worldevents", JsonUtils.toJsonElement(updatedList));

                                        HttpRequest putRequest = HttpRequest.newBuilder()
                                                .uri(URI.create(World_event_tracker.secrets.get("url").getAsString() + "api/v2/user/ut"))
                                                .header("authorization", "Bearer " + World_event_tracker.secrets.get("password").getAsString())
                                                .header("Content-Type", "application/json")
                                                .PUT(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                                                .build();

                                        NetManager.HTTP_CLIENT.sendAsync(putRequest, HttpResponse.BodyHandlers.ofString())
                                                .whenCompleteAsync((res, throwable1) -> {
                                                    try {
                                                        NetUtils.applyDefaultCallback(res, throwable1,
                                                                (ok) -> McUtils.sendLocalMessage(Text.literal("§aSuccessfully unregistered from the event."), Prepend.DEFAULT.get(), false),
                                                                (err) -> McUtils.sendLocalMessage(Text.literal("§cYou were not registered for that event."), Prepend.DEFAULT.get(), false));
                                                    } catch (Exception e) {
                                                        throw new RuntimeException(e);
                                                    }
                                                });

                                    });

                                } catch (Exception e) {
                                    throw new RuntimeException("Failed to fetch or modify world event list", e);
                                }

                                return Command.SINGLE_SUCCESS;
                            }));

            // Register everything under base command
            dispatcher.register(World_event_tracker.BASE_COMMAND.then(trackCommand).then(untrackCommand));
        });


            try {
                InputStream inputStream = getClass().getClassLoader().getResourceAsStream("WEsecrets.json");
                if (inputStream == null) {
                    throw new IOException("Secret file not found");
                }
                // Attempt to parse the JSON from the input stream
                secrets = JsonParser.parseReader(new InputStreamReader(inputStream)).getAsJsonObject();
                World_event_tracker.LOGGER.info("Secrets loaded successfully.");
            } catch (Exception e) {
                // Log the error and inform the user that the configuration is corrupt
                World_event_tracker.LOGGER.error("Failed to load or parse secrets configuration: ", e);
                // Optionally, reset the secrets to a safe default or empty configuration
                secrets = new JsonObject();
                // Notify the user in-game about the corruption and potential steps (e.g. reinstall or reset config)
                McUtils.sendLocalMessage(
                        Text.literal("§cConfiguration corruption detected. Please reinstall or reset your mod configuration."),
                        Prepend.DEFAULT.get(), true);
            }

        Handlers.init();
        Managers.Connection.init();
        Managers.Net.init();
        Managers.Feature.init();
        Models.WorldState.init();
    }
}