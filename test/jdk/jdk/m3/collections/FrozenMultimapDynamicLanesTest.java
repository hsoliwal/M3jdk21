/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * @test
 * @summary FrozenMultimap dynamic arrays retain CSR runs, frozen masks and callback contracts
 * @modules jdk.m3.collections/jdk.internal.m3.collections
 * @run main/othervm FrozenMultimapDynamicLanesTest
 */
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import jdk.internal.m3.collections.FrozenMultimap;

public final class FrozenMultimapDynamicLanesTest {
    private static long checks;
    private static long trace;

    public static void main(String[] args) {
        deterministicCapacityCases();
        randomizedSources();
        invalidAndImmutabilityContracts();
        callbackNeutralSource();
        System.out.printf("FrozenMultimapDynamicLanesTest PASS %d assertions sourceTrace=%016x%n",
                checks, trace);
    }

    private static void deterministicCapacityCases() {
        int[] sizes = {0, 1, 2, 3, 7, 8, 9, 16, 17, 63, 64, 65,
                127, 128, 129, 1024, 4097, 65536};
        for (int size : sizes) {
            LinkedHashMap<Integer, Collection<Integer>> linked = new LinkedHashMap<>();
            for (int i = 0; i < size; i++) {
                Collection<Integer> run = i % 17 == 0 ? List.of()
                        : i % 5 == 0 ? List.of(i) : List.of(i, i, i ^ 13);
                linked.put(i * 13 + 7, run);
            }
            verify(FrozenMultimap.copyOf(linked), linked);
            HashMap<Integer, Collection<Integer>> hashed = new HashMap<>(linked);
            verify(FrozenMultimap.copyOf(hashed), hashed);
            if (size <= 4097) {
                TreeMap<Integer, Collection<Integer>> tree = new TreeMap<>(linked);
                verify(FrozenMultimap.copyOf(tree), tree);
                verify(FrozenMultimap.copyOf(Collections.unmodifiableMap(linked)), linked);
            }
        }
        FrozenMultimap<Integer, Integer> empty = new FrozenMultimap<>();
        verify(empty, Collections.emptyMap());
    }

    private static void randomizedSources() {
        for (int seed = 0; seed < 96; seed++) {
            Random random = new Random(seed * 7919L + 0xC5A5L);
            LinkedHashMap<Long, Collection<Long>> source = new LinkedHashMap<>();
            int size = random.nextInt(1400);
            for (int i = 0; i < size; i++) {
                int runLength = random.nextInt(9);
                ArrayList<Long> run = new ArrayList<>();
                for (int v = 0; v < runLength; v++) {
                    run.add(((long) i << 32) | random.nextInt(17));
                }
                source.put((long) i * 65537 + seed, run);
            }
            verify(FrozenMultimap.copyOf(source), source);
            if ((seed & 7) == 0) {
                SizeThrowsMap<Long, Collection<Long>> subclass = new SizeThrowsMap<>();
                subclass.putAll(source);
                verify(FrozenMultimap.copyOf(subclass), source);
            }
        }
    }

