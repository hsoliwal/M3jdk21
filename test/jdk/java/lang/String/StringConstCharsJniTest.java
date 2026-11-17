/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal and others
 */

/*
 * @test id=G1
 * @summary M3JDK String layout invariant (decisions 47, 51), stage J: JNI GetStringCritical pins
 *          the const chars (chValue) of every String, filling them once before the critical
 *          region; GetStringChars and GetStringRegion copy from them and never fill them
 * @requires vm.gc.G1
 * @modules java.base/java.lang:open
 * @run main/othervm/native -XX:+UseG1GC -Xcheck:jni -XX:-UseStringDeduplication
 *      StringConstCharsJniTest
 * @run main/othervm/native -XX:+UseG1GC -Xcheck:jni -XX:-UseStringDeduplication
 *      -XX:-CompactStrings StringConstCharsJniTest
 * @run main/othervm/native -XX:+UseG1GC -Xcheck:jni -XX:-UseStringDeduplication -Xcomp
 *      -XX:-TieredCompilation StringConstCharsJniTest
 */

/*
 * @test id=Serial
 * @requires vm.gc.Serial
 * @modules java.base/java.lang:open
 * @run main/othervm/native -XX:+UseSerialGC -Xcheck:jni -XX:-UseStringDeduplication
 *      StringConstCharsJniTest
 */

/*
 * @test id=Parallel
 * @requires vm.gc.Parallel
 * @modules java.base/java.lang:open
 * @run main/othervm/native -XX:+UseParallelGC -Xcheck:jni -XX:-UseStringDeduplication
 *      StringConstCharsJniTest
 */

/*
 * @test id=Z
 * @requires vm.gc.Z
 * @modules java.base/java.lang:open
 * @run main/othervm/native -XX:+UseZGC -Xcheck:jni -XX:-UseStringDeduplication
 *      StringConstCharsJniTest
 * @run main/othervm/native -XX:+UseZGC -XX:+ZGenerational -Xcheck:jni
 *      -XX:-UseStringDeduplication StringConstCharsJniTest
 */

/*
 * @test id=Shenandoah
 * @requires vm.gc.Shenandoah
 * @modules java.base/java.lang:open
 * @run main/othervm/native -XX:+UseShenandoahGC -Xcheck:jni -XX:-UseStringDeduplication
 *      StringConstCharsJniTest
 */

