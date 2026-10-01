/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0 */
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class M3JoinedStringSemantics {
    private static final Field STORAGE = field("m3Storage");
    private static final Field VALUE = field("value");

    public static void main(String[] args) throws Exception {
        boolean enabled = Boolean.getBoolean("m3.enabled");

        String high = new String(new char[] {'x', '\ud83d'});
        String low = new String(new char[] {'\ude00', 'y'});
        String seam = String.join("", high, low);
        String expected = new String(new char[] {'x', '\ud83d', '\ude00', 'y'});

        eq(expected, seam, "join contents");
        eq(4, seam.length(), "join length");
        eq('\ud83d', seam.charAt(1), "high surrogate");
        eq('\ude00', seam.charAt(2), "low surrogate");
        eq(0x1F600, seam.codePointAt(1), "code point across segment seam");
        eq(3, seam.codePointCount(0, seam.length()), "code point count");
        eq(expected.hashCode(), seam.hashCode(), "hash");
        eq(0, seam.compareTo(expected), "compare");
        check(Arrays.equals(expected.toCharArray(), seam.toCharArray()), "toCharArray");
        check(Arrays.equals(expected.getBytes(StandardCharsets.UTF_8),
                            seam.getBytes(StandardCharsets.UTF_8)), "UTF-8 encoding");

        String slice = seam.substring(1, 3);
        eq(new String(new char[] {'\ud83d', '\ude00'}), slice, "cross-seam substring");
        eq(0x1F600, slice.codePointAt(0), "substring code point");

        String delimited = String.join("|",
                new String(new char[] {'a'}),
                new String(new char[] {'b'}),
                new String(new char[] {'c'}));
        eq("a|b|c", delimited, "multi-piece delimiter join");
        eq(2, delimited.indexOf('b'), "search char");
        eq(1, delimited.indexOf("|b|"), "search string");
        eq("A|B|C", delimited.toUpperCase(), "upper");
        eq("a/b/c", delimited.replace('|', '/'), "replace");

        String repeated = new String(new char[] {'q', 'r'}).repeat(8);
        eq("qrqrqrqrqrqrqrqr", repeated, "repeat");

        String copied = new String(seam);
        check(copied != seam, "copy identity");
        eq(seam, copied, "copy contents");

        String canonical = expected.intern();
        check(seam.intern() == canonical, "intern identity");

        if (enabled) {
            check(STORAGE.get(seam) != null, "join should be segmented");
            check(STORAGE.get(slice) != null, "substring should preserve segmented backing");
            byte[] sentinel = (byte[]) VALUE.get(seam);
            eq(0, sentinel.length, "joined String.value is compatibility sentinel");
        } else {
            check(STORAGE.get(seam) == null, "feature-off join must stay contiguous");
        }

        System.out.println("M3_STRING_SEMANTICS_PASS enabled=" + enabled);
    }

    private static Field field(String name) {
        try {
            Field field = String.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    private static void eq(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void eq(int expected, int actual, String label) {
        if (expected != actual) {
            throw new AssertionError(label + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void eq(char expected, char actual, String label) {
        if (expected != actual) {
            throw new AssertionError(label + " expected=" + (int)expected + " actual=" + (int)actual);
        }
    }

    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }
}
