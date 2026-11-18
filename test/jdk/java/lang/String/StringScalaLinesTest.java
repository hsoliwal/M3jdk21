/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary Scala-style String.linesWithSeparators retains exact CR/LF/CRLF and M3 ranges without changing lines()
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED StringScalaLinesTest
 */

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class StringScalaLinesTest {
    private static void eq(List<String> expected, List<String> actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("expected " + expected + ", got " + actual);
        }
    }

    private static List<String> reference(String source, boolean retain) {
        List<String> lines = new ArrayList<>();
        int start = 0;
        while (start < source.length()) {
            int pos = start;
            while (pos < source.length()
                    && source.charAt(pos) != '\r' && source.charAt(pos) != '\n') {
                pos++;
            }
            int next = pos;
            if (next < source.length()) {
                next++;
                if (source.charAt(pos) == '\r' && next < source.length()
                        && source.charAt(next) == '\n') next++;
            }
            lines.add(source.substring(start, retain ? next : pos));
            start = next;
        }
        return lines;
    }

    private static void direct() {
        eq(List.of(), "".linesWithSeparators().toList());
        eq(List.of("x"), "x".linesWithSeparators().toList());
        eq(List.of("x\n"), "x\n".linesWithSeparators().toList());
        eq(List.of("\n", "\n"), "\n\n".linesWithSeparators().toList());
        eq(List.of("a\r\n", "b\r", "c\n", "d"),
                "a\r\nb\rc\nd".linesWithSeparators().toList());
        eq(List.of("a\u2028b\u0085c\u000Bd"),
                "a\u2028b\u0085c\u000Bd".linesWithSeparators().toList());
        eq(List.of("\r\n", "\r\n"), "\r\n\r\n".linesWithSeparators().toList());
        eq(List.of("", "", "tail"), "\n\r\ntail".lines().toList());
        eq(List.of("", "", "tail"), reference("\n\r\ntail", false));
        String original = new String(new char[] {'a', 'b', 'c'});
        if (original.linesWithSeparators().findFirst().orElseThrow() != original) {
            throw new AssertionError("single line must return original receiver identity");
        }
    }

    private static void differential() {
        char[] alphabet = {'a', '\n', '\r', ' ', '\u0000', '\u000B', '\u0085',
                '\u2028', '\u2029', '\uD83D', '\uDE00', 'z'};
        Random random = new Random(0x5ca1a110L);
        for (int trial = 0; trial < 6_000; trial++) {
            int count = random.nextInt(100);
            StringBuilder builder = new StringBuilder(count);
            for (int i = 0; i < count; i++) {
                builder.append(alphabet[random.nextInt(alphabet.length)]);
            }
            String source = builder.toString();
            eq(reference(source, true), source.linesWithSeparators().toList());
            eq(reference(source, false), source.lines().toList());
            eq(reference(source, true), source.linesWithSeparators().parallel().toList());
        }
    }

    private static void lazyAndCanonical() throws ReflectiveOperationException {
        String source = "a\r\n".repeat(25_000) + "last";
        AtomicInteger observed = new AtomicInteger();
        try (Stream<String> lines = source.linesWithSeparators()) {
            eq(List.of("a\r\n"), lines.peek(x -> observed.incrementAndGet())
                    .limit(1).toList());
        }
        if (observed.get() != 1) throw new AssertionError("line traversal was eager");

        String composed = "ab".concat("\r\ncd").concat("\nef");
        Method m3 = String.class.getDeclaredMethod("m3");
        m3.setAccessible(true);
        Object backing = m3.invoke(composed);
        List<String> pieces = composed.linesWithSeparators().toList();
        eq(List.of("ab\r\n", "cd\n", "ef"), pieces);
        if (backing != null) {
            for (String piece : pieces) {
                if (m3.invoke(piece) == null) {
                    throw new AssertionError("line lost canonical M3 owner");
                }
            }
        }
    }

    /** Exercises the M3 path even when a stock test JVM cannot inject the M3 flag. */
    private static void forcedCanonicalComposition() throws ReflectiveOperationException {
        Class<?> storageClass = Class.forName("java.lang.M3String");
        Method admitArrays = storageClass.getDeclaredMethod("admitArrays", char[][].class);
        admitArrays.setAccessible(true);
        // CRLF intentionally straddles two distinct interned source atoms.
        char[][] atoms = {{'a', '\r'}, {'\n', 'b', '\n'}, {'x', '\r'}};
        Object storage = admitArrays.invoke(null, (Object) atoms);
        if (storage == null) throw new AssertionError("canonical atom composition refused");

        Constructor<String> constructor = String.class.getDeclaredConstructor(storageClass);
        constructor.setAccessible(true);
        String composed = constructor.newInstance(storage);
        Method m3 = String.class.getDeclaredMethod("m3");
        m3.setAccessible(true);
        if (m3.invoke(composed) == null) throw new AssertionError("composition not M3-backed");
        List<String> pieces = composed.linesWithSeparators().toList();
        eq(List.of("a\r\n", "b\n", "x\r"), pieces);
        eq(List.of("a", "b", "x"), composed.lines().toList());
        for (String piece : pieces) {
            if (m3.invoke(piece) == null) {
                throw new AssertionError("separator-preserving line lost canonical M3 owner");
            }
        }
    }

    public static void main(String[] args) throws Exception {
        direct();
        differential();
        lazyAndCanonical();
        forcedCanonicalComposition();
    }
}
