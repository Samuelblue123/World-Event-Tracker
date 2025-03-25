package org.wet.world_event_tracker.utils;

import com.google.gson.*;

public class JsonUtils {
    public static final Gson GSON = new GsonBuilder().create();

    public static JsonElement toJsonElement(String convert) {
        return GSON.fromJson(convert, JsonElement.class);
    }

    public static JsonObject toJsonObject(String convert) {
        return GSON.fromJson(convert, JsonObject.class);
    }

    public static JsonArray toJsonArray(String convert) { return GSON.fromJson(convert, JsonArray.class); }

}