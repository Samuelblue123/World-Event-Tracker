package org.wet.world_event_tracker.handlers.chat;


import net.minecraft.text.Text;

import org.wet.world_event_tracker.World_event_tracker;
import org.wet.world_event_tracker.components.Handler;
import org.wet.world_event_tracker.handlers.chat.event.ChatMessageReceived;
import org.wet.world_event_tracker.mc.event.WynnChatMessage;
import org.wet.world_event_tracker.models.worldState.event.WorldStateEvents;
import org.wet.world_event_tracker.models.worldState.type.WorldState;
import org.wet.world_event_tracker.utils.McUtils;
import org.wet.world_event_tracker.utils.text.TextUtils;
import org.wet.world_event_tracker.utils.text.type.TextParseOptions;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

public final class ChatHandler extends Handler {
    private static final Pattern EMPTY_LINE_PATTERN = Pattern.compile("^\\s*(§r|À+)?\\s*$");
    private static final Pattern NPC_CONFIRM_PATTERN =
            Pattern.compile("^ *§[47]Press §[cf](SNEAK|SHIFT) §[47]to continue$");
    private static final Pattern NPC_SELECT_PATTERN =
            Pattern.compile("^ *§[47cf](Select|CLICK) §[47cf]an option (§[47])?to continue$");
    private static final int CHAT_SCREEN_TICK_DELAY = 1;

    private ArrayList<Text> collectedLines = new ArrayList<>();
    private long chatScreenTicks = 0;
    private LinkedList<Text> lastCollected = new LinkedList<>();

    @Override
    public void init() {
        WynnChatMessage.EVENT.register(this::onWynnMessage);
        WorldStateEvents.CHANGE.register(this::onConnectionChange);
    }

    private void onConnectionChange(WorldState state) {
        lastCollected.clear();
        collectedLines.clear();
        chatScreenTicks = 0;
    }

    private void onWynnMessage(Text message) {
        assert McUtils.mc().world != null;
        long currentTicks = McUtils.mc().world.getTime();

        List<Text> lines = TextUtils.splitLines(message);
        handleLines(lines, message, currentTicks);
    }

    private synchronized void handleLines(List<Text> lines, Text message, long currentTicks) {
        if (lines.size() > 1 || (message.getString().isEmpty() && (currentTicks <= chatScreenTicks + CHAT_SCREEN_TICK_DELAY))) {
            if (currentTicks <= chatScreenTicks + CHAT_SCREEN_TICK_DELAY) {
                collectedLines.addAll(lines);
            } else {
                if (chatScreenTicks != 0) {
                    processCollected();
                }
                collectedLines = new ArrayList<>(lines);
                chatScreenTicks = currentTicks;
            }
        } else {
            if (chatScreenTicks != 0) {
                processCollected();
            }
            lastCollected.addLast(message);
            while (lastCollected.size() > 5) lastCollected.removeFirst();
            postChatLine(message);
        }
    }

