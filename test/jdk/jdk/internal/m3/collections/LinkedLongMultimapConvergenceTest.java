/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * @test
 * @summary Differential contracts for the primitive globally ordered multimap.
 * @modules jdk.m3.collections/jdk.internal.m3.collections
 * @run main/othervm --add-opens=jdk.m3.collections/jdk.internal.m3.collections=ALL-UNNAMED LinkedLongMultimapConvergenceTest
 */

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import jdk.internal.m3.collections.LinkedLongMultimap;

public class LinkedLongMultimapConvergenceTest {
    private record Pair(long key, long value) { }
    private static long checks;

    public static void main(String[] args) throws Exception {
        extremesAndCopies();
        for (long seed : new long[] {17, 0x5eed, 0x20261115}) {
            differential(seed);
        }
        callbacksAndExceptions();
        noRetainedPerMappingObjects();
        System.out.println("LINKED_LONG_MULTIMAP_PASS checks=" + checks
                + " random_operations=60000");
    }

    private static void extremesAndCopies() {
        LinkedLongMultimap map = new LinkedLongMultimap();
        check(map.isEmpty(), "empty constructor");
        array(new long[0], map.removeAll(0), "empty removal");
        long[] keys = {0, Long.MIN_VALUE, Long.MAX_VALUE, -1, 0, Long.MIN_VALUE};
        long[] values = {Long.MIN_VALUE, 0, -1, Long.MAX_VALUE, 7, 0};
        List<Pair> model = new ArrayList<>();
        for (int i = 0; i < keys.length; i++) {
            check(map.put(keys[i], values[i]), "duplicates accepted");
            model.add(new Pair(keys[i], values[i]));
        }
        verify(map, model);
        long[] copy = map.get(0);
        copy[0] = 100;
        array(new long[] {Long.MIN_VALUE, 7}, map.get(0), "detached get");
        long[] removed = map.removeAll(Long.MIN_VALUE);
        array(new long[] {0, 0}, removed, "duplicate removal order");
        model.removeIf(p -> p.key == Long.MIN_VALUE);
        map.put(Long.MIN_VALUE, 55);
        model.add(new Pair(Long.MIN_VALUE, 55));
        removed[0] = 999;
        verify(map, model);
        array(new long[] {0, Long.MAX_VALUE, -1, Long.MIN_VALUE},
                map.keysToArray(), "removed key reinserts last");
        map.clear();
        map.clear();
        verify(map, List.of());
        map.put(Long.MIN_VALUE, Long.MAX_VALUE);
        verify(map, List.of(new Pair(Long.MIN_VALUE, Long.MAX_VALUE)));
    }

    private static void differential(long seed) {
        Random random = new Random(seed);
        LinkedLongMultimap map = new LinkedLongMultimap();
        List<Pair> model = new ArrayList<>();
        for (int step = 0; step < 20_000; step++) {
            long key = number(random);
            long value = number(random);
            switch (random.nextInt(10)) {
                case 0, 1, 2, 3, 4 -> {
                    check(map.put(key, value), "put result");
                    model.add(new Pair(key, value));
                }
                case 5, 6 -> {
                    array(values(model, key), map.removeAll(key), "removeAll snapshot");
                    model.removeIf(p -> p.key == key);
                }
                case 7 -> {
                    array(values(model, key), map.get(key), "get");
                    check(map.containsKey(key) == model.stream().anyMatch(p -> p.key == key),
                            "containsKey");
                    check(map.containsEntry(key, value) == model.contains(new Pair(key, value)),
                            "containsEntry");
                }
                case 8 -> {
                    if (random.nextInt(8) == 0) {
                        map.clear();
                        model.clear();
                    }
                }
                default -> { }
            }
            verify(map, model);
        }
    }

    private static long number(Random random) {
        return switch (random.nextInt(12)) {
            case 0 -> Long.MIN_VALUE;
            case 1 -> Long.MAX_VALUE;
            case 2 -> 0;
            case 3 -> -1;
            default -> random.nextInt(41) - 20;
        };
    }

    private static long[] values(List<Pair> rows, long key) {
        return rows.stream().filter(p -> p.key == key).mapToLong(Pair::value).toArray();
    }

    private static void verify(LinkedLongMultimap map, List<Pair> model) {
        check(map.size() == model.size(), "pair count");
        check(map.isEmpty() == model.isEmpty(), "empty state");
        List<Pair> visited = new ArrayList<>();
        map.forEach((key, value) -> visited.add(new Pair(key, value)));
        check(visited.equals(model), "global insertion order");
        LinkedHashSet<Long> keys = new LinkedHashSet<>();
        for (Pair pair : model) keys.add(pair.key);
        array(keys.stream().mapToLong(Long::longValue).toArray(), map.keysToArray(),
                "first-key order");
    }

    private static void callbacksAndExceptions() {
        LinkedLongMultimap map = new LinkedLongMultimap();
        expect(NullPointerException.class, () -> map.forEach(null));
        for (int mutation = 0; mutation < 3; mutation++) {
            map.clear();
            map.put(0, 10);
            map.put(1, 20);
            int action = mutation;
            expect(ConcurrentModificationException.class, () -> map.forEach((k, v) -> {
                if (action == 0) map.put(0, 30);
                else if (action == 1) map.removeAll(k);
                else map.clear();
            }));
        }
        map.clear();
        map.put(1, 2);
        IllegalStateException signal = new IllegalStateException("callback");
        try {
            map.forEach((k, v) -> { throw signal; });
            throw new AssertionError("callback failure lost");
        } catch (IllegalStateException caught) {
            check(caught == signal, "callback exception identity");
        }
        array(new long[] {2}, map.get(1), "callback throw preserves owner");
    }

    private static void noRetainedPerMappingObjects() throws Exception {
        for (Field field : LinkedLongMultimap.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) continue;
            Class<?> type = field.getType();
            check(type.isPrimitive() || type.isArray() && type.getComponentType().isPrimitive()
                    || type.getName().equals("jdk.m3.collections.LongIntHashMap"),
                    "primitive-only owner field: " + field.getName());
        }
        LinkedLongMultimap map = new LinkedLongMultimap();
        for (int i = 0; i < 4097; i++) map.put(i % 7, i);
        int capacity = lane(map, "keyLane").length;
        for (int key = 0; key < 7; key++) map.removeAll(key);
        for (long value : lane(map, "keyLane")) check(value == 0, "removed key cleared");
        for (long value : lane(map, "valueLane")) check(value == 0, "removed value cleared");
        for (int i = 0; i < 4097; i++) map.put(i % 7, -i);
        check(lane(map, "keyLane").length == capacity, "free slots reused without growth");
        map.clear();
        for (long word : lane(map, "stateMaskWords")) check(word == 0, "clear releases states");
    }

    private static long[] lane(Object owner, String name) throws Exception {
        Field field = owner.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return (long[]) field.get(owner);
    }

    private static void array(long[] expected, long[] actual, String message) {
        check(Arrays.equals(expected, actual), message);
    }

    private static void expect(Class<? extends Throwable> type, Runnable action) {
        try { action.run(); }
        catch (Throwable failure) {
            check(type.isInstance(failure), "expected " + type + ", got " + failure);
            return;
        }
        throw new AssertionError("missing " + type.getName());
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
