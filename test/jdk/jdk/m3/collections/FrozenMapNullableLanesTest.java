/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 *
 * @test
 * @summary Nullable immutable map lanes preserve encounter order and original strict factories
 * @modules jdk.m3.collections/jdk.internal.m3.collections
 * @run main/othervm FrozenMapNullableLanesTest
 */
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicInteger;
import jdk.internal.m3.collections.FrozenMap;

public final class FrozenMapNullableLanesTest {
    private static int checks;

    public static void main(String[] args) {
        nullableViews();
        snapshotBeforeCallbacks();
        preflightAndFailures();
        originalContract();
        generatedOracle();
        storageShape();
        System.out.println("FrozenMapNullableLanesTest PASS: " + checks + " assertions");
    }

    private static void nullableViews() {
        Object[] keys = {"a", null, "b", "ignored"};
        Object[] values = {null, "null-key", 3, "ignored"};
        FrozenMap<String, Object> owner = FrozenMap.copyOfNullableLanes(keys, values, 3);
        Map<String, Object> map = owner.asMap();
        eq(new ArrayList<>(map.keySet()), Arrays.asList("a", null, "b"), "key order");
        eq(new ArrayList<>(map.values()), Arrays.asList(null, "null-key", 3), "value order");
        check(map.containsKey("a") && map.containsKey(null), "present null state/key");
        check(map.containsValue(null), "contains null value");
        eq(map.getOrDefault("a", "missing"), null, "present null default");
        eq(map.getOrDefault("z", "missing"), "missing", "absent default");
        eq(map.get(null), "null-key", "null lookup");
        eq(owner.keyAt(1), null, "ordinal null key");
        eq(owner.valueAt(0), null, "ordinal null value");
        check(map.entrySet().contains(new AbstractMap.SimpleImmutableEntry<>("a", null)), "nullable entry");
        check(map.entrySet().contains(new AbstractMap.SimpleImmutableEntry<>(null, "null-key")), "null-key entry");
        check(!map.entrySet().contains(new AbstractMap.SimpleImmutableEntry<>("z", null)), "missing nullable entry");
        check(!map.keySet().spliterator().hasCharacteristics(Spliterator.NONNULL), "nullable key flags");
        check(!map.values().spliterator().hasCharacteristics(Spliterator.NONNULL), "nullable value flags");
        check(map.entrySet().spliterator().hasCharacteristics(Spliterator.NONNULL), "entry flags");
        check(map.keySet().spliterator().hasCharacteristics(Spliterator.ORDERED), "ordered flags");
        eq(map.values().parallelStream().toList(), Arrays.asList(null, "null-key", 3), "parallel encounter order");
        keys[0] = "changed";
        values[1] = "changed";
        eq(map.get(null), "null-key", "detached values");
        check(map.containsKey("a") && !map.containsKey("changed"), "detached keys");
        fails(UnsupportedOperationException.class, () -> map.put("new", 1));
        fails(UnsupportedOperationException.class, () -> map.remove("missing"));
        fails(UnsupportedOperationException.class, () -> map.putAll(Map.of()));
        fails(UnsupportedOperationException.class, map::clear);
        fails(UnsupportedOperationException.class, () -> map.compute("a", (k, v) -> {
            throw new AssertionError("mutator callback ran");
        }));
        fails(UnsupportedOperationException.class, () -> map.replaceAll((k, v) -> {
            throw new AssertionError("mutator callback ran");
        }));
        fails(UnsupportedOperationException.class, () -> map.entrySet().iterator().next().setValue(4));
        fails(UnsupportedOperationException.class, () -> map.keySet().remove("missing"));
        fails(UnsupportedOperationException.class, () -> map.values().remove(null));
        fails(UnsupportedOperationException.class, () -> map.entrySet().iterator().remove());
        fails(UnsupportedOperationException.class, () -> owner.put(null, 1));
        fails(UnsupportedOperationException.class, () -> owner.remove(null));
    }

    private static void snapshotBeforeCallbacks() {
        Object[] keys = new Object[2];
        Object[] values = {"first", "second"};
        AtomicInteger calls = new AtomicInteger();
        Object first = new Object() {
            @Override public int hashCode() {
                calls.incrementAndGet();
                keys[1] = "changed";
                values[1] = "changed";
                return 7;
            }
        };
        keys[0] = first;
        keys[1] = "retained";
        Map<Object, String> map = FrozenMap.<Object, String>copyOfNullableLanes(keys, values, 2).asMap();
        check(calls.get() > 0, "hash witness");
        eq(new ArrayList<>(map.keySet()), Arrays.asList(first, "retained"), "copy before key callback");
        eq(map.get("retained"), "second", "both copies before callback");
        check(!map.containsKey("changed"), "callback cannot change captured keys");
    }

