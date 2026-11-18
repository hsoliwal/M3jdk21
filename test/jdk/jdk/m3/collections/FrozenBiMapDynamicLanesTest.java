/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * @test
 * @summary FrozenBiMap exact/dynamic lanes preserve immutable inverse, index and callback contracts
 * @modules jdk.m3.collections/jdk.internal.m3.collections
 * @run main/othervm FrozenBiMapDynamicLanesTest
 */
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import jdk.internal.m3.collections.FrozenBiMap;

public final class FrozenBiMapDynamicLanesTest {
    private static long checks;
    private static long sourceTrace;

    public static void main(String[] args) {
        deterministicCapacityCases();
        randomizedSources();
        exceptionAndIdentityContracts();
        callbackNeutralSource();
        System.out.printf("FrozenBiMapDynamicLanesTest PASS %d assertions sourceTrace=%016x%n",
                checks, sourceTrace);
    }

    private static void deterministicCapacityCases() {
        int[] lengths = {0, 1, 2, 3, 7, 8, 9, 16, 17, 64, 65, 127, 128,
                129, 1024, 4097, 65536};
        for (int length : lengths) {
            LinkedHashMap<Integer, Integer> linked = new LinkedHashMap<>();
            for (int i = 0; i < length; i++) linked.put(i * 13, i * 17 + 1);
            verify(FrozenBiMap.copyOf(linked), linked);
            HashMap<Integer, Integer> hashed = new HashMap<>(linked);
            verify(FrozenBiMap.copyOf(hashed), hashed);
            TreeMap<Integer, Integer> tree = new TreeMap<>(linked);
            verify(FrozenBiMap.copyOf(tree), tree);
            Map<Integer, Integer> wrapped = Collections.unmodifiableMap(linked);
            verify(FrozenBiMap.copyOf(wrapped), wrapped);
        }
    }

    private static void randomizedSources() {
        for (int seed = 0; seed < 64; seed++) {
            Random random = new Random(seed * 49157L + 0xB1B1A73L);
            LinkedHashMap<Long, Long> source = new LinkedHashMap<>();
            int amount = random.nextInt(1800);
            for (int i = 0; i < amount; i++) {
                long key = (long) i << 32 | random.nextInt();
                long value = Long.rotateLeft(key, 17) ^ 0x9E3779B97F4A7C15L;
                source.put(key, value);
            }
            verify(FrozenBiMap.copyOf(source), source);
            if (seed % 4 == 0) {
                SizeThrowsMap<Long, Long> custom = new SizeThrowsMap<>();
                source.forEach(custom::put);
                verify(FrozenBiMap.copyOf(custom), source);
            }
        }
    }

    private static <K, V> void verify(FrozenBiMap<K, V> actual, Map<K, V> source) {
        List<Map.Entry<K, V>> ordered = new ArrayList<>(source.entrySet());
        check(actual.size() == ordered.size(), "size equals source encounter cardinality");
        check(actual.inverse().size() == actual.size(), "reciprocal size");
        check(actual.inverse().inverse() == actual, "cached inverse identity");
        check(actual.inverse().inverse().inverse() == actual.inverse(), "inverse involution");
        Map<K, V> map = actual.asMap();
        Map<V, K> inverseMap = actual.inverse().asMap();
        check(map.size() == ordered.size() && inverseMap.size() == ordered.size(),
                "both map facade sizes");
        int index = 0;
        for (Map.Entry<K, V> entry : ordered) {
            K key = entry.getKey();
            V value = entry.getValue();
            check(actual.containsKey(key), "forward contains key");
            check(actual.containsValue(value), "reverse contains value");
            check(value.equals(actual.get(key)), "forward get equality");
            check(key.equals(actual.inverseGet(value)), "reverse get equality");
            check(value.equals(map.get(key)), "map facade get");
            check(key.equals(inverseMap.get(value)), "inverse map get");
            check(map.getOrDefault(key, null).equals(value), "map default");
            check(map.entrySet().contains(new AbstractMap.SimpleImmutableEntry<>(key, value)),
                    "entry projection lookup");
            check(inverseMap.entrySet().contains(new AbstractMap.SimpleImmutableEntry<>(value, key)),
                    "inverse entry projection lookup");
            check(map.keySet().contains(key) && map.values().contains(value), "set/value views");
            index++;
        }
        check(index == ordered.size(), "iterated expected size");
        List<Map.Entry<K, V>> actualOrder = new ArrayList<>(map.entrySet());
        List<Map.Entry<V, K>> inverseOrder = new ArrayList<>(inverseMap.entrySet());
        check(actualOrder.equals(ordered), "forward entry encounter order");
        for (int i = 0; i < ordered.size(); i++) {
            check(inverseOrder.get(i).getKey().equals(ordered.get(i).getValue()),
                    "inverse key encounter order");
            check(inverseOrder.get(i).getValue().equals(ordered.get(i).getKey()),
                    "inverse value encounter order");
            check(actualOrder.get(i) != ordered.get(i), "entry projected not source alias");
        }
        List<Map.Entry<K, V>> foreach = new ArrayList<>();
        BiConsumer<K, V> capture = (key, value) ->
                foreach.add(new AbstractMap.SimpleImmutableEntry<>(key, value));
        map.forEach(capture);
        check(foreach.equals(ordered), "forEach encounter order");
        check(actual.get(null) == null && !actual.containsKey(null), "null key absent");
        check(actual.inverseGet(null) == null && !actual.containsValue(null),
                "null reverse query absent");
        expect(UnsupportedOperationException.class, () -> actual.put(null, null));
        expect(UnsupportedOperationException.class, () -> actual.forcePut(null, null));
        expect(UnsupportedOperationException.class, () -> actual.remove(null));
        expect(UnsupportedOperationException.class, () -> map.clear());
        expect(UnsupportedOperationException.class, () -> inverseMap.clear());
        if (!ordered.isEmpty()) {
            Map.Entry<K, V> first = map.entrySet().iterator().next();
            expect(UnsupportedOperationException.class, () -> first.setValue(first.getValue()));
        }
        sourceTrace ^= Long.rotateLeft(actual.size() * 0x9E3779B97F4A7C15L, 19);
    }

