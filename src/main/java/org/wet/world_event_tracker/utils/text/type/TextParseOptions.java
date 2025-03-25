package org.wet.world_event_tracker.utils.text.type;

public class TextParseOptions {
    public static final TextParseOptions DEFAULT = new TextParseOptions();

    public String newline;
    public String formatCode;
    public boolean extractUsernames;

    TextParseOptions() {
        newline = "";
        formatCode = "§";
        extractUsernames = false;
    }

    private TextParseOptions copy() {
        TextParseOptions out = new TextParseOptions();
        out.newline = this.newline;
        out.formatCode = this.formatCode;
        out.extractUsernames = this.extractUsernames;
        return out;
    }

    public TextParseOptions withNewline(String newline) {
        TextParseOptions out = this.copy();
        out.newline = newline;
        return out;
    }

    public TextParseOptions withFormatCode(String formatCode) {
        TextParseOptions out = this.copy();
        out.formatCode = formatCode;
        return out;
    }

    public TextParseOptions withExtractUsernames(boolean extractUsernames) {
        TextParseOptions out = this.copy();
        out.extractUsernames = extractUsernames;
        return out;
    }
}
