// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fast-search/src/test/java/jdk/internal/m3/search/M3FastSearchCoreSuite.java (blob c85bda31c093) by M3_SEARCH_RELOCATE v2.
package jdk.m3.search;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;

/** Dependency-free randomized oracle for the recovered primitive byte-search runtime. */
public final class M3FastSearchCoreSuite {
    private M3FastSearchCoreSuite() {}

    public static void main(String[] args) {
        namedCases();
        boundedCursorArithmetic();
        randomized();
        ownedSnapshots();
        buffers();
        fallback();
        System.out.println("PASS: recovered m3-fast-search primitive runtime");
    }

    private static void namedCases() {
        check(new M3KmpSearch(bytes("aba")), bytes("xxabaaba"), 0, 2);
        check(new M3BoyerMooreHorspoolSearch(bytes("needle-value")),
                bytes("prefix-needle-value-suffix"), 0, 7);
        check(new M3KmpSearch(new byte[] {(byte) 0xff, 0, 1}),
                new byte[] {5, (byte) 0xff, 0, 1, 9}, 0, 1);
        check(new M3BoyerMooreHorspoolSearch(bytes("abc")), bytes("xxabc"), -10, 2);
        check(new M3BoyerMooreHorspoolSearch(bytes("abc")), bytes("abc"), 4, -1);
        check(new M3KmpSearch(bytes("aba")), bytes("ababa"), 1, 2);
        check(new M3BoyerMooreHorspoolSearch(bytes("aba")), bytes("ababa"), 1, 2);
        check(new M3JavaAdaptiveSearchEngine().compile(new byte[] {(byte) 0xff}),
                new byte[] {0, (byte) 0xff}, 1, 1);
        checkLast(new M3KmpSearch(bytes("aba")), bytes("ababa"), Integer.MAX_VALUE, 2);
        checkLast(new M3KmpSearch(bytes("aba")), bytes("ababa"), 1, 0);
        checkLast(new M3BoyerMooreHorspoolSearch(bytes("aba")),
                bytes("ababa"), Integer.MAX_VALUE, 2);
        checkLast(new M3BoyerMooreHorspoolSearch(bytes("abc")), bytes("xxabcabc"), 4, 2);
        checkLast(new M3BoyerMooreHorspoolSearch(bytes("abc")), bytes("xxabcabc"), -1, -1);
    }

    private static void randomized() {
        Random random = new Random(0xF4575EA2CL);
        M3SearchEngine engine = new M3JavaAdaptiveSearchEngine();
        for (int sample = 0; sample < 20_000; sample++) {
            byte[] haystack = new byte[random.nextInt(96)];
            byte[] needle = new byte[1 + random.nextInt(24)];
            random.nextBytes(haystack);
            random.nextBytes(needle);
            int from = random.nextInt(haystack.length + 12) - 6;
            int expected = oracle(haystack, needle, from);
            int reverseFrom = random.nextInt(haystack.length + 12) - 6;
            int expectedLast = oracleLast(haystack, needle, reverseFrom);
            try (M3CompiledSearch compiled = engine.compile(needle)) {
                int actual = compiled.indexOf(haystack, from);
                if (actual != expected) {
                    throw new AssertionError(
                            "sample=" + sample + " expected=" + expected + " actual=" + actual
                                    + " strategy=" + compiled.strategy()
                                    + " from=" + from
                                    + " h=" + Arrays.toString(haystack)
                                    + " n=" + Arrays.toString(needle));
                }
                int actualLast = compiled.lastIndexOf(haystack, reverseFrom);
                if (actualLast != expectedLast) {
                    throw new AssertionError(
                            "sample=" + sample + " reverse expected=" + expectedLast
                                    + " actual=" + actualLast
                                    + " strategy=" + compiled.strategy()
                                    + " from=" + reverseFrom);
                }
            }
            check(new M3KmpSearch(needle), haystack, from, expected);
            check(new M3BoyerMooreHorspoolSearch(needle), haystack, from, expected);
            checkLast(new M3KmpSearch(needle), haystack, reverseFrom, expectedLast);
            checkLast(new M3BoyerMooreHorspoolSearch(needle), haystack, reverseFrom, expectedLast);
        }
    }

    /**
     * Independent long-arithmetic oracle exercises near-2 GiB logical cursor boundaries
     * without allocating gigabyte buffers. All executable searches use real small arrays.
     */
    private static void boundedCursorArithmetic() {
        int checked = 0;
        int[] lengths = {1, 2, 3, 17, 1024, 65536, Integer.MAX_VALUE};
        int[] shifts = {1, 2, 3, 13, 257, 65536, Integer.MAX_VALUE};
        for (int length : lengths) {
            int[] cursors = {0, length / 2, length - 1};
            for (int cursor : cursors) {
                for (int shift : shifts) {
                    int actual = M3BoyerMooreHorspoolSearch.boundedNextCursor(cursor, shift, length);
                    int expected = (int) Math.min((long) length, (long) cursor + shift);
                    if (actual != expected || actual <= cursor || actual > length) {
                        throw new AssertionError(
                                "bad bounded cursor length=" + length + " cursor="
                                        + cursor + " shift=" + shift);
                    }
                    checked++;
                }
            }
        }
        if (checked != lengths.length * 3 * shifts.length) {
            throw new AssertionError("cursor test coverage");
        }
    }

