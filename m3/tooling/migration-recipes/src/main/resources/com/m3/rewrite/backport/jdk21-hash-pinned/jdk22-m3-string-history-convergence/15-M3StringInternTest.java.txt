/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 *
 * @test
 * @summary M3-backed String.intern preserves JDK identity and collision semantics
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringInternTest
 */

public class M3StringInternTest {
    private static long checks;

    public static void main(String[] args) {
        String composed = String.join("", "m3", "-", "intern");
        String scalar = new String("m3-intern".toCharArray());
        check(composed != scalar, "distinct wrappers before intern");
        check(composed.intern() == scalar.intern(), "composed/scalar intern identity");
        check(composed.intern() == composed.intern(), "stable repeated intern");

        String source = String.join("", "xx", "range", "yy");
        String range = source.substring(2, 7);
        String rangeScalar = new String("range".toCharArray());
        check(range.intern() == rangeScalar.intern(), "range/scalar intern identity");

        String unicode = String.join("", "\u03b2", "\ud83d", "\ude42", "\u00ff");
        String unicodeScalar = new String("\u03b2\ud83d\ude42\u00ff".toCharArray());
        check(unicode.intern() == unicodeScalar.intern(), "Unicode seam intern identity");

        // Java-hash collisions must remain distinct interned strings.
        String collisionLeft = String.join("", "A", "a");
        String collisionRight = String.join("", "B", "B");
        check(collisionLeft.hashCode() == collisionRight.hashCode(), "known collision");
        check(collisionLeft.intern() != collisionRight.intern(), "collision intern distinction");
        check(collisionLeft.intern().equals("Aa"), "collision left content");
        check(collisionRight.intern().equals("BB"), "collision right content");

        String empty = new String(new char[0]);
        check(empty.intern() == "".intern(), "empty intern identity");

        System.out.println("M3_STRING_INTERN_PASS|checks=" + checks);
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
