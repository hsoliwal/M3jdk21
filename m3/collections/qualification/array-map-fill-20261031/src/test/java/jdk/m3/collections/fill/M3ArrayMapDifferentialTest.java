/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.collections.fill;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;
import jdk.m3.collections.apache.M3HashedMap;
import jdk.m3.collections.apache.M3LRUMap;
import jdk.m3.collections.apache.M3LinkedMap;
import org.apache.commons.collections4.MapIterator;
import org.apache.commons.collections4.OrderedMap;
import org.apache.commons.collections4.OrderedMapIterator;
import org.apache.commons.collections4.map.HashedMap;
import org.apache.commons.collections4.map.LRUMap;
import org.apache.commons.collections4.map.LinkedMap;
import org.junit.jupiter.api.Test;

/**
 * Differential proof of the ARRAY_MAP members against Apache Commons Collections 4.5.0: the same seeded operation
 * script runs on the provider class (oracle) and on the M3 member; every return value and every exception (class and
 * message) must match, and after every step the size, the equality both ways, the hash code and the iteration order
 * (ordered members; entry-set equality for HashedMap) must match.
 */
class M3ArrayMapDifferentialTest {

    private static final int SEEDS = 40;
    private static final int STEPS = 2500;

    /** The calls that differ by type: index access and the LRU two-argument get. */
    private record Side(Map<Integer, String> map, java.util.function.IntFunction<Object> getAt,
            java.util.function.IntFunction<Object> valueAt, java.util.function.Function<Object, Object> indexOf,
            java.util.function.IntFunction<Object> removeAt, java.util.function.Supplier<List<Integer>> asList,
            java.util.function.Function<Object, Object> peek) {
    }

    private static Side hashed(Map<Integer, String> map) {
        return new Side(map, null, null, null, null, null, null);
    }

    private static Side linked(LinkedMap<Integer, String> map) {
        return new Side(map, map::get, map::getValue, map::indexOf, map::remove, map::asList, null);
    }

    private static Side linked(M3LinkedMap<Integer, String> map) {
        return new Side(map, map::get, map::getValue, map::indexOf, map::remove, map::asList, null);
    }

    private static Side lru(LRUMap<Integer, String> map) {
        return new Side(map, null, null, null, null, null, k -> map.get(k, false));
    }

    private static Side lru(M3LRUMap<Integer, String> map) {
        return new Side(map, null, null, null, null, null, k -> map.get(k, false));
    }

    /** A call's outcome: its value, or its exception class and message. */
    private static Object call(Supplier<Object> body) {
        try {
            Object value = body.get();
            return value instanceof Map.Entry<?, ?> e ? "Entry " + e.getKey() + "=" + e.getValue() : value;
        } catch (RuntimeException ex) {
            return "THROWS " + ex.getClass().getName() + ": " + ex.getMessage();
        }
    }

    /** False for LRUMap runs: the provider loses null keys on eviction (DIVERGENCE.tsv, nullKeyAfterEviction...). */
    private static boolean nullKeys = true;

    /** The key domain: 26 keys keep maps small and collisions frequent; the wide runs cross 64-bit mask words. */
    private static int domain = 26;

    private static Integer key(Random random) {
        int k = random.nextInt(domain) - 2;
        if (k != -2) {
            return k;
        }
        return nullKeys ? null : Integer.valueOf(24);
    }

    private static String value(Random random) {
        int v = random.nextInt(12);
        return v == 0 ? null : "v" + v;
    }

    private static List<Object> order(Map<Integer, String> map) {
        List<Object> out = new ArrayList<>();
        MapIterator<Integer, String> it = ((org.apache.commons.collections4.IterableMap<Integer, String>) map).mapIterator();
        while (it.hasNext()) {
            out.add(it.next());
            out.add(it.getValue());
        }
        return out;
    }

    private static void same(String what, Object expected, Object actual, int seed, int step) {
        assertEquals(expected, actual, () -> "seed " + seed + " step " + step + ": " + what);
    }

