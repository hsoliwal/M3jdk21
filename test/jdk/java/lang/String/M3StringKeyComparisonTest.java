/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary Native scalar runs and differently segmented M3 String keys preserve exact UTF-16 equality, order and collection lookup
 * @modules java.base/java.lang:+open
 *          java.base/jdk.internal.misc
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:-UseM3StringStorage -Dm3.string.immediate=true M3StringKeyComparisonTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -Dm3.string.immediate=true M3StringKeyComparisonTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -Dm3.string.immediate=false M3StringKeyComparisonTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:-CompactStrings M3StringKeyComparisonTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -Xint M3StringKeyComparisonTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:TieredStopAtLevel=1 -Xbatch M3StringKeyComparisonTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:-TieredCompilation -XX:CompileThreshold=100 -Xbatch M3StringKeyComparisonTest
 */

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import jdk.internal.misc.Unsafe;

public class M3StringKeyComparisonTest {
    private static final Unsafe UNSAFE = Unsafe.getUnsafe();
    private static final int[] LENGTHS = {
        0, 1, 2, 3, 4, 7, 8, 9, 15, 16, 17, 31, 32, 33, 63, 64, 65, 129
    };
    private static final int[][] LATIN_LAYOUTS = {{1, 1}, {2, 1}, {2, 0}};
    private static final int[][] WIDE_LAYOUTS = {{2, 1}, {2, 0}};
    private static Method mapped;
    private static Method range;
    private static Method whole;
    private static Method compareRange;
    private static Method comparePrefix;
    private static Method compareFlatPrefix;
    private static Method bridgeCompareTo;
    private static Method factsIfPrepared;
    private static Method packChars;
    private static Method immediateOf;
    private static Constructor<?> tuple;
    private static Field stringM3;
    private static long checks;

    public static void main(String[] args) throws Exception {
        if (args.length > 1 || args.length == 1 && !args[0].equals("native-ranges-only")) {
            throw new IllegalArgumentException("optional argument: native-ranges-only");
        }
        bind();
        nativeRanges();
        tupleRanges();
        immediateRanges();
        bridgeRanges();
        // This explicit mode also permits native-memory smoke tests using the actual M3 classes
        // patched into a bootstrap JDK. Public String/collection integration still requires M3JDK.
        if (args.length == 0) collectionKeys();
        System.out.println("M3_STRING_KEY_COMPARISON_PASS|checks=" + checks
                + "|publicString=" + (args.length == 0));
    }

    private static void bind() throws Exception {
        Class<?> atom = Class.forName("java.lang.M3StringAtom");
        Class<?> owner = Class.forName("java.lang.M3StringOwner");
        Class<?> m3 = Class.forName("java.lang.M3String");
        Class<?> immediate = Class.forName("java.lang.M3StringImmediate");
        mapped = method(atom, "mapped", Object.class, long.class, byte.class, boolean.class,
                int.class, byte.class, int.class, long.class, long.class);
        compareRange = method(atom, "compareRange", int.class, atom, int.class, int.class);
        range = method(m3, "range", owner, int.class, int.class);
        whole = method(m3, "whole", owner);
        comparePrefix = method(m3, "comparePrefix", m3, int.class);
        compareFlatPrefix = method(m3, "compareFlatPrefix", String.class, int.class);
        bridgeCompareTo = method(Class.forName("java.lang.M3StringPrecomputeBridge"),
                "compareTo", String.class, String.class, m3, m3);
        factsIfPrepared = method(m3, "factsIfPrepared");
        packChars = method(immediate, "packChars", char[].class, int.class, int.class);
        immediateOf = method(immediate, "of", long.class);
        tuple = Class.forName("java.lang.M3StringTuple")
                .getDeclaredConstructor(m3, m3, long.class, long.class);
        tuple.setAccessible(true);
    }

    private static Method method(Class<?> type, String name, Class<?>... parameters)
            throws Exception {
        Method result = type.getDeclaredMethod(name, parameters);
        result.setAccessible(true);
        return result;
    }

