package org.wet.world_event_tracker.utils;

import org.wet.world_event_tracker.World_event_tracker;

import java.io.File;

public class FileUtils {
    public static void mkdir(File dir) {
        if (dir.isDirectory()) return;
        if (!dir.mkdirs()) {
            World_event_tracker.LOGGER.error("couldn't make directory {}", dir);
        }
    }
}
