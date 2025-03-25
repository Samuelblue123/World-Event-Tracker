package org.wet.world_event_tracker.utils;


import net.minecraft.text.Style;
import net.minecraft.util.Formatting;

public class ColourUtils {
    public static Style GREEN = getColourStyle(Formatting.GREEN);
    public static Style RED = getColourStyle(Formatting.RED);
    public static Style YELLOW = getColourStyle(Formatting.YELLOW);
    public static Style GOLD = getColourStyle(Formatting.GOLD);
    public static Style AQUA = getColourStyle(Formatting.AQUA);
    public static Style LIGHT_PURPLE = getColourStyle(Formatting.LIGHT_PURPLE);

    public static Style getColourStyle(Formatting formatting) {
        return Style.EMPTY.withColor(formatting);
    }

}
