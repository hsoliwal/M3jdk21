/* *****************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.m3.collections.guava.escape;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Random;
import java.util.function.Function;
import java.util.function.IntFunction;
import org.junit.jupiter.api.Test;

/**
 * CharEscaper and UnicodeEscaper grow their destination geometrically while a capacity lane replays
 * the old growth points: against verbatim pre-change copies (RefCharEscaper, RefUnicodeEscaper,
 * RefPlatform), equal output or equal exception (class and message) for random escape tables,
 * replacement lengths up to 3000, surrogates, re-entrant escapers and thread-local buffer reuse;
 * and allocation stays linear in the output where the old growth was quadratic.
 */
//FLLDCIM: LEETCODIFY — differential proof for geometric escaper growth (dynamic array)
class EscaperGrowthLanesTest {
    private static final String ALPHABET = "ab<>&\"'#xyz é中";

    private static IntFunction<char[]> table(Random random, int maxLength) {
        char[][] rows = new char[ALPHABET.length() + 2][];
        for (int i = 0; i < rows.length; i++) {
            if (random.nextInt(3) > 0) {
                rows[i] = new char[random.nextInt(maxLength + 1)];
                for (int k = 0; k < rows[i].length; k++) {
                    rows[i][k] = (char) ('A' + random.nextInt(26));
                }
            }
        }
        return cp -> {
            int at = ALPHABET.indexOf(cp);
            return at >= 0 ? rows[at] : cp > 0xFFFF ? rows[rows.length - 1] : null;
        };
    }

    private static String text(Random random, int length) {
        StringBuilder text = new StringBuilder(length + 2);
        for (int i = 0; i < length; i++) {
            int pick = random.nextInt(40);
            if (pick == 0) {
                text.appendCodePoint(0x1F600 + random.nextInt(40));
            } else if (pick == 1 && random.nextInt(8) == 0) {
                text.append(random.nextBoolean() ? '\ud800' : '\udc00');
            } else {
                text.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
            }
        }
        return text.toString();
    }

    private static String outcome(Function<String, String> escaper, String input) {
        try {
            return "=" + escaper.apply(input);
        } catch (RuntimeException | Error e) {
            return "!" + e.getClass().getName() + ": " + e.getMessage();
        }
    }

    static final class PatchedChar extends CharEscaper {
        private final IntFunction<char[]> table;

        PatchedChar(IntFunction<char[]> table) {
            this.table = table;
        }

        @Override
        protected char[] escape(char c) {
            return table.apply(c);
        }
    }

    static final class OldChar extends RefCharEscaper {
        private final IntFunction<char[]> table;

        OldChar(IntFunction<char[]> table) {
            this.table = table;
        }

        @Override
        protected char[] escape(char c) {
            return table.apply(c);
        }
    }

    static final class PatchedUnicode extends UnicodeEscaper {
        private final IntFunction<char[]> table;

        PatchedUnicode(IntFunction<char[]> table) {
            this.table = table;
        }

        @Override
        protected char[] escape(int cp) {
            return table.apply(cp);
        }
    }

    static final class OldUnicode extends RefUnicodeEscaper {
        private final IntFunction<char[]> table;

        OldUnicode(IntFunction<char[]> table) {
            this.table = table;
        }

        @Override
        protected char[] escape(int cp) {
            return table.apply(cp);
        }
    }

    private static void lockstep(long seed, boolean unicode) {
        Random random = new Random(seed);
        int[] maxLengths = {0, 1, 2, 6, 40, 700, 3000};
        for (int spec = 0; spec < 160; spec++) {
            IntFunction<char[]> table = table(random, maxLengths[spec % maxLengths.length]);
            Function<String, String> patched = unicode
                    ? new PatchedUnicode(table)::escape : new PatchedChar(table)::escape;
            Function<String, String> old = unicode
                    ? new OldUnicode(table)::escape : new OldChar(table)::escape;
            for (int k = 0; k < 12; k++) {
                String input = text(random, random.nextInt(k % 4 == 0 ? 3000 : 120));
                assertEquals(outcome(old, input), outcome(patched, input), "spec " + spec);
            }
        }
    }

    @Test
    void charEscaperMatchesOldCopy() {
        lockstep(0x51A5E1L, false);
    }

    @Test
    void unicodeEscaperMatchesOldCopy() {
        lockstep(0x51A5E2L, true);
    }

    /** An escape that itself escapes a string reuses the thread-local buffer mid-loop. */
    @Test
    void reentrantEscapesMatchOldCopy() {
        Random random = new Random(0x51A5E3L);
        for (int spec = 0; spec < 200; spec++) {
            IntFunction<char[]> base = table(random, 1 + spec % 50);
            CharEscaper[] patched = new CharEscaper[1];
            RefCharEscaper[] old = new RefCharEscaper[1];
            patched[0] = new PatchedChar(c -> c == '#' ? patched[0].escape("<a&>").toCharArray()
                    : base.apply(c));
            old[0] = new OldChar(c -> c == '#' ? old[0].escape("<a&>").toCharArray()
                    : base.apply(c));
            for (int k = 0; k < 10; k++) {
                String input = text(random, random.nextInt(400));
                assertEquals(outcome(old[0]::escape, input), outcome(patched[0]::escape, input));
            }
        }
    }

    /** A nextEscapeIndex that may step back makes the old loop copy a negative run. */
    static final class ScriptedUnicode extends UnicodeEscaper {
        private final IntFunction<char[]> table;
        private final Random script;

        ScriptedUnicode(IntFunction<char[]> table, long seed) {
            this.table = table;
            this.script = new Random(seed);
        }

        @Override
        protected char[] escape(int cp) {
            return table.apply(cp);
        }

