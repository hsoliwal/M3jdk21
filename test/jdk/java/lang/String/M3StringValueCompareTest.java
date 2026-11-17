/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal and others
 */

/*
 * @test
 * @summary M3JDK decision 50: the compare methods of the original String API read the values of
 *          M3-backed Strings through the stock paths, behind their O(1) gates. Over atoms, tuples
 *          and slices of Latin-1 and UTF-16 text against M3-backed and flat Strings, equals,
 *          compareTo, compareToIgnoreCase, equalsIgnoreCase, both regionMatches and both
 *          contentEquals equal a char[] oracle of their specification; a non-positive length
 *          matches for M3-backed Strings as for any String
 * @modules java.base/java.lang:open
 * @run main M3StringValueCompareTest
 * @run main/othervm -XX:-CompactStrings M3StringValueCompareTest
 * @run main/othervm -Xcomp -XX:-TieredCompilation M3StringValueCompareTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage
 *      M3StringValueCompareTest
 */

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class M3StringValueCompareTest {
    private static final List<String> FAILURES = new ArrayList<>();
    private static Method internChars;
    private static Method concat;
    private static Method slice;
    private static Constructor<String> fromM3;
    private static Field stringM3;
    private static long checks;

    public static void main(String[] args) throws Throwable {
        bind();
        String[] texts = {
            "Hello M3 World, \u00e9t\u00e9 \u00ff\u00b5\u00df and more Latin-1 text 0123456789",
            "Wide \u0101\u03a3\u03c3\u03c2\u0130\u0131 \ud801\udc00\ud801\udc28 \u2c00\u2c30 "
                    + "\ud835\udd1e end \ud800 x \udc00",
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".repeat(7) + "\u0100",
        };
        for (String text : texts) {
            List<String> receivers = shapes(text);
            List<String> arguments = new ArrayList<>();
            for (String variant : variants(text)) {
                arguments.add(flat(variant));
                arguments.add(fromM3.newInstance(intern(variant)));
            }
            for (String receiver : receivers) {
                for (String argument : arguments) {
                    compare(receiver, argument);
                    compare(argument, receiver);
                }
            }
        }
        System.out.println("M3StringValueCompareTest checks=" + checks
                + " failures=" + FAILURES.size());
        if (!FAILURES.isEmpty()) {
            Map<String, List<String>> byCheck = new TreeMap<>();
            for (String failure : FAILURES) {
                String check = failure.substring(failure.lastIndexOf(": ") + 2)
                        .replaceAll("[0-9]+", "#");
                byCheck.computeIfAbsent(check, k -> new ArrayList<>()).add(failure);
            }
            byCheck.forEach((check, all) -> System.out.println(
                    all.size() + " x " + check + ", e.g. " + all.get(0)));
            throw new AssertionError(FAILURES.size() + " failed checks");
        }
    }

    private static void bind() throws Exception {
        Class<?> pool = Class.forName("java.lang.M3StringPool");
        Class<?> m3 = Class.forName("java.lang.M3String");
        internChars = method(pool, "internChars", char[].class, int.class, int.class);
        concat = method(pool, "concat", m3, m3);
        slice = method(m3, "slice", int.class, int.class);
        fromM3 = String.class.getDeclaredConstructor(m3);
        fromM3.setAccessible(true);
        stringM3 = String.class.getDeclaredField("m3");
        stringM3.setAccessible(true);
    }

    /** The text as an atom, a tuple of 8 atoms, and slices of both that hold the whole text. */
    private static List<String> shapes(String text) throws Throwable {
        Object atom = intern(text);
        Object tuple = null;
        for (int piece = 0; piece < 8; piece++) {
            int begin = text.length() * piece / 8;
            int end = text.length() * (piece + 1) / 8;
            Object part = intern(text.substring(begin, end));
            tuple = tuple == null ? part : invoke(concat, null, tuple, part);
        }
        Object wider = intern("<" + text + ">");
        Object widerTuple = invoke(concat, null, invoke(concat, null, intern("<<"), tuple),
                intern(">"));
        List<String> shapes = new ArrayList<>();
        shapes.add(fromM3.newInstance(atom));
        shapes.add(fromM3.newInstance(tuple));
        shapes.add(fromM3.newInstance(invoke(slice, wider, 1, text.length() + 1)));
        shapes.add(fromM3.newInstance(invoke(slice, widerTuple, 2, text.length() + 2)));
        shapes.add(flat(text));
        for (String shape : shapes) {
            check(shape.equals(text) && shape.length() == text.length(), "shape holds the text");
        }
        return shapes;
    }

    /** Equal, case-swapped, changed first/middle/last unit, shorter and longer spellings. */
    private static List<String> variants(String text) {
        int n = text.length();
        StringBuilder swapped = new StringBuilder(n);
        text.codePoints().forEach(cp -> swapped.appendCodePoint(Character.isUpperCase(cp)
                ? Character.toLowerCase(cp) : Character.toUpperCase(cp)));
        return List.of(text, swapped.toString(), changed(text, 0), changed(text, n / 2),
                changed(text, n - 1), text.substring(0, n - 1), text + "a", text.substring(1));
    }

    private static String changed(String text, int index) {
        char[] units = text.toCharArray();
        units[index]++;
        return new String(units);
    }

    private static void compare(String a, String b) {
        char[] x = a.toCharArray();
        char[] y = b.toCharArray();
        String label = kind(a) + " vs " + kind(b) + " (" + x.length + "/" + y.length + " units)";
        check(a.equals(b) == java.util.Arrays.equals(x, y), label + ": equals");
        check(a.compareTo(b) == compareOracle(x, y), label + ": compareTo");
        check(Integer.signum(a.compareToIgnoreCase(b)) == Integer.signum(compareCiOracle(x, y)),
                label + ": compareToIgnoreCase sign");
        String flatA = flat(a);
        if (stringM3Of(flatA) == null && stringM3Of(flat(b)) == null) {
            check(a.compareToIgnoreCase(b) == flatA.compareToIgnoreCase(flat(b)),
                    label + ": compareToIgnoreCase equals the flat value");
        }
        check(a.equalsIgnoreCase(b)
                == (x.length == y.length && regionCiOracle(x, 0, y, 0, x.length)),
                label + ": equalsIgnoreCase");
        check(a.contentEquals(new StringBuilder(b)) == java.util.Arrays.equals(x, y),
                label + ": contentEquals(StringBuilder)");
        check(a.contentEquals(CharBuffer.wrap(y)) == java.util.Arrays.equals(x, y),
                label + ": contentEquals(CharSequence)");
        int[] offsets = {0, 1, 2, x.length / 2};
        for (int to : offsets) {
            for (int oo : offsets) {
                int[] lengths = {-1, 0, 1, 5, x.length / 3, x.length - to, y.length - oo};
                for (int len : lengths) {
                    region(a, to, b, oo, len, x, y, label);
                }
            }
        }
    }

    private static void region(String a, int to, String b, int oo, int len, char[] x, char[] y,
            String label) {
        String where = label + " at " + to + "," + oo + "," + len;
        try {
            check(a.regionMatches(to, b, oo, len) == regionOracle(x, to, y, oo, len),
                    where + ": regionMatches");
            check(a.regionMatches(true, to, b, oo, len) == regionCiOracle(x, to, y, oo, len),
                    where + ": regionMatches(ignoreCase)");
        } catch (RuntimeException e) {
            FAILURES.add(where + ": regionMatches threw " + e);
        }
    }

    private static int compareOracle(char[] x, char[] y) {
        for (int k = 0; k < Math.min(x.length, y.length); k++) {
            if (x[k] != y[k]) {
                return x[k] - y[k];
            }
        }
        return x.length - y.length;
    }

    /** Folded code points (toLowerCase(toUpperCase(cp))) compared in order, then the counts. */
    private static int compareCiOracle(char[] x, char[] y) {
        int[] fx = folded(x, 0, x.length);
        int[] fy = folded(y, 0, y.length);
        for (int k = 0; k < Math.min(fx.length, fy.length); k++) {
            if (fx[k] != fy[k]) {
                return fx[k] - fy[k];
            }
        }
        return fx.length - fy.length;
    }

    private static boolean regionOracle(char[] x, int to, char[] y, int oo, int len) {
        if (oo < 0 || to < 0 || to > (long) x.length - len || oo > (long) y.length - len) {
            return false;
        }
        for (int k = 0; k < len; k++) {
            if (x[to + k] != y[oo + k]) {
                return false;
            }
        }
        return true;
    }

    /** The specification: the code points of both substrings are equal after folding. */
    private static boolean regionCiOracle(char[] x, int to, char[] y, int oo, int len) {
        if (oo < 0 || to < 0 || to > (long) x.length - len || oo > (long) y.length - len) {
            return false;
        }
        if (len <= 0) {
            return true;
        }
        return java.util.Arrays.equals(folded(x, to, len), folded(y, oo, len));
    }

    private static int[] folded(char[] units, int from, int count) {
        int[] points = new int[count];
        int size = 0;
        for (int k = from; k < from + count; ) {
            int cp = Character.codePointAt(units, k, from + count);
            points[size++] = Character.toLowerCase(Character.toUpperCase(cp));
            k += Character.charCount(cp);
        }
        return java.util.Arrays.copyOf(points, size);
    }

    private static String kind(String s) {
        Object storage = stringM3Of(s);
        return storage == null ? "flat" : "M3";
    }

    private static Object stringM3Of(String s) {
        try {
            return stringM3.get(s);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    /** A String built by the stock path (flat on a VM without M3 storage). */
    private static String flat(String text) {
        return new String(text.toCharArray());
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