    private static void sameState(Side oracle, Side m3, boolean ordered, int seed, int step) {
        same("size", oracle.map.size(), m3.map.size(), seed, step);
        if (ordered) { // before equals: an LRU map's equals calls get() on the other map, which reorders it
            same("order", order(oracle.map), order(m3.map), seed, step);
            same("toString", oracle.map.toString(), m3.map.toString(), seed, step);
            same("keySet order", new ArrayList<>(oracle.map.keySet()), new ArrayList<>(m3.map.keySet()), seed, step);
            same("values order", new ArrayList<>(oracle.map.values()), new ArrayList<>(m3.map.values()), seed, step);
        } else {
            same("entrySet", new HashSet<>(oracle.map.entrySet()), new HashSet<>(m3.map.entrySet()), seed, step);
        }
        same("hashCode", oracle.map.hashCode(), m3.map.hashCode(), seed, step);
        same("equals oracle->m3", true, oracle.map.equals(m3.map), seed, step);
        boolean back = m3.map.equals(oracle.map);
        if (!back) {
            System.err.println("DIVERGE seed " + seed + " step " + step + " | oracle " + order(oracle.map) + " | m3 "
                    + order(m3.map) + " | keys " + probe(m3.map, oracle.map));
        }
        same("equals m3->oracle", true, back, seed, step);
        if (ordered) {
            same("order after equals", order(oracle.map), order(m3.map), seed, step);
        }
    }

    private static String probe(Map<Integer, String> mine, Map<Integer, String> other) {
        StringBuilder out = new StringBuilder();
        for (Object key : new ArrayList<>(mine.keySet())) {
            out.append(key).append(':').append(mine.containsKey(key)).append('/').append(other.containsKey(key)).append(' ');
        }
        return out.toString();
    }

    /** One random operation on both sides; returns the two outcomes. */
    private static Object[] step(Random random, Side o, Side m, boolean ordered) {
        Integer k = key(random);
        String v = value(random);
        int op = random.nextInt(ordered ? 40 : 30);
        java.util.function.Function<Side, Object> f = switch (op) {
            case 0, 1, 2, 3, 4, 5 -> s -> s.map.put(k, v);
            case 6, 7 -> s -> s.map.get(k);
            case 8 -> s -> s.map.remove(k);
            case 9 -> s -> s.map.containsKey(k);
            case 10 -> s -> s.map.containsValue(v);
            case 11 -> s -> s.map.getOrDefault(k, "dflt");
            case 12 -> s -> s.map.putIfAbsent(k, v);
            case 13 -> s -> s.map.remove(k, v);
            case 14 -> s -> s.map.replace(k, v);
            case 15 -> s -> s.map.replace(k, v, "r" + v);
            case 16 -> s -> s.map.computeIfAbsent(k, x -> x != null && x % 3 == 0 ? null : "c" + x);
            case 17 -> s -> s.map.computeIfPresent(k, (x, y) -> y.length() > 2 ? null : y + "p");
            case 18 -> s -> s.map.compute(k, (x, y) -> y == null ? "n" + x : y.length() > 3 ? null : y + "c");
            case 19 -> s -> s.map.merge(k, v == null ? "m" : v, (a, b) -> a.length() > 3 ? null : a + b);
            case 20 -> s -> {
                Map<Integer, String> more = new java.util.LinkedHashMap<>();
                more.put(k, v);
                more.put(k == null ? 1 : k + 1, "pa");
                s.map.putAll(more);
                return null;
            };
            case 21 -> s -> random.nextInt(10) == 0 ? clear(s) : s.map.size();
            case 22 -> s -> s.map.keySet().remove(k);
            case 23 -> s -> ordered ? s.map.values().remove(v) : s.map.values().contains(v); // first match is order-dependent
            case 24 -> s -> s.map.entrySet().remove(Map.entry(k == null ? 0 : k, v == null ? "v1" : v));
            case 25 -> s -> s.map.entrySet().contains(new java.util.AbstractMap.SimpleEntry<>(k, v));
            case 26 -> s -> {
                List<Object> seen = new ArrayList<>();
                s.map.forEach((a, b) -> seen.add(a + "=" + b));
                return ordered ? seen : sorted(seen); // the provider's HashedMap order is unspecified
            };
            case 27 -> s -> {
                s.map.replaceAll((a, b) -> a != null && a % 4 == 0 ? b + "x" : b);
                return null;
            };
            case 28 -> s -> iterateRemoving(s.map, random.nextInt(3) + 2, ordered);
            case 29 -> s -> s.map.isEmpty();
            case 30 -> s -> ((OrderedMap<Integer, String>) s.map).firstKey();
            case 31 -> s -> ((OrderedMap<Integer, String>) s.map).lastKey();
            case 32 -> s -> ((OrderedMap<Integer, String>) s.map).nextKey(k);
            case 33 -> s -> ((OrderedMap<Integer, String>) s.map).previousKey(k);
            case 34, 35 -> s -> walk(((OrderedMap<Integer, String>) s.map).mapIterator(), new Random(k == null ? 7 : k));
            default -> s -> s.getAt != null ? indexed(s, random.nextInt(8), k) : s.peek != null ? s.peek.apply(k)
                    : s.map.size();
        };
        long state = random.nextLong();
        random.setSeed(state);
        Object expected = call(() -> f.apply(o));
        random.setSeed(state);
        Object actual = call(() -> f.apply(m));
        return new Object[] {op, expected, actual};
    }

