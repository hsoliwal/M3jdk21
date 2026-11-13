/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Genuine small-heap allocation failures against the production map classes.
 * With no arguments, fork each case under a separate 16 MiB Serial GC JVM.
 * A single child accepts {@code byte|int|object zero|nonzero|ensure};
 * {@code ensure} applies only to the byte map. No production allocation hooks
 * or transformed source fixtures are used.
 */
public final class PrimitiveMapRealOomeTest {
    private static final int BYTE = 0;
    private static final int INT = 1;
    private static final int OBJECT = 2;
    private static final int ZERO = 0;
    private static final int NONZERO = 1;
    private static final int ENSURE = 2;
    private static final int EXPECTED = 20_000;
    private static final int BALLAST_BYTES = 4 * 1024;
    private static final int MAX_BALLAST = 8192;
    private static final int RELEASED_BLOCKS = 128;
    private static final long PENDING_KEY = 0x4000_0000_0000_0000L;
    private static final long NAN_KEY = 0x7ff8_0000_0000_0001L;
    private static final Object ABSENT = new Object();
    private static final Object ZERO_VALUE = new Object();
    private static final Object PENDING_VALUE = new Object();
    private static final Object[] VALUES = {new Object(), new Object(), new Object(), null};
    private static int assertions;

    private PrimitiveMapRealOomeTest() { }

    public static void main(String[] arguments) throws Exception {
        if (arguments.length == 0) {
            forkCases();
            return;
        }
        if (arguments.length != 2) throw new IllegalArgumentException("owner operation");
        int owner = switch (arguments[0]) {
            case "byte" -> BYTE;
            case "int" -> INT;
            case "object" -> OBJECT;
            default -> throw new IllegalArgumentException("byte, int or object");
        };
        int operation = switch (arguments[1]) {
            case "zero" -> ZERO;
            case "nonzero" -> NONZERO;
            case "ensure" -> ENSURE;
            default -> throw new IllegalArgumentException("zero, nonzero or ensure");
        };
        if (operation == ENSURE && owner != BYTE)
            throw new IllegalArgumentException("ensure is only supported for byte");
        warmup();
        Fixture fixture = new Fixture(owner, operation);
        Snapshot before = Snapshot.of(fixture.map);
        before.assertUnchanged(fixture.map);
        fixture.verifyExisting();
        ok(fixture.capacity() == 32_768, "exact growth precondition");
        ok(fixture.size() == fixture.threshold, "one insertion must trigger growth");
        ok(!fixture.containsPending(), "pending key is absent");

        // Fixed holder capacity prevents holder growth from being the pressure
        // boundary. Release a bounded amount of headroom while retaining ballast.
        // The VM may reject either replacement lane; both failures must preserve
        // every map field. Deterministic allocation-site tests cover each lane.
        byte[][] ballast = new byte[MAX_BALLAST][];
        int retained = 0;
        boolean ballastOome = false;
        try {
            while (retained < ballast.length) {
                ballast[retained] = new byte[BALLAST_BYTES];
                retained++;
            }
        } catch (OutOfMemoryError expected) {
            ballastOome = true;
        }
        boolean sufficientBallast = retained >= RELEASED_BLOCKS;
        if (sufficientBallast) {
            for (int i = 0; i < RELEASED_BLOCKS; i++) ballast[--retained] = null;
        }
        boolean mutationOome = false;
        try {
            if (sufficientBallast) fixture.attempt();
        } catch (OutOfMemoryError expected) {
            mutationOome = true;
        } finally {
            // No reflection, diagnostic construction or assertions while ballast
            // is retained: those must not be mistaken for map allocation failure.
            for (int i = 0; i < ballast.length; i++) ballast[i] = null;
            ballast = null;
        }
        System.gc();
        ok(ballastOome && sufficientBallast && mutationOome,
                "must trigger actual JVM OOME during map growth");
        System.out.printf("M3_PRIMITIVE_MAP_REAL_OOME_OBSERVED owner=%s operation=%s ballast=%d%n",
                arguments[0], arguments[1], retained);
        before.assertUnchanged(fixture.map);
        fixture.verifyExisting();
        ok(!fixture.containsPending(), "failed insertion must remain absent");
        fixture.attempt();
        ok(fixture.capacity() == 65_536, "retry grows exactly once");
        ok(fixture.size() == fixture.threshold + (operation == ENSURE ? 0 : 1),
                "retry size accounting");
        fixture.verifyExisting();
        fixture.verifyRetry();
        System.out.printf("M3_PRIMITIVE_MAP_REAL_OOME_PASS owner=%s operation=%s assertions=%d ballast=%d%n",
                arguments[0], arguments[1], assertions, retained);
    }

