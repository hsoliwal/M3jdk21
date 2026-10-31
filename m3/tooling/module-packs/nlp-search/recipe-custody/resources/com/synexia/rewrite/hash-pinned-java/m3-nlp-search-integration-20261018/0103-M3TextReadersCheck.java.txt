// SPDX-License-Identifier: Apache-2.0
package org.apache.lucene.analysis;

import java.io.Reader;
import java.util.Objects;
import java.util.Random;

/** Composite-text ingress, exercised without flattening or slicing the source. */
public final class M3TextReadersCheck {
    private static int checks;
    private M3TextReadersCheck() { }
    private static void check(boolean condition) {
        checks++; if (!condition) throw new AssertionError("reader assertion " + checks);
    }
    private record Composite(String left, String right) implements CharSequence {
        // Deliberately split UTF-16 units, including pairs, into separate test segments.
        Composite(String text, int split) { this(text.substring(0, split), text.substring(split)); }
        public int length() { return left.length() + right.length(); }
        public char charAt(int i) {
            Objects.checkIndex(i, length());
            return i < left.length() ? left.charAt(i) : right.charAt(i - left.length());
        }
        public CharSequence subSequence(int a, int b) { throw new AssertionError("must not slice input"); }
        public String toString() { throw new AssertionError("must not flatten input"); }
    }
    public static void main(String[] args) throws Exception {
        var random = new Random(20261014);
        for (int trial = 0; trial < 10000; trial++) {
            char[] units = new char[trial % 101];
            for (int i = 0; i < units.length; i++) units[i] = (char) random.nextInt(65536);
            String text = new String(units);
            Composite source = new Composite(text, random.nextInt(text.length() + 1));
            try (Reader reader = M3TextReaders.reader(source)) {
                int position = 0;
                while (position < text.length()) {
                    if (random.nextBoolean()) { check(reader.read() == text.charAt(position++)); }
                    else {
                        char[] out = new char[17];
                        int wanted = Math.min(1 + random.nextInt(12), text.length() - position);
                        check(reader.read(out, 3, wanted) == wanted);
                        check(out[0] == 0 && out[2] == 0 && out[3 + wanted] == 0);
                        for (int i = 0; i < wanted; i++) check(out[3 + i] == text.charAt(position++));
                    }
                }
                check(reader.read() == -1);
                check(reader.read(new char[1], 0, 0) == -1); // preserve Lucene owner semantics
            }
        }
        Reader closed = M3TextReaders.reader(new Composite("close", 2));
        closed.close(); check(closed.read() == -1);
        String example = "A\uD83D\uDE00\uD800B";
        for (int split = 0; split <= example.length(); split++) {
            var source = new Composite(example, split);
            try (Reader reader = M3TextReaders.reader(source)) {
                for (int i = 0; i < source.length(); i++) check(reader.read() == source.charAt(i));
            }
        }
        try { M3TextReaders.reader(null); throw new AssertionError("null accepted"); }
        catch (NullPointerException expected) { checks++; }
        // Existing String fast path remains usable through the additive facade.
        try (Reader reader = M3TextReaders.reader("abc")) {
            char[] buffer = new char[4]; check(reader.read(buffer, 0, 4) == 3);
            check(new String(buffer, 0, 3).equals("abc"));
        }
        System.out.println("LUCENE_M3_READER_PASS assertions=" + checks + " random_utf16_cases=10000 flatten_calls=0");
    }
}
