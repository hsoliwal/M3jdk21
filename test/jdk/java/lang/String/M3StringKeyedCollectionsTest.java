/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary Composite String keys preserve equality, hash collision, ordering and ownership contracts
 * @run main/othervm -Xmx128m --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringKeyedCollectionsTest
 * @run main/othervm -Xmx128m --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:-CompactStrings M3StringKeyedCollectionsTest
 */

import java.lang.ref.Reference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/** Optional reference-only mode validates the fixture on a stock VM, never M3 integration. */
public class M3StringKeyedCollectionsTest {
    private static final boolean REFERENCE_ONLY = Boolean.getBoolean("m3.oracle.only");
    private static final int KEY_COUNT = 64;
    private static long checks;

    public static void main(String[] arguments) throws Exception {
        String[] keys = new String[KEY_COUNT];
        String[] queries = new String[KEY_COUNT];
        char[][] expected = new char[KEY_COUNT][];
        for (int key = 0; key < KEY_COUNT; key++) {
            char[] units = ("long-collision-key-prefix/" + collisionTail(key)).toCharArray();
            expected[key] = units;
            keys[key] = segmented(units, 3);
            queries[key] = new String(units.clone());
            check(keys[key] != queries[key], "equal key uses a distinct String object");
            check(keys[key].equals(queries[key]) && queries[key].equals(keys[key]),
                    "different segmentation remains equal in both directions");
            check(keys[key].hashCode() == hash(units), "exact Java hash oracle");
            check(queries[key].hashCode() == keys[key].hashCode(), "segmentation-independent hash");
        }
        for (String key : keys) check(key.hashCode() == keys[0].hashCode(), "all keys collide");
        if (!REFERENCE_ONLY) requireDifferentOwners(keys[0], queries[0]);
        verifyComparisons(keys, queries, expected);

        List<Supplier<Map<String, Integer>>> maps = List.of(
                () -> new HashMap<>(128), () -> new LinkedHashMap<>(128), TreeMap::new,
                ConcurrentHashMap::new, ConcurrentSkipListMap::new, Hashtable::new,
                WeakHashMap::new);
        for (Supplier<Map<String, Integer>> factory : maps) {
            exerciseMap(factory.get(), keys, queries);
        }
        List<Supplier<Set<String>>> sets = List.of(
                () -> new HashSet<>(128), LinkedHashSet::new, TreeSet::new,
                ConcurrentHashMap::newKeySet, ConcurrentSkipListSet::new);
        for (Supplier<Set<String>> factory : sets) exerciseSet(factory.get(), keys, queries);

        naturalOrder(keys, queries, expected);
        immutableFactories(keys, queries);
        identityWeakAndComparatorContracts(keys, queries);
        propertyAndStreamConsumers(keys, queries);
        Reference.reachabilityFence(keys);
        Reference.reachabilityFence(queries);
        System.out.println("M3_STRING_KEYED_COLLECTIONS_PASS checks=" + checks
                + " referenceOnly=" + REFERENCE_ONLY);
    }

    private static String collisionTail(int number) {
        char[] result = new char[12];
        for (int bit = 0; bit < 6; bit++) {
            result[bit * 2] = (number & (1 << bit)) == 0 ? 'A' : 'B';
            result[bit * 2 + 1] = (number & (1 << bit)) == 0 ? 'a' : 'B';
        }
        return new String(result);
    }

    private static String segmented(char[] units, int width) {
        String[] parts = new String[(units.length + width - 1) / width];
        for (int index = 0; index < parts.length; index++) {
            int start = index * width;
            int length = Math.min(width, units.length - start);
            char[] padded = new char[length + 2];
            padded[0] = '!';
            padded[length + 1] = '?';
            System.arraycopy(units, start, padded, 1, length);
            parts[index] = new String(padded).substring(1, length + 1);
        }
        return String.join("", parts);
    }

