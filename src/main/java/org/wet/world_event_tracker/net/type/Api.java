package org.wet.world_event_tracker.net.type;

import org.wet.world_event_tracker.World_event_tracker;
import org.wet.world_event_tracker.net.event.NetEvents;

import java.util.List;

public abstract class Api {
    public final String name;
    private final List<Class<? extends Api>> dependencies;
    protected String baseURL;
    private boolean enabled = false;
    private int missingDeps;

    // TODO move api get posts here
    // TODO improve dependencies system to show which dependencies specifically are unloaded to ensure no duplications
    protected Api(String name, List<Class<? extends Api>> dependencies) {
        this.name = name;
        this.dependencies = dependencies;
        missingDeps = dependencies.size();
        NetEvents.LOADED.register(this::onApiLoaded);
        NetEvents.DISABLED.register(this::onApiDisabled);
    }

    private void onApiLoaded(Api api) {
        if (this.depends(api)) dependencyLoaded();
    }

    private void onApiDisabled(Api api) {
        if (this.depends(api)) dependencyUnloaded();
    }

    public boolean depends(Api api) {
        return dependencies.contains(api.getClass());
    }

    private void dependencyLoaded() {
        --missingDeps;
        if (missingDeps == 0) {
            ready();
        }
    }

    private void dependencyUnloaded() {
        ++missingDeps;
        unready();
    }

    protected void ready() {
        enable();
    }

    protected void unready() {
        disable();
    }

    public void enable() {
        if (!enabled) {
            World_event_tracker.LOGGER.info("enabling {}", name);
            enabled = true;
            NetEvents.LOADED.invoker().interact(this);
        }
    }

    public void disable() {
        if (enabled) {
            World_event_tracker.LOGGER.warn("disabling {} service", name);
            enabled = false;
            NetEvents.DISABLED.invoker().interact(this);
        }
    }

    public boolean isDisabled() {
        return !enabled;
    }

    public abstract void init();
}