    private static void exceptionAndIdentityContracts() {
        expect(NullPointerException.class, () -> FrozenBiMap.copyOf(null));
        Map<String, String> nullKey = new LinkedHashMap<>();
        nullKey.put(null, "v");
        expect(NullPointerException.class, () -> FrozenBiMap.copyOf(nullKey));
        Map<String, String> nullValue = new LinkedHashMap<>();
        nullValue.put("k", null);
        expect(NullPointerException.class, () -> FrozenBiMap.copyOf(nullValue));
        Map<String, String> duplicateValue = new LinkedHashMap<>();
        duplicateValue.put("a", "v");
        duplicateValue.put("b", "v");
        expect(IllegalArgumentException.class, () -> FrozenBiMap.copyOf(duplicateValue));
        IdentityHashMap<String, String> identity = new IdentityHashMap<>();
        identity.put(new String("same"), "a");
        identity.put(new String("same"), "b");
        expect(IllegalArgumentException.class, () -> FrozenBiMap.copyOf(identity));

        LinkedHashMap<String, String> source = new LinkedHashMap<>();
        source.put("key", "value");
        FrozenBiMap<String, String> frozen = FrozenBiMap.copyOf(source);
        source.clear();
        check("value".equals(frozen.get("key")), "source mutation detached");
        check("key".equals(frozen.inverseGet("value")), "inverse source detached");
        FrozenBiMap<Object, Object> empty = new FrozenBiMap<>();
        check(empty.inverse() == empty, "empty inverse self instance");
        check(empty.asMap().isEmpty(), "empty map");
        expect(NoSuchElementException.class, () -> empty.asMap().keySet().iterator().next());
    }

    private static void callbackNeutralSource() {
        TraceMap traceMap = new TraceMap();
        FrozenBiMap<String, String> result = FrozenBiMap.copyOf(traceMap);
        check(result.size() == 3, "custom source copied");
        check(traceMap.sizeCalls == 0, "custom map size never invoked");
        check(traceMap.iteratorCalls == 1, "iterator acquired once");
        check(traceMap.keyCalls == 3 && traceMap.valueCalls == 3, "entry callbacks once each");
        check(traceMap.nextCalls == 3 && traceMap.hasNextCalls == 4,
                "iterator callbacks once per element + final probe");
        sourceTrace ^= traceMap.signature;
        check(result.inverseGet("v2").equals("k2"), "trace inverse maps correct");
        SizeThrowsMap<String, String> subclass = new SizeThrowsMap<>();
        subclass.put("a", "1");
        subclass.put("b", "2");
        check(FrozenBiMap.copyOf(subclass).size() == 2, "JDK subclass size callback avoided");
    }

    private static final class SizeThrowsMap<K, V> extends LinkedHashMap<K, V> {
        private static final long serialVersionUID = 1L;
        @Override public int size() { throw new AssertionError("user size() callback forbidden"); }
    }

    private static final class TraceMap extends AbstractMap<String, String> {
        int sizeCalls, iteratorCalls, hasNextCalls, nextCalls, keyCalls, valueCalls;
        long signature;

        @Override public int size() {
            sizeCalls++;
            throw new AssertionError("custom map size called");
        }

        @Override public Set<Map.Entry<String, String>> entrySet() {
            signature = Long.rotateLeft(signature ^ 101, 5);
            return new AbstractSet<>() {
                @Override public int size() { return 3; }
                @Override public Iterator<Map.Entry<String, String>> iterator() {
                    iteratorCalls++;
                    signature = Long.rotateLeft(signature ^ 103, 5);
                    return new Iterator<>() {
                        int position;
                        @Override public boolean hasNext() {
                            hasNextCalls++;
                            signature = Long.rotateLeft(signature ^ 107, 5);
                            return position < 3;
                        }
                        @Override public Map.Entry<String, String> next() {
                            nextCalls++;
                            signature = Long.rotateLeft(signature ^ 109, 5);
                            int current = position++;
                            return new Map.Entry<>() {
                                @Override public String getKey() {
                                    keyCalls++;
                                    signature = Long.rotateLeft(signature ^ 113, 5);
                                    return "k" + current;
                                }
                                @Override public String getValue() {
                                    valueCalls++;
                                    signature = Long.rotateLeft(signature ^ 127, 5);
                                    return "v" + current;
                                }
                                @Override public String setValue(String value) {
                                    throw new UnsupportedOperationException();
                                }
                            };
                        }
                    };
                }
            };
        }
    }

    private static void check(boolean condition, String reason) {
        checks++;
        if (!condition) throw new AssertionError(reason + " after assertion " + checks);
    }

    private static void expect(Class<? extends Throwable> type, Runnable operation) {
        checks++;
        try {
            operation.run();
        } catch (Throwable failure) {
            if (type.isInstance(failure)) return;
            throw new AssertionError("expected " + type + " got " + failure, failure);
        }
        throw new AssertionError("missing expected " + type);
    }
}
