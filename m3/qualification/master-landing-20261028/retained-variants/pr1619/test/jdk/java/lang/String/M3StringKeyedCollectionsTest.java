/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 *
 * @test
 * @summary Composed String keys preserve collection contracts and exact shared-range comparison
 * @run main/othervm -Xmx256m --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringKeyedCollectionsTest
 * @run main/othervm -Xmx256m --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:-CompactStrings M3StringKeyedCollectionsTest
 */

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class M3StringKeyedCollectionsTest {
    private static long checks;
    private static final Field BACKING;
    private static final Field OWNER;
    private static final Field COORDINATE;
    private static final Field VALUE;
    private static final Method FACTS;
    private static final Method FACTS_IF_PREPARED;
    private static final ThreadLocal<Boolean> ADMISSION_GUARD;

    static {
        try {
            Class<?> type = Class.forName("java.lang.M3String");
            BACKING = accessible(String.class.getDeclaredField("m3"));
            OWNER = accessible(type.getDeclaredField("owner"));
            COORDINATE = accessible(type.getDeclaredField("value"));
            VALUE = accessible(String.class.getDeclaredField("value"));
            FACTS = accessible(type.getDeclaredMethod("facts"));
            FACTS_IF_PREPARED = accessible(type.getDeclaredMethod("factsIfPrepared"));
            ADMISSION_GUARD = admissionGuard(type);
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    private record Key(char[] units, String flat, String whole, String left, String right) { }

    private record Body(Object owner, long coordinate, Object compatibility) {
        static Body of(String value) throws ReflectiveOperationException {
            Object storage = backing(value);
            check(storage != null, "fixture has actual M3 backing");
            return new Body(OWNER.get(storage), COORDINATE.getLong(storage), VALUE.get(value));
        }

        void unchanged(String value) throws ReflectiveOperationException {
            Object storage = backing(value);
            check(OWNER.get(storage) == owner && COORDINATE.getLong(storage) == coordinate,
                    "key operations retain canonical owner and coordinate");
            check(VALUE.get(value) == compatibility, "key operations retain compatibility sentinel");
        }
    }

    private record MapCase(String name, Supplier<Map<String, Integer>> factory,
                           boolean nullKeys, boolean nullValues) { }

    public static void main(String[] arguments) throws Exception {
        List<Key> keys = keys();
        for (Key key : keys) {
            Body left = Body.of(key.left);
            Body right = Body.of(key.right);
            check(left.owner != right.owner || left.coordinate != right.coordinate,
                    "equal keys deliberately have distinct owner/range coordinates");
            check(left.owner.getClass().getName().equals("java.lang.M3StringTuple"),
                    "equal range fixture exercises a tuple owner");
            check(backing(key.flat) == null, "flat oracle does not enter M3 storage");
            assertKey(key);
        }
        maps(keys);
        sets(keys);
        collisions();
        orderingAndComparators(keys);
        immutableAndStreams(keys);
        identityAndQueryDirection(keys.getFirst());
        weakKey(keys.getFirst());
        concurrency(keys.getFirst());
        sharedRangeHelper(keys);
        for (Key key : keys) {
            Body before = Body.of(key.left);
            assertKey(key);
            before.unchanged(key.left);
        }
        System.out.println("M3_STRING_KEYED_COLLECTIONS_PASS checks=" + checks);
    }

    private static List<Key> keys() throws ReflectiveOperationException {
        List<Key> result = new ArrayList<>();
        for (int trial = 0; trial < 24; trial++) {
            char[] units = new char[320 + trial * 3];
            for (int index = 0; index < units.length; index++) {
                units[index] = switch ((index + trial) % 13) {
                    case 0 -> '\0';
                    case 1 -> '\u00ff';
                    case 2 -> '\u0100';
                    case 3 -> '\ud800';
                    case 4 -> '\udc00';
                    case 5 -> '\uffff';
                    default -> (char) ('a' + (index + trial) % 26);
                };
            }
            units[units.length - 1] = (char) ('A' + trial);
            String whole = composite(units, 7 + trial % 5);
            String left = surrounded(whole, "left", "tail");
            String right = surrounded(whole, "other-left", "other-tail");
            result.add(new Key(units, flat(units), whole, left, right));
        }
        return result;
    }

    private static String surrounded(String text, String before, String after) {
        String parent = new String(before.toCharArray()).concat(text)
                .concat(new String(after.toCharArray()));
        return parent.substring(before.length(), before.length() + text.length());
    }

    private static String composite(char[] units, int pieces) {
        String result = "";
        int start = 0;
        for (int part = 1; part <= pieces; part++) {
            int end = (int) ((long) part * units.length / pieces);
            if (end != start) result = result.concat(new String(units, start, end - start));
            start = end;
        }
        return result;
    }

    private static void assertKey(Key key) throws ReflectiveOperationException {
        int hash = 0;
        for (char unit : key.units) hash = 31 * hash + unit;
        for (String spelling : List.of(key.whole, key.left, key.right)) {
            check(same(spelling, key.units), "independent UTF-16 spelling oracle");
            check(spelling.hashCode() == hash, "Java hash of composed/range key");
            check(spelling.equals(key.flat) && key.flat.equals(spelling), "flat key equality both ways");
            check(spelling.equals(key.left) && key.left.equals(spelling), "composed key equality");
            check(spelling.compareTo(key.flat) == 0 && spelling.compareTo(key.right) == 0,
                    "flat and composed key ordering equality");
            check(spelling.startsWith(key.right), "equal shared-range prefix");
        }
        String outer = new String("prefix".toCharArray()).concat(key.left);
        check(outer.startsWith(key.right, 6), "shared-range prefix at nonzero logical offset");
        check(!outer.startsWith(key.right, -1), "negative prefix offset");
        check(!outer.startsWith(key.right, Integer.MAX_VALUE), "overflow-safe prefix offset");
        FACTS.invoke(backing(key.left));
        check(key.left.equals(key.whole) && key.whole.equals(key.left), "prepared range and whole owner");
        char[] miss = key.units.clone();
        miss[miss.length - 1] ^= 1;
        String differentWhole = composite(miss, 11);
        check(!key.left.equals(differentWhole) && !differentWhole.equals(key.left),
                "prepared range and whole-owner different hashes");
        check(FACTS_IF_PREPARED.invoke(backing(key.left)) != null, "range facts remain prepared");
    }

    private static void maps(List<Key> keys) {
        List<MapCase> cases = List.of(
                new MapCase("HashMap", HashMap::new, true, true),
                new MapCase("LinkedHashMap", LinkedHashMap::new, true, true),
                new MapCase("access-order LinkedHashMap", () -> new LinkedHashMap<>(16, .75f, true), true, true),
                new MapCase("TreeMap", TreeMap::new, false, true),
                new MapCase("ConcurrentHashMap", ConcurrentHashMap::new, false, false),
                new MapCase("ConcurrentSkipListMap", ConcurrentSkipListMap::new, false, false),
                new MapCase("Hashtable", Hashtable::new, false, false));
        for (MapCase spec : cases) {
            Map<String, Integer> actual = spec.factory.get();
            Map<String, Integer> oracle = new LinkedHashMap<>();
            for (int index = 0; index < keys.size(); index++) {
                Key key = keys.get(index);
                check(actual.put(key.left, index) == null, spec.name + " first insertion");
                oracle.put(key.flat, index);
            }
            for (int index = 0; index < keys.size(); index++) {
                Key key = keys.get(index);
                check(Objects.equals(actual.get(key.right), index), spec.name + " equal range lookup");
                check(actual.containsKey(key.whole), spec.name + " whole-owner lookup");
                check(Objects.equals(actual.put(key.right, index + 100), index), spec.name + " equal-key replacement");
                oracle.put(key.flat, index + 100);
                String representative = actual.keySet().stream().filter(k -> same(k, key.units)).findFirst().orElseThrow();
                check(representative == key.left, spec.name + " original key representative");
                check(Objects.equals(actual.computeIfAbsent(key.right, k -> -1), index + 100), spec.name + " existing computeIfAbsent");
                check(actual.replace(key.whole, index + 100, index + 200), spec.name + " conditional replace");
                oracle.put(key.flat, index + 200);
                check(actual.merge(key.right, 1, Integer::sum) == index + 201, spec.name + " merge");
                oracle.put(key.flat, index + 201);
            }
            parity(actual, oracle, spec.name);
            if (!(actual instanceof ConcurrentHashMap<?, ?>)
                    && !(actual instanceof ConcurrentSkipListMap<?, ?>)) {
                var iterator = actual.entrySet().iterator();
                Map.Entry<String, Integer> first = iterator.next();
                String firstKey = first.getKey();
                Map.Entry<String, Integer> second = iterator.next();
                check(first != second && first.getKey() == firstKey,
                        spec.name + " iterator entries retain their mapping");
                first.setValue(777);
                oracle.put(flat(firstKey.toCharArray()), 777);
                check(Objects.equals(actual.get(firstKey), 777), spec.name + " entry setValue");
            }
            for (int index = 0; index < keys.size(); index += 2) {
                Key key = keys.get(index);
                check(Objects.equals(actual.remove(key.right), oracle.remove(key.flat)), spec.name + " equal range removal");
            }
            parity(actual, oracle, spec.name);
            if (spec.nullKeys) {
                actual.put(null, 1);
                check(actual.containsKey(null) && actual.remove(null) == 1, spec.name + " null key");
            } else {
                expect(NullPointerException.class, () -> actual.put(null, 1), spec.name + " null-key refusal");
            }
            if (spec.nullValues) {
                actual.put(keys.getFirst().left, null);
                check(actual.containsKey(keys.getFirst().right) && actual.get(keys.getFirst().right) == null,
                        spec.name + " null value differs from absence");
            } else {
                expect(NullPointerException.class, () -> actual.put(keys.getFirst().left, null), spec.name + " null-value refusal");
            }
        }
        HashMap<String, Integer> zeros = new HashMap<>();
        String empty = new String(new char[0]);
        String nul = new String(new char[] {0});
        String twoNuls = new String(new char[] {0, 0});
        zeros.put(empty, 0); zeros.put(nul, 1); zeros.put(twoNuls, 2); zeros.put(null, 3);
        check(zeros.size() == 4 && zeros.get("") == 0 && zeros.get("\0") == 1
                        && zeros.get("\0\0") == 2 && zeros.get(null) == 3,
                "zero hashes, empty text and null remain distinct keys");
    }

    private static void parity(Map<String, Integer> actual, Map<String, Integer> oracle, String label) {
        check(actual.size() == oracle.size(), label + " cardinality");
        for (var entry : oracle.entrySet()) {
            check(actual.containsKey(entry.getKey()), label + " flat oracle membership");
            check(Objects.equals(actual.get(entry.getKey()), entry.getValue()), label + " flat oracle value");
        }
        check(actual.equals(oracle) && oracle.equals(actual), label + " Map equality");
        check(actual.hashCode() == oracle.hashCode(), label + " Map hashCode");
    }

    private static void sets(List<Key> keys) {
        List<Supplier<Set<String>>> cases = List.of(HashSet::new, LinkedHashSet::new,
                TreeSet::new, ConcurrentHashMap::newKeySet, ConcurrentSkipListSet::new,
                CopyOnWriteArraySet::new);
        for (Supplier<Set<String>> factory : cases) {
            Set<String> actual = factory.get();
            for (Key key : keys) {
                check(actual.add(key.left), "set first key insertion");
                check(!actual.add(key.right), "set equal differently composed key");
                check(actual.contains(key.flat), "set flat lookup");
            }
            Set<String> oracle = keys.stream().map(Key::flat).collect(Collectors.toSet());
            check(actual.equals(oracle) && oracle.equals(actual), "Set equality");
            check(actual.hashCode() == oracle.hashCode(), "Set hashCode");
            for (Key key : keys) check(actual.remove(key.whole), "set whole-owner removal");
            check(actual.isEmpty(), "set empty after equal-key removal");
        }
    }

    private static void collisions() {
        String common = new String("shared-prefix-".repeat(24).toCharArray());
        Map<String, Integer> table = new HashMap<>(128);
        List<String> queries = new ArrayList<>();
        int hash = 0;
        for (int value = 0; value < 64; value++) {
            char[] suffix = new char[12];
            for (int bit = 0; bit < 6; bit++) {
                suffix[bit * 2] = (value & (1 << bit)) == 0 ? 'A' : 'B';
                suffix[bit * 2 + 1] = (value & (1 << bit)) == 0 ? 'a' : 'B';
            }
            String spelling = common.concat(new String(suffix));
            String stored = surrounded(spelling, "one", "tail");
            String query = surrounded(spelling, "two", "other");
            if (value == 0) hash = stored.hashCode();
            check(stored.hashCode() == hash, "equal Java hash collision family");
            table.put(stored, value);
            queries.add(query);
        }
        check(table.size() == 64, "hash collisions retain all different keys through treeification");
        for (int index = 0; index < queries.size(); index++) {
            check(table.get(queries.get(index)) == index, "colliding differently composed key lookup");
        }
    }

    private static void orderingAndComparators(List<Key> keys) {
        TreeMap<String, Integer> sorted = new TreeMap<>();
        for (int index = 0; index < keys.size(); index++) sorted.put(keys.get(index).left, index);
        List<Key> expected = new ArrayList<>(keys);
        expected.sort((a, b) -> compare(a.units, 0, b.units, 0, Math.min(a.units.length, b.units.length), true));
        int index = 0;
        for (String key : sorted.keySet()) check(same(key, expected.get(index++).units), "natural UTF-16 key order");
        Key middle = expected.get(expected.size() / 2);
        check(sorted.ceilingKey(middle.right) == middle.left, "equal range ceiling returns stored key");
        check(sorted.floorKey(middle.flat) == middle.left, "flat floor returns stored key");
        check(sorted.subMap(expected.get(3).right, true, expected.get(10).right, false).size() == 7,
                "live sorted range uses exact string order");
        AtomicInteger comparisons = new AtomicInteger();
        Comparator<String> caseInsensitive = (a, b) -> {
            comparisons.incrementAndGet();
            return String.CASE_INSENSITIVE_ORDER.compare(a, b);
        };
        String upper = surrounded(new String("ABC".repeat(128).toCharArray()), "x", "y");
        String lower = surrounded(new String("abc".repeat(128).toCharArray()), "other", "end");
        TreeMap<String, Integer> folded = new TreeMap<>(caseInsensitive);
        folded.put(upper, 1);
        folded.put(lower, 2);
        check(folded.size() == 1 && folded.get(lower) == 2 && comparisons.get() > 0,
                "caller comparator controls key equivalence and executes");
        check(folded.firstKey() == upper, "comparator-equal update retains original key");
        LinkedHashMap<String, Integer> access = new LinkedHashMap<>(16, .75f, true);
        access.put(keys.get(0).left, 0); access.put(keys.get(1).left, 1);
        access.get(keys.get(0).right);
        check(access.lastEntry().getKey() == keys.get(0).left, "equal-range lookup preserves access-order effect");
    }

    private static void immutableAndStreams(List<Key> keys) {
        Map<String, Integer> source = new LinkedHashMap<>();
        List<String> duplicates = new ArrayList<>();
        for (int index = 0; index < keys.size(); index++) {
            Key key = keys.get(index);
            source.put(key.left, index);
            duplicates.add(key.left); duplicates.add(key.right); duplicates.add(key.flat);
        }
        Map<String, Integer> frozen = Map.copyOf(source);
        Set<String> frozenSet = Set.copyOf(duplicates);
        for (int index = 0; index < keys.size(); index++) {
            check(frozen.get(keys.get(index).right) == index, "immutable map composed-key lookup");
            check(frozenSet.contains(keys.get(index).right), "immutable set composed-key lookup");
        }
        Key first = keys.getFirst();
        expect(IllegalArgumentException.class, () -> Map.of(first.left, 1, first.right, 2), "immutable map duplicate key");
        expect(IllegalArgumentException.class, () -> Set.of(first.left, first.right), "immutable set duplicate key");
        check(duplicates.stream().distinct().count() == keys.size(), "stream distinct composed keys");
        Map<String, Long> grouped = duplicates.stream().collect(Collectors.groupingBy(k -> k, Collectors.counting()));
        check(grouped.size() == keys.size(), "groupingBy composed-key cardinality");
        for (Key key : keys) check(grouped.get(key.right) == 3L, "groupingBy equivalent key count");
        Map<String, Integer> merged = duplicates.stream().collect(Collectors.toMap(k -> k, k -> 1, Integer::sum));
        for (Key key : keys) check(merged.get(key.right) == 3, "toMap composed key merge");
    }

    private static void identityAndQueryDirection(Key key) {
        IdentityHashMap<String, Integer> identity = new IdentityHashMap<>();
        identity.put(key.left, 1); identity.put(key.right, 2);
        check(key.left != key.right && identity.size() == 2 && identity.get(key.left) == 1
                        && identity.get(key.right) == 2 && identity.get(key.whole) == null,
                "shared payload does not collapse Java key identity");
        Map<String, Integer> ordinary = new HashMap<>();
        ordinary.put(key.left, 17);
        Probe probe = new Probe(key.flat, false);
        check(ordinary.get(probe) == 17 && probe.calls > 0, "generic lookup keeps query-first equals");
        check(!key.left.equals(probe), "String equality does not accept an arbitrary CharSequence-like key");
        Probe throwing = new Probe(key.flat, true);
        try {
            ordinary.get(throwing);
            throw new AssertionError("query exception must propagate");
        } catch (Sentinel expected) {
            check(expected == throwing.failure, "query callback exception identity");
        }
    }

    private static final class Sentinel extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    private static final class Probe {
        private final String text;
        private final boolean throwing;
        private final Sentinel failure = new Sentinel();
        private int calls;
        Probe(String text, boolean throwing) { this.text = text; this.throwing = throwing; }
        @Override public int hashCode() { return text.hashCode(); }
        @Override public boolean equals(Object other) {
            calls++;
            if (throwing) throw failure;
            return other instanceof String string && same(string, text.toCharArray());
        }
    }

    private static void weakKey(Key key) throws InterruptedException {
        WeakHashMap<String, Integer> weak = new WeakHashMap<>();
        WeakReference<String> original = addWeak(weak, key.left);
        for (int attempt = 0; original.get() != null && attempt < 60; attempt++) {
            System.gc();
            Thread.sleep(10);
        }
        check(original.get() == null, "weak map releases original String object despite shared live payload");
        check(weak.isEmpty(), "weak entry disappears when original key object is collected");
        Reference.reachabilityFence(key.left);
    }

    private static WeakReference<String> addWeak(WeakHashMap<String, Integer> map, String equal) {
        String original = new String(equal);
        check(original != equal && original.equals(equal), "weak fixture distinct equal String");
        map.put(original, 1);
        check(map.get(equal) == 1, "weak map equal-key lookup before collection");
        return new WeakReference<>(original);
    }

    private static void concurrency(Key key) throws InterruptedException {
        for (ConcurrentMap<String, Integer> map : List.<ConcurrentMap<String, Integer>>of(
                new ConcurrentHashMap<>(), new ConcurrentSkipListMap<>())) {
            AtomicInteger prepared = new AtomicInteger();
            AtomicReference<Throwable> failure = new AtomicReference<>();
            Thread[] workers = new Thread[4];
            for (int index = 0; index < workers.length; index++) {
                String query = index % 2 == 0 ? key.left : key.right;
                workers[index] = new Thread(() -> {
                    try {
                        map.computeIfAbsent(query, k -> { prepared.incrementAndGet(); return 0; });
                        for (int step = 0; step < 100; step++) map.merge(query, 1, Integer::sum);
                    } catch (Throwable problem) { failure.compareAndSet(null, problem); }
                });
                workers[index].start();
            }
            for (Thread worker : workers) worker.join();
            if (failure.get() != null) throw new AssertionError("concurrent key consumer", failure.get());
            check(map.size() == 1 && map.get(key.flat) == 400, "concurrent equal keys share one atomic mapping");
            if (map instanceof ConcurrentHashMap<?, ?>) {
                check(prepared.get() == 1, "ConcurrentHashMap computeIfAbsent invokes once");
            }
        }
    }

    private static void sharedRangeHelper(List<Key> keys) throws Exception {
        if (Boolean.getBoolean("m3.string.keys.baseline")) {
            System.out.println("M3_STRING_KEY_HELPER_BASELINE_NOT_PRESENT");
            return;
        }
        Class<?> storageType = Class.forName("java.lang.M3String");
        Class<?> cursorType = Class.forName("java.lang.M3String$CodeUnitCursor");
        Constructor<?> constructor = cursorType.getDeclaredConstructor(storageType);
        constructor.setAccessible(true);
        Method compare = accessible(cursorType.getDeclaredMethod("compareRange",
                cursorType, int.class, int.class, int.class));
        Random random = new Random(0x4d334b455952414eL);
        for (int round = 0; round < 240; round++) {
            Key a = keys.get(random.nextInt(keys.size()));
            Key b = round % 2 == 0 ? a : keys.get(random.nextInt(keys.size()));
            Object left = constructor.newInstance(backing(a.left));
            Object right = constructor.newInstance(backing(b.right));
            for (int pass = 0; pass < 5; pass++) {
                int start = random.nextInt(a.units.length + 1);
                int other = pass % 2 == 0 && a == b ? start : random.nextInt(b.units.length + 1);
                int length = random.nextInt(Math.min(a.units.length - start, b.units.length - other) + 1);
                int expected = compare(a.units, start, b.units, other, length, false);
                check((int) compare.invoke(left, right, start, other, length) == expected,
                        "exact helper handles shared, shifted, different and backward-seek ranges");
                if (a == b) {
                    check((int) compare.invoke(left, left, start, other, length) == expected,
                            "same cursor alias keeps shifted-range semantics");
                }
            }
            expectInvocation(IndexOutOfBoundsException.class, compare, left, right, -1, 0, 1);
            expectInvocation(IndexOutOfBoundsException.class, compare, left, right, 0, 0, Integer.MAX_VALUE);
            expectInvocation(IndexOutOfBoundsException.class, compare, left, right, 0, -1, 1);
            expectInvocation(IndexOutOfBoundsException.class, compare, left, right, 0, 0, -1);
        }
    }

    private static int compare(char[] a, int ai, char[] b, int bi, int count, boolean lengths) {
        for (int index = 0; index < count; index++) {
            int difference = a[ai + index] - b[bi + index];
            if (difference != 0) return difference;
        }
        return lengths ? a.length - b.length : 0;
    }

    private static boolean same(String value, char[] expected) {
        if (value.length() != expected.length) return false;
        for (int index = 0; index < expected.length; index++) {
            if (value.charAt(index) != expected[index]) return false;
        }
        return true;
    }

    private static String flat(char[] units) {
        Boolean previous = ADMISSION_GUARD.get();
        ADMISSION_GUARD.set(Boolean.TRUE);
        try { return new String(units); }
        finally {
            if (previous == null) ADMISSION_GUARD.remove();
            else ADMISSION_GUARD.set(previous);
        }
    }

    private static Object backing(String value) throws IllegalAccessException {
        value.length();
        return BACKING.get(value);
    }

    @SuppressWarnings("unchecked")
    private static ThreadLocal<Boolean> admissionGuard(Class<?> type) throws ReflectiveOperationException {
        return (ThreadLocal<Boolean>) accessible(type.getDeclaredField("ADMITTING")).get(null);
    }

    private static Field accessible(Field field) { field.setAccessible(true); return field; }
    private static Method accessible(Method method) { method.setAccessible(true); return method; }

    private static void expect(Class<? extends Throwable> kind, Runnable action, String label) {
        try { action.run(); }
        catch (Throwable failure) {
            check(kind.isInstance(failure), label + " original exception type");
            return;
        }
        throw new AssertionError(label + " did not throw");
    }

    private static void expectInvocation(Class<? extends Throwable> kind, Method method,
                                         Object receiver, Object... arguments) throws Exception {
        try { method.invoke(receiver, arguments); }
        catch (InvocationTargetException failure) {
            check(kind.isInstance(failure.getCause()), "range helper original exception type");
            return;
        }
        throw new AssertionError("range helper did not reject bounds");
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
