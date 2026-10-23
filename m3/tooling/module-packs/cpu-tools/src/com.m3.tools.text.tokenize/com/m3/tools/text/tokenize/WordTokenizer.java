// Copyright 2026 Hitesh Soliwal and contributors
// Modified 2026-10-18: map the pinned Synexia CPU tool into its existing M3JDK21 module name.
// SPDX-License-Identifier: Apache-2.0
package com.m3.tools.text.tokenize;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
/** JDK word-break semantics; no per-language model and no hidden tokenizer state. */
public final class WordTokenizer {
    private WordTokenizer() { }
    public static List<String> words(String text) {
        return words(text, () -> { });
    }
    /** Exact JDK BreakIterator word semantics with cooperative boundary checkpoints. */
    public static List<String> words(String text, Runnable checkpoint) {
        Objects.requireNonNull(text);
        Objects.requireNonNull(checkpoint, "checkpoint");
        checkpoint.run();
        BreakIterator iterator = BreakIterator.getWordInstance(Locale.ROOT);
        iterator.setText(text);
        List<String> result = new ArrayList<>();
        int start = iterator.first();
        int boundaries = 0;
        for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
            if ((boundaries++ & 1023) == 0) checkpoint.run();
            String span = text.substring(start, end);
            if (span.codePoints().anyMatch(Character::isLetterOrDigit)) result.add(span);
        }
        checkpoint.run();
        return List.copyOf(result);
    }
    public static void main(String[] args) {
        if (args.length == 1 && args[0].equals("--self-test")) {
            if (!words("Hello, 42 world").equals(List.of("Hello","42","world")))
                throw new AssertionError("word boundaries");
            System.out.println("M3_JMOD_TEST_PASS text.tokenize");
        } else { words(String.join(" ", args)).forEach(System.out::println); }
    }
}
