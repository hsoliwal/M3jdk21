package com.synexia.mindex.experiment;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Lossless boundary tokenizer. Every punctuation/whitespace code point is retained. */
public final class Tokenizer {
    private Tokenizer() { }
    public static List<String> split(String text) {
        Objects.requireNonNull(text, "text");
        List<String> result = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int cp = text.codePointAt(start);
            int end = start + Character.charCount(cp);
            if (word(cp)) {
                while (end < text.length() && word(text.codePointAt(end))) {
                    end += Character.charCount(text.codePointAt(end));
                }
            }
            result.add(text.substring(start, end));
            start = end;
        }
        return List.copyOf(result);
    }
    private static boolean word(int cp) {
        int type = Character.getType(cp);
        return Character.isLetterOrDigit(cp) || cp == '_' || cp == '\''
                || type == Character.NON_SPACING_MARK || type == Character.COMBINING_SPACING_MARK;
    }
}
