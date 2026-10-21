/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary Structural M3String slices preserve UTF-16 and detach discarded tuple branches
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringCompositeSliceTest
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:-CompactStrings M3StringCompositeSliceTest
 */

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

public class M3StringCompositeSliceTest {
    private static final Field STRING_M3;
    private static final Field VALUE_OWNER;
    private static final Field OWNER_LENGTH;
    private static final Field TUPLE_LEFT;
    private static final Field TUPLE_RIGHT;
    private static final Field TUPLE_HEIGHT;
    private static final Method VALUE_START;
    private static final Method VALUE_LENGTH;
    private static long checks;

    static {
        try {
            STRING_M3 = accessible(String.class.getDeclaredField("m3"));
            Class<?> valueClass = Class.forName("java.lang.M3String");
            Class<?> ownerClass = Class.forName("java.lang.M3StringOwner");
            Class<?> tupleClass = Class.forName("java.lang.M3StringTuple");
            VALUE_OWNER = accessible(valueClass.getDeclaredField("owner"));
            OWNER_LENGTH = accessible(ownerClass.getDeclaredField("length"));
            TUPLE_LEFT = accessible(tupleClass.getDeclaredField("left"));
            TUPLE_RIGHT = accessible(tupleClass.getDeclaredField("right"));
            TUPLE_HEIGHT = accessible(tupleClass.getDeclaredField("height"));
            VALUE_START = accessible(valueClass.getDeclaredMethod("start"));
            VALUE_LENGTH = accessible(valueClass.getDeclaredMethod("length"));
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    private static <T extends java.lang.reflect.AccessibleObject> T accessible(T value) {
        value.setAccessible(true);
        return value;
    }

    private static void check(boolean pass, String message) {
        checks++;
        if (!pass) throw new AssertionError(message);
    }

    private static Object m3(String value) throws IllegalAccessException {
        Object result = STRING_M3.get(value);
        if (result == null) throw new AssertionError("expected M3 storage for composite value");
        return result;
    }

    private static Object owner(Object value) throws IllegalAccessException {
        return VALUE_OWNER.get(value);
    }

    private static int start(Object value) throws ReflectiveOperationException {
        return (int) VALUE_START.invoke(value);
    }

    private static int length(Object value) throws ReflectiveOperationException {
        return (int) VALUE_LENGTH.invoke(value);
    }

    private static int ownerLength(Object value) throws IllegalAccessException {
        return OWNER_LENGTH.getInt(value);
    }

    private static boolean tuple(Object value) {
        return value.getClass().getSimpleName().equals("M3StringTuple");
    }

    private static String composite(char[] units, int chunkSize) {
        String result = "";
        for (int at = 0; at < units.length;) {
            int count = Math.min(chunkSize, units.length - at);
            result = result.concat(new String(Arrays.copyOfRange(units, at, at + count)));
            at += count;
        }
        return result;
    }

    private static char codeUnit(int index) {
        return (char) (0xf000 + index);
    }

    private static char[] suffixUnits(int iteration) {
        char[] result = new char[24];
        for (int index = 0; index < result.length; index++) {
            result[index] = (char) (0xe000 + iteration * result.length + index);
        }
        return result;
    }

    private static int expectedCodePoint(char[] units, int index) {
        char first = units[index];
        if (Character.isHighSurrogate(first)
                && index + 1 < units.length
                && Character.isLowSurrogate(units[index + 1])) {
            return Character.toCodePoint(first, units[index + 1]);
        }
        return first;
    }

    private static int expectedCodePointBefore(char[] units, int end) {
        char last = units[end - 1];
        if (Character.isLowSurrogate(last)
                && end > 1
                && Character.isHighSurrogate(units[end - 2])) {
            return Character.toCodePoint(units[end - 2], last);
        }
        return last;
    }

    private static int[] expectedCodePoints(char[] units) {
        int[] points = new int[units.length];
        int count = 0;
        for (int index = 0; index < units.length; index++) {
            char unit = units[index];
            if (Character.isHighSurrogate(unit)
                    && index + 1 < units.length
                    && Character.isLowSurrogate(units[index + 1])) {
                points[count++] = Character.toCodePoint(unit, units[++index]);
            } else {
                points[count++] = unit;
            }
        }
        return Arrays.copyOf(points, count);
    }

    private static void sameUnits(String actual, char[] expected, String where) {
        check(actual.length() == expected.length, where + " length");
        int hash = 0;
        for (int index = 0; index < expected.length; index++) {
            check(actual.charAt(index) == expected[index], where + " UTF-16 unit " + index);
            hash = 31 * hash + expected[index];
        }
        check(actual.hashCode() == hash, where + " hash");
        check(Arrays.equals(actual.toCharArray(), expected), where + " toCharArray");
        char[] copied = new char[expected.length];
        actual.getChars(0, actual.length(), copied, 0);
        check(Arrays.equals(copied, expected), where + " getChars");
        String oracle = new String(expected.clone());
        check(actual.equals(oracle) && oracle.equals(actual), where + " equality");
        int[] codePoints = expectedCodePoints(expected);
        check(actual.codePointCount(0, actual.length()) == codePoints.length,
                where + " codePointCount");
        check(Arrays.equals(actual.codePoints().toArray(), codePoints), where + " codePoints");
        for (int index = 0; index < expected.length; index++) {
            check(actual.codePointAt(index) == expectedCodePoint(expected, index),
                    where + " codePointAt " + index);
        }
        for (int end = 1; end <= expected.length; end++) {
            check(actual.codePointBefore(end) == expectedCodePointBefore(expected, end),
                    where + " codePointBefore " + end);
        }
        Object storage = STRING_M3.get(actual);
        if (storage != null && tuple(owner(storage))) {
            assertTree(storage, where);
        }
    }

    private static void exhaustiveUtf16Ranges() throws Exception {
        char[] units = {
            'A', '\uD83D', '\uDE03', '\uD800', 'x', '\uDC00', '\u0000', '\u00ff', '\u03a9'
        };
        String composed = composite(units, 1);
        check(tuple(owner(m3(composed))), "one-code-unit chunks form a tuple");
        sameUnits(composed, units, "complete UTF-16 source");
        for (int begin = 0; begin <= units.length; begin++) {
            for (int end = begin; end <= units.length; end++) {
                String actual = composed.substring(begin, end);
                char[] expected = Arrays.copyOfRange(units, begin, end);
                sameUnits(actual, expected, "range [" + begin + "," + end + ")");
            }
        }

        String oracle = new String(units.clone());
        int[][] invalid = {
            {-1, 0}, {0, -1}, {units.length + 1, units.length + 1},
            {0, units.length + 1}, {5, 4}, {units.length, units.length + 1}
        };
        for (int[] range : invalid) {
            check(failureType(composed, range[0], range[1])
                            == failureType(oracle, range[0], range[1]),
                    "substring exception parity " + Arrays.toString(range));
        }
    }

    private static Class<?> failureType(String value, int begin, int end) {
        try {
            value.substring(begin, end);
            return null;
        } catch (RuntimeException failure) {
            return failure.getClass();
        }
    }

    private static void randomUtf16Ranges() throws Exception {
        char[] alphabet = {
            'a', 'Z', '0', '\u0000', '\u00ff', '\u03a9', '\uD83D', '\uDE03',
            '\uD800', '\uDC00', '\u2028'
        };
        char[] units = new char[256];
        for (int index = 0; index < units.length; index++) {
            units[index] = alphabet[(index * 17 + index / 3) % alphabet.length];
        }
        String composed = composite(units, 7);
        check(tuple(owner(m3(composed))), "random UTF-16 input is composite");
        sameUnits(composed, units, "random UTF-16 source");
        long state = 0x4d33534c494345L;
        for (int trial = 0; trial < 240; trial++) {
            state = state * 6364136223846793005L + 1442695040888963407L;
            int begin = (int) Long.remainderUnsigned(state, units.length - 32);
            state = state * 6364136223846793005L + 1442695040888963407L;
            int max = Math.min(128, units.length - begin);
            int count = 32 + (int) Long.remainderUnsigned(state, max - 31);
            int end = begin + count;
            String actual = composed.substring(begin, end);
            sameUnits(actual, Arrays.copyOfRange(units, begin, end),
                    "random range " + trial);
        }
    }

    private static Set<Object> visibleLeafOwners(Object value, int begin, int end)
            throws ReflectiveOperationException {
        Set<Object> result = Collections.newSetFromMap(new IdentityHashMap<>());
        collectVisible(owner(value), start(value) + begin, start(value) + end, result);
        return result;
    }

    private static void collectVisible(
            Object currentOwner, int begin, int end, Set<Object> result)
            throws ReflectiveOperationException {
        if (begin >= end) return;
        if (!tuple(currentOwner)) {
            result.add(currentOwner);
            return;
        }
        Object left = TUPLE_LEFT.get(currentOwner);
        Object right = TUPLE_RIGHT.get(currentOwner);
        int leftLength = length(left);
        if (begin < leftLength) {
            int leftBegin = Math.max(0, begin);
            int leftEnd = Math.min(leftLength, end);
            if (leftBegin < leftEnd) {
                collectVisible(owner(left), start(left) + leftBegin, start(left) + leftEnd, result);
            }
        }
        int rightLength = ownerLength(currentOwner) - leftLength;
        if (end > leftLength) {
            int rightBegin = Math.max(0, begin - leftLength);
            int rightEnd = Math.min(rightLength, end - leftLength);
            if (rightBegin < rightEnd) {
                collectVisible(
                        owner(right), start(right) + rightBegin, start(right) + rightEnd, result);
            }
        }
    }

    private static boolean reaches(Object currentOwner, Object wanted, Set<Object> visited)
            throws ReflectiveOperationException {
        if (currentOwner == wanted) return true;
        if (!tuple(currentOwner) || !visited.add(currentOwner)) return false;
        return reaches(owner(TUPLE_LEFT.get(currentOwner)), wanted, visited)
                || reaches(owner(TUPLE_RIGHT.get(currentOwner)), wanted, visited);
    }

    private static int verifyTree(Object currentOwner, IdentityHashMap<Object, Integer> heights)
            throws ReflectiveOperationException {
        if (!tuple(currentOwner)) return 0;
        Integer known = heights.get(currentOwner);
        if (known != null) return known;
        Object left = TUPLE_LEFT.get(currentOwner);
        Object right = TUPLE_RIGHT.get(currentOwner);
        Object leftOwner = owner(left);
        Object rightOwner = owner(right);
        int leftHeight = verifyTree(leftOwner, heights);
        int rightHeight = verifyTree(rightOwner, heights);
        if (tuple(leftOwner)) {
            check(start(left) == 0 && length(left) == ownerLength(leftOwner),
                    "tuple left child is a whole-owner coordinate");
        }
        if (tuple(rightOwner)) {
            check(start(right) == 0 && length(right) == ownerLength(rightOwner),
                    "tuple right child is a whole-owner coordinate");
        }
        int actualHeight = 1 + Math.max(leftHeight, rightHeight);
        check(TUPLE_HEIGHT.getInt(currentOwner) == actualHeight,
                "cached tuple height equals actual owner-graph height");
        heights.put(currentOwner, actualHeight);
        return actualHeight;
    }

    private static int leafOccurrences(Object currentOwner) throws ReflectiveOperationException {
        if (!tuple(currentOwner)) return 1;
        return leafOccurrences(owner(TUPLE_LEFT.get(currentOwner)))
                + leafOccurrences(owner(TUPLE_RIGHT.get(currentOwner)));
    }

    private static int ceilLog2(int value) {
        int result = 0;
        int power = 1;
        while (power < value) {
            power <<= 1;
            result++;
        }
        return result;
    }

    private static void assertTree(Object value, String where) throws Exception {
        Object rootOwner = owner(value);
        if (!tuple(rootOwner)) return;
        check(start(value) == 0 && length(value) == ownerLength(rootOwner),
                where + " tuple view is normalized to its whole owner");
        int depth = verifyTree(rootOwner, new IdentityHashMap<>());
        int leaves = leafOccurrences(rootOwner);
        int logarithmicBound = 2 * ceilLog2(leaves + 1) + 2;
        check(depth <= logarithmicBound,
                where + " actual depth " + depth + " <= " + logarithmicBound);
    }

    private static boolean crossesTupleSeam(Object value, int begin, int end)
            throws ReflectiveOperationException {
        return crosses(owner(value), start(value) + begin, start(value) + end);
    }

    private static boolean crosses(Object currentOwner, int begin, int end)
            throws ReflectiveOperationException {
        if (!tuple(currentOwner) || begin >= end) return false;
        Object left = TUPLE_LEFT.get(currentOwner);
        Object right = TUPLE_RIGHT.get(currentOwner);
        int leftLength = length(left);
        if (begin < leftLength && end > leftLength) return true;
        if (end <= leftLength) {
            return crosses(owner(left), start(left) + begin, start(left) + end);
        }
        if (begin >= leftLength) {
            return crosses(owner(right),
                    start(right) + begin - leftLength, start(right) + end - leftLength);
        }
        return false;
    }

    private static void structuralRetentionAndDepth() throws Exception {
        char[] keepUnits = new char[256];
        for (int index = 0; index < keepUnits.length; index++) keepUnits[index] = codeUnit(index);
        String keep = composite(keepUnits, 8);
        Object keepValue = m3(keep);
        check(tuple(owner(keepValue)), "retention source is tuple-backed");

        int prefixLength = 203;
        check(crossesTupleSeam(keepValue, 0, prefixLength),
                "test prefix cuts through a nested tuple seam");

        Set<Object> discardedKeepLeaves =
                visibleLeafOwners(keepValue, prefixLength, keepUnits.length);
        discardedKeepLeaves.removeAll(visibleLeafOwners(keepValue, 0, prefixLength));
        check(!discardedKeepLeaves.isEmpty(), "test has fully discarded keep-side leaves");

        String tail = composite(suffixUnits(0), 8);
        String broad = keep.concat(tail);
        Object broadValue = m3(broad);
        Object broadRoot = owner(broadValue);
        Set<Object> firstTailLeaves =
                visibleLeafOwners(m3(tail), 0, length(m3(tail)));
        String clippedBroad = broad.substring(0, prefixLength);
        sameUnits(clippedBroad, Arrays.copyOfRange(keepUnits, 0, prefixLength),
                "first structural prefix");
        Object clippedBroadOwner = owner(m3(clippedBroad));
        check(!reaches(clippedBroadOwner, broadRoot,
                        Collections.newSetFromMap(new IdentityHashMap<>())),
                "nested structural slice detaches the original full root");
        for (Object leaf : discardedKeepLeaves) {
            check(!reaches(clippedBroadOwner, leaf,
                            Collections.newSetFromMap(new IdentityHashMap<>())),
                    "nested structural slice detaches discarded keep-side leaves");
        }
        for (Object leaf : firstTailLeaves) {
            check(!reaches(clippedBroadOwner, leaf,
                            Collections.newSetFromMap(new IdentityHashMap<>())),
                    "nested structural slice detaches appended-tail leaves");
        }
        assertTree(m3(clippedBroad), "first structural prefix graph");
        String clippedAgain = broad.substring(0, prefixLength);
        sameUnits(clippedAgain, Arrays.copyOfRange(keepUnits, 0, prefixLength),
                "repeated structural prefix");
        check(owner(m3(clippedAgain)) == clippedBroadOwner,
                "identical structural slice reuses the canonical tuple owner");

        String previousPrefix = clippedBroad;
        for (int iteration = 0; iteration < 128; iteration++) {
            char[] suffix = suffixUnits(iteration);
            String tailValue = composite(suffix, 8);
            String current = previousPrefix.concat(tailValue);
            char[] expectedCurrent = new char[prefixLength + suffix.length];
            System.arraycopy(keepUnits, 0, expectedCurrent, 0, prefixLength);
            System.arraycopy(suffix, 0, expectedCurrent, prefixLength, suffix.length);
            sameUnits(current, expectedCurrent, "append " + iteration);

            Object currentValue = m3(current);
            Object oldRoot = owner(currentValue);
            check(tuple(oldRoot), "truncate/append source is tuple-backed " + iteration);
            Set<Object> discardedTailLeaves =
                    visibleLeafOwners(m3(tailValue), 0, length(m3(tailValue)));

            String prefix = current.substring(0, prefixLength);
            char[] expectedPrefix = Arrays.copyOfRange(keepUnits, 0, prefixLength);
            sameUnits(prefix, expectedPrefix, "retained prefix " + iteration);
            Object prefixValue = m3(prefix);
            Object prefixOwner = owner(prefixValue);
            check(!reaches(prefixOwner, oldRoot, Collections.newSetFromMap(new IdentityHashMap<>())),
                    "discarded full root is unreachable " + iteration);
            for (Object leaf : discardedKeepLeaves) {
                check(!reaches(prefixOwner, leaf,
                                Collections.newSetFromMap(new IdentityHashMap<>())),
                        "discarded keep-side leaf owner is unreachable " + iteration);
            }
            for (Object leaf : discardedTailLeaves) {
                check(!reaches(prefixOwner, leaf,
                                Collections.newSetFromMap(new IdentityHashMap<>())),
                        "discarded appended-tail leaf owner is unreachable " + iteration);
            }
            assertTree(prefixValue, "retained prefix graph " + iteration);
            previousPrefix = prefix;
        }
    }

    public static void main(String[] args) throws Exception {
        exhaustiveUtf16Ranges();
        randomUtf16Ranges();
        structuralRetentionAndDepth();
        System.out.println("M3_STRING_COMPOSITE_SLICE_PASS checks=" + checks);
    }
}