    /** Independent allocations force content comparison, including unaligned native addresses. */
    private static void nativeRanges() throws Exception {
        for (boolean wide : new boolean[] {false, true}) {
            int[][] layouts = wide ? WIDE_LAYOUTS : LATIN_LAYOUTS;
            for (int length : LENGTHS) {
                char[] expected = pattern(length, wide);
                for (int[] leftLayout : layouts) {
                    for (int[] rightLayout : layouts) {
                        char[] leftUnits = surrounded(expected, 1, 3, 'L');
                        char[] rightUnits = surrounded(expected, 3, 1, 'R');
                        try (NativeRun left = new NativeRun(leftUnits, leftLayout, 1);
                             NativeRun right = new NativeRun(rightUnits, rightLayout, 3)) {
                            Object leftRange = left.range(1, length);
                            Object rightRange = right.range(3, length);
                            check(nativeCompare(left, 1, right, 3, length) == 0,
                                    "equal native ranges across layouts");
                            check(prefix(leftRange, rightRange, length) == 0,
                                    "equal scalar ranges with different owners/offsets");
                            check((int) compareFlatPrefix.invoke(leftRange,
                                    new String(expected), length) == 0, "flat prefix parity");
                            check(nativeCompare(left, 1, left, 1, length) == 0,
                                    "same coordinate native range");
                            check(length == 0 || factsIfPrepared.invoke(leftRange) == null,
                                    "scalar comparison does not prepare facts");
                            for (int mismatch = 0; mismatch < length; mismatch++) {
                                char[] different = rightUnits.clone();
                                different[3 + mismatch] ^= wide ? 0x8101 : 0x81;
                                try (NativeRun changed = new NativeRun(different, rightLayout, 5)) {
                                    int difference = expected[mismatch] - different[3 + mismatch];
                                    check(nativeCompare(left, 1, changed, 3, length) == difference,
                                            "exact first UTF-16 difference including vector tails");
                                    check(nativeCompare(changed, 3, left, 1, length) == -difference,
                                            "reversed native difference");
                                    check(prefix(leftRange, changed.range(3, length), length)
                                            == difference, "scalar prefix exact difference");
                                    check(nativeCompare(left, 1, changed, 3, mismatch) == 0,
                                            "mismatch outside compared prefix");
                                }
                            }
                            expectBounds(() -> compareRange.invoke(left.owner, -1,
                                    right.owner, 3, length));
                            expectBounds(() -> compareRange.invoke(left.owner, 1,
                                    right.owner, 3, rightUnits.length));
                            expectBounds(() -> compareRange.invoke(left.owner, 1,
                                    right.owner, rightUnits.length, 1));
                            expectBounds(() -> comparePrefix.invoke(leftRange, rightRange, length + 1));
                        }
                    }
                }
            }
        }
        char[] shifted = {'a', 'a', 'a', 'a', 'b'};
        try (NativeRun run = new NativeRun(shifted, LATIN_LAYOUTS[0], 1)) {
            check(nativeCompare(run, 0, run, 1, 4) == 'a' - 'b',
                    "same owner with different offsets must compare content");
        }
    }