    private static <K, V> void verify(FrozenMultimap<K, V> owner,
            Map<K, ? extends Collection<? extends V>> source) {
        LinkedHashMap<K, List<V>> expected = new LinkedHashMap<>();
        ArrayList<Map.Entry<K, V>> flat = new ArrayList<>();
        for (Map.Entry<K, ? extends Collection<? extends V>> entry : source.entrySet()) {
            if (entry.getValue().isEmpty()) continue;
            ArrayList<V> run = new ArrayList<>(entry.getValue());
            expected.put(entry.getKey(), run);
            for (V value : run) flat.add(Map.entry(entry.getKey(), value));
        }
        check(owner.size() == flat.size(), "value count");
        check(owner.keyCount() == expected.size(), "nonempty key count");
        check(owner.keySet().equals(expected.keySet()), "key set equality");
        check(new ArrayList<>(owner.keySet()).equals(new ArrayList<>(expected.keySet())),
                "key encounter order");
        check(owner.asMap().size() == expected.size(), "asMap key count");
        check(owner.entries().size() == flat.size(), "entries value count");
        check(new ArrayList<>(owner.entries()).equals(flat), "flattened encounter order");
        check(owner.get(null).isEmpty(), "null get immutable empty");
        check(owner.asMap().get(null) == null, "asMap missing null");
        check(!owner.containsKey(null) && !owner.containsEntry(null, null), "null misses");
        check(!owner.containsEntry(null, new Object()), "null key miss");
        check(!owner.containsKey(new Object()), "unknown key miss");
        check(!owner.containsEntry(new Object(), null), "unknown pair miss");
        for (Map.Entry<K, List<V>> entry : expected.entrySet()) {
            K key = entry.getKey();
            List<V> run = entry.getValue();
            check(owner.containsKey(key), "key present");
            check(owner.get(key).equals(run), "run values equal");
            check(owner.asMap().get(key).equals(run), "asMap run equal");
            check(owner.asMap().get(key) != run, "value run projected");
            check(!owner.containsEntry(key, null), "null value query");
            for (V value : run) {
                check(owner.containsEntry(key, value), "value lookup");
                check(owner.get(key).contains(value), "run membership");
            }
            expect(UnsupportedOperationException.class, () -> owner.get(key).clear());
        }
        check(owner.asMap().keySet().equals(owner.keySet()), "map and keySet views");
        check(owner.asMap().entrySet().size() == expected.size(), "map entrySet size");
        check(owner.asMap().entrySet().stream().allMatch(e ->
                e.getValue().equals(expected.get(e.getKey()))), "map projected entries");
        check(FrozenMultimap.scaffoldDescriptor().contains("valueOffsetLane"), "descriptor retained");
        expect(UnsupportedOperationException.class, () -> owner.put(null, null));
        expect(UnsupportedOperationException.class, () -> owner.removeAll(null));
        expect(UnsupportedOperationException.class, () -> owner.keySet().clear());
        expect(UnsupportedOperationException.class, () -> owner.asMap().clear());
        expect(UnsupportedOperationException.class, () -> owner.entries().clear());
        expect(UnsupportedOperationException.class, () -> owner.get(null).clear());
        if (!flat.isEmpty()) {
            Map.Entry<K, V> first = owner.entries().iterator().next();
            expect(UnsupportedOperationException.class, () -> first.setValue(first.getValue()));
        }
        if (!expected.isEmpty()) {
            Map.Entry<K, Collection<V>> first = owner.asMap().entrySet().iterator().next();
            expect(UnsupportedOperationException.class, () -> first.setValue(first.getValue()));
            expect(UnsupportedOperationException.class, () -> first.getValue().clear());
        }
        trace ^= Long.rotateLeft(owner.size() * 0x9E3779B97F4A7C15L, 13)
                ^ Long.rotateLeft(owner.keyCount() * 0xD1B54A32D192ED03L, 23);
    }

    private static void invalidAndImmutabilityContracts() {
        expect(NullPointerException.class, () -> FrozenMultimap.copyOf(null));
        Map<String, Collection<String>> nullKey = new LinkedHashMap<>();
        nullKey.put(null, List.of("v"));
        expect(NullPointerException.class, () -> FrozenMultimap.copyOf(nullKey));
        Map<String, Collection<String>> nullRun = new LinkedHashMap<>();
        nullRun.put("k", null);
        expect(NullPointerException.class, () -> FrozenMultimap.copyOf(nullRun));
        Map<String, Collection<String>> nullValue = new LinkedHashMap<>();
        nullValue.put("k", Arrays.asList("x", null));
        expect(NullPointerException.class, () -> FrozenMultimap.copyOf(nullValue));
        IdentityHashMap<String, Collection<String>> duplicate = new IdentityHashMap<>();
        duplicate.put(new String("same"), List.of("one"));
        duplicate.put(new String("same"), List.of("two"));
        expect(IllegalArgumentException.class, () -> FrozenMultimap.copyOf(duplicate));

        LinkedHashMap<String, Collection<String>> mutable = new LinkedHashMap<>();
        ArrayList<String> run = new ArrayList<>(List.of("alpha", "beta", "alpha"));
        mutable.put("key", run);
        FrozenMultimap<String, String> frozen = FrozenMultimap.copyOf(mutable);
        mutable.clear();
        run.clear();
        check(frozen.get("key").equals(List.of("alpha", "beta", "alpha")),
                "source map and list detached");
        check(frozen.containsEntry("key", "alpha"), "duplicated values retained");
        check(!frozen.get("missing").iterator().hasNext(), "missing run empty");
        expect(UnsupportedOperationException.class,
                () -> frozen.entries().iterator().next().setValue("x"));
        FrozenMultimap<String, String> empty = new FrozenMultimap<>();
        check(empty.keySet().isEmpty(), "empty keys");
        check(empty.asMap().isEmpty(), "empty map");
        expect(NoSuchElementException.class, () -> empty.entries().iterator().next());
        expect(NoSuchElementException.class, () -> empty.keySet().iterator().next());
    }

