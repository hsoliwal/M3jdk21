/* SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 Hitesh Soliwal and contributors */
package com.m3.collections.interop;

import static org.junit.jupiter.api.Assertions.*;
import com.google.common.collect.BiMap;
import com.google.common.collect.Maps;
import com.m3.collections.M3LinkedHashMap;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.*;
import org.junit.jupiter.api.Test;
import org.springframework.util.MultiValueMap;

final class M3BiMapTest {
    private record Key(int id) { @Override public int hashCode() { return 7; } }
    private record Outcome(Object value, Class<?> error) { }
    @FunctionalInterface private interface Call { Object run(); }
    private static Outcome outcome(Call call) {
        try { return new Outcome(call.run(), null); }
        catch (RuntimeException ex) { return new Outcome(null, ex.getClass()); }
    }
    private static <K, V> void assertBijection(BiMap<K, V> map) {
        assertSame(map, map.inverse().inverse());
        assertEquals(map.size(), map.inverse().size());
        assertEquals(map.keySet(), map.inverse().values());
        assertEquals(map.values(), map.inverse().keySet());
        for (var e : map.entrySet()) {
            assertTrue(map.inverse().containsKey(e.getValue()));
            assertEquals(e.getKey(), map.inverse().get(e.getValue()));
        }
    }
    private static Key key(Random random) { return random.nextInt(9) == 0 ? null : new Key(random.nextInt(32)); }