    private static void preflightAndFailures() {
        AtomicInteger calls = new AtomicInteger();
        Object key = new Object() {
            @Override public int hashCode() { calls.incrementAndGet(); return 1; }
        };
        fails(IndexOutOfBoundsException.class, () -> FrozenMap.copyOfNullableLanes(
                new Object[] {key}, new Object[0], 1));
        fails(IndexOutOfBoundsException.class, () -> FrozenMap.copyOfNullableLanes(
                new Object[0], new Object[] {1}, 1));
        fails(IndexOutOfBoundsException.class, () -> FrozenMap.copyOfNullableLanes(
                new Object[] {key}, new Object[] {1}, -1));
        eq(calls.get(), 0, "all geometry checked before callbacks");
        fails(NullPointerException.class, () -> FrozenMap.copyOfNullableLanes(null, new Object[0], 0));
        fails(NullPointerException.class, () -> FrozenMap.copyOfNullableLanes(new Object[0], null, 0));
        fails(IllegalArgumentException.class, () -> FrozenMap.copyOfNullableLanes(
                new Object[] {null, null}, new Object[] {1, 2}, 2));
        fails(IllegalArgumentException.class, () -> FrozenMap.copyOfNullableLanes(
                new Object[] {new String("same"), new String("same")}, new Object[] {1, 2}, 2));
        eq(FrozenMap.copyOfNullableLanes(new Object[] {null, null}, new Object[] {1, 2}, 1).size(),
                1, "unused tail ignored");
        RuntimeException hashFailure = new RuntimeException("hash");
        Object hashBomb = new Object() {
            @Override public int hashCode() { throw hashFailure; }
        };
        check(fails(RuntimeException.class, () -> FrozenMap.copyOfNullableLanes(
                new Object[] {hashBomb}, new Object[] {1}, 1)) == hashFailure, "hash identity");
        RuntimeException equalityFailure = new RuntimeException("equals");
        Object equalityBomb = new Object() {
            @Override public int hashCode() { return 0; }
            @Override public boolean equals(Object other) { throw equalityFailure; }
        };
        Object collision = new Object() { @Override public int hashCode() { return 0; } };
        check(fails(RuntimeException.class, () -> FrozenMap.copyOfNullableLanes(
                new Object[] {equalityBomb, collision}, new Object[] {1, 2}, 2)) == equalityFailure,
                "equality identity");
    }

    private static void originalContract() {
        LinkedHashMap<String, String> original = new LinkedHashMap<>();
        original.put("b", "v");
        original.put("a", "w");
        FrozenMap<String, String> old = FrozenMap.copyOf(original);
        check(!old.containsKey(null) && !old.containsValue(null), "original null queries");
        eq(old.getOrDefault(null, "default"), "default", "original null default");
        check(old.asMap().keySet().spliterator().hasCharacteristics(Spliterator.NONNULL), "original key flags");
        check(old.asMap().values().spliterator().hasCharacteristics(Spliterator.NONNULL), "original value flags");
        eq(old.asMap().keySet().stream().toList(), List.of("b", "a"), "original order");
        original.put(null, "bad");
        fails(NullPointerException.class, () -> FrozenMap.copyOf(original));
        original.remove(null);
        original.put("a", null);
        fails(NullPointerException.class, () -> FrozenMap.copyOf(original));
        check(new FrozenMap<>().asMap().keySet().spliterator().hasCharacteristics(Spliterator.NONNULL),
                "original empty factory flags");
    }

    private static void generatedOracle() {
        Random random = new Random(0x46a07a11L);
        for (int trial = 0; trial < 600; trial++) {
            int size = random.nextInt(40);
            ArrayList<Integer> order = new ArrayList<>();
            for (int i = 0; i < size; i++) order.add(i == 0 ? null : i);
            Collections.shuffle(order, random);
            Object[] keys = order.toArray();
            Object[] values = new Object[size];
            LinkedHashMap<Integer, Integer> expected = new LinkedHashMap<>();
            for (int i = 0; i < size; i++) {
                values[i] = random.nextBoolean() ? null : random.nextInt(8);
                expected.put(order.get(i), (Integer) values[i]);
            }
            Map<Integer, Integer> actual = FrozenMap.<Integer, Integer>copyOfNullableLanes(keys, values, size).asMap();
            eq(actual, expected, "map oracle");
            eq(expected, actual, "symmetric map oracle");
            eq(actual.hashCode(), expected.hashCode(), "hash oracle");
            eq(new ArrayList<>(actual.entrySet()), new ArrayList<>(expected.entrySet()), "entry order oracle");
            eq(actual.keySet().stream().toList(), new ArrayList<>(expected.keySet()), "key order oracle");
            eq(actual.values().parallelStream().toList(), new ArrayList<>(expected.values()), "value order oracle");
            for (Integer query : Arrays.asList(null, -1, 1, 8, 39)) {
                eq(actual.containsKey(query), expected.containsKey(query), "contains oracle");
                eq(actual.getOrDefault(query, -7), expected.getOrDefault(query, -7), "lookup oracle");
                eq(actual.containsValue(query), expected.containsValue(query), "value oracle");
            }
        }
    }

    private static void storageShape() {
        for (Field field : FrozenMap.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) continue;
            check(Modifier.isFinal(field.getModifiers()), "immutable field " + field.getName());
            check(!Map.class.isAssignableFrom(field.getType()), "no retained Map wrapper");
            check(!Map.Entry.class.isAssignableFrom(field.getType()), "no retained entry");
            if (field.getType().isArray()) {
                check(!Map.Entry.class.isAssignableFrom(field.getType().getComponentType()), "no entry array");
            }
        }
    }

    private static void eq(Object actual, Object expected, String label) {
        check(Objects.equals(actual, expected), label + ": " + actual + " != " + expected);
    }

    private static void check(boolean passed, String label) {
        checks++;
        if (!passed) throw new AssertionError(label);
    }

    private static Throwable fails(Class<? extends Throwable> expected, Runnable operation) {
        checks++;
        try { operation.run(); }
        catch (Throwable failure) {
            if (expected.isInstance(failure)) return failure;
            throw new AssertionError("expected " + expected.getName(), failure);
        }
        throw new AssertionError("expected " + expected.getName());
    }
}