    private static void callbackNeutralSource() {
        TraceMap source = new TraceMap();
        FrozenMultimap<String, String> owner = FrozenMultimap.copyOf(source);
        check(owner.keyCount() == 2 && owner.size() == 4, "custom source with empty run");
        check(source.sizeCalls == 0, "custom source size unobserved");
        check(source.entrySetCalls == 1, "entrySet callback once");
        check(source.iteratorCalls == 1, "source iterator once");
        check(source.nextCalls == 3 && source.hasNextCalls == 4, "source enumeration callbacks");
        check(source.keyCalls == 3 && source.valueCalls == 3, "key/value called once");
        check(source.runs.stream().allMatch(x -> x.sizeCalls == 0),
                "custom value size never called");
        check(source.runs.stream().allMatch(x -> x.iteratorCalls == 1),
                "custom values each traversed once");
        check(source.runs.stream().allMatch(x -> x.nextCalls == x.actual.size()),
                "custom values visit exactly once");
        check(new ArrayList<>(owner.entries()).equals(List.of(
                Map.entry("k1", "v1"), Map.entry("k1", "v1"),
                Map.entry("k3", "v3"), Map.entry("k3", "v4"))), "flattened custom trace");
        trace ^= source.signature;
        SizeThrowsMap<String, Collection<String>> subclass = new SizeThrowsMap<>();
        subclass.put("known", List.of("one"));
        check(FrozenMultimap.copyOf(subclass).get("known").contains("one"),
                "LinkedHashMap subclass size not called");
    }

    private static final class SizeThrowsMap<K, V> extends LinkedHashMap<K, V> {
        private static final long serialVersionUID = 1L;
        @Override public int size() { throw new AssertionError("source.size invoked"); }
    }

    private static final class TraceRun extends AbstractCollection<String> {
        private final List<String> actual;
        private int sizeCalls;
        private int iteratorCalls;
        private int nextCalls;
        TraceRun(String... values) { actual = List.of(values); }
        @Override public int size() {
            sizeCalls++;
            throw new AssertionError("valueCollection.size invoked");
        }
        @Override public Iterator<String> iterator() {
            iteratorCalls++;
            return new Iterator<>() {
                private int cursor;
                @Override public boolean hasNext() { return cursor < actual.size(); }
                @Override public String next() {
                    if (!hasNext()) throw new NoSuchElementException();
                    nextCalls++;
                    return actual.get(cursor++);
                }
            };
        }
    }

    private static final class TraceMap extends AbstractMap<String, Collection<String>> {
        private final List<TraceRun> runs = List.of(
                new TraceRun("v1", "v1"), new TraceRun(), new TraceRun("v3", "v4"));
        private final List<String> keys = List.of("k1", "k2", "k3");
        private int sizeCalls;
        private int entrySetCalls;
        private int iteratorCalls;
        private int nextCalls;
        private int hasNextCalls;
        private int keyCalls;
        private int valueCalls;
        private long signature = 0xC011EC7A51L;

        @Override public int size() {
            sizeCalls++;
            throw new AssertionError("source.size invoked");
        }

        @Override public Set<Map.Entry<String, Collection<String>>> entrySet() {
            entrySetCalls++;
            return new AbstractSet<>() {
                @Override public int size() { throw new AssertionError("entrySet.size invoked"); }
                @Override public Iterator<Map.Entry<String, Collection<String>>> iterator() {
                    iteratorCalls++;
                    return new Iterator<>() {
                        private int index;
                        @Override public boolean hasNext() {
                            hasNextCalls++;
                            return index < keys.size();
                        }
                        @Override public Map.Entry<String, Collection<String>> next() {
                            if (index >= keys.size()) throw new NoSuchElementException();
                            int ordinal = index++;
                            nextCalls++;
                            signature = Long.rotateLeft(signature ^ ordinal, 7);
                            return new AbstractMap.SimpleImmutableEntry<>(
                                    keys.get(ordinal), runs.get(ordinal)) {
                                @Override public String getKey() {
                                    keyCalls++;
                                    signature = Long.rotateLeft(signature ^ ordinal, 7);
                                    return super.getKey();
                                }
                                @Override public Collection<String> getValue() {
                                    valueCalls++;
                                    signature = Long.rotateLeft(signature ^ (ordinal << 3), 9);
                                    return super.getValue();
                                }
                            };
                        }
                    };
                }
            };
        }
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static void expect(Class<? extends Throwable> type, Runnable action) {
        checks++;
        try {
            action.run();
        } catch (Throwable failure) {
            if (type.isInstance(failure)) return;
            throw new AssertionError("Expected " + type.getName() + " got " + failure, failure);
        }
        throw new AssertionError("Expected failure " + type.getName());
    }
}

