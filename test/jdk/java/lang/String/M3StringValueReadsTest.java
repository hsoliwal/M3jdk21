/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal and others
 */

/*
 * @test
 * @summary M3JDK decision 50: the unit reads of an M3-backed String read value with its canonical
 *          coder. Over atoms, tuples and slices of Latin-1 and UTF-16 text, charAt, codePointAt,
 *          codePointBefore, codePointCount, offsetByCodePoints, getChars, toCharArray and
 *          hashCode equal the char[] oracle; out-of-range indexes throw the stock
 *          StringIndexOutOfBoundsException; hashCode and codePointCount never build the facts
 *          of a range
 * @modules java.base/java.lang:open
 * @run main M3StringValueReadsTest
 * @run main/othervm -XX:-CompactStrings M3StringValueReadsTest
 * @run main/othervm -Xcomp -XX:-TieredCompilation M3StringValueReadsTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage
 *      M3StringValueReadsTest
 */

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class M3StringValueReadsTest {
    /** Above M3StringCodePointPrecompute.MAX_SOURCE_UNITS: no geometry, the count reads value. */
    private static final int LARGE = 40_000;
    private static Method internChars;
    private static Method concat;
    private static Method slice;
    private static Method factsIfPrepared;
    private static Constructor<String> fromM3;
    private static Field stringM3;
    private static final List<String> FAILURES = new ArrayList<>();
    private static long checks;

    public static void main(String[] args) throws Throwable {
        bind();
        String latin = spell(300, false);
        String wide = spell(300, true);
        for (String text : new String[] {latin, wide, "\ude42" + wide + "\ud83d", "x\ud800"}) {
            Object atom = intern(text);
            reads(atom, text, "atom");
            Object tuple = tuple(text);
            reads(tuple, text, "tuple");
            int n = text.length();
            reads(invoke(slice, atom, 1, n - 1), text.substring(1, n - 1), "atom slice");
            reads(invoke(slice, tuple, n / 3, n), text.substring(n / 3), "tuple slice");
        }
        rangeHashBuildsNoFacts(wide);
        largeCountBuildsNoFacts();
        System.out.println("M3StringValueReadsTest checks=" + checks
                + " failures=" + FAILURES.size());
        if (!FAILURES.isEmpty()) {
            FAILURES.stream().filter(f -> !f.contains("charAt")).forEach(System.out::println);
            FAILURES.stream().filter(f -> f.contains("charAt")).limit(3)
                    .forEach(System.out::println);
            throw new AssertionError(FAILURES.size() + " failed checks");
        }
    }

    private static void bind() throws Exception {
        Class<?> pool = Class.forName("java.lang.M3StringPool");
        Class<?> m3 = Class.forName("java.lang.M3String");
        internChars = method(pool, "internChars", char[].class, int.class, int.class);
        concat = method(pool, "concat", m3, m3);
        slice = method(m3, "slice", int.class, int.class);
        factsIfPrepared = method(m3, "factsIfPrepared");
        fromM3 = String.class.getDeclaredConstructor(m3);
        fromM3.setAccessible(true);
        stringM3 = String.class.getDeclaredField("m3");
        stringM3.setAccessible(true);
    }

    /** Latin-1 incl. NUL and 0xff, or UTF-16 with surrogate pairs and lone surrogates. */
    private static String spell(int size, boolean wide) {
        char[] units = new char[size];
        for (int index = 0; index < size; index++) {
            int step = index * 13 + 5;
            units[index] = wide ? (char) (0x100 + step % 0x2f00) : (char) (step % 0x100);
            if (wide && index % 17 == 3 && index + 1 < size) {
                units[index] = '\ud835';
                units[++index] = '\udd1e';
            } else if (wide && index % 29 == 7) {
                units[index] = index % 2 == 0 ? '\ud800' : '\udfff';
            }
        }
        return new String(units);
    }

    private static Object tuple(String text) throws Throwable {
        Object tuple = null;
        for (int piece = 0; piece < 8; piece++) {
            int begin = text.length() * piece / 8;
            int end = text.length() * (piece + 1) / 8;
            if (begin < end) {
                Object part = intern(text.substring(begin, end));
                tuple = tuple == null ? part : invoke(concat, null, tuple, part);
            }
        }
        return tuple;
    }

    private static void reads(Object storage, String text, String shape) throws Throwable {
        String s = fromM3.newInstance(storage);
        String label = shape + " of " + text.length() + " units";
        check(stringM3.get(s) == storage, label + ": M3-backed");
        char[] units = text.toCharArray();
        int n = units.length;
        int hash = 0;
        for (int index = 0; index < n; index++) {
            hash = 31 * hash + units[index];
            check(s.charAt(index) == units[index], label + ": charAt " + index);
            check(s.codePointAt(index) == Character.codePointAt(units, index),
                    label + ": codePointAt " + index);
            check(s.codePointBefore(index + 1) == Character.codePointBefore(units, index + 1),
                    label + ": codePointBefore " + (index + 1));
        }
        for (int begin = 0; begin <= n; begin += Math.max(1, n / 7)) {
            for (int end = begin; end <= n; end += Math.max(1, n / 5)) {
                check(s.codePointCount(begin, end)
                        == Character.codePointCount(units, begin, end - begin),
                        label + ": codePointCount " + begin + ".." + end);
                char[] part = new char[end - begin + 2];
                s.getChars(begin, end, part, 1);
                check(Arrays.equals(part, 1, part.length - 1, units, begin, end),
                        label + ": getChars " + begin + ".." + end);
            }
            for (int offset = -3; offset <= 3; offset++) {
                int expected = offsetOracle(units, begin, offset);
                int actual;
                try {
                    actual = s.offsetByCodePoints(begin, offset);
                } catch (IndexOutOfBoundsException e) {
                    actual = Integer.MIN_VALUE;
                }
                check(actual == expected, label + ": offsetByCodePoints " + begin + "," + offset);
            }
        }
        check(Arrays.equals(s.toCharArray(), units), label + ": toCharArray");
        check(s.hashCode() == hash, label + ": hashCode");
        for (int bad : new int[] {-1, n, Integer.MAX_VALUE}) {
            sioobe(() -> s.charAt(bad), label + ": charAt(" + bad + ")");
            sioobe(() -> s.codePointAt(bad), label + ": codePointAt(" + bad + ")");
        }
        sioobe(() -> s.codePointBefore(0), label + ": codePointBefore(0)");
        sioobe(() -> s.codePointBefore(n + 1), label + ": codePointBefore(n + 1)");
    }

    private static int offsetOracle(char[] units, int index, int offset) {
        try {
            return Character.offsetByCodePoints(units, 0, units.length, index, offset);
        } catch (IndexOutOfBoundsException e) {
            return Integer.MIN_VALUE;
        }
    }

    /** A first hashCode of a range String hashes value; it does not build the range's facts. */
    private static void rangeHashBuildsNoFacts(String text) throws Throwable {
        Object atom = intern(text + "#");
        Object range = invoke(slice, atom, 2, text.length() - 3);
        String s = fromM3.newInstance(range);
        int hash = 0;
        for (int index = 2; index < text.length() - 3; index++) {
            hash = 31 * hash + text.charAt(index);
        }
        check(s.hashCode() == hash, "range hashCode");
        check(invoke(factsIfPrepared, range) == null,
                "range hashCode built the facts of the range (a full scan for one hash)");
    }

    /** Past the geometry bound a codePointCount reads value; it does not build range facts. */
    private static void largeCountBuildsNoFacts() throws Throwable {
        String text = spell(LARGE, true);
        Object atom = intern(text);
        String s = fromM3.newInstance(atom);
        char[] units = text.toCharArray();
        check(s.codePointCount(1, LARGE - 1) == Character.codePointCount(units, 1, LARGE - 2),
                "large codePointCount");
        check(invoke(factsIfPrepared, invoke(slice, atom, 1, LARGE - 1)) == null,
                "large codePointCount built the facts of the range (a full scan for one count)");
    }

    private static void sioobe(Runnable read, String label) {
        checks++;
        try {
            read.run();
        } catch (StringIndexOutOfBoundsException expected) {
            return;
        } catch (RuntimeException other) {
            FAILURES.add(label + " threw " + other.getClass().getName()
                    + ", not the stock StringIndexOutOfBoundsException");
            return;
        }
        FAILURES.add(label + " threw nothing");
    }

    private static Object intern(String text) throws Throwable {
        char[] units = text.toCharArray();
        Object storage = invoke(internChars, null, units, 0, units.length);
        if (storage == null) {
            throw new AssertionError("the pool refused " + units.length + " units");
        }
        return storage;
    }

    private static Object invoke(Method method, Object target, Object... arguments)
            throws Throwable {
        try {
            return method.invoke(target, arguments);
        } catch (InvocationTargetException failure) {
            throw failure.getCause();
        }
    }

    private static Method method(Class<?> type, String name, Class<?>... parameters)
            throws Exception {
        Method method = type.getDeclaredMethod(name, parameters);
        method.setAccessible(true);
        return method;
    }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) {
            FAILURES.add(label);
        }
    }
}