    private static void exerciseMap(Map<String, Integer> map, String[] keys, String[] queries) {
        String label = map.getClass().getSimpleName();
        for (int index = 0; index < keys.length; index++) {
            check(map.put(keys[index], index) == null, label + " insert");
        }
        check(map.size() == keys.length, label + " collisions retain distinct text");
        for (int index = 0; index < keys.length; index++) {
            check(map.get(queries[index]) == index, label + " equal-key lookup");
            check(map.containsKey(queries[index]), label + " contains equal key");
            int id = index;
            check(map.computeIfAbsent(queries[index], ignored -> {
                throw new AssertionError("unexpected callback for existing key " + id);
            }) == index, label + " computeIfAbsent equality");
            check(map.put(queries[index], index + 1000) == index, label + " equal-key replacement");
            check(map.size() == keys.length, label + " no duplicate key after replacement");
        }
        if (map instanceof LinkedHashMap) {
            List<Integer> order = new ArrayList<>(map.values());
            for (int index = 0; index < keys.length; index++) {
                check(order.get(index) == index + 1000, "insertion order retained");
            }
        }
        for (int index = 0; index < keys.length; index++) {
            check(map.remove(keys[index], index + 1000), label + " conditional remove");
        }
        check(map.isEmpty(), label + " empty after removal");
    }

    private static void exerciseSet(Set<String> set, String[] keys, String[] queries) {
        for (int index = 0; index < keys.length; index++) check(set.add(keys[index]), "set insert");
        for (int index = 0; index < keys.length; index++) {
            check(set.contains(queries[index]), "set equal-key lookup");
            check(!set.add(queries[index]), "set rejects equal differently segmented duplicate");
        }
        check(set.size() == keys.length, "set cardinality with collisions");
        for (String query : queries) check(set.remove(query), "set equal-key removal");
        check(set.isEmpty(), "set removal completion");
    }

    private static void naturalOrder(String[] keys, String[] queries, char[][] units) {
        List<Integer> expected = new ArrayList<>();
        for (int index = 0; index < units.length; index++) expected.add(index);
        expected.sort((left, right) -> compare(units[left], units[right]));
        TreeMap<String, Integer> tree = new TreeMap<>();
        ConcurrentSkipListMap<String, Integer> concurrent = new ConcurrentSkipListMap<>();
        for (int index = keys.length - 1; index >= 0; index--) {
            tree.put(keys[index], index);
            concurrent.put(keys[index], index);
        }
        check(expected.equals(new ArrayList<>(tree.values())), "TreeMap independent UTF-16 order");
        check(expected.equals(new ArrayList<>(concurrent.values())), "skip-list independent UTF-16 order");
        for (int ordinal = 0; ordinal < expected.size(); ordinal++) {
            int id = expected.get(ordinal);
            check(tree.floorKey(queries[id]).equals(keys[id]), "floor equality coordinate");
            check(tree.ceilingKey(queries[id]).equals(keys[id]), "ceiling equality coordinate");
            if (ordinal > 0) check(tree.lowerEntry(queries[id]).getValue().equals(expected.get(ordinal - 1)),
                    "lower key order");
            if (ordinal + 1 < expected.size()) {
                check(concurrent.higherEntry(queries[id]).getValue().equals(expected.get(ordinal + 1)),
                        "higher key order");
            }
        }
    }

    private static void verifyComparisons(String[] keys, String[] queries, char[][] units) {
        for (int left = 0; left < keys.length; left++) {
            for (int right = 0; right < keys.length; right++) {
                int difference = compare(units[left], units[right]);
                check(keys[left].compareTo(queries[right]) == difference, "tuple/scalar exact compare result");
                check(queries[left].compareTo(keys[right]) == difference, "scalar/tuple exact compare result");
                check(keys[left].equals(queries[right]) == (left == right), "hash collision never proves equality");
            }
        }
        char[][] seams = {{}, {'a'}, {'a', '\0'}, {'a', '\ud83d', '\ude42'},
                {'a', '\ud83d'}, {'a', '\ude42'}, {'a', '\uffff'}, {'a', '\ud800', 'b'}};
        for (char[] left : seams) {
            for (char[] right : seams) {
                check(segmented(left, 1).compareTo(segmented(right, 2)) == compare(left, right),
                        "surrogate and prefix ordering uses code units");
            }
        }
        expect(NullPointerException.class, () -> keys[0].compareTo(null));
        check(!keys[0].equals(null) && !keys[0].equals(new StringBuilder(keys[0])),
                "String equals type and null contract");
    }

