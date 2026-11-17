/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary Python-compatible tab stops preserve UTF-16 and M3 String ranges
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED StringExpandTabsTest
 */

import java.lang.reflect.Method;
import java.util.Random;

public class StringExpandTabsTest {
    private static void expect(String expected, String actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("expected=" + printable(expected)
                    + " actual=" + printable(actual));
        }
    }

    private static String printable(String text) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            out.append(String.format("\\u%04x", (int) text.charAt(i)));
        }
        return out.toString();
    }

    private static void examples() {
        expect("a   b", "a\tb".expandTabs(4));
        expect("        ", "\t".expandTabs());
        expect("", "\t".expandTabs(0));
        expect("", "\t".expandTabs(-100));
        expect("      ", "\t\t".expandTabs(3));
        expect("ab\r    cd\n    ", "ab\r\tcd\n\t".expandTabs(4));
        expect("a\r\n    x", "a\r\n\tx".expandTabs(4));
        expect("a\b  ", "a\b\t".expandTabs(4));
        expect("\uD83C\uDFAF   X", "\uD83C\uDFAF\tX".expandTabs(4));
        expect("\uD800   Z", "\uD800\tZ".expandTabs(4));
        expect("\uDC00   ", "\uDC00\t".expandTabs(4));
        expect("A\u000B   B", "A\u000B\tB".expandTabs(5));
        String noTabs = new String(new char[] {'a', 'b'});
        if (noTabs.expandTabs() != noTabs || noTabs.expandTabs(-1) != noTabs) {
            throw new AssertionError("tab-free input lost identity");
        }
        try {
            "a\tx".expandTabs(Integer.MAX_VALUE);
            throw new AssertionError("huge expansion should refuse before allocating");
        } catch (OutOfMemoryError expected) {
            // The output length exceeds the Java String limit.
        }
    }

    private static String oracle(String text, int width) {
        StringBuilder result = new StringBuilder();
        int column = 0;
        for (int offset = 0; offset < text.length();) {
            int point = text.codePointAt(offset);
            offset += Character.charCount(point);
            if (point == '\t') {
                int spaces = width <= 0 ? 0 : width - column % width;
                for (int index = 0; index < spaces; index++) {
                    result.append(' ');
                }
                column += spaces;
            } else {
                result.appendCodePoint(point);
                column = point == '\n' || point == '\r' ? 0 : column + 1;
            }
        }
        return result.toString();
    }

    private static void randomized() {
        char[] alphabet = {'a', 'Z', '\t', '\r', '\n', '\u000B',
                '\uD83C', '\uDFAF', '\uD800', '\uDC00', '\u4E2D'};
        Random random = new Random(0x3ec7abL);
        for (int trial = 0; trial < 5_000; trial++) {
            int size = random.nextInt(100);
            StringBuilder source = new StringBuilder(size);
            for (int index = 0; index < size; index++) {
                source.append(alphabet[random.nextInt(alphabet.length)]);
            }
            String text = source.toString();
            int width = random.nextInt(13) - 3;
            expect(oracle(text, width), text.expandTabs(width));
        }
    }

    private static void composition() throws ReflectiveOperationException {
        String input = "\uD83C\uDFAF".concat("\tA").concat("\r\n\tZ");
        String expanded = input.expandTabs(4);
        expect("\uD83C\uDFAF   A\r\n    Z", expanded);
        Method storage = String.class.getDeclaredMethod("m3");
        storage.setAccessible(true);
        if (storage.invoke(input) != null && storage.invoke(expanded) == null) {
            throw new AssertionError("M3 composite input flattened by expandTabs");
        }
    }

    public static void main(String[] args) throws Exception {
        examples();
        randomized();
        composition();
    }
}