    private static Object clear(Side s) {
        s.map.clear();
        return null;
    }

    private static Object indexed(Side s, int which, Integer k) {
        int at = k == null ? -1 : k % Math.max(1, s.map.size() + 2);
        return switch (which) {
            case 0, 1 -> s.getAt.apply(at);
            case 2 -> s.valueAt.apply(at);
            case 3 -> s.indexOf.apply(k);
            case 4 -> s.removeAt.apply(at);
            case 5 -> s.asList.get().subList(0, Math.min(2, s.map.size())).toString() + s.asList.get().indexOf(k);
            case 6 -> call(() -> s.asList.get().remove(0));
            default -> new ArrayList<>(s.asList.get()).toString() + s.asList.get().contains(k);
        };
    }

    /**
     * Removes through the entry-set iterator: by position for ordered members, by key for HashedMap (whose order is
     * unspecified, so a positional choice would remove different entries on the two sides).
     */
    private static Object iterateRemoving(Map<Integer, String> map, int every, boolean ordered) {
        List<Object> seen = new ArrayList<>();
        int i = 0;
        for (Iterator<Map.Entry<Integer, String>> it = map.entrySet().iterator(); it.hasNext(); i++) {
            Map.Entry<Integer, String> e = it.next();
            seen.add(e.getKey() + "=" + e.getValue());
            if (ordered ? i % every == 0 : e.getKey() == null || e.getKey() % every == 0) {
                it.remove();
            }
        }
        return ordered ? seen : sorted(seen);
    }

    private static List<Object> sorted(List<Object> seen) {
        List<Object> copy = new ArrayList<>(seen);
        copy.sort(java.util.Comparator.comparing(String::valueOf));
        return copy;
    }

    /** A bidirectional walk; remove() only right after next() (the provider defect after previous() is not walked). */
    private static Object walk(OrderedMapIterator<Integer, String> it, Random random) {
        List<Object> seen = new ArrayList<>();
        boolean afterNext = false;
        for (int n = 0; n < 12; n++) {
            int op = random.nextInt(8);
            seen.add(call(() -> switch (op) {
                case 0, 1, 2 -> it.hasNext() ? it.next() : "end";
                case 3 -> it.hasPrevious() ? it.previous() : "start";
                case 4 -> it.getKey();
                case 5 -> it.getValue();
                case 6 -> it.setValue("w" + op);
                default -> "skip";
            }));
            afterNext = op <= 2;
            if (afterNext && random.nextInt(4) == 0) {
                seen.add(call(() -> {
                    it.remove();
                    return "removed";
                }));
            }
        }
        String text = it.toString();
        seen.add(text.startsWith("Iterator[") ? text : "<identity>"); // an empty iterator has Object.toString
        return seen;
    }