    private static void forkCases() throws Exception {
        String executable = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        String[][] cases = {{"byte", "zero"}, {"byte", "nonzero"}, {"byte", "ensure"},
                {"int", "zero"}, {"int", "nonzero"}, {"object", "zero"}, {"object", "nonzero"}};
        int failures = 0;
        for (String[] mode : cases) {
            Process child = new ProcessBuilder(executable, "-XX:-UsePerfData", "-Xms16m", "-Xmx16m",
                    "-XX:+UseSerialGC", "-cp", System.getProperty("java.class.path"),
                    PrimitiveMapRealOomeTest.class.getName(), mode[0], mode[1]).inheritIO().start();
            if (!child.waitFor(30, TimeUnit.SECONDS)) {
                child.destroyForcibly();
                child.waitFor();
                throw new AssertionError("small-heap child exceeded 30 seconds");
            }
            if (child.exitValue() != 0) failures++;
        }
        ok(failures == 0, "small-heap child failures: " + failures);
    }

    private static void warmup() {
        // Initialize both branches of every growth path and PrimitiveKind's
        // switch machinery before the heap-pressure interval.
        LongByteHashMap bytes = new LongByteHashMap(0, 0.125f);
        bytes.put(1, (byte) 1);
        bytes.put(0, (byte) 2);
        bytes.put(2, (byte) 3);
        bytes.ensureCapacity(40);
        LongIntHashMap ints = new LongIntHashMap(0, 0.125f);
        ints.put(1, 1);
        ints.put(0, 2);
        ints.put(2, 3);
        PrimitiveObjectHashMap<Object> objects = new PrimitiveObjectHashMap<>(PrimitiveKind.DOUBLE, 0, 0.125f);
        objects.put(1, VALUES[0], ABSENT);
        objects.put(0, ZERO_VALUE, ABSENT);
        objects.put(2, VALUES[1], ABSENT);
    }

    private static final class Fixture {
        final int owner;
        final int operation;
        final Object map;
        final LongByteHashMap bytes;
        final LongIntHashMap ints;
        final PrimitiveObjectHashMap<Object> objects;
        final int threshold;
        final int nonzeroEntries;
        final boolean hadZero;

        Fixture(int owner, int operation) throws ReflectiveOperationException {
            this.owner = owner;
            this.operation = operation;
            bytes = owner == BYTE ? new LongByteHashMap(EXPECTED) : null;
            ints = owner == INT ? new LongIntHashMap(EXPECTED) : null;
            objects = owner == OBJECT
                    ? new PrimitiveObjectHashMap<>(PrimitiveKind.DOUBLE, EXPECTED, 0.65f) : null;
            map = owner == BYTE ? bytes : owner == INT ? ints : objects;
            Field field = map.getClass().getDeclaredField("threshold");
            field.setAccessible(true);
            threshold = field.getInt(map);
            hadZero = operation != ZERO;
            if (hadZero) {
                if (owner == BYTE) bytes.put(0, (byte) 99);
                else if (owner == INT) ints.put(0, 99);
                else objects.put(0, ZERO_VALUE, ABSENT);
            }
            nonzeroEntries = threshold - (hadZero ? 1 : 0);
            for (int i = 1; i <= nonzeroEntries; i++) {
                long key = key(i);
                if (owner == BYTE) bytes.put(key, (byte) (31 * i + 7));
                else if (owner == INT) ints.put(key, 31 * i + 7);
                else objects.put(key, VALUES[i & 3], ABSENT);
            }
        }

        int capacity() { return owner == BYTE ? bytes.capacity() : owner == INT ? ints.capacity() : objects.capacity(); }
        int size() { return owner == BYTE ? bytes.size() : owner == INT ? ints.size() : objects.size(); }
        long pending() { return operation == ZERO ? 0L : PENDING_KEY; }
        boolean containsPending() {
            return owner == BYTE ? bytes.containsKey(pending())
                    : owner == INT ? ints.containsKey(pending()) : objects.containsKeyBits(pending());
        }

        void attempt() {
            if (operation == ENSURE) bytes.ensureCapacity(threshold + 1);
            else if (owner == BYTE) bytes.put(pending(), (byte) 47);
            else if (owner == INT) ints.put(pending(), 47);
            else objects.put(pending(), PENDING_VALUE, ABSENT);
        }

