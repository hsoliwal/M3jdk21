// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.regexcursor;

import java.util.Random;
import java.util.regex.Matcher;

/** Seeded generators and traces shared by the cursor tests (donor scenario: test_rope.rs). */
final class M3RegexCursorFixtures {
    private static final String[] ATOMS = {
        "a", "b", "c", "x", ".", "\\d", "\\w", "\\s", "\\D", "\\W", "\\S", "[abc]", "[^a]", "[a-c]",
        "[^\\d]", "[\\dx]", "[-a]", "[a-]", "\\.", "\\-", "\\t", "\\n", "\\x41", "\\u00e9", "1", " ", "é"
    };
    private static final String[] QUANTIFIERS = {
        "*", "+", "?", "*?", "+?", "??", "{2}", "{1,3}", "{0,2}", "{2,}", "{1,2}?", "{0}"
    };
    private static final String[] ANCHORS = {"^", "$", "\\A", "\\z", "\\Z"};
    private static final String INPUT_UNITS = "aaabbbcccx1 \n\ré A_.-\t";

    private M3RegexCursorFixtures() {}

    static String pattern(Random random, int depth, boolean anchors) {
        int pick = random.nextInt(depth <= 0 ? 4 : 9);
        switch (pick) {
            case 0, 1, 2:
                return quantify(random, ATOMS[random.nextInt(ATOMS.length)]);
            case 3:
                return anchors && random.nextInt(4) == 0 ? ANCHORS[random.nextInt(ANCHORS.length)] : "a";
            case 4, 5:
                return pattern(random, depth - 1, anchors) + pattern(random, depth - 1, anchors);
            case 6:
                return pattern(random, depth - 1, anchors) + "|" + pattern(random, depth - 1, anchors);
            case 7:
                return quantify(random, "(" + pattern(random, depth - 1, anchors) + ")");
            default:
                return quantify(random, "(?:" + pattern(random, depth - 1, anchors) + ")");
        }
    }

    private static String quantify(Random random, String atom) {
        return random.nextInt(3) == 0 ? atom + QUANTIFIERS[random.nextInt(QUANTIFIERS.length)] : atom;
    }

    static String input(Random random, int maxLength, boolean surrogates) {
        int length = random.nextInt(maxLength + 1);
        StringBuilder out = new StringBuilder(length);
        for (int index = 0; index < length; index++) {
            if (surrogates && random.nextInt(40) == 0) {
                out.append("😀");
            } else {
                out.append(INPUT_UNITS.charAt(random.nextInt(INPUT_UNITS.length())));
            }
        }
        return out.toString();
    }

    /** Random strictly increasing cuts in (0, length); every third plan uses 1-unit chunks. */
    static int[] cuts(Random random, int length) {
        if (length <= 1) return new int[0];
        if (random.nextInt(3) == 0) {
            int[] all = new int[length - 1];
            for (int index = 0; index < all.length; index++) all[index] = index + 1;
            return all;
        }
        return random.ints(random.nextInt(length), 1, length).sorted().distinct().toArray();
    }

    /** Random M3 rope spelling {@code text}: atoms of both widths, slices and a random tree. */
    static M3RopeString rope(Random random, String text) {
        if (random.nextInt(5) == 0 && !text.isEmpty()) {
            M3RopeString padded = tree(random, "zz" + text + "ā");
            return padded.slice(2, 2 + text.length());
        }
        return tree(random, text);
    }

    private static M3RopeString tree(Random random, String text) {
        if (text.length() <= 3 || random.nextInt(6) == 0) return leaf(random, text);
        int split = 1 + random.nextInt(text.length() - 1);
        return tree(random, text.substring(0, split)).concat(tree(random, text.substring(split)));
    }

    private static M3RopeString leaf(Random random, String text) {
        if (random.nextBoolean()) return M3RopeString.atom(text);
        String wide = "Ā" + text + "q";
        return M3RopeString.atom(wide).slice(1, 1 + text.length());
    }

    static M3ChunkCursor cursor(Random random, String text) {
        return random.nextBoolean()
                ? new M3StringCursor(rope(random, text))
                : M3CharSequenceCursor.chunked(text, cuts(random, text.length()));
    }

    static String trace(Matcher m, int length) {
        StringBuilder out = new StringBuilder("F:");
        for (int guard = 0; guard <= length + 1 && m.find(); guard++) {
            groups(out, m.groupCount(), m::start, m::end);
        }
        out.append(" M:");
        if (m.matches()) groups(out, m.groupCount(), m::start, m::end);
        out.append(" L:");
        if (m.lookingAt()) groups(out, m.groupCount(), m::start, m::end);
        return out.toString();
    }

    static String trace(M3CursorMatcher m, int length) {
        StringBuilder out = new StringBuilder("F:");
        for (int guard = 0; guard <= length + 1 && m.find(); guard++) {
            groups(out, m.groupCount(), m::start, m::end);
        }
        out.append(" M:");
        if (m.matches()) groups(out, m.groupCount(), m::start, m::end);
        out.append(" L:");
        if (m.lookingAt()) groups(out, m.groupCount(), m::start, m::end);
        return out.toString();
    }

    private static void groups(StringBuilder out, int count,
            java.util.function.IntUnaryOperator start, java.util.function.IntUnaryOperator end) {
        out.append('[');
        for (int group = 0; group <= count; group++) {
            out.append(start.applyAsInt(group)).append(',').append(end.applyAsInt(group)).append(';');
        }
        out.append(']');
    }
}
