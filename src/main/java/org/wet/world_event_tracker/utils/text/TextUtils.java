package org.wet.world_event_tracker.utils.text;


import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextHandler;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.MutableText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.wet.world_event_tracker.mc.mixin.accessors.ChatHudAccessorInvoker;
import org.wet.world_event_tracker.utils.McUtils;
import org.wet.world_event_tracker.utils.text.type.TextParseOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

public class TextUtils {
    private static final Pattern BLOCK_MARKER_PATTERN = Pattern.compile("^(§.)?\uDAFF\uDFFC\uE001\uDB00\uDC06(§.)?$");

    public static List<Text> splitLines(Text message) {
        ArrayList<Text> splitted = new ArrayList<>();
        MutableText currentPart = Text.empty();
        for (Text part : message.getWithStyle(message.getStyle())) {
            if (part.getString().equals("\n")) {
                splitted.add(currentPart);
                currentPart = Text.empty();
            } else {
                currentPart.append(part);
            }
        }
        if (!currentPart.equals(Text.empty())) splitted.add(currentPart);
        return splitted;
    }


    public static String parseStyled(StringVisitable text, TextParseOptions options) {
        TextVisitors.first = true;
        TextVisitors.options = options;
        TextVisitors.currentVisit = new StringBuilder();
        text.visit(TextVisitors.STYLED_VISITOR, Style.EMPTY);
        return TextVisitors.currentVisit.toString();
    }

    public static String parsePlain(StringVisitable text) {
        TextVisitors.currentVisit = new StringBuilder();
        text.visit(TextVisitors.PLAIN_VISITOR, Style.EMPTY);
        return TextVisitors.currentVisit.toString();
    }

    public static Text stringVisitableToText(StringVisitable visitable) {
        MutableText out = Text.empty();
        visitable.visit((style, asString) -> {
            out.append(Text.literal(asString).setStyle(style));
            return Optional.empty();
        }, Style.EMPTY);
        return out;
    }

    public static Text toBlockMessage(Text text, Style prependStyle) {
        MinecraftClient client = MinecraftClient.getInstance();
        ChatHud chatHud = client.inGameHud.getChatHud();
        ChatHudAccessorInvoker chatHudAccessorInvoker = (ChatHudAccessorInvoker) chatHud;
        TextHandler textHandler = client.textRenderer.getTextHandler();
        List<StringVisitable> lines = textHandler.wrapLines(text, chatHudAccessorInvoker.invokeGetWidth(), text.getStyle(), Text.literal("\uDAFF\uDFFC\uE001\uDB00\uDC06")
                .append(" ").setStyle(prependStyle));
        MutableText out = (MutableText) stringVisitableToText(lines.getFirst());
        for (int i = 1; i < lines.size(); ++i) {
            out.append("\n");
            out.append(stringVisitableToText(lines.get(i)));
        }

        return out;
    }

    public static String highlightUser(String message) {
        return message.replaceAll("(?i)(" + McUtils.playerName() + ")", "§e$1§d");
    }

    static class TextVisitors {
        static StringBuilder currentVisit;
        public static final StringVisitable.StyledVisitor<String> PLAIN_VISITOR = (style, asString) -> {
            currentVisit.append(asString.replaceAll("§.", ""));
            return Optional.empty();
        };
        static boolean first = false;
        static boolean afterBlockMarker;
        static TextParseOptions options;
        public static final StringVisitable.StyledVisitor<String> STYLED_VISITOR = (style, asString) -> {
            if (options.extractUsernames && style.getHoverEvent() != null) {
                handleStylesWithHover(style, asString);
            } else {
                handleStyles(style, asString);
            }
            return Optional.empty();
        };

        private static void handleStylesWithHover(Style style, String asString) {
            assert style.getHoverEvent() != null;
            if (style.getHoverEvent().getValue(style.getHoverEvent().getAction()) instanceof Text) {
                List<Text> onHover = ((Text) Objects.requireNonNull(
                        style.getHoverEvent().getValue(style.getHoverEvent().getAction()))).getSiblings();
                if (asString.indexOf('/') == -1) {
                    if (onHover != null) {
                        if (onHover.size() > 2 && onHover.get(1).getString() != null && Objects.requireNonNull(
                                onHover.get(1).getString()).contains("nickname is")) {
                            handleStyles(style.withItalic(false), onHover.getFirst().getString());
                        } else if (!onHover.isEmpty() && onHover.getFirst().getString() != null && onHover.getFirst()
                                .getString().contains("real username is")) {
                            if (onHover.size() > 1) {
                                handleStyles(style.withItalic(false), onHover.get(1).getString());
                            } else {
                                handleStyles(style.withItalic(false), onHover.getFirst().getSiblings().getFirst()
                                        .getString());
                            }
                        }
                    }
                }
            } else {
                handleStyles(style, asString);
            }
        }

        private static void handleStyles(Style style, String asString) {
            if (BLOCK_MARKER_PATTERN.matcher(asString).find() && !first) {
                afterBlockMarker = true;
                return;
            }
            if (!afterBlockMarker) {
                if (style.getColor() != null) {
                    int colorIndex = 0;
                    for (Formatting format : Formatting.values()) {
                        if (format.getColorValue() != null && format.getColorValue()
                                .equals(style.getColor().getRgb())) {
                            colorIndex = format.getColorIndex();
                            break;
                        }
                    }
                    TextVisitors.currentVisit.append(options.formatCode)
                            .append(Objects.requireNonNull(Formatting.byColorIndex(colorIndex)).getCode());
                }
                if (style.isBold()) {
                    TextVisitors.currentVisit.append(options.formatCode).append(Formatting.BOLD.getCode());
                }
                if (style.isItalic()) {
                    TextVisitors.currentVisit.append(options.formatCode).append(Formatting.ITALIC.getCode());
                }
                if (style.isUnderlined()) {
                    TextVisitors.currentVisit.append(options.formatCode).append(Formatting.UNDERLINE.getCode());
                }
                if (style.isStrikethrough()) {
                    TextVisitors.currentVisit.append(options.formatCode).append(Formatting.STRIKETHROUGH.getCode());
                }
                if (style.isObfuscated()) {
                    TextVisitors.currentVisit.append(options.formatCode).append(Formatting.OBFUSCATED.getCode());
                }
            } else {
                afterBlockMarker = false;
                asString = asString.substring(1);
            }
            TextVisitors.currentVisit.append(asString.replaceAll("\\n", options.newline)
                    .replaceAll("§", options.formatCode));
            if (first) first = false;
        }
    }
}