    /** Compare non-interned tuple shapes, nested slices, shared leaves and surrogate seams. */
    private static void tupleRanges() throws Exception {
        char[] units = pattern(129, true);
        units[31] = '\ud83d';
        units[32] = '\ude42';
        try (NativeRun first = new NativeRun(units, WIDE_LAYOUTS[0], 1);
             NativeRun second = new NativeRun(units, WIDE_LAYOUTS[1], 3)) {
            Object scalar = first.range(0, units.length);
            Object left = compose(first.range(0, 32),
                    compose(first.range(32, 17), first.range(49, 80)));
            Object right = compose(compose(second.range(0, 9), second.range(9, 55)),
                    second.range(64, 65));
            Object sameLeaves = compose(compose(first.range(0, 9), first.range(9, 55)),
                    first.range(64, 65));
            for (Object a : new Object[] {scalar, left, right, sameLeaves}) {
                for (Object b : new Object[] {scalar, left, right, sameLeaves}) {
                    for (int count : LENGTHS) {
                        check(prefix(a, b, count) == 0, "tuple shape and scalar prefix parity");
                    }
                }
            }
            Method slice = method(scalar.getClass(), "slice", int.class, int.class);
            Object sliceLeft = slice.invoke(slice.invoke(left, 7, 121), 3, 107);
            Object sliceRight = slice.invoke(right, 10, 114);
            Object before = factsIfPrepared.invoke(sliceLeft);
            check(prefix(sliceLeft, sliceRight, 104) == 0, "nested cross-seam range equality");
            check((int) compareFlatPrefix.invoke(sliceLeft,
                    new String(Arrays.copyOfRange(units, 10, 114)), 104) == 0,
                    "tuple/flat nested range equality");
            check(factsIfPrepared.invoke(sliceLeft) == before,
                    "tuple comparison does not prepare range facts");
            for (int mismatch : new int[] {0, 8, 9, 31, 32, 48, 49, 63, 64, 128}) {
                char[] changedUnits = units.clone();
                changedUnits[mismatch] ^= 0x8101;
                try (NativeRun changed = new NativeRun(changedUnits, WIDE_LAYOUTS[0], 5)) {
                    Object changedTuple = compose(changed.range(0, 65), changed.range(65, 64));
                    int expected = units[mismatch] - changedUnits[mismatch];
                    check(prefix(left, changedTuple, units.length) == expected,
                            "first mismatch at either side of a tuple seam");
                    check(prefix(changedTuple, left, units.length) == -expected,
                            "reversed tuple seam difference");
                    checkBridge(units, changedUnits, left, changedTuple, expected);
                }
            }
        }
    }

    private static void immediateRanges() throws Exception {
        for (String spelling : new String[] {"a", "abc", "Az_12345", "a\u0000\u00ff"}) {
            char[] units = spelling.toCharArray();
            long packed = (long) packChars.invoke(null, units, 0, units.length);
            check(packed != 0, "fixture has an immediate representation");
            Object immediate = whole.invoke(null, immediateOf.invoke(null, packed));
            try (NativeRun atom = new NativeRun(units, LATIN_LAYOUTS[1], 1)) {
                Object scalar = atom.range(0, units.length);
                check(prefix(immediate, scalar, units.length) == 0, "immediate/native equality");
                check(prefix(scalar, immediate, units.length) == 0, "native/immediate equality");
                Object a = compose(immediate, scalar);
                Object b = compose(scalar, immediate);
                check(prefix(a, b, 2 * units.length) == 0, "mixed immediate/native tuple runs");
            }
        }
    }

    private static void bridgeRanges() throws Exception {
        char[] units = pattern(129, true);
        try (NativeRun left = new NativeRun(units, WIDE_LAYOUTS[0], 1);
             NativeRun right = new NativeRun(units, WIDE_LAYOUTS[1], 3)) {
            for (int leftLength : LENGTHS) {
                for (int rightLength : LENGTHS) {
                    checkBridge(Arrays.copyOf(units, leftLength), Arrays.copyOf(units, rightLength),
                            left.range(0, leftLength), right.range(0, rightLength),
                            leftLength - rightLength);
                }
            }
        }
    }

    /** Exercise the actual bridge's both-M3, either mixed direction, and flat fallback branches. */
    private static void checkBridge(char[] leftUnits, char[] rightUnits, Object leftStorage,
            Object rightStorage, int expected) throws Exception {
        String left = new String(leftUnits);
        String right = new String(rightUnits);
        for (Object leftBacking : new Object[] {null, leftStorage}) {
            for (Object rightBacking : new Object[] {null, rightStorage}) {
                check((int) bridgeCompareTo.invoke(null, left, right, leftBacking, rightBacking)
                        == expected, "bridge preserves exact difference and length tie-break");
            }
        }
    }