        void verifyExisting() {
            for (int i = 1; i <= nonzeroEntries; i++) {
                long key = key(i);
                if (owner == BYTE) ok(bytes.containsKey(key)
                        && bytes.get(key) == (byte) (31 * i + 7), "retained byte entry");
                else if (owner == INT) ok(ints.containsKey(key)
                        && ints.getOrDefault(key, -1) == 31 * i + 7, "retained int entry");
                else ok(objects.containsKeyBits(key)
                        && objects.getOrDefault(key, ABSENT) == VALUES[i & 3], "retained reference entry");
            }
            if (hadZero) {
                if (owner == BYTE) ok(bytes.containsKey(0) && bytes.get(0) == 99, "retained zero byte");
                else if (owner == INT) ok(ints.containsKey(0) && ints.getOrDefault(0, -1) == 99, "retained zero int");
                else ok(objects.containsKeyBits(0) && objects.getOrDefault(0, ABSENT) == ZERO_VALUE,
                        "retained zero reference");
            }
            if (owner == OBJECT) {
                boolean rawNan = false;
                for (double value : (double[]) objects.keyArray()) {
                    if (Double.doubleToRawLongBits(value) == NAN_KEY) rawNan = true;
                }
                ok(rawNan, "stored NaN payload survives growth");
                ok(objects.getOrDefault(0x7ff8_0000_0000_0002L, ABSENT) == VALUES[2],
                        "alternate NaN payload retains equality and reference identity");
            }
        }

        void verifyRetry() {
            if (operation == ENSURE) {
                ok(!containsPending(), "capacity retry does not insert");
            } else if (owner == BYTE) {
                ok(containsPending() && bytes.get(pending()) == 47, "retry inserts byte");
            } else if (owner == INT) {
                ok(containsPending() && ints.getOrDefault(pending(), -1) == 47, "retry inserts int");
            } else {
                ok(containsPending() && objects.getOrDefault(pending(), ABSENT) == PENDING_VALUE,
                        "retry inserts exact object reference");
            }
        }
    }

    private static long key(int index) {
        return index == 1 ? Long.MIN_VALUE : index == 2 ? NAN_KEY : index;
    }

    private record State(Field field, Object original, Object copy) { }

    private record Snapshot(List<State> states) {
        static Snapshot of(Object map) throws IllegalAccessException {
            List<State> states = new ArrayList<>();
            for (Field field : map.getClass().getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                field.setAccessible(true);
                Object original = field.get(map);
                Object copy = original;
                if (original != null && original.getClass().isArray()) {
                    int length = Array.getLength(original);
                    copy = Array.newInstance(original.getClass().getComponentType(), length);
                    System.arraycopy(original, 0, copy, 0, length);
                }
                states.add(new State(field, original, copy));
            }
            return new Snapshot(states);
        }

        void assertUnchanged(Object map) throws IllegalAccessException {
            for (State state : states) {
                Object actual = state.field.get(map);
                if (state.original != null && state.original.getClass().isArray()) {
                    ok(actual == state.original, state.field.getName() + " lane identity");
                    ok(sameContents(state.copy, actual), state.field.getName() + " raw lane contents");
                } else if (state.field.getType().isPrimitive()) {
                    ok(Objects.equals(state.original, actual), state.field.getName() + " scalar state");
                } else {
                    ok(state.original == actual, state.field.getName() + " reference identity");
                }
            }
        }
    }

    private static boolean sameContents(Object expected, Object actual) {
        if (expected instanceof long[] values) return Arrays.equals(values, (long[]) actual);
        if (expected instanceof int[] values) return Arrays.equals(values, (int[]) actual);
        if (expected instanceof byte[] values) return Arrays.equals(values, (byte[]) actual);
        if (expected instanceof double[] values) {
            double[] other = (double[]) actual;
            if (values.length != other.length) return false;
            for (int i = 0; i < values.length; i++) {
                if (Double.doubleToRawLongBits(values[i]) != Double.doubleToRawLongBits(other[i])) return false;
            }
            return true;
        }
        if (expected instanceof Object[] values) {
            Object[] other = (Object[]) actual;
            if (values.length != other.length) return false;
            for (int i = 0; i < values.length; i++) if (values[i] != other[i]) return false;
            return true;
        }
        throw new AssertionError("unexpected lane kind");
    }

    private static void ok(boolean condition, String description) {
        assertions++;
        if (!condition) throw new AssertionError(description);
    }
}