    private static void run(String name, Supplier<Side> oracle, Supplier<Side> m3, boolean ordered) {
        long steps = 0;
        for (int seed = 0; seed < SEEDS; seed++) {
            Side o = oracle.get();
            Side m = m3.get();
            Random random = new Random(seed * 7919L + name.hashCode());
            for (int step = 0; step < STEPS; step++) {
                Object[] r = step(random, o, m, ordered);
                same(name + " op " + r[0], r[1], r[2], seed, step);
                sameState(o, m, ordered, seed, step);
                steps++;
            }
        }
        assertTrue(steps == (long) SEEDS * STEPS);
    }

    @Test
    void hashedMapAgreesWithTheProvider() {
        run("HashedMap", () -> hashed(new HashedMap<>()), () -> hashed(new M3HashedMap<>()), false);
    }

    @Test
    void linkedMapAgreesWithTheProvider() {
        run("LinkedMap", () -> linked(new LinkedMap<>()), () -> linked(new M3LinkedMap<>()), true);
    }

    @Test
    void lruMapAgreesWithTheProvider() {
        nullKeys = false;
        try {
            run("LRUMap", () -> lru(new LRUMap<>(7)), () -> lru(new M3LRUMap<>(7)), true);
            run("LRUMap-scan", () -> lru(new LRUMap<>(3, true)), () -> lru(new M3LRUMap<>(3, true)), true);
        } finally {
            nullKeys = true;
        }
    }

    /** Wide keys: maps of a few hundred entries, so rank/select cross mask words and lanes grow and compact. */
    @Test
    void wideMapsAgreeWithTheProvider() {
        domain = 600;
        nullKeys = false;
        try {
            run("LinkedMap-wide", () -> linked(new LinkedMap<>()), () -> linked(new M3LinkedMap<>()), true);
            run("HashedMap-wide", () -> hashed(new HashedMap<>()), () -> hashed(new M3HashedMap<>()), false);
            run("LRUMap-wide", () -> lru(new LRUMap<>(150)), () -> lru(new M3LRUMap<>(150)), true);
        } finally {
            domain = 26;
            nullKeys = true;
        }
    }

    /**
     * Provider defect (Commons Collections 4.5.0): when a full LRUMap evicts and reuses the eldest entry for a null key,
     * AbstractHashedMap.reuseEntry stores the raw key without convertKey, so the null key iterates but cannot be found.
     * The M3 member keeps null keys consistent. If the provider fixes this, the first assertion fails and the
     * DIVERGENCE.tsv row goes.
     */
    @Test
    void nullKeyAfterEvictionIsAProviderDefectTheM3MemberDoesNotHave() {
        LRUMap<Integer, String> provider = new LRUMap<>(2);
        M3LRUMap<Integer, String> m3 = new M3LRUMap<>(2);
        for (Map<Integer, String> map : List.<Map<Integer, String>>of(provider, m3)) {
            map.put(1, "a");
            map.put(2, "b");
            map.put(null, "n");
        }
        assertEquals(List.of(2, "b", "<null>", "n"), order(provider).stream().map(x -> x == null ? "<null>" : x).toList());
        assertEquals(false, provider.containsKey(null), "provider defect still present");
        assertEquals(true, m3.containsKey(null));
        assertEquals("n", m3.get(null));
        assertEquals(order(provider), order(m3));
    }

