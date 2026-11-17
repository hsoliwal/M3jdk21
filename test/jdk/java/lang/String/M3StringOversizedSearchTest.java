/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 */
/*
 * @test id=default
 * @summary Flat builders search oversized needles without changing their storage
 * @run main/othervm M3StringOversizedSearchTest
 * @run main/othervm -XX:-CompactStrings M3StringOversizedSearchTest
 */
/*
 * @test id=canonical
 * @summary Canonical needles above the precompute budget use exact bounded search
 * @modules java.base/java.lang:+open
 * @run main/othervm -Xint -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage
 *      M3StringOversizedSearchTest canonical
 * @run main/othervm -Xint -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage
 *      -XX:-CompactStrings M3StringOversizedSearchTest canonical
 */
/*
 * @test id=refused
 * @summary Refused admission preserves ordinary builder search semantics
 * @modules java.base/java.lang:+open
 * @run main/othervm -Xint -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage
 *      -Dm3.string.pool.maxBytes=0 M3StringOversizedSearchTest refused
 */

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

public final class M3StringOversizedSearchTest {
    private static long checks;
    private static Method storage;
    private static Field lane;
    private static boolean canonical;
    private static Method canonicalForward, canonicalReverse;
    private static Method directForward, directReverse;

    private static void equal(int actual, int wanted) {
        checks++;
        if (actual != wanted) throw new AssertionError(actual + " != " + wanted);
    }

    // Independent array KMP oracle: never calls String or builder search, including under M3.
    private static int expected(char[] text, char[] needle, int from, boolean backwards) {
        int n = text.length, m = needle.length;
        int start = backwards ? Math.min(from, n - m) : Math.max(from, 0);
        if (backwards && start < 0) return -1;
        if (m == 0) return Math.min(start, n);
        if (!backwards && start > n - m) return -1;
        int[] links = new int[m];
        for (int i = 1, k = 0; i < m; i++) {
            char value = unit(needle, i, backwards);
            while (k > 0 && value != unit(needle, k, backwards)) k = links[k - 1];
            if (value == unit(needle, k, backwards)) k++;
            links[i] = k;
        }
        int first = backwards ? n - m - start : start;
        for (int i = first, k = 0; i < n; i++) {
            char value = unit(text, i, backwards);
            while (k > 0 && value != unit(needle, k, backwards)) k = links[k - 1];
            if (value == unit(needle, k, backwards)) k++;
            if (k == m) return backwards ? n - 1 - i : i - m + 1;
        }
        return -1;
    }

    private static char unit(char[] array, int index, boolean backwards) {
        return array[backwards ? array.length - 1 - index : index];
    }

    private static void scenario(char[] text, char[] needle) throws Exception {
        String pattern = new String(needle);
        if (storage != null && needle.length > 8192) {
            boolean admitted = storage.invoke(pattern) != null;
            if (admitted != canonical) throw new AssertionError("Unexpected needle admission");
        }
        StringBuilder builder = new StringBuilder().append(text);
        StringBuffer buffer = new StringBuffer().append(text);
        int capacity = builder.capacity(), bufferCapacity = buffer.capacity();
        if (lane != null && (lane.get(builder) != null || lane.get(buffer) != null)) {
            throw new AssertionError("Test requires genuinely flat source builders");
        }
        Object canonicalSource = canonical && text.length > 64
                ? storage.invoke(new String(text)) : null;
        if (canonical && text.length > 64 && canonicalSource == null) {
            throw new AssertionError("Canonical-source test was not admitted");
        }
        Object canonicalNeedle = canonical ? storage.invoke(pattern) : null;
        int[] starts = {Integer.MIN_VALUE, -1, 0, 1, text.length / 2,
                text.length - needle.length, text.length, Integer.MAX_VALUE};
        for (int from : starts) {
            equal(builder.indexOf(pattern, from), expected(text, needle, from, false));
            equal(buffer.indexOf(pattern, from), expected(text, needle, from, false));
            equal(builder.lastIndexOf(pattern, from), expected(text, needle, from, true));
            equal(buffer.lastIndexOf(pattern, from), expected(text, needle, from, true));
            // Direct internal calls exercise the validated canonical-source/String route.
            // The independent oracle never calls these methods or String search.
            if (canonicalSource != null && needle.length > 0) {
                int start = Math.clamp(from, 0, text.length);
                if (start <= text.length - needle.length) {
                    equal((Integer) canonicalForward.invoke(null, canonicalSource,
                            pattern, start, text.length), expected(text, needle, from, false));
                    if (canonicalNeedle != null) {
                        equal((Integer) directForward.invoke(canonicalSource, canonicalNeedle,
                                from, text.length), expected(text, needle, from, false));
                    }
                }
                int maximum = Math.min(from, text.length - needle.length);
                if (maximum >= 0) {
                    equal((Integer) canonicalReverse.invoke(null, canonicalSource,
                            pattern, maximum), expected(text, needle, from, true));
                    if (canonicalNeedle != null) {
                        equal((Integer) directReverse.invoke(canonicalSource, canonicalNeedle,
                                from), expected(text, needle, from, true));
                    }
                }
            }
        }
        equal(builder.indexOf(pattern), expected(text, needle, 0, false));
        equal(buffer.indexOf(pattern), expected(text, needle, 0, false));
        equal(builder.lastIndexOf(pattern), expected(text, needle, text.length, true));
        equal(buffer.lastIndexOf(pattern), expected(text, needle, text.length, true));
        equal(builder.capacity(), capacity);
        equal(buffer.capacity(), bufferCapacity);
        equal(builder.length(), text.length);
        equal(buffer.length(), text.length);
        for (int i = 0; i < text.length; i++) {
            equal(builder.charAt(i), text[i]);
            equal(buffer.charAt(i), text[i]);
        }
        if (lane != null && (lane.get(builder) != null || lane.get(buffer) != null)) {
            throw new AssertionError("Search changed source representation");
        }
    }

