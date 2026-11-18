/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary Kotlin String take/takeLast and lazy ifEmpty/ifBlank preserve contracts and M3 ranges
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED StringKotlinExtensionsTest
 */

import java.lang.reflect.Method;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class StringKotlinExtensionsTest {
    private static void eq(String expected, String actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("expected " + expected + ", got " + actual);
        }
    }

    private static void negativeCounts() {
        try {
            "a".take(-1);
            throw new AssertionError("take(-1) must reject");
        } catch (IllegalArgumentException expected) {
        }
        try {
            "a".takeLast(Integer.MIN_VALUE);
            throw new AssertionError("takeLast(MIN_VALUE) must reject");
        } catch (IllegalArgumentException expected) {
        }
    }

    private static void basic() {
        String text = new String(new char[] {'a', 'b', 'c'});
        eq("a", text.take(1));
        eq("bc", text.takeLast(2));
        eq("", text.take(0));
        eq("", text.takeLast(0));
        if (text.take(3) != text || text.take(100) != text
                || text.takeLast(3) != text || text.takeLast(100) != text) {
            throw new AssertionError("full-length ranges lost receiver identity");
        }
        eq("\uD83D", "\uD83D\uDE00x".take(1));
        eq("\uDE00x", "\uD83D\uDE00x".takeLast(2));
        negativeCounts();
    }

    private static void lazyAndUnicode() {
        AtomicInteger calls = new AtomicInteger();
        String text = new String(new char[] {'x'});
        if (text.ifEmpty(() -> { calls.incrementAndGet(); return "unused"; }) != text
                || text.ifBlank(() -> { calls.incrementAndGet(); return "unused"; }) != text
                || text.ifEmpty(null) != text || text.ifBlank(null) != text
                || calls.get() != 0) {
            throw new AssertionError("nonempty/nonblank callback was evaluated");
        }
        eq("empty", "".ifEmpty(() -> {
            calls.incrementAndGet();
            return "empty";
        }));
        eq("blank", " \t\u00A0\u2007\u202F\u2028".ifBlank(() -> {
            calls.incrementAndGet();
            return "blank";
        }));
        eq(null, "".ifEmpty(() -> null));
        eq(null, "\u00A0".ifBlank(() -> null));
        eq(" \t", " \t".ifEmpty(() -> "not empty"));
        eq("\u0000", "\u0000".ifBlank(() -> "not blank"));
        if (calls.get() != 2) throw new AssertionError("supplier invocation count");
        try {
            "".ifEmpty(null);
            throw new AssertionError("empty callback required");
        } catch (NullPointerException expected) {
        }
        try {
            "\u00A0".ifBlank(null);
            throw new AssertionError("blank callback required");
        } catch (NullPointerException expected) {
        }
    }

    private static boolean kotlinBlankOracle(String value) {
        for (int i = 0; i < value.length(); i++) {
            char unit = value.charAt(i);
            int kind = Character.getType(unit);
            if (kind != Character.SPACE_SEPARATOR && kind != Character.LINE_SEPARATOR
                    && kind != Character.PARAGRAPH_SEPARATOR
                    && !(unit >= 0x0009 && unit <= 0x000D)
                    && !(unit >= 0x001C && unit <= 0x001F)) return false;
        }
        return true;
    }

    private static void differential() {
        char[] alphabet = {'a', ' ', '\t', '\n', '\u001C', '\u00A0', '\u2007',
                '\u2028', '\u202F', '\u0000', '\uD83D', '\uDE00'};
        Random random = new Random(0x4b0f11a4L);
        for (int trial = 0; trial < 5_000; trial++) {
            int size = random.nextInt(48);
            StringBuilder builder = new StringBuilder(size);
            for (int i = 0; i < size; i++) {
                builder.append(alphabet[random.nextInt(alphabet.length)]);
            }
            String text = builder.toString();
            int count = random.nextInt(size + 8);
            eq(text.substring(0, Math.min(size, count)), text.take(count));
            eq(text.substring(Math.max(0, size - count)), text.takeLast(count));
            AtomicInteger calls = new AtomicInteger();
            String resultEmpty = text.ifEmpty(() -> {
                calls.incrementAndGet();
                return "default";
            });
            eq(text.isEmpty() ? "default" : text, resultEmpty);
            String resultBlank = text.ifBlank(() -> {
                calls.incrementAndGet();
                return "fallback";
            });
            eq(kotlinBlankOracle(text) ? "fallback" : text, resultBlank);
            if (calls.get() != (text.isEmpty() ? 1 : 0)
                    + (kotlinBlankOracle(text) ? 1 : 0)) {
                throw new AssertionError("lazy callback cardinality at trial " + trial);
            }
        }
    }

    private static void canonicalBacking() throws ReflectiveOperationException {
        String text = "ab".concat("cd").concat("ef");
        Method m3 = String.class.getDeclaredMethod("m3");
        m3.setAccessible(true);
        if (m3.invoke(text) != null && (m3.invoke(text.take(4)) == null
                || m3.invoke(text.takeLast(4)) == null)) {
            throw new AssertionError("take/takeLast lost canonical M3 owner");
        }
    }

    public static void main(String[] args) throws Exception {
        basic();
        lazyAndUnicode();
        differential();
        canonicalBacking();
    }
}
