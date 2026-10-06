/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 *
 * @test
 * @summary M3-backed String.intern preserves JDK identity and collision semantics
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringInternTest
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:-CompactStrings M3StringInternTest
 */

import java.lang.reflect.Field;

public class M3StringInternTest {
    private static final Field STRING_M3;
    private static final Field M3_OWNER;
    private static long checks;

    static {
        try {
            STRING_M3 = String.class.getDeclaredField("m3");
            STRING_M3.setAccessible(true);
            Class<?> m3 = Class.forName("java.lang.M3String");
            M3_OWNER = m3.getDeclaredField("owner");
            M3_OWNER.setAccessible(true);
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    public static void main(String[] args) throws Exception {
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

        String unitValue = String.valueOf('x');
        String unitScalar = new String(new char[] {'x'});
        unitValue.length();
        unitScalar.length();
        Object unitValueM3 = STRING_M3.get(unitValue);
        Object unitScalarM3 = STRING_M3.get(unitScalar);
        check(unitValueM3 != null && unitScalarM3 != null, "unit Strings admitted to M3");
        check(M3_OWNER.get(unitValueM3) == M3_OWNER.get(unitScalarM3),
                "valueOf/scalar canonical unit owner identity");

        String wideValue = String.valueOf('\u0100');
        String wideScalar = new String(new char[] {'\u0100'});
        wideValue.length();
        wideScalar.length();
        check(M3_OWNER.get(STRING_M3.get(wideValue)) == M3_OWNER.get(STRING_M3.get(wideScalar)),
                "wide valueOf/scalar canonical unit owner identity");

        String deletedToEmpty = String.join("", "x").replace("x", "");
        check(deletedToEmpty.isEmpty(), "canonical delete-to-empty content");
        check(deletedToEmpty.intern() == "".intern(), "canonical delete-to-empty intern identity");

        String continuedToEmpty = String.join("", "\\\n").translateEscapes();
        check(continuedToEmpty.isEmpty(), "canonical continuation-to-empty content");
        check(continuedToEmpty.intern() == "".intern(),
                "canonical continuation-to-empty intern identity");

        char[] mutableChars = new char[] {'a', 'b', '\u0100', 'c'};
        String charSnapshot = new String(mutableChars);
        String charSnapshotPeer = new String(new char[] {'a', 'b', '\u0100', 'c'});
        charSnapshot.length();
        charSnapshotPeer.length();
        check(M3_OWNER.get(STRING_M3.get(charSnapshot))
                        == M3_OWNER.get(STRING_M3.get(charSnapshotPeer)),
                "equal char[] constructors share canonical owner");
        mutableChars[0] = 'z';
        mutableChars[2] = 'Q';
        check(charSnapshot.equals("ab\u0100c"), "char[] constructor snapshots mutable input");

        char[] rangedChars = new char[] {'x', 'a', 'b', '\u0100', 'c', 'y'};
        String ranged = new String(rangedChars, 1, 4);
        check(ranged.equals("ab\u0100c"), "char[] range constructor content");
        check(M3_OWNER.get(STRING_M3.get(ranged))
                        == M3_OWNER.get(STRING_M3.get(charSnapshot)),
                "char[] range shares canonical owner");
        rangedChars[1] = 'q';
        check(ranged.equals("ab\u0100c"), "char[] range snapshots mutable input");

        String empty = new String(new char[0]);
        check(empty.intern() == "".intern(), "empty intern identity");

        System.out.println("M3_STRING_INTERN_PASS|checks=" + checks);
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