    private static void collectionKeys() throws Exception {
        stringM3 = String.class.getDeclaredField("m3");
        stringM3.setAccessible(true);
        Field feature = String.class.getDeclaredField("M3_JOINED_STRINGS");
        feature.setAccessible(true);
        boolean enabled = feature.getBoolean(null);
        List<char[]> spellings = new ArrayList<>();
        for (String value : new String[] {"", "a", "ab", "abc", "Aa", "BB", "a\u0000",
                "\u00ff", "\u0100", "\ud83d", "\ude42", "\ud83d\ude42",
                "same-long-prefix-alpha", "same-long-prefix-beta"}) {
            spellings.add(value.toCharArray());
        }
        spellings.add(pattern(129, false));
        spellings.add(pattern(129, true));
        // Equal-hash keys force hash buckets/tree bins to consult exact text, not the Java hash.
        for (int bits = 0; bits < 32; bits++) {
            char[] value = new char[10];
            for (int pair = 0; pair < 5; pair++) {
                value[2 * pair] = (bits & (1 << pair)) == 0 ? 'A' : 'B';
                value[2 * pair + 1] = (bits & (1 << pair)) == 0 ? 'a' : 'B';
            }
            spellings.add(value);
        }
        List<List<String>> variants = new ArrayList<>();
        for (char[] units : spellings) {
            String flat = flat(units);
            String scalar = new String(units);
            String sliced = new String(surrounded(units, 3, 4, 'X'))
                    .substring(3, 3 + units.length);
            int first = units.length / 3;
            int second = 2 * units.length / 3;
            String tuple = new String(units, 0, first)
                    .concat(new String(units, first, second - first))
                    .concat(new String(units, second, units.length - second));
            String differentlySplit = new String(units, 0, units.length / 2)
                    .concat(new String(units, units.length / 2, units.length - units.length / 2));
            check(stringM3.get(flat) == null, "explicit flat fixture");
            if (enabled && units.length != 0) {
                check(stringM3.get(scalar) != null, "scalar admitted in feature-on mode");
                check(stringM3.get(sliced) != null, "slice admitted in feature-on mode");
                check(stringM3.get(tuple) != null, "tuple admitted in feature-on mode");
            }
            variants.add(List.of(flat, scalar, sliced, tuple, differentlySplit));
        }
        for (int a = 0; a < variants.size(); a++) {
            for (int b = 0; b < variants.size(); b++) {
                int expected = compare(spellings.get(a), spellings.get(b));
                for (String left : variants.get(a)) {
                    check(left.hashCode() == hash(spellings.get(a)), "Java hash parity");
                    for (String right : variants.get(b)) {
                        check(left.compareTo(right) == expected, "exact String.compareTo result");
                        check(left.equals(right) == (expected == 0), "String.equals content parity");
                    }
                }
            }
        }
        for (Map<String, Integer> map : List.<Map<String, Integer>>of(
                new HashMap<>(128), new LinkedHashMap<>(), new TreeMap<>(),
                new ConcurrentHashMap<>(128))) {
            for (int form = 0; form < 5; form++) {
                map.clear();
                for (int row = 0; row < variants.size(); row++) {
                    check(map.put(variants.get(row).get(form), row) == null, "distinct key insertion");
                }
                check(map.size() == variants.size(), "collection retains colliding keys");
                for (int row = 0; row < variants.size(); row++) {
                    for (String key : variants.get(row)) {
                        check(Integer.valueOf(row).equals(map.get(key)), "lookup across key shapes");
                        check(map.containsKey(key), "containsKey across key shapes");
                        check(map.replace(key, row, row), "conditional replace across key shapes");
                    }
                    check(map.remove(variants.get(row).get((form + 1) % 5), row),
                            "conditional removal across key shapes");
                }
                check(map.isEmpty(), "all keys removed");
            }
        }
        for (Set<String> set : List.<Set<String>>of(new HashSet<>(128), new TreeSet<>())) {
            for (List<String> row : variants) {
                check(set.add(row.get(1)), "new set key");
                for (String key : row) {
                    check(set.contains(key) && !set.add(key), "set content equivalence");
                }
            }
            check(set.size() == variants.size(), "set collision cardinality");
            if (set instanceof TreeSet<String> ordered) {
                char[] previous = null;
                for (String key : ordered) {
                    char[] current = key.toCharArray();
                    check(previous == null || compare(previous, current) < 0,
                            "TreeSet follows unsigned UTF-16 order");
                    previous = current;
                }
            }
        }
        for (List<String> row : variants) {
            check(stringM3.get(row.getFirst()) == null,
                    "equality/hash/order/collections never admit flat key fixtures");
        }
    }

