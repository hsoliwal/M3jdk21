/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * @test
 * @summary FrozenMap array/DOOP copy: exact JDK cardinality, untrusted Map fallback,
 *          collision/duplicate/null behavior, view identity, and mutation contracts
 * @modules jdk.m3.collections/jdk.internal.m3.collections
 * @run main/othervm FrozenMapExactLanesTest
 */
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SplittableRandom;
import jdk.internal.m3.collections.FrozenMap;

public final class FrozenMapExactLanesTest {
    private static long checks;

    public static void main(String[] args) {
        for (int size : new int[] {0, 1, 2, 7, 8, 9, 13, 14, 20, 21, 64, 256, 4096, 65536}) {
            checkMap(populated(new LinkedHashMap<>(), size), size);
            checkMap(populated(new HashMap<>(), size), size);
            if (size <= 4096) {
                checkMap(new UntrustedSizeMap<>(populated(new LinkedHashMap<>(), size)), size);
            }
        }
        SplittableRandom random = new SplittableRandom(0xF20A56D08L);
        for (int round = 0; round < 160; round++) {
            int n = random.nextInt(650);
            Map<Integer, Integer> data = new LinkedHashMap<>();
            for (int i = 0; i < n; i++) {
                int key = random.nextInt(-2000, 2001);
                data.put(key, random.nextInt(-16, 17));
            }
            checkMap(data, data.size());
            checkMap(new UntrustedSizeMap<>(data), data.size());
        }
        Map<Integer, Integer> linkedSubclass = new LinkedHashMap<>() {
            @Override public int size() {
                throw new AssertionError("LinkedHashMap subclass.size() called");
            }
        };
        checkMap(populated(linkedSubclass, 33), 33);
        Map<Integer, Integer> hashSubclass = new HashMap<>() {
            @Override public int size() {
                throw new AssertionError("HashMap subclass.size() called");
            }
        };
        checkMap(populated(hashSubclass, 33), 33);
        checkCollisionKeys();
        checkInvalidSources();
        checkImmutabilityAndOwnership();
        System.out.println("FrozenMapExactLanesTest PASS: " + checks + " assertions");
    }

    private static Map<Integer, Integer> populated(Map<Integer, Integer> map, int count) {
        for (int i = 0; i < count; i++) {
            int key = Integer.rotateLeft(i * 31, 9);
            map.put(key, i & 127);
        }
        return map;
    }

    private static void checkMap(Map<Integer, Integer> source, int expectedSize) {
        LinkedHashMap<Integer, Integer> reference = new LinkedHashMap<>();
        for (Map.Entry<Integer, Integer> e : source.entrySet()) {
            reference.put(e.getKey(), e.getValue());
        }
        FrozenMap<Integer, Integer> actual = FrozenMap.copyOf(source);
        eq(actual.size(), expectedSize, "size");
        eq(actual.isEmpty(), expectedSize == 0, "isEmpty");
        check(actual.asMap().equals(reference), "map equality");
        check(reference.equals(actual.asMap()), "reverse map equality");
        eq(actual.containsKey(null), false, "null key query");
        eq(actual.get(null), null, "null get");
        eq(actual.getOrDefault(null, 42), 42, "null get default");
        eq(actual.containsValue(null), false, "null value query");
        eq(actual.get(123456789), null, "missing get");
        eq(actual.getOrDefault(123456789, 42), 42, "missing default");
        eq(actual.entrySet().size(), expectedSize, "entry set size");
        eq(actual.asMap().keySet().size(), expectedSize, "key set size");
        eq(actual.asMap().values().size(), expectedSize, "values size");

        Iterator<Map.Entry<Integer, Integer>> iter = reference.entrySet().iterator();
        Iterator<Map.Entry<Integer, Integer>> output = actual.entrySet().iterator();
        Iterator<Integer> keys = actual.asMap().keySet().iterator();
        Iterator<Integer> values = actual.asMap().values().iterator();
        for (int i = 0; i < expectedSize; i++) {
            Map.Entry<Integer, Integer> item = iter.next();
            Map.Entry<Integer, Integer> frozen = output.next();
            eq(actual.keyAt(i), item.getKey(), "encounter key");
            eq(actual.valueAt(i), item.getValue(), "encounter value");
            eq(frozen.getKey(), item.getKey(), "entry key");
            eq(frozen.getValue(), item.getValue(), "entry value");
            eq(keys.next(), item.getKey(), "facade key");
            eq(values.next(), item.getValue(), "facade value");
            eq(actual.get(item.getKey()), item.getValue(), "indexed lookup");
            eq(actual.getOrDefault(item.getKey(), -99), item.getValue(), "defaulted lookup");
            check(actual.containsKey(item.getKey()), "known key");
            check(actual.containsValue(item.getValue()), "known value");
            check(actual.entrySet().contains(item), "entrySet containment");
        }
        check(!iter.hasNext(), "reference exhausted");
        check(!output.hasNext(), "entries exhausted");
        check(!keys.hasNext(), "keys exhausted");
        check(!values.hasNext(), "values exhausted");
        check(!actual.entrySet().contains(new AbstractMap.SimpleImmutableEntry<>(null, 1)),
                "null entry lookup");
        if (expectedSize == 0) {
            fails(NoSuchElementException.class, output::next, "empty iterator");
        }
    }