/*
 * @test id=M3
 * @modules java.base/java.lang:open
 * @run main/othervm/native -Xcheck:jni -XX:-UseStringDeduplication
 *      -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage StringConstCharsJniTest
 */

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StringConstCharsJniTest {
    private static final Field CH_VALUE;

    static {
        try {
            CH_VALUE = String.class.getDeclaredField("chValue");
            CH_VALUE.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static final List<String> FAILURES = new ArrayList<>();
    private static int checks;

    /** Two nested GetStringCritical calls; out: both addresses, both isCopy flags. */
    private static native int criticalTwice(String value, char[] expected, long[] out);

    /** GetStringCritical of inner inside the critical region of outer. */
    private static native int criticalNested(String outer, char[] outerExpected,
                                             String inner, char[] innerExpected,
                                             boolean[] innerCopy);

    /** GetStringChars of the whole String and GetStringRegion of [start, start + len). */
    private static native int charsAndRegion(String value, char[] expected, int start, int len);

    /** A String made with JNI AllocObject, never constructed. */
    private static native String allocUnconstructed();

    /** GetStringChars, GetStringCritical and GetStringLength of an unconstructed String. */
    private static native int unconstructed(String value);

    public static void main(String[] args) {
        System.loadLibrary("StringConstCharsJniTest");
        char[] latin1 = text(false, 300);
        char[] utf16 = text(true, 300);
        fresh("Latin-1", latin1);
        fresh("UTF-16", utf16);
        fresh("empty", new char[0]);
        fresh("one char", new char[] {'x'});
        fresh("large Latin-1", text(false, 1 << 20));
        fresh("large UTF-16", text(true, 1 << 19));
        javaFilled("Latin-1", latin1);
        javaFilled("UTF-16", utf16);
        nested(latin1, utf16);
        nested(utf16, latin1);
        unconstructed();
        System.out.println("checks " + checks + ", failures " + FAILURES.size());
        if (!FAILURES.isEmpty()) {
            FAILURES.forEach(System.out::println);
            throw new AssertionError(FAILURES.size() + " JNI const chars checks failed");
        }
        System.out.println("RESULT PASS");
    }

    /** A fresh String: reads leave chValue null; the critical call fills and pins it. */
    private static void fresh(String label, char[] expected) {
        String value = new String(expected);
        check(chValue(value) == null, label + ": a fresh String has no const chars");
        int start = expected.length / 3;
        int len = expected.length / 3;
        int reads = charsAndRegion(value, expected, start, len);
        check(reads == 0b1111, label + ": GetStringChars/GetStringRegion mask " + reads);
        check(chValue(value) == null, label + ": GetStringChars/GetStringRegion left chValue null");
        critical(label, value, expected);
        char[] constChars = chValue(value);
        check(constChars != null && Arrays.equals(constChars, expected),
                label + ": GetStringCritical filled chValue with the chars");
        reads = charsAndRegion(value, expected, start, len);
        check(reads == 0b1111, label + ": reads from the const chars, mask " + reads);
        char[] copy = value.toCharArray();
        check(copy != constChars && Arrays.equals(copy, expected),
                label + ": toCharArray returns a copy of the VM-filled chars");
        critical(label + " again", value, expected);
        check(chValue(value) == constChars, label + ": chValue is filled once, never replaced");
    }

    /** chValue filled by Java first: the critical call pins that array and fills nothing. */
    private static void javaFilled(String label, char[] expected) {
        String value = new String(expected);
        value.toCharArray();
        char[] constChars = chValue(value);
        check(constChars != null, label + " Java-filled: toCharArray fills chValue");
        critical(label + " Java-filled", value, expected);
        check(chValue(value) == constChars, label + " Java-filled: the VM reuses chValue");
    }

    private static void critical(String label, String value, char[] expected) {
        long[] out = new long[4];
        int mask = criticalTwice(value, expected, out);
        check(mask >= 0 && (mask & 3) == 3, label + ": GetStringCritical returns the chars, "
                + mask);
        check(mask >= 0 && (mask & 4) == 4, label + ": two nested critical calls, one address");
        check(out[2] == 0 && out[3] == 0, label + ": GetStringCritical isCopy is JNI_FALSE ("
                + out[2] + ", " + out[3] + ")");
    }

    /**
     * Inside a critical region the VM never allocates: a fresh inner String is copied (GCs that
     * count critical regions) or pinned after a fill (pinning GCs); both return its chars.
     */
    private static void nested(char[] outerChars, char[] innerChars) {
        String outer = new String(outerChars);
        String inner = new String(innerChars);
        boolean[] innerCopy = new boolean[1];
        int mask = criticalNested(outer, outerChars, inner, innerChars, innerCopy);
        check(mask >= 0 && (mask & 5) == 5, "nested: outer and inner chars, mask " + mask);
        char[] innerConst = chValue(inner);
        check(innerCopy[0] ? innerConst == null
                           : innerConst != null && Arrays.equals(innerConst, innerChars),
                "nested: a copy leaves chValue null, a pin holds the chars (copy="
                + innerCopy[0] + ")");
        check(chValue(outer) != null, "nested: the outer critical call filled its chValue");
    }

    /** JDK 21 contract: GetStringChars of an unconstructed String is null; nothing is filled. */
    private static void unconstructed() {
        String raw = allocUnconstructed();
        int mask = unconstructed(raw);
        check(mask == 0b1111, "unconstructed: chars null, critical copy, length 0, mask " + mask);
        check(chValue(raw) == null, "unconstructed: GetStringCritical does not fill chValue");
    }

    private static char[] text(boolean wide, int length) {
        char[] chars = new char[length];
        for (int i = 0; i < length; i++) {
            chars[i] = (char) ('a' + i % 26);
        }
        if (length > 4) {
            chars[1] = (char) 0xe9;
            chars[3] = (char) 0xff;
        }
        if (wide && length > 8) {
            chars[2] = (char) 0x0100;
            chars[5] = (char) 0x20ac;
            chars[6] = (char) 0xd83d;
            chars[7] = (char) 0xde42;
        }
        return chars;
    }

    private static char[] chValue(String value) {
        try {
            return (char[]) CH_VALUE.get(value);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) {
            FAILURES.add(label);
        }
    }
}