    /** Use the existing per-thread admission guard to reproduce a bootstrap/refused flat peer. */
    @SuppressWarnings("unchecked")
    private static String flat(char[] units) throws Exception {
        Field field = Class.forName("java.lang.M3String").getDeclaredField("ADMITTING");
        field.setAccessible(true);
        ThreadLocal<Boolean> admitting = (ThreadLocal<Boolean>) field.get(null);
        Boolean previous = admitting.get();
        admitting.set(Boolean.TRUE);
        try {
            return new String(units);
        } finally {
            if (previous == null) admitting.remove();
            else admitting.set(previous);
        }
    }

    private static Object compose(Object left, Object right) throws Exception {
        // Distinct tuple identities deliberately prevent the pool from hiding representation cases.
        return whole.invoke(null, tuple.newInstance(left, right, 17L, 19L));
    }

    private static int prefix(Object left, Object right, int count) throws Exception {
        return (int) comparePrefix.invoke(left, right, count);
    }

    private static int nativeCompare(NativeRun left, int leftStart, NativeRun right,
            int rightStart, int count) throws Exception {
        return (int) compareRange.invoke(left.owner, leftStart, right.owner, rightStart, count);
    }

    private static char[] pattern(int length, boolean wide) {
        char[] units = new char[length];
        for (int index = 0; index < length; index++) {
            units[index] = (char) ((index * 37 + 11) & 0xff);
            if (wide && index % 3 == 0) units[index] |= 0x8100;
        }
        return units;
    }

    private static char[] surrounded(char[] source, int before, int after, char sentinel) {
        char[] result = new char[before + source.length + after];
        Arrays.fill(result, sentinel);
        System.arraycopy(source, 0, result, before, source.length);
        return result;
    }

    private static int compare(char[] left, char[] right) {
        for (int index = 0; index < Math.min(left.length, right.length); index++) {
            if (left[index] != right[index]) return left[index] - right[index];
        }
        return left.length - right.length;
    }

    private static int hash(char[] units) {
        int result = 0;
        for (char unit : units) result = 31 * result + unit;
        return result;
    }

    private static void expectBounds(CheckedAction action) throws Exception {
        try {
            action.run();
            throw new AssertionError("expected bounds rejection");
        } catch (InvocationTargetException failure) {
            check(failure.getCause() instanceof IndexOutOfBoundsException,
                    "bounds checked before reading native memory");
        }
    }

    private interface CheckedAction {
        void run() throws Exception;
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    /** Test-owned immutable memory: owners are never registered with the pool or freed twice. */
    private static final class NativeRun implements AutoCloseable {
        final long allocation;
        final Object owner;

        NativeRun(char[] units, int[] layout, int misalignment) throws Exception {
            int width = layout[0];
            boolean bigEndian = layout[1] != 0;
            allocation = UNSAFE.allocateMemory(Math.max(1L, (long) units.length * width)
                    + misalignment);
            long address = allocation + misalignment;
            Object created;
            try {
                boolean latin1 = true;
                for (int index = 0; index < units.length; index++) {
                    char unit = units[index];
                    latin1 &= unit <= 0xff;
                    if (width == 1) {
                        if (unit > 0xff) throw new AssertionError("invalid Latin1 fixture");
                        UNSAFE.putByte(address + index, (byte) unit);
                    } else {
                        long at = address + ((long) index << 1);
                        UNSAFE.putByte(at, (byte) (bigEndian ? unit >>> 8 : unit));
                        UNSAFE.putByte(at + 1, (byte) (bigEndian ? unit : unit >>> 8));
                    }
                }
                // Shared numeric IDs intentionally prove nothing about equality or order.
                created = mapped.invoke(null, this, address, (byte) width, bigEndian,
                        units.length, (byte) (latin1 ? 0 : 1), hash(units), 17L, 19L);
            } catch (Throwable failure) {
                UNSAFE.freeMemory(allocation);
                throw failure;
            }
            owner = created;
        }

        Object range(int start, int length) throws Exception {
            return M3StringKeyComparisonTest.range.invoke(null, owner, start, length);
        }

        @Override
        public void close() {
            UNSAFE.freeMemory(allocation);
        }
    }
}
