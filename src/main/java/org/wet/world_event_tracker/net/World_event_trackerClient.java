package org.wet.world_event_tracker.net;


import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.wet.world_event_tracker.World_event_tracker;
import org.wet.world_event_tracker.components.Managers;
import org.wet.world_event_tracker.net.type.Api;
import org.wet.world_event_tracker.utils.JsonUtils;
import org.wet.world_event_tracker.utils.McUtils;
import org.wet.world_event_tracker.utils.NetUtils;
import org.wet.world_event_tracker.utils.type.Prepend;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class World_event_trackerClient extends Api {
    private static World_event_trackerClient instance;
    private final Text retryMessage = Text.literal("Could not connect to guild server. Click ")
            .setStyle(Style.EMPTY.withColor(Formatting.RED))
            .append(Text.literal("here").setStyle(
                    Style.EMPTY.withUnderline(true).withColor(Formatting.RED)
                            .withClickEvent(
                                    new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                            "/retryLastFailed")))).
            append(Text.literal(" to retry.")
                    .setStyle(Style.EMPTY.withColor(Formatting.RED)));
    private final Text successMessage = Text.literal("Success!").setStyle(Style.EMPTY.withColor(Formatting.GREEN));
    private final String apiBasePath = "api/v2/";
    public String guildPrefix = "";
    public String uuid = "none";
    private String token;
    private JsonElement validationKey;
    private JsonObject wynnPlayerInfo;

    public World_event_trackerClient() {
        super("guild", List.of(WynnApiClient.class));
        instance = this;
    }

    public static World_event_trackerClient getInstance() {
        return instance;
    }

    public String getToken(boolean refresh) {
        if (token == null || refresh) getGuildServerToken();
        return token;
    }

    private boolean getGuildServerToken() {
        if (wynnPlayerInfo != null) {
            try {
                JsonObject requestBody = new JsonObject();
                requestBody.add("validationKey", validationKey);
                requestBody.add("username", JsonUtils.toJsonElement(McUtils.playerName()));
                HttpRequest.Builder builder = HttpRequest.newBuilder()
                        .uri(URI.create(World_event_tracker.secrets.get("url").getAsString()+ apiBasePath + "guilds/auth/get-token/" + uuid))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()));
                if (World_event_tracker.isDevelopment()) builder.version(HttpClient.Version.HTTP_1_1);
                HttpResponse<String> response = NetManager.HTTP_CLIENT.send(builder.build(),
                        HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() / 100 == 2) {
                    World_event_tracker.LOGGER.info("Api token refresh call successful: {}", response.statusCode());
                    JsonObject responseObject = JsonUtils.toJsonObject(response.body());
                    token = responseObject.get("token").getAsString();
                    return true;
                }
                World_event_tracker.LOGGER.error("get token error: status {} {}", response.statusCode(), response.body());
            } catch (JsonSyntaxException e) {
                World_event_tracker.LOGGER.error("Json syntax exception: {}", (Object) e.getStackTrace());
            } catch (Exception e) {
                World_event_tracker.LOGGER.error("get token error: {}", e.getMessage());
            }
        } else {
            World_event_tracker.LOGGER.warn("wynn player not initialized, can't refresh token");
        }
        return false;
    }

    private void applyCallback(CompletableFuture<HttpResponse<String>> callback, HttpResponse<String> response, Throwable exception) {
        if (exception != null) {
            assert Formatting.RED.getColorValue() != null;
//            McUtils.sendLocalMessage(Text.literal("Fatal API error: " + exception + " " + exception.getMessage())
//                    .withColor(Formatting.RED.getColorValue()), Prepend.DEFAULT.get(), false);
            callback.completeExceptionally(exception);
            return;
        }
        callback.complete(response);
    }

    public CompletableFuture<HttpResponse<String>> get(String path) {
        path = apiBasePath + path;
        CompletableFuture<HttpResponse<String>> out = new CompletableFuture<>();
        if (isDisabled()) {
            World_event_tracker.LOGGER.warn("skipped api get because api service were crashed");
            McUtils.sendLocalMessage(Text.literal("A request was skipped.")
                    .setStyle(Style.EMPTY.withColor(Formatting.YELLOW)), Prepend.DEFAULT.get(), false);
            return out;
        }
        HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(World_event_tracker.secrets.get("url").getAsString()+ path))
                .header("Authorization", "bearer " + World_event_tracker.secrets.get("password").getAsString())
                .GET();
        if (World_event_tracker.isDevelopment()) builder.version(HttpClient.Version.HTTP_1_1);
        CompletableFuture<HttpResponse<String>> response = tryToken(builder);
        response.whenCompleteAsync((res, exception) -> {
                    World_event_tracker.LOGGER.info("api GET completed: res {} exception {}", res.statusCode(), exception);
                    applyCallback(out, res, exception);
                    // else {
//                        if (res.statusCode() / 100 == 2)
//                            out.complete(JsonUtils.toJsonElement(res.body()));
//                        else {
//                            if (handleError)
//                                checkError(res, builder, false);
//                            else out.complete(JsonUtils.toJsonElement(res.body()));
//                        }
//                    }
                }
        );
        return out;
    }

    public CompletableFuture<HttpResponse<String>> post(String path, JsonObject body) {
        CompletableFuture<HttpResponse<String>> out = new CompletableFuture<>();
        path = apiBasePath + path;
        if (isDisabled()) {
            World_event_tracker.LOGGER.warn("skipped api post because api service were crashed");
            McUtils.sendLocalMessage(Text.literal("A request was skipped.")
                    .setStyle(Style.EMPTY.withColor(Formatting.YELLOW)), Prepend.DEFAULT.get(), false);
            return out;
        }
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(World_event_tracker.secrets.get("url").getAsString()+ path))
                .headers("Content-Type", "application/json", "Authorization",
                        "bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()));
        if (World_event_tracker.isDevelopment()) builder.version(HttpClient.Version.HTTP_1_1);
        CompletableFuture<HttpResponse<String>> response = tryToken(builder);
        response.whenCompleteAsync((res, exception) -> {
            World_event_tracker.LOGGER.info("api POST completed: res {} exception {}", res, exception);
            applyCallback(out, res, exception);
        });
        return out;
    }

    public CompletableFuture<HttpResponse<String>> delete(String path) {
        CompletableFuture<HttpResponse<String>> out = new CompletableFuture<>();
        path = apiBasePath + path;
        if (isDisabled()) {
            World_event_tracker.LOGGER.warn("Skipped api delete because api services weren't enabled");
            McUtils.sendLocalMessage(Text.literal("A request was skipped.")
                    .setStyle(Style.EMPTY.withColor(Formatting.YELLOW)), Prepend.DEFAULT.get(), false);
            return out;
        }
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(World_event_tracker.secrets.get("url").getAsString()+ path))
                .header("Authorization", "bearer " + token)
                .DELETE();
        if (World_event_tracker.isDevelopment()) builder.version(HttpClient.Version.HTTP_1_1);
        CompletableFuture<HttpResponse<String>> response = tryToken(builder);
        response.whenCompleteAsync((res, exception) -> {
            World_event_tracker.LOGGER.info("api DELETE completed: res {} exception {}", res, exception);
            applyCallback(out, res, exception);
        });
        return out;
    }

    private CompletableFuture<HttpResponse<String>> tryToken(HttpRequest.Builder builder) {
        CompletableFuture<HttpResponse<String>> response = NetManager.HTTP_CLIENT.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString());
        CompletableFuture<HttpResponse<String>> out = new CompletableFuture<>();
        response.whenCompleteAsync((res, exception) -> {
            if (exception != null) {
                out.completeExceptionally(exception);
            } else {
                if (res.statusCode() == 401) {
                    World_event_tracker.LOGGER.info("Refreshing api token");
                    if (!getGuildServerToken()) {
                        out.complete(res);
                        return;
                    }
                    builder.setHeader("Authorization", "bearer " + World_event_tracker.secrets.get("password").getAsString());
                    try {
                        HttpResponse<String> res2 = NetManager.HTTP_CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString());
                        out.complete(res2);
                    } catch (Exception e) {
                        out.completeExceptionally(e);
                    }
                } else {
                    out.complete(res);
                }
            }
        });
        return out;
    }

    private void successMessage() {
        McUtils.sendLocalMessage(successMessage, Prepend.DEFAULT.get(), false);
    }

    

    @Override
    public void init() {
    }

    @Override
    protected void ready() {
        wynnPlayerInfo = Managers.Net.wynn.wynnPlayerInfo;
        try {
            guildPrefix = wynnPlayerInfo.get("guild").getAsJsonObject().get("prefix").getAsString();
            uuid = wynnPlayerInfo.get("uuid").getAsString();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(World_event_tracker.secrets.get("url").getAsString()+"api/v2/user"))
                    .header("Authorization", "Bearer " + World_event_tracker.secrets.get("password").getAsString())
                    .header("uuid", uuid)
                    .GET()
                    .version(HttpClient.Version.HTTP_1_1) //remove this in final version
                    .build();
            NetManager.HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .whenCompleteAsync((response, error) -> {
                        try {
                            NetUtils.applyDefaultCallback(response, error, (resOK) -> {
                                World_event_tracker.LOGGER.info("successfully loaded base url");
                                super.enable();
                            }, (e) -> {
                                String guildString = null;
                                if (wynnPlayerInfo.get("guild").isJsonObject()) {
                                    guildString = wynnPlayerInfo.get("guild").getAsJsonObject().get("prefix")
                                            .getAsString();
                                }
                                Managers.Net.apiCrash(Text.literal(
                                                "Couldn't fetch base url for server of guild \"" + guildString + "\". " +
                                                        "Talk to a chief about setting one up for your guild. If you believe this is a mistake, check logs for more details.")
                                        .setStyle(Style.EMPTY.withColor(Formatting.RED)), this);
                                World_event_tracker.LOGGER.error("Fetch World Event Tracker exception: {}", e);
                            });
                        } catch (Exception e) {
                            String guildString = null;
                            if (wynnPlayerInfo.get("guild").isJsonObject()) {
                                guildString = wynnPlayerInfo.get("guild").getAsJsonObject().get("prefix").getAsString();
                            }
                            Managers.Net.apiCrash(Text.literal(
                                            "Couldn't fetch base url for server of guild \"" + guildString + "\". " +
                                                    "Talk to a chief about setting one up for your guild. If you believe this is a mistake, check logs for more details.")
                                    .setStyle(Style.EMPTY.withColor(Formatting.RED)), this);
                            World_event_tracker.LOGGER.error("Fetch guild exception: {} {}", e, e.getMessage());

                        }
                    });
        } catch (Exception e) {
            String guildString = null;
            if (wynnPlayerInfo.get("guild").isJsonObject()) {
                guildString = wynnPlayerInfo.get("guild").getAsJsonObject().get("prefix").getAsString();
            }
            World_event_tracker.LOGGER.error(e.toString());
            Managers.Net.apiCrash(Text.literal(
                            "Couldn't fetch base url for server of guild \"" + guildString + "\". " +
                                    "Talk to a chief about setting one up for your guild.")
                    .setStyle(Style.EMPTY.withColor(Formatting.RED)), this);
        }
    }

    protected void unready() {
        validationKey = null;
        wynnPlayerInfo = null;
        uuid = null;
        token = null;
        super.unready();
    }

}