    private static void nulls() {
        for (int kind = 0; kind < 4; kind++) {
            try {
                switch (kind) {
                    case 0 -> new StringBuilder().indexOf(null);
                    case 1 -> new StringBuilder().lastIndexOf(null);
                    case 2 -> new StringBuffer().indexOf(null);
                    default -> new StringBuffer().lastIndexOf(null);
                }
                throw new AssertionError("Null needle accepted");
            } catch (NullPointerException expected) { checks++; }
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 0) {
            canonical = args[0].equals("canonical");
            if (!canonical && !args[0].equals("refused")) throw new AssertionError("mode");
            storage = String.class.getDeclaredMethod("m3");
            storage.setAccessible(true);
            lane = StringBuilder.class.getSuperclass().getDeclaredField("m3Lane");
            lane.setAccessible(true);
            if (canonical) {
                Class<?> owner = Class.forName("java.lang.M3StringSearchPrecompute");
                Class<?> text = Class.forName("java.lang.M3String");
                canonicalForward = owner.getDeclaredMethod("indexOf", text, String.class,
                        int.class, int.class);
                canonicalReverse = owner.getDeclaredMethod("lastIndexOf", text, String.class,
                        int.class);
                canonicalForward.setAccessible(true);
                canonicalReverse.setAccessible(true);
                directForward = text.getDeclaredMethod("indexOf", text, int.class, int.class);
                directReverse = text.getDeclaredMethod("lastIndexOf", text, int.class);
                directForward.setAccessible(true);
                directReverse.setAccessible(true);
            }
        }
        for (boolean wide : new boolean[] {false, true}) {
            char[] text = new char[24001];
            Arrays.fill(text, 'a');
            text[text.length - 1] = 'b';
            if (wide) text[0] = '\u20ac';
            for (int size : new int[] {8191, 8192, 8193, 9001, 16384}) {
                char[] needle = new char[size];
                Arrays.fill(needle, 'a');
                scenario(text, needle);
                needle[size - 1] = 'b'; scenario(text, needle);
                needle[size - 1] = 'a'; needle[0] = 'b'; scenario(text, needle);
            }
        }
        char[] seam = new char[20001];
        Arrays.fill(seam, 'a'); seam[9999] = '\ud800'; seam[10000] = '\udc00';
        scenario(seam, Arrays.copyOfRange(seam, 9998, 19000));
        scenario(seam, new char[] {'\ud800', '\udc00'});
        scenario(seam, new char[0]);
        scenario(new char[0], new char[0]);
        scenario(new char[0], new char[] {'x'});
        nulls();
        System.out.println("OVERSIZED_SEARCH_PASS checks=" + checks
                + " mode=" + (args.length == 0 ? "default" : args[0]));
    }
}