    @Test
    void constructorsValidateLikeTheProvider() {
        List<Supplier<Object>> oracle = List.of(() -> new LinkedMap<>(-1), () -> new LinkedMap<>(4, 0f),
                () -> new LinkedMap<>(4, Float.NaN), () -> new HashedMap<>(-3), () -> new LRUMap<>(0),
                () -> new LRUMap<>(2, 3), () -> new LRUMap<>(new HashMap<>()), () -> new LRUMap<>(5, -1),
                () -> new LinkedMap<>(Map.of(1, "a", 2, "b")).size(), () -> new LRUMap<>(4, 2, 0.5f, true).maxSize());
        List<Supplier<Object>> m3 = List.of(() -> new M3LinkedMap<>(-1), () -> new M3LinkedMap<>(4, 0f),
                () -> new M3LinkedMap<>(4, Float.NaN), () -> new M3HashedMap<>(-3), () -> new M3LRUMap<>(0),
                () -> new M3LRUMap<>(2, 3), () -> new M3LRUMap<>(new HashMap<>()), () -> new M3LRUMap<>(5, -1),
                () -> new M3LinkedMap<>(Map.of(1, "a", 2, "b")).size(), () -> new M3LRUMap<>(4, 2, 0.5f, true).maxSize());
        for (int i = 0; i < oracle.size(); i++) {
            assertEquals(call(oracle.get(i)), call(m3.get(i)), "constructor case " + i);
        }
    }

    @Test
    void emptyIteratorsAndViewsAgree() {
        List<Supplier<Object>> cases = List.of(() -> new LinkedMap<Integer, String>().mapIterator().next(),
                () -> new LinkedMap<Integer, String>().mapIterator().getKey(),
                () -> new LinkedMap<Integer, String>().mapIterator().previous(),
                () -> {
                    new HashedMap<Integer, String>().mapIterator().remove();
                    return null;
                },
                () -> new LinkedMap<Integer, String>().firstKey());
        List<Supplier<Object>> m3 = List.of(() -> new M3LinkedMap<Integer, String>().mapIterator().next(),
                () -> new M3LinkedMap<Integer, String>().mapIterator().getKey(),
                () -> new M3LinkedMap<Integer, String>().mapIterator().previous(),
                () -> {
                    new M3HashedMap<Integer, String>().mapIterator().remove();
                    return null;
                },
                () -> new M3LinkedMap<Integer, String>().firstKey());
        for (int i = 0; i < cases.size(); i++) {
            assertEquals(call(cases.get(i)), call(m3.get(i)), "empty case " + i);
        }
        M3LinkedMap<Integer, String> map = new M3LinkedMap<>();
        assertTrue(map.keySet() == map.keySet() && map.values() == map.values() && map.entrySet() == map.entrySet());
    }

    @Test
    void cloneAndSerializationKeepContentAndOrder() throws Exception {
        M3LRUMap<Integer, String> lru = new M3LRUMap<>(50);
        Random random = new Random(11);
        for (int i = 0; i < 400; i++) {
            lru.put(random.nextInt(80), "v" + i);
            if (i % 5 == 0) {
                lru.get(random.nextInt(80));
            }
        }
        M3LRUMap<Integer, String> copy = lru.clone();
        assertEquals(order(lru), order(copy));
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(lru);
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            @SuppressWarnings("unchecked")
            M3LRUMap<Integer, String> back = (M3LRUMap<Integer, String>) in.readObject();
            assertEquals(order(lru), order(back));
            assertEquals(lru.maxSize(), back.maxSize());
        }
        copy.put(-1, "only in the copy");
        assertTrue(!lru.containsKey(-1));
    }

    @Test
    void theFillIsAFixedPoint() throws Exception {
        Path base = Path.of(System.getProperty("user.dir"));
        String template = Files.readString(base.resolve("templates/ARRAY_MAP.java.tmpl"), StandardCharsets.UTF_8);
        Path implementations = base.resolve("../../implementations");
        for (Map<String, String> member : M3ArrayMapFill.members(base.resolve("MEMBERS.tsv"))) {
            Path source = implementations.resolve(member.get("module"))
                    .resolve("src/main/java/jdk/m3/collections/apache/" + member.get("CLASS") + ".java");
            assertEquals(Files.readString(source, StandardCharsets.UTF_8), M3ArrayMapFill.render(template, member),
                    member.get("CLASS") + " is not the template's output: regenerate, do not hand-edit");
        }
    }
}