    private static void immutableFactories(String[] keys, String[] queries) {
        Map<String, Integer> input = new HashMap<>();
        for (int index = 0; index < keys.length; index++) input.put(keys[index], index);
        Map<String, Integer> immutable = Map.copyOf(input);
        for (int index = 0; index < keys.length; index++) {
            check(immutable.get(queries[index]) == index, "immutable map colliding lookup");
        }
        Map<String, Integer> pair = Map.of(keys[0], 0, keys[1], 1);
        check(pair.get(queries[0]) == 0 && pair.get(queries[1]) == 1, "Map.of collision lookup");
        expect(IllegalArgumentException.class, () -> Map.of(keys[0], 0, queries[0], 1));
        Set<String> immutableSet = Set.of(keys);
        for (String query : queries) check(immutableSet.contains(query), "Set.of collision lookup");
        expect(IllegalArgumentException.class, () -> Set.of(keys[0], queries[0]));
        check(Collections.singletonMap(keys[0], 0).get(queries[0]) == 0, "singleton map equality");
        check(Collections.unmodifiableMap(input).get(queries[1]) == 1, "unmodifiable map equality");
    }

    private static void identityWeakAndComparatorContracts(String[] keys, String[] queries) {
        IdentityHashMap<String, Integer> identity = new IdentityHashMap<>();
        identity.put(keys[0], 0);
        identity.put(queries[0], 1);
        check(identity.size() == 2 && identity.get(keys[0]) == 0 && identity.get(queries[0]) == 1,
                "equal payload never replaces Java reference identity");
        WeakHashMap<String, Integer> weak = new WeakHashMap<>();
        weak.put(keys[0], 7);
        check(weak.get(queries[0]) == 7, "live weak key retains String equality semantics");
        Reference.reachabilityFence(keys[0]);

        AtomicInteger calls = new AtomicInteger();
        Comparator<String> byLength = (left, right) -> {
            calls.incrementAndGet();
            return Integer.compare(left.length(), right.length());
        };
        TreeMap<String, Integer> custom = new TreeMap<>(byLength);
        custom.put(keys[0], 0);
        custom.put(keys[1], 1);
        check(custom.size() == 1, "custom comparator defines key equivalence");
        int before = calls.get();
        check(custom.get(queries[0]) == 1 && calls.get() > before,
                "custom comparator remains observable on lookup");
        TreeSet<String> insensitive = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        insensitive.add(segmented("CaseKey".toCharArray(), 2));
        check(!insensitive.add(new String("cASEkEY".toCharArray())),
                "case-insensitive comparator retains its own equivalence");
    }

    private static void propertyAndStreamConsumers(String[] keys, String[] queries) {
        Properties properties = new Properties();
        properties.setProperty(keys[0], "value");
        check("value".equals(properties.getProperty(queries[0])), "Properties String-key lookup");
        List<String> repeated = Arrays.asList(keys[0], queries[0], keys[1], queries[1]);
        check(repeated.stream().distinct().count() == 2, "distinct uses value equality");
        Map<String, Long> groups = repeated.stream().collect(Collectors.groupingBy(
                value -> value, Collectors.counting()));
        check(groups.size() == 2 && groups.get(queries[0]) == 2L && groups.get(queries[1]) == 2L,
                "groupingBy preserves String-key groups");
    }

    private static int hash(char[] units) {
        int hash = 0;
        for (char unit : units) hash = 31 * hash + unit;
        return hash;
    }

    private static int compare(char[] left, char[] right) {
        for (int index = 0; index < Math.min(left.length, right.length); index++) {
            int difference = left[index] - right[index];
            if (difference != 0) return difference;
        }
        return left.length - right.length;
    }

    private static void requireDifferentOwners(String left, String right) throws Exception {
        Field storage = String.class.getDeclaredField("m3");
        storage.setAccessible(true);
        Object leftStorage = storage.get(left);
        Object rightStorage = storage.get(right);
        check(leftStorage != null && rightStorage != null, "target Strings have canonical M3 storage");
        Field owner = Class.forName("java.lang.M3String").getDeclaredField("owner");
        owner.setAccessible(true);
        check(owner.get(leftStorage) != owner.get(rightStorage),
                "fixture equality requires content comparison across distinct canonical owners");
    }

    private static void expect(Class<? extends RuntimeException> expected, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException failure) {
            check(expected.isInstance(failure), "expected exception category");
            return;
        }
        throw new AssertionError("expected " + expected.getName());
    }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }
}