    // add back last real message for first chat screen checking
    private synchronized void processCollected() {
        ArrayList<Text> filteredCollected;
        chatScreenTicks = 0;
        for (Text line : collectedLines) {
            World_event_tracker.LOGGER.info("pre collected: {}", TextUtils.parsePlain(line));
        }
        World_event_tracker.LOGGER.info("pre collected spacer: \n\n\n");
        if (NPC_CONFIRM_PATTERN.matcher(TextUtils.parseStyled(collectedLines.getLast(), TextParseOptions.DEFAULT)).find()) {
            if (collectedLines.size() < 4) {
                World_event_tracker.LOGGER.warn("Unable to safely remove 4 lines for npc confirm pattern.");
                return;
            }
            for (int i = 0; i < 4; i++) {
                collectedLines.removeLast();
            }
        } else if (NPC_SELECT_PATTERN.matcher(TextUtils.parseStyled(collectedLines.getLast(), TextParseOptions.DEFAULT)).find()) {
            try {
                collectedLines.removeLast();
                do {
                    collectedLines.removeLast();
                } while (!EMPTY_LINE_PATTERN.matcher(TextUtils.parseStyled(collectedLines.getLast(), TextParseOptions.DEFAULT)).find());
                collectedLines.removeLast();
                collectedLines.removeLast();
                collectedLines.removeLast();
            } catch (Exception e) {
                World_event_tracker.LOGGER.warn("Unable to safely remove lines for npc select pattern.");
                return;
            }
        } else if (collectedLines.size() > 4 &&
                EMPTY_LINE_PATTERN.matcher(TextUtils.parsePlain(collectedLines.getLast())).find() &&
                EMPTY_LINE_PATTERN.matcher(TextUtils.parsePlain(collectedLines.get(collectedLines.size() - 2))).find() &&
                EMPTY_LINE_PATTERN.matcher(TextUtils.parsePlain(collectedLines.get(collectedLines.size() - 3))).find() &&
                EMPTY_LINE_PATTERN.matcher(TextUtils.parsePlain(collectedLines.get(collectedLines.size() - 4))).find()
        ) {
            collectedLines.removeLast();
            collectedLines.removeLast();
            collectedLines.removeLast();
            collectedLines.removeLast();

        } else if (collectedLines.size() >= 2 && EMPTY_LINE_PATTERN.matcher(TextUtils.parsePlain(collectedLines.get(collectedLines.size() - 2))).find() && !EMPTY_LINE_PATTERN.matcher(TextUtils.parsePlain(collectedLines.getLast())).find()) {
            collectedLines.removeLast();
            collectedLines.removeLast();
        }
        filteredCollected = new ArrayList<>();
        for (Text line : collectedLines) {
            if (!EMPTY_LINE_PATTERN.matcher(TextUtils.parsePlain(line)).find()) filteredCollected.add(line);
        }
        collectedLines = filteredCollected;

        if (collectedLines.isEmpty()) return;

        LinkedList<Text> newLines = new LinkedList<>();
        int candidate = -1;
        int offset = 0;
        for (int start = collectedLines.size() - 1; start >= 0; start--) {
            boolean works = true;
            for (int i = 0; i < lastCollected.size(); i++) {
                int index = 0;
                works = true;
                while (i + index < lastCollected.size() && start + index < collectedLines.size()) {
                    if (!TextUtils.parsePlain(lastCollected.get(i + index)).equals(TextUtils.parsePlain(collectedLines.get(start + index)))) {
                        works = false;
                        break;
                    }
                    ++index;
                }
                if (i + index < lastCollected.size()) {
                    works = false;
                    continue;
                }
                if (works) {
                    candidate = start;
                    offset = i;
                    break;
                }
            }
            if (!works && candidate != -1) break;
        }
//        World_event_tracker.LOGGER.info("i stuff cand: {} ofset: {}", candidate, offset);
//        for (Text line : lastCollected) {
//            World_event_tracker.LOGGER.info("last collected: {}", TextUtils.parsePlain(line));
//        }
//        World_event_tracker.LOGGER.info("last collected spacer: \n\n\n");
        for (Text line : collectedLines) {
            World_event_tracker.LOGGER.info("collected: {}", TextUtils.parsePlain(line));
        }
        World_event_tracker.LOGGER.info("collected spacer: \n\n\n");

        if (candidate != -1) {
            filteredCollected = new ArrayList<>();
            for (int j = candidate; j < collectedLines.size(); j++) {
                filteredCollected.add(collectedLines.get(j));
            }
            collectedLines = filteredCollected;
            int extra = collectedLines.size() - lastCollected.size() + offset;
            for (int j = collectedLines.size() - 1; j >= collectedLines.size() - extra; j--) {
                newLines.addFirst(collectedLines.get(j));
            }
        }

        lastCollected = new LinkedList<>(collectedLines);
        collectedLines.clear();
        while (lastCollected.size() > 5) lastCollected.removeFirst();
        processNewLines(newLines);
    }

    private void processNewLines(LinkedList<Text> newLines) {
        for (Text line : newLines) {
            World_event_tracker.LOGGER.info("newline: {}", TextUtils.parseStyled(line, TextParseOptions.DEFAULT));
        }
        if (!newLines.isEmpty())
            World_event_tracker.LOGGER.info("newline spacer: \n\n\n");
        for (Text line : newLines) {
            postChatLine(line);
        }
    }

    private void postChatLine(Text line) {
        ChatMessageReceived.EVENT.invoker().interact(line);
    }
}