    private record Collision(int id) {
        @Override public int hashCode() {
            return id & 7;
        }
    }

    private static void checkCollisionKeys() {
        Map<Collision, String> source = new LinkedHashMap<>();
        for (int i = 0; i < 1800; i++) {
            source.put(new Collision(i), "v" + (i % 3));
        }
        FrozenMap<Collision, String> result = FrozenMap.copyOf(source);
        eq(result.size(), source.size(), "collision size");
        int i = 0;
        for (Map.Entry<Collision, String> e : source.entrySet()) {
            eq(result.keyAt(i), e.getKey(), "collision encounter");
            eq(result.get(new Collision(e.getKey().id())), e.getValue(), "collision lookup");
            i++;
        }
    }

    private static void checkInvalidSources() {
        fails(NullPointerException.class, () -> FrozenMap.copyOf(null), "null map");
        fails(NullPointerException.class, () -> FrozenMap.copyOf(entries(
                List.of(new AbstractMap.SimpleImmutableEntry<>(null, "x")))), "null key");
        fails(NullPointerException.class, () -> FrozenMap.copyOf(entries(
                List.of(new AbstractMap.SimpleImmutableEntry<>("k", null)))), "null value");
        fails(NullPointerException.class, () -> FrozenMap.copyOf(entries(
                Collections.singletonList(null))), "null entry");
        fails(IllegalArgumentException.class, () -> FrozenMap.copyOf(entries(
                List.of(new AbstractMap.SimpleImmutableEntry<>("x", 1),
                        new AbstractMap.SimpleImmutableEntry<>(new String("x"), 2)))),
                "duplicate equal keys");
        IdentityHashMap<String, Integer> identity = new IdentityHashMap<>();
        identity.put(new String("same"), 1);
        identity.put(new String("same"), 2);
        fails(IllegalArgumentException.class, () -> FrozenMap.copyOf(identity),
                "identity map equal duplicate");
    }

    private static <K, V> Map<K, V> entries(List<Map.Entry<K, V>> entries) {
        return new AbstractMap<>() {
            @Override public Set<Map.Entry<K, V>> entrySet() {
                return new AbstractSet<>() {
                    @Override public int size() { return entries.size(); }
                    @Override public Iterator<Map.Entry<K, V>> iterator() {
                        return entries.iterator();
                    }
                };
            }
        };
    }

    private static void checkImmutabilityAndOwnership() {
        Map<String, String> source = new LinkedHashMap<>();
        String key = new String("key");
        String value = new String("value");
        source.put(key, value);
        FrozenMap<String, String> result = FrozenMap.copyOf(source);
        check(result.keyAt(0) == key, "shallow key identity");
        check(result.valueAt(0) == value, "shallow value identity");
        source.clear();
        eq(result.size(), 1, "source detach");
        eq(result.get(key), value, "value retained");
        fails(UnsupportedOperationException.class, () -> result.put("x", "x"), "put");
        fails(UnsupportedOperationException.class, () -> result.remove("missing"), "remove");
        fails(UnsupportedOperationException.class,
                () -> result.asMap().put("x", "y"), "facade put");
        fails(UnsupportedOperationException.class,
                () -> result.asMap().keySet().remove("absent"), "facade key mutation");
        fails(UnsupportedOperationException.class,
                () -> result.asMap().values().clear(), "facade value clear");
        fails(UnsupportedOperationException.class,
                () -> result.entrySet().iterator().next().setValue("x"), "entry mutation");
        fails(IndexOutOfBoundsException.class, () -> result.keyAt(-1), "negative ordinal");
        fails(IndexOutOfBoundsException.class, () -> result.valueAt(1), "high ordinal");
        check(FrozenMap.scaffoldDescriptor().startsWith("M3FrozenMap|"), "descriptor");
    }

    private static void check(boolean yes, String reason) {
        checks++;
        if (!yes) throw new AssertionError(reason);
    }
    private static void eq(Object actual, Object expected, String reason) {
        checks++;
        if (!java.util.Objects.equals(actual, expected)) {
            throw new AssertionError(reason + " actual=" + actual + " expected=" + expected);
        }
    }
    private static void fails(Class<? extends Throwable> type, Runnable action, String reason) {
        try {
            action.run();
        } catch (Throwable ex) {
            check(type.isInstance(ex), reason + ": " + ex);
            return;
        }
        throw new AssertionError(reason + ": exception not thrown");
    }

    /** size() intentionally throws; copyOf must not query untrusted Map cardinality. */
    private static final class UntrustedSizeMap<K, V> extends AbstractMap<K, V> {
        private final Map<K, V> source;
        UntrustedSizeMap(Map<K, V> source) {
            this.source = source;
        }
        @Override public int size() {
            throw new AssertionError("untrusted Map.size() called");
        }
        @Override public Set<Entry<K, V>> entrySet() {
            return source.entrySet();
        }
    }
}
