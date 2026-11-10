// SPDX-License-Identifier: Apache-2.0
// Differential for JLS 15.18.1 string conversion in javac-emitted indy concatenation.
// Prints one line per (shape, value tuple); the outputs of different JDKs / VM flags are diffed byte for byte.
import java.util.ArrayList;
import java.util.List;

public class ConcatDifferential {
    record NullText(String tag) { @Override public String toString() { return null; } }
    record Text(String tag) { @Override public String toString() { return tag; } }

    static final String[] STRINGS = {null, "", "a", "αβ", "😀", "\uD800", "null", "x".repeat(40)};
    static final Object[] OBJECTS = {null, new NullText("n"), new Text("t"), new Text(""), 7, 'c', new StringBuilder("sb"),
            new char[0].getClass().getSimpleName(), (CharSequence) "cs"};
    static final int[] INTS = {0, -1, Integer.MIN_VALUE, Integer.MAX_VALUE};
    static final long[] LONGS = {0L, Long.MIN_VALUE, Long.MAX_VALUE};
    static final double[] DOUBLES = {0.0, -0.0, 1.0E300, Double.NaN, Double.NEGATIVE_INFINITY, 4.9E-324};
    static final float[] FLOATS = {0.0f, -0.0f, 1.5f, Float.NaN};
    static final char[] CHARS = {'a', 'α', '\uD800', '\0'};

    static String s1(String a) { return a + ""; }
    static String s2(String a, String b) { return a + b; }
    static String s3(String a, String b) { return "<" + a + "|" + b + ">"; }
    static String s4(String a, int i, String b) { return a + i + b; }
    static String s5(String a, String b, String c, String d) { return "p" + a + "q" + b + "r" + c + "s" + d + "t"; }
    static String o1(Object a) { return "x" + a + "y"; }
    static String o2(Object a, Object b) { return a + "" + b; }
    static String o3(Object a, String b, Object c) { return "[" + a + "," + b + "," + c + "]"; }
    static String cs(CharSequence a, String b) { return a + "-" + b; }
    static String mix(String a, double d, boolean z, char c, String b, float f, long l) {
        return "m" + a + d + "-" + z + "-" + c + b + "x" + f + l;
    }
    static String prim(byte b, short s, int i, long l, char c, boolean z, float f, double d) {
        return b + "|" + s + "|" + i + "|" + l + "|" + c + "|" + z + "|" + f + "|" + d;
    }
    static String many(String a, Object b, int c, String d, Object e, long f, String g, char h, String i) {
        return a + b + c + d + e + f + g + h + i + "!";
    }

    static String show(String s) {
        StringBuilder out = new StringBuilder(s.length() + 8).append(s.length()).append(':');
        for (int k = 0; k < s.length(); k++) {
            char c = s.charAt(k);
            if (c >= 0x20 && c < 0x7f) out.append(c); else out.append(String.format("\\u%04x", (int) c));
        }
        return out.toString();
    }

    public static void main(String[] args) {
        List<String> lines = new ArrayList<>();
        for (String a : STRINGS) {
            lines.add("s1 " + show(s1(a)));
            for (String b : STRINGS) {
                lines.add("s2 " + show(s2(a, b)));
                lines.add("s3 " + show(s3(a, b)));
                for (int i : INTS) lines.add("s4 " + show(s4(a, i, b)));
                lines.add("s5 " + show(s5(a, b, b, a)));
                lines.add("cs " + show(cs(a, b)));
            }
            for (Object o : OBJECTS) {
                lines.add("o3 " + show(o3(o, a, o)));
                lines.add("many " + show(many(a, o, 3, a, o, 9L, a, 'z', a)));
            }
            for (double d : DOUBLES) for (char c : CHARS) for (float f : FLOATS)
                lines.add("mix " + show(mix(a, d, d > 0, c, a, f, LONGS[c % LONGS.length])));
        }
        for (Object a : OBJECTS) {
            lines.add("o1 " + show(o1(a)));
            for (Object b : OBJECTS) lines.add("o2 " + show(o2(a, b)));
        }
        for (int i : INTS) for (double d : DOUBLES)
            lines.add("prim " + show(prim((byte) i, (short) i, i, (long) i * 3, (char) ('A' + (i & 7)), i < 0, (float) d, d)));
        // JLS 15.18.1: the result of a non-constant concatenation is a new String object.
        String base = "k";
        lines.add("fresh " + (s2(base, "") != base) + " " + (o1(null) != o1(null)));
        lines.forEach(System.out::println);
        System.err.println("CONCAT_DIFFERENTIAL lines=" + lines.size());
    }
}
