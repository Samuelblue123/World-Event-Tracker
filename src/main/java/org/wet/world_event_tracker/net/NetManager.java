package org.wet.world_event_tracker.net;


import net.minecraft.text.Text;
import org.wet.world_event_tracker.World_event_tracker;
import org.wet.world_event_tracker.net.type.Api;
import org.wet.world_event_tracker.utils.McUtils;
import org.wet.world_event_tracker.utils.type.Prepend;

import java.net.http.HttpClient;
import java.util.HashMap;
import java.util.Map;

public class NetManager {
    public static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private final Map<String, Api> apis = new HashMap<>();
    public World_event_trackerClient user = new World_event_trackerClient();
    public SocketIOClient socket = new SocketIOClient();
    public WynnApiClient wynn = new WynnApiClient();

    public void apiCrash(Text message, Api api) {
        McUtils.sendLocalMessage(message, Prepend.DEFAULT.get(), false);
        api.disable();
    }

    @Deprecated
    public <T extends Api> T getApi(String name, Class<T> apiClass) {
        Api api = apis.get(name);
        if (apiClass.isInstance(api)) return apiClass.cast(api);
        World_event_tracker.LOGGER.error("Requested api \"{}\" does not exist/has not been loaded.", name);
        return null;
    }

    public void init() {
        registerApi(wynn);
        registerApi(user);
        registerApi(socket);
        initApis();
    }

    private <T extends Api> void registerApi(T api) {
        apis.put(api.name, api);
    }

    private void initApis() {
        for (Api a : apis.values()) {
            if (a.isDisabled()) a.init();
        }
    }
}
