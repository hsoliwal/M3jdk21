/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal and others
 */

package gc.stringdedup;

/*
 * @test id=Serial
 * @summary M3JDK String layout invariant (decisions 47, 51), stage I: string deduplication
 *          shares the const chars (chValue) of equal Strings as it shares their value arrays
 * @requires vm.gc.Serial
 * @modules java.base/java.lang:open
 * @run main/othervm -XX:+UseSerialGC -XX:+UseStringDeduplication
 *      -XX:StringDeduplicationAgeThreshold=1 gc.stringdedup.TestStringDeduplicationCharValue
 */

/*
 * @test id=G1
 * @requires vm.gc.G1
 * @modules java.base/java.lang:open
 * @run main/othervm -XX:+UseG1GC -XX:+UseStringDeduplication
 *      -XX:StringDeduplicationAgeThreshold=1 gc.stringdedup.TestStringDeduplicationCharValue
 * @run main/othervm -XX:+UseG1GC -XX:+UseStringDeduplication -XX:-CompactStrings
 *      -XX:StringDeduplicationAgeThreshold=1 gc.stringdedup.TestStringDeduplicationCharValue
 */

/*
 * @test id=Parallel
 * @requires vm.gc.Parallel
 * @modules java.base/java.lang:open
 * @run main/othervm -XX:+UseParallelGC -XX:+UseStringDeduplication
 *      -XX:StringDeduplicationAgeThreshold=1 gc.stringdedup.TestStringDeduplicationCharValue
 */

/*
 * @test id=Shenandoah
 * @requires vm.gc.Shenandoah
 * @modules java.base/java.lang:open
 * @run main/othervm -XX:+UseShenandoahGC -XX:+UseStringDeduplication
 *      -XX:StringDeduplicationAgeThreshold=1 gc.stringdedup.TestStringDeduplicationCharValue
 */

/*
 * @test id=Z
 * @requires vm.gc.Z
 * @modules java.base/java.lang:open
 * @run main/othervm -XX:+UseZGC -XX:+UseStringDeduplication
 *      -XX:StringDeduplicationAgeThreshold=1 gc.stringdedup.TestStringDeduplicationCharValue
 * @run main/othervm -XX:+UseZGC -XX:+ZGenerational -XX:+UseStringDeduplication
 *      -XX:StringDeduplicationAgeThreshold=1 gc.stringdedup.TestStringDeduplicationCharValue
 */

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BooleanSupplier;

public class TestStringDeduplicationCharValue {
    private static final Field VALUE;
    private static final Field CH_VALUE;

    static {
        try {
            VALUE = String.class.getDeclaredField("value");
            VALUE.setAccessible(true);
            CH_VALUE = String.class.getDeclaredField("chValue");
            CH_VALUE.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static final long TIMEOUT_NANOS = 20_000_000_000L;
    private static final List<String> FAILURES = new ArrayList<>();
    private static int checks;

    public static void main(String[] args) throws Exception {
        String stamp = Long.toString(System.nanoTime(), 36);
        char[] latin1 = text(false, stamp);
        char[] utf16 = text(true, stamp);
        char[] internedText = text(false, stamp + "-interned");

        // Pairs whose chars are both filled: value and chValue end up shared.
        String[] latinPair = filledPair(latin1);
        String[] utf16Pair = filledPair(utf16);
        // Checked before any collection: the awaits of one pair collect the other pair too.
        check(value(latinPair[0]) != value(latinPair[1]), "Latin-1 pair starts with two values");
        check(value(utf16Pair[0]) != value(utf16Pair[1]), "UTF-16 pair starts with two values");
        check(chValue(latinPair[0]) != chValue(latinPair[1]), "Latin-1 pair starts unshared");
        check(chValue(utf16Pair[0]) != chValue(utf16Pair[1]), "UTF-16 pair starts unshared");
        // An interned String: deduplication is forbidden, its chValue is never replaced.
        String interned = new String(internedText).intern();
        interned.toCharArray();
        char[] internedChars = chValue(interned);
        String internedTwin = new String(internedText);
        internedTwin.toCharArray();

        shared("Latin-1 pair", latinPair, latin1);
        shared("UTF-16 pair", utf16Pair, utf16);

        // Strings made later, chValue null: a UTF-16 coder is seeded with the shared chars, a
        // Latin-1 coder never is (it may be the default of a String under construction).
        String utf16Fresh = new String(utf16);
        String latinFresh = new String(latin1);
        await("UTF-16 fresh: value shared", () -> value(utf16Fresh) == value(utf16Pair[0]));
        await("Latin-1 fresh: value shared", () -> value(latinFresh) == value(latinPair[0]));
        await("UTF-16 fresh: chValue seeded with the shared chars",
                () -> chValue(utf16Fresh) == chValue(utf16Pair[0]));
        char[] latinFreshChars = chValue(latinFresh);
        check(latinFreshChars == null || latinFreshChars == chValue(latinPair[0]),
                "Latin-1 fresh: chValue is null or the shared chars, never another array");

        check(chValue(interned) == internedChars, "interned: chValue never replaced");
        for (String s : new String[] {latinPair[0], latinPair[1], latinFresh}) {
            content("Latin-1", s, latin1);
        }
        for (String s : new String[] {utf16Pair[0], utf16Pair[1], utf16Fresh}) {
            content("UTF-16", s, utf16);
        }
        content("interned", interned, internedText);
        content("interned twin", internedTwin, internedText);

        System.out.println("checks " + checks + ", failures " + FAILURES.size());
        if (!FAILURES.isEmpty()) {
            FAILURES.forEach(System.out::println);
            throw new AssertionError(FAILURES.size() + " const chars deduplication checks failed");
        }
        System.out.println("RESULT PASS");
    }

    private static String[] filledPair(char[] text) {
        String[] pair = {new String(text), new String(text)};
        pair[0].toCharArray();
        pair[1].toCharArray();
        return pair;
    }

    private static void shared(String label, String[] pair, char[] text) throws Exception {
        await(label + ": value shared", () -> value(pair[0]) == value(pair[1]));
        await(label + ": chValue shared", () -> chValue(pair[0]) == chValue(pair[1]));
        content(label, pair[0], text);
    }

    private static void content(String label, String s, char[] text) {
        char[] chars = chValue(s);
        check(chars == null || Arrays.equals(chars, text), label + ": chValue holds the chars");
        check(Arrays.equals(s.toCharArray(), text) && s.equals(new String(text)),
                label + ": the String keeps its text");
    }

    /** Collects until the condition holds or the timeout passes, as the dedup tests wait. */
    private static void await(String label, BooleanSupplier condition) throws Exception {
        long start = System.nanoTime();
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() - start > TIMEOUT_NANOS) {
                check(false, label + " (timed out)");
                return;
            }
            System.gc();
            Thread.sleep(50);
        }
        check(true, label);
    }

    private static char[] text(boolean wide, String stamp) {
        String base = "DeduplicationConstChars:" + stamp + ":";
        char[] chars = new char[base.length() + 24];
        base.getChars(0, base.length(), chars, 0);
        for (int i = base.length(); i < chars.length; i++) {
            chars[i] = (char) ('A' + i % 26);
        }
        chars[base.length()] = (char) 0xe9;
        if (wide) {
            chars[base.length() + 1] = (char) 0x0100;
            chars[base.length() + 2] = (char) 0x20ac;
        }
        return chars;
    }

    private static Object value(String s) {
        return get(VALUE, s);
    }

    private static char[] chValue(String s) {
        return (char[]) get(CH_VALUE, s);
    }

    private static Object get(Field field, String s) {
        try {
            return field.get(s);
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
