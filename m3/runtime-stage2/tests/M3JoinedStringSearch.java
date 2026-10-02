/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0 */
import java.lang.reflect.Field;

public class M3JoinedStringSearch {
    private static final Field STORAGE = field(String.class, "m3Storage");
    private static final Field MATERIALIZED;

    static {
        try {
            MATERIALIZED = field(Class.forName("java.lang.M3StringStorage"), "materialized");
        } catch (ClassNotFoundException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    public static void main(String[] args) throws Exception {
        boolean enabled = Boolean.getBoolean("m3.enabled");

        String joined = String.join("",
                "0123456789-",
                "alpha",
                "-",
                new String(new char[] {'\ud83d'}),
                new String(new char[] {'\ude00'}),
                "-beta-",
                "0123456789");
        String flat = new String(joined.toCharArray());

        parity(flat, joined, "alpha");
        parity(flat, joined, "😀");
        parity(flat, joined, "-beta-");
        parity(flat, joined, "");
        parity(flat, joined, "missing");

        String longNeedle = "abcdefghijklmnopqrstuvwxyz".repeat(3);
        String longJoined = String.join("", "head-", longNeedle.substring(0, 37),
                longNeedle.substring(37), "-tail");
        eq(5, longJoined.indexOf(longNeedle), "long KMP index");
        eq(5, longJoined.lastIndexOf(longNeedle), "long KMP last");

        int emoji = 0x1F600;
        eq(flat.indexOf(emoji), joined.indexOf(emoji), "supplementary index");
        eq(flat.lastIndexOf(emoji), joined.lastIndexOf(emoji), "supplementary last");
        eq(flat.indexOf('a', -100), joined.indexOf('a', -100), "negative from");
        eq(flat.lastIndexOf('0', Integer.MAX_VALUE),
                joined.lastIndexOf('0', Integer.MAX_VALUE), "large from");
        eq(flat.indexOf("alpha", 4), joined.indexOf("alpha", 4), "from substring");
        eq(flat.lastIndexOf("0123", joined.length()),
                joined.lastIndexOf("0123", joined.length()), "last substring");

        check(joined.startsWith("012345"), "startsWith");
        check(joined.startsWith("alpha", 11), "startsWith offset");
        check(joined.endsWith("6789"), "endsWith");
        check(joined.regionMatches(11, "xxalphayy", 2, 5), "regionMatches exact");

        eq(flat.indexOf("alpha", 0, flat.length()),
                joined.indexOf("alpha", 0, joined.length()), "bounded substring");
        eq(flat.indexOf('b', 0, flat.length()),
                joined.indexOf('b', 0, joined.length()), "bounded char");

        if (enabled) {
            Object storage = STORAGE.get(joined);
            check(storage != null, "joined storage");
            check(MATERIALIZED.get(storage) == null,
                    "search/prefix/region operations must not flatten source");
            Object longStorage = STORAGE.get(longJoined);
            check(longStorage != null, "long joined storage");
            check(MATERIALIZED.get(longStorage) == null,
                    "long KMP search must not flatten source");

            long signal = invokeLong(joined, "m3BitSignal64");
            int flags = invokeInt(joined, "m3CharacterFlags");
            check(signal != 0L, "presence signal");
            check((flags & 1) == 0, "non-ASCII flag");
            check((flags & (1 << 4)) != 0, "surrogate flag");
            check((flags & (1 << 8)) != 0, "word flag");
        }

        System.out.println("M3_STRING_SEARCH_PASS enabled=" + enabled);
    }

    private static void parity(String flat, String joined, String needle) {
        eq(flat.indexOf(needle), joined.indexOf(needle), "indexOf " + needle);
        eq(flat.lastIndexOf(needle), joined.lastIndexOf(needle), "lastIndexOf " + needle);
        eq(flat.contains(needle), joined.contains(needle), "contains " + needle);
    }

    private static long invokeLong(String value, String name) throws Exception {
        var method = String.class.getDeclaredMethod(name);
        method.setAccessible(true);
        return (long) method.invoke(value);
    }

    private static int invokeInt(String value, String name) throws Exception {
        var method = String.class.getDeclaredMethod(name);
        method.setAccessible(true);
        return (int) method.invoke(value);
    }

    private static Field field(Class<?> owner, String name) {
        try {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    private static void eq(boolean expected, boolean actual, String label) {
        if (expected != actual) throw new AssertionError(label);
    }

    private static void eq(int expected, int actual, String label) {
        if (expected != actual) {
            throw new AssertionError(label + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }
}