    private static void ownedSnapshots() {
        M3SearchEngine engine = new M3JavaAdaptiveSearchEngine();
        for (byte[] input : new byte[][] {
                bytes("abababab"), bytes("needle-value"), new byte[] {(byte) 0xff, 0, 1}}) {
            byte[] expected = input.clone();
            M3CompiledSearch[] compiled = {
                    engine.compile(input), new M3KmpSearch(input),
                    new M3BoyerMooreHorspoolSearch(input)};
            input[0] ^= 0x7f;
            byte[] haystack = new byte[expected.length + 3];
            System.arraycopy(expected, 0, haystack, 2, expected.length);
            for (M3CompiledSearch search : compiled) {
                try (search) {
                    if (search.indexOf(haystack, 0) != 2
                            || search.needleLength() != expected.length) {
                        throw new AssertionError("compiled strategy must own its pattern snapshot");
                    }
                }
            }
        }
    }

    private static void buffers() {
        byte[] payload = bytes("xxababa/needle-value/needle-valueyy");
        M3SearchEngine engine = new M3JavaAdaptiveSearchEngine();
        for (boolean direct : new boolean[] {false, true}) {
            ByteBuffer buffer = direct
                    ? ByteBuffer.allocateDirect(payload.length + 6)
                    : ByteBuffer.allocate(payload.length + 6);
            for (int index = 0; index < payload.length; index++) {
                buffer.put(3 + index, payload[index]);
            }
            buffer.position(3);
            buffer.limit(3 + payload.length);
            int position = buffer.position();
            int limit = buffer.limit();
            try (M3CompiledSearch kmp = engine.compile(bytes("aba"));
                 M3CompiledSearch horspool = engine.compile(bytes("needle-value"))) {
                if (kmp.indexOf(buffer, 0) != 2
                        || kmp.lastIndexOf(buffer, Integer.MAX_VALUE) != 4
                        || horspool.indexOf(buffer, 0) != 8
                        || horspool.lastIndexOf(buffer, Integer.MAX_VALUE) != 21) {
                    throw new AssertionError("buffer search parity");
                }
            }
            if (buffer.position() != position || buffer.limit() != limit) {
                throw new AssertionError("search must not mutate buffer state");
            }
        }
    }

    private static void fallback() {
        System.clearProperty("m3.native.enabled");
        M3SearchEngine best = M3SearchEngines.best();
        if (!best.available() || !"JAVA_ADAPTIVE".equals(best.name())) {
            throw new AssertionError("Java fallback unavailable");
        }
        if (new M3JniSearchEngine().available()) {
            throw new AssertionError("JNI must remain opt-in");
        }
    }

    private static void check(
            M3CompiledSearch search, byte[] haystack, int from, int expected) {
        try (search) {
            int actual = search.indexOf(haystack, from);
            if (actual != expected) {
                throw new AssertionError(
                        search.strategy() + " expected=" + expected + " actual=" + actual);
            }
        }
    }

    private static void checkLast(
            M3CompiledSearch search, byte[] haystack, int from, int expected) {
        try (search) {
            int actual = search.lastIndexOf(haystack, from);
            if (actual != expected) {
                throw new AssertionError(
                        search.strategy() + " reverse expected=" + expected + " actual=" + actual);
            }
        }
    }

    private static int oracleLast(byte[] haystack, byte[] needle, int fromIndex) {
        if (fromIndex < 0 || needle.length > haystack.length) return -1;
        for (int start = Math.min(fromIndex, haystack.length - needle.length);
                start >= 0;
                start--) {
            boolean same = true;
            for (int index = 0; index < needle.length; index++) {
                if (haystack[start + index] != needle[index]) {
                    same = false;
                    break;
                }
            }
            if (same) return start;
        }
        return -1;
    }

    private static int oracle(byte[] haystack, byte[] needle, int fromIndex) {
        int from = Math.max(0, fromIndex);
        for (int start = from; start + needle.length <= haystack.length; start++) {
            boolean same = true;
            for (int i = 0; i < needle.length; i++) {
                if (haystack[start + i] != needle[i]) {
                    same = false;
                    break;
                }
            }
            if (same) return start;
        }
        return -1;
    }

    private static byte[] bytes(String value) {
        return value.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
}
