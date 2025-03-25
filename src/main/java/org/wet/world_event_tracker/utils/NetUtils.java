package org.wet.world_event_tracker.utils;


import com.google.gson.JsonElement;
import net.minecraft.text.Text;
import org.wet.world_event_tracker.World_event_tracker;
import org.wet.world_event_tracker.utils.type.Prepend;

import java.net.http.HttpResponse;
import java.util.function.Consumer;

public class NetUtils {
    /**
     * @param response  http response object from api request
     * @param exception exception from api request
     * @param onSuccess consumer for successful api calls (2xx)
     * @param onFailed  consumer for unsuccessful api calls
     * @throws Exception throws an exception on fatal api errors
     */
    public static void applyDefaultCallback(HttpResponse<String> response, Throwable exception, Consumer<JsonElement> onSuccess, Consumer<String> onFailed) throws Exception {
        if (exception != null) {
            throw (Exception) exception;
        }
        if (response.statusCode()==637) {
            McUtils.sendLocalMessage(Text.literal("§cYou are not registered on the system, please type \"/wet register\" to register"), Prepend.DEFAULT.get(), false);

        }else {
            if (response.statusCode() / 100 == 2) {
                onSuccess.accept(JsonUtils.toJsonElement(response.body()));
            } else {
                onFailed.accept(JsonUtils.toJsonObject(response.body()).get("error").getAsString());
            }
        }
    }

    public static Consumer<String> defaultFailed(String name, boolean feedback) {
        return (error) -> {
            if (feedback)
                McUtils.sendLocalMessage(Text.literal("§c" + name + " failed. Reason: " + error), Prepend.DEFAULT.get(), false);
            World_event_tracker.LOGGER.error("{} error: {}", name, error);
        };
    }

    public static void defaultException(String name, Exception e) {
        McUtils.sendLocalMessage(Text.literal("§cSomething went wrong. Check logs for more details."), Prepend.DEFAULT.get(), false);
        World_event_tracker.LOGGER.error("{} exception: {} {}", name, e, e.getMessage());
    }
}