        @Override
        protected int nextEscapeIndex(CharSequence csq, int start, int end) {
            int pick = script.nextInt(4);
            return pick == 0 ? Math.max(0, start - 1 - script.nextInt(4))
                    : pick == 1 ? end : super.nextEscapeIndex(csq, start, end);
        }
    }

    static final class ScriptedOldUnicode extends RefUnicodeEscaper {
        private final IntFunction<char[]> table;
        private final Random script;

        ScriptedOldUnicode(IntFunction<char[]> table, long seed) {
            this.table = table;
            this.script = new Random(seed);
        }

        @Override
        protected char[] escape(int cp) {
            return table.apply(cp);
        }

        @Override
        protected int nextEscapeIndex(CharSequence csq, int start, int end) {
            int pick = script.nextInt(4);
            return pick == 0 ? Math.max(0, start - 1 - script.nextInt(4))
                    : pick == 1 ? end : super.nextEscapeIndex(csq, start, end);
        }
    }

    @Test
    void steppingBackMatchesOldFailures() {
        Random random = new Random(0x51A5E4L);
        int failures = 0;
        for (int spec = 0; spec < 3000; spec++) {
            IntFunction<char[]> table = table(random, 1 + spec % 900);
            String input = text(random, random.nextInt(spec % 5 == 0 ? 1500 : 60));
            String old = outcome(new ScriptedOldUnicode(table, spec)::escape, input);
            assertEquals(old, outcome(new ScriptedUnicode(table, spec)::escape, input), input);
            failures += old.startsWith("!") ? 1 : 0;
        }
        assertTrue(failures > 0, "no scripted case reached an old failure");
    }

    /** The script returns 1000 and then 961: the old loop copies a negative run and overflows. */
    @Test
    void steppingBackReplaysOldOverflow() {
        char[] x = new char[30];
        char[] y = new char[270];
        IntFunction<char[]> table = cp -> cp == 'x' ? x : cp == 'y' ? y : null;
        char[] text = "a".repeat(1200).toCharArray();
        text[1000] = 'x';
        text[961] = 'y';
        String input = new String(text);
        int[][] scripts = {{1000, 961}, {1000, 961}};
        UnicodeEscaper patched = new UnicodeEscaper() {
            int call;

            @Override
            protected char[] escape(int cp) {
                return table.apply(cp);
            }

            @Override
            protected int nextEscapeIndex(CharSequence csq, int start, int end) {
                return call < 2 ? scripts[0][call++] : end;
            }
        };
        RefUnicodeEscaper old = new RefUnicodeEscaper() {
            int call;

            @Override
            protected char[] escape(int cp) {
                return table.apply(cp);
            }

            @Override
            protected int nextEscapeIndex(CharSequence csq, int start, int end) {
                return call < 2 ? scripts[1][call++] : end;
            }
        };
        String expected = outcome(old::escape, input);
        assertTrue(expected.startsWith("!java.lang.ArrayIndexOutOfBoundsException"), expected);
        assertEquals(expected, outcome(patched::escape, input));
    }

    private static String shadowCap(Class<?> owner, int cap, int need, int remaining) {
        try {
            Method atom = owner.getDeclaredMethod("shadowCap", int.class, int.class, int.class);
            atom.setAccessible(true);
            return "=" + atom.invoke(null, cap, need, remaining);
        } catch (java.lang.reflect.InvocationTargetException e) {
            return "!" + e.getCause().getClass().getName() + ": " + e.getCause().getMessage();
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    /** The old rule: keep the length while it fits, else need plus the pad, overflow asserting. */
    private static String oldGrowth(int cap, int need, int pad) {
        if (cap >= need) {
            return "=" + cap;
        }
        int size = need + pad;
        return size < 0 ? "!java.lang.AssertionError: Cannot increase internal buffer any further"
                : "=" + size;
    }

    @Test
    void capacityLaneReplaysOldGrowth() {
        Random random = new Random(0x51A5E5L);
        for (int k = 0; k < 20_000; k++) {
            int need = k % 3 == 0 ? Integer.MAX_VALUE - random.nextInt(4096) : random.nextInt(1 <<
                    20);
            int cap = k % 4 == 0 ? need : k % 4 == 1 ? need - 1 - random.nextInt(64) : need +
                    random.nextInt(64);
            int remaining = random.nextInt(k % 2 == 0 ? 1 << 20 : 64);
            assertEquals(oldGrowth(cap, need, 2 * remaining),
                    shadowCap(CharEscaper.class, cap, need, remaining), "char " + k);
            assertEquals(oldGrowth(cap, need, remaining + 32),
                    shadowCap(UnicodeEscaper.class, cap, need, remaining), "unicode " + k);
        }
    }

    private static long allocatedBytes() {
        try {
            Object bean = Class.forName("java.lang.management.ManagementFactory")
                    .getMethod("getThreadMXBean").invoke(null);
            Method read = Class.forName("com.sun.management.ThreadMXBean")
                    .getMethod("getCurrentThreadAllocatedBytes");
            return (long) read.invoke(bean);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    /** 2000 chars each escaped to 2000 chars: the old additive pads regrew about 1000 times. */
    @Test
    void growthAllocatesLinearOutput() {
        char[] wide = new char[2000];
        IntFunction<char[]> table = cp -> cp == '<' ? wide : null;
        String input = "<".repeat(2000);
        for (Function<String, String> escaper : java.util.List.<Function<String, String>>of(
                new PatchedChar(table)::escape, new PatchedUnicode(table)::escape)) {
            escaper.apply(input);
            long before = allocatedBytes();
            String out = escaper.apply(input);
            long allocated = allocatedBytes() - before;
            assertEquals(4_000_000, out.length());
            assertTrue(allocated < 64L * 1024 * 1024, "allocated " + allocated + " bytes");
        }
    }
}