    @Test void differentialAllMutatorsBothDirections() {
        long operations = 0;
        for (int seed = 0; seed < 16; seed++) {
            BiMap<Key, Key> actual = M3BiMap.create();
            BiMap<Key, Key> expected = com.google.common.collect.HashBiMap.create();
            Random random = new Random(0xB1A900L + seed);
            for (int i = 0; i < 3000; i++) {
                Key k = key(random), v = key(random), old = key(random);
                int operation = random.nextInt(19);
                boolean reverse = random.nextBoolean();
                BiMap<Key, Key> a = reverse ? actual.inverse() : actual;
                BiMap<Key, Key> e = reverse ? expected.inverse() : expected;
                assertEquals(outcome(() -> apply(e, operation, k, v, old)),
                        outcome(() -> apply(a, operation, k, v, old)), "seed=" + seed + " step=" + i + " op=" + operation);
                assertEquals(expected, actual); assertEquals(actual, expected);
                assertEquals(expected.inverse(), actual.inverse());
                assertEquals(expected.hashCode(), actual.hashCode());
                assertEquals(expected.getOrDefault(k, old), actual.getOrDefault(k, old));
                assertBijection(actual); operations++;
            }
        }
        assertEquals(48000, operations);
        System.out.println("BIMAP_DIFFERENTIAL_OPERATIONS=" + operations);
    }
    private static Object apply(BiMap<Key, Key> map, int op, Key k, Key v, Key old) {
        return switch (op) {
            case 0 -> map.put(k, v);
            case 1 -> map.forcePut(k, v);
            case 2 -> map.remove(k);
            case 3 -> map.get(k);
            case 4 -> map.putIfAbsent(k, v);
            case 5 -> map.remove(k, v);
            case 6 -> map.replace(k, v);
            case 7 -> map.replace(k, old, v);
            case 8 -> map.compute(k, (x, y) -> v);
            case 9 -> map.computeIfAbsent(k, x -> v);
            case 10 -> map.computeIfPresent(k, (x, y) -> v);
            case 11 -> map.merge(k, v, (x, y) -> y);
            case 12 -> map.values().remove(v);
            case 13 -> map.keySet().remove(k);
            case 14 -> map.entrySet().remove(new AbstractMap.SimpleImmutableEntry<>(k, v));
            case 15 -> {
                Object result = null;
                for (var entry : map.entrySet()) if (Objects.equals(entry.getKey(), k)) { result = entry.setValue(v); break; }
                yield result;
            }
            case 16 -> {
                boolean removed = false;
                var iterator = map.keySet().iterator();
                while (iterator.hasNext()) if (Objects.equals(iterator.next(), k)) { iterator.remove(); removed = true; break; }
                yield removed;
            }
            case 17 -> map.containsValue(v);
            default -> { if (k == null && v == null) map.clear(); yield map.size(); }
        };
    }
    @Test void nullsAndAllForcePutSizeOutcomes() {
        var map = M3BiMap.<String, Integer>create();
        map.put(null, 1); map.put("b", null); assertBijection(map);
        assertThrows(IllegalArgumentException.class, () -> map.put("c", null));
        assertEquals(2, map.size());
        assertEquals(1, map.forcePut(null, 2)); assertEquals(2, map.size());
        assertNull(map.forcePut("c", 3)); assertEquals(3, map.size());
        assertEquals(3, map.forcePut("c", 2)); assertEquals(2, map.size());
        assertFalse(map.containsKey(null)); assertBijection(map);
    }
    @Test void viewsStayLiveAndMutateBothDirections() {
        var map = M3BiMap.<String, Integer>create();
        var keys = map.keySet(); var values = map.values(); var entries = map.entrySet(); var inverse = map.inverse();
        map.put("a", 1); inverse.put(2, "b");
        assertEquals(Set.of("a", "b"), keys); assertEquals(Set.of(1, 2), values);
        entries.remove(new AbstractMap.SimpleEntry<>("a", 1)); assertFalse(inverse.containsKey(1));
        values.clear(); assertTrue(map.isEmpty());
        inverse.put(null, null); assertTrue(keys.contains(null)); assertTrue(entries.contains(new AbstractMap.SimpleEntry<>(null, null)));
        keys.remove(null); assertTrue(inverse.isEmpty());
    }
    @Test void keyValueEntryArrayAndStreamMutations() {
        var map = M3BiMap.create(Map.of(1, 11, 2, 22, 3, 33));
        assertEquals(map.values(), new HashSet<>(Arrays.asList(map.values().toArray(new Integer[0]))));
        assertEquals(map.entrySet(), map.entrySet().stream().collect(Collectors.toSet()));
        Map.Entry<Integer, Integer> entry = map.entrySet().iterator().next();
        int key = entry.getKey(); entry.setValue(99); assertEquals(key, map.inverse().get(99));
        assertThrows(IllegalArgumentException.class, () -> map.inverse().put(100, key));
        assertTrue(map.values().removeIf(v -> v == 99)); assertFalse(map.containsKey(key));
        assertBijection(map);
    }
    @Test void entryUpdatesPermitIteratorContinuationAndRemove() {
        var map = M3BiMap.<Integer, String>create(); map.put(1, "a"); map.put(2, "b"); map.put(3, "c");
        var it = map.entrySet().iterator(); var first = it.next(); first.setValue("z"); it.remove();
        assertFalse(map.inverse().containsKey("z")); assertTrue(it.hasNext());
        var second = it.next(); assertEquals(2, second.getKey()); second.setValue("y");
        assertEquals(3, it.next().getKey()); assertFalse(it.hasNext()); assertBijection(map);
        assertThrows(UnsupportedOperationException.class, () -> map.keySet().add(4));
        assertThrows(UnsupportedOperationException.class, () -> map.values().add("x"));
    }
    @Test void entrySetDuplicateRefusalAndStaleIterator() {
        var map = M3BiMap.<String, Integer>create(); map.put("a", 1); map.put("b", 2);
        var it = map.entrySet().iterator(); var entry = it.next();
        assertThrows(IllegalArgumentException.class, () -> entry.setValue(2)); assertEquals(Map.of("a", 1, "b", 2), map);
        map.inverse().put(3, "c"); assertThrows(ConcurrentModificationException.class, it::hasNext);
        assertThrows(ConcurrentModificationException.class, () -> entry.setValue(4)); assertBijection(map);
    }
    @Test void bulkViewRemovalRetentionAndSelfArguments() {
        for (int mode = 0; mode < 3; mode++) {
            var map = M3BiMap.create(Map.of(1, 10, 2, 20, 3, 30));
            Collection<?> view = mode == 0 ? map.keySet() : mode == 1 ? map.values() : map.entrySet();
            assertFalse(view.retainAll(view)); assertTrue(view.removeAll(view));
            assertTrue(map.isEmpty()); assertTrue(map.inverse().isEmpty());
        }
        var map = M3BiMap.create(Map.of(1, 10, 2, 20, 3, 30));
        map.keySet().retainAll(Set.of(1, 3)); map.values().removeAll(Set.of(10));
        assertEquals(Map.of(3, 30), map); assertBijection(map);
    }
    @Test void replaceAllSupportsCyclesAndRetainedViews() {
        var map = M3BiMap.<Integer, Integer>create(); for (int i = 0; i < 64; i++) map.put(i, i);
        var values = map.values(); var inverse = map.inverse(); var keys = map.keySet();
        var oldIterator = keys.iterator(); map.replaceAll((k, v) -> (v + 1) % 64);
        assertThrows(ConcurrentModificationException.class, oldIterator::next);
        assertSame(inverse, map.inverse()); assertEquals(64, values.size());
        for (int i = 0; i < 64; i++) assertEquals(i, inverse.get((i + 1) % 64));
        inverse.replaceAll((v, k) -> k + 100); assertTrue(keys.contains(100)); assertBijection(map);
    }
    @Test void replaceAllDuplicateAndCallbackFailureDoNotPublish() {
        var map = M3BiMap.<String, Integer>create(); map.put(null, 1); map.put("b", 2);
        var before = new HashMap<>(map); var inverse = map.inverse();
        assertThrows(IllegalArgumentException.class, () -> map.replaceAll((k, v) -> 7));
        assertEquals(before, map); assertBijection(map); assertSame(inverse, map.inverse());
        assertThrows(IllegalArgumentException.class, () -> map.replaceAll((k, v) -> null));
        assertEquals(before, map);
        RuntimeException marker = new RuntimeException("marker");
        assertSame(marker, assertThrows(RuntimeException.class, () -> map.replaceAll((k, v) -> { if (v == 2) throw marker; return 4; })));
        assertEquals(before, map); assertBijection(map);
    }
    @Test void replaceAllReentrantMutationIsDetectedNotRolledBack() {
        var map = M3BiMap.create(Map.of(1, 10));
        assertThrows(ConcurrentModificationException.class, () -> map.replaceAll((k, v) -> { map.put(2, 20); return 30; }));
        assertEquals(Map.of(1, 10, 2, 20), map); assertBijection(map);
    }
    @Test void sequentialPutAllKeepsSuccessfulPrefix() {
        var input = new LinkedHashMap<String, Integer>(); input.put("a", 1); input.put("b", 1);
        var map = M3BiMap.<String, Integer>create();
        assertThrows(IllegalArgumentException.class, () -> map.putAll(input));
        assertEquals(Map.of("a", 1), map); assertBijection(map);
        map.putAll(map); assertEquals(1, map.size());
    }
    @Test void insertionOrderAndCanonicalRepresentative() {
        var map = M3BiMap.<Key, Key>create(); var key = new Key(1); var value = new Key(2); map.put(key, value);
        var successor = new Key(3); assertSame(value, map.put(new Key(1), successor));
        assertSame(key, map.keySet().iterator().next()); assertSame(key, map.inverse().get(successor));
        map.put(new Key(4), new Key(5));
        assertEquals(List.of(key, new Key(4)), new ArrayList<>(map.keySet()));
        assertEquals(List.of(successor, new Key(5)), new ArrayList<>(map.values()));
    }
    @Test void copiesAreIndependentAndUnmodifiableClosure() {
        var original = M3BiMap.<String, Integer>create(); original.put(null, 1); original.put("b", null);
        var copy = M3BiMaps.copyOf(original); var readOnly = M3BiMaps.unmodifiableCopyOf(original);
        original.clear(); assertEquals(2, copy.size()); assertEquals(2, readOnly.size());
        assertThrows(UnsupportedOperationException.class, () -> readOnly.inverse().forcePut(3, "x"));
        assertThrows(UnsupportedOperationException.class, () -> readOnly.entrySet().iterator().next().setValue(3));
        copy.clear(); assertEquals(2, readOnly.size()); assertBijection(readOnly);
    }
    @Test void collectorParallelAndIndependentPartials() {
        var input = IntStream.range(0, 8192).boxed().toList();
        var sequential = input.stream().collect(M3BiMaps.toBiMap(Function.identity(), i -> "v" + i));
        var parallel = input.parallelStream().collect(M3BiMaps.toBiMap(Function.identity(), i -> "v" + i));
        assertEquals(sequential, parallel); assertEquals(input, new ArrayList<>(parallel.keySet()));
        var collector = M3BiMaps.<Integer, Integer, Integer>toBiMap(Function.identity(), Function.identity());
        assertFalse(collector.characteristics().contains(Collector.Characteristics.CONCURRENT));
        assertBijection(parallel);
    }
    @Test void collectorRejectsBothDuplicateSidesAndAllowsNullOnce() {
        assertThrows(IllegalArgumentException.class, () -> Stream.of(1, 1).collect(M3BiMaps.toBiMap(Function.identity(), Function.identity())));
        assertThrows(IllegalArgumentException.class, () -> Stream.of(1, 2).parallel().collect(M3BiMaps.toBiMap(Function.identity(), x -> 0)));
        var map = Stream.of(1, 2).collect(M3BiMaps.toBiMap(i -> i == 1 ? null : i, i -> i == 2 ? null : i));
        assertEquals(1, map.get(null)); assertEquals(2, map.inverse().get(null)); assertBijection(map);
    }
    @Test void actualEclipseImportAndLiveMapBridge() {
        var donor = org.eclipse.collections.impl.bimap.mutable.HashBiMap.<String, Integer>newMap();
        donor.put("a", 1); donor.put("b", 2);
        var map = M3BiMaps.fromEclipse(donor); donor.clear(); assertEquals(2, map.size());
        var eclipse = M3BiMaps.asEclipseMap(map);
        eclipse.updateValue("a", () -> 0, value -> value + 10);
        assertEquals("a", map.inverse().get(11));
        assertThrows(IllegalArgumentException.class, () -> eclipse.put("c", 2));
        eclipse.asUnmodifiable(); assertBijection(map);
    }
    @Test void actualSpringSingleValueBridgeKeepsBimapUniqueness() {
        var owner = M3BiMap.<String, Integer>create();
        var spring = MultiValueMap.fromSingleValue(owner);
        spring.add("a", 1); spring.set("b", 2);
        assertThrows(IllegalArgumentException.class, () -> spring.set("c", 2));
        owner.inverse().put(3, "c"); assertEquals(List.of(3), spring.get("c"));
        spring.remove("a"); assertFalse(owner.inverse().containsKey(1)); assertBijection(owner);
    }
    @Test void guavaFilterConverterAndInverseCompositions() {
        var map = M3BiMap.<Integer, String>create(); map.put(1, "a"); map.put(2, "b"); map.put(3, "c");
        var filtered = Maps.filterKeys((BiMap<Integer, String>) map, k -> k % 2 != 0);
        assertEquals(Map.of(1, "a", 3, "c"), filtered);
        filtered.inverse().put("d", 5); assertEquals("d", map.get(5));
        assertThrows(IllegalArgumentException.class, () -> filtered.put(6, "e"));
        var converter = Maps.asConverter(map); assertEquals("b", converter.convert(2)); assertEquals(2, converter.reverse().convert("b"));
        filtered.clear(); assertEquals(Map.of(2, "b"), map); assertBijection(map);
    }
    @Test void synchronizedWrapperDelegatesCommonLock() throws Exception {
        var map = M3BiMaps.<Integer, Integer>synchronizedMap();
        try (var executor = Executors.newFixedThreadPool(4)) {
            var work = new ArrayList<Future<?>>();
            for (int worker = 0; worker < 4; worker++) {
                final int base = worker * 256;
                work.add(executor.submit(() -> { for (int i = base; i < base + 256; i++) { if ((i & 1) == 0) map.put(i, i + 10000); else map.inverse().put(i + 10000, i); } }));
            }
            for (var future : work) future.get(15, TimeUnit.SECONDS);
        }
        synchronized (map) { assertEquals(1024, map.size()); assertBijection(map); }
    }
    @Test void emptyValidationAndIteratorProtocol() {
        var map = M3BiMap.<String, Integer>create(); var iterator = map.entrySet().iterator();
        assertFalse(iterator.hasNext()); assertThrows(NoSuchElementException.class, iterator::next);
        assertThrows(IllegalStateException.class, iterator::remove);
        assertThrows(NullPointerException.class, () -> map.replaceAll(null));
        assertThrows(NullPointerException.class, () -> map.forEach(null));
        assertThrows(NullPointerException.class, () -> map.putAll(null));
        assertThrows(NullPointerException.class, () -> M3BiMaps.toBiMap(null, Function.identity()));
        assertThrows(NullPointerException.class, () -> M3BiMaps.fromEclipse(null));
        assertThrows(NullPointerException.class, () -> M3BiMaps.asEclipseMap(null));
    }
    @Test void entryArraysCannotBypassInverseMaintenance() {
        var map = M3BiMap.<Integer, String>create(); map.put(1, "a"); map.put(2, "b");
        Object[] array = map.entrySet().toArray();
        @SuppressWarnings("unchecked") Map.Entry<Integer, String> entry = (Map.Entry<Integer, String>) array[0];
        assertThrows(IllegalArgumentException.class, () -> entry.setValue("b"));
        entry.setValue("z"); assertEquals(1, map.inverse().get("z"));
        assertFalse(map.inverse().containsKey("a")); assertBijection(map);
    }
    @Test void forEachAndOwnershipShape() throws Exception {
        var map = M3BiMap.create(Map.of(1, 10, 2, 20)); var copy = new HashMap<Integer, Integer>();
        map.forEach(copy::put); assertEquals(map, copy);
        var field = M3BiMap.class.getDeclaredField("forward"); field.setAccessible(true);
        assertInstanceOf(M3LinkedHashMap.class, field.get(map));
        assertInstanceOf(M3LinkedHashMap.class, field.get(map.inverse()));
        assertNotSame(field.get(map), field.get(map.inverse()));
        assertFalse(java.io.Serializable.class.isAssignableFrom(M3BiMap.class));
        for (var f : M3BiMap.class.getDeclaredFields()) assertFalse(Map.Entry.class.isAssignableFrom(f.getType()));
    }
}
