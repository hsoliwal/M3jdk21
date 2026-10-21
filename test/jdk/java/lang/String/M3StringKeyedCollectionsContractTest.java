/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * GPLv2 with the Classpath exception.
 */

/*
 * @test
 * @summary String-keyed map and set contracts across equal composite spellings, collision, range, identity and UTF-16
 * @run main/othervm M3StringKeyedCollectionsContractTest
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Properties;
import java.util.Random;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

public class M3StringKeyedCollectionsContractTest {
    private static long checks;
    private static void verify(boolean value, String reason) {
        checks++;
        if (!value) throw new AssertionError(reason + " check=" + checks);
    }
    private static void equal(Object expected, Object actual, String reason) {
        verify(java.util.Objects.equals(expected, actual),
                reason + " expected=" + expected + " actual=" + actual);
    }
    private static void fails(Class<? extends Throwable> expected,
            Runnable action, String reason) {
        checks++;
        try { action.run(); }
        catch (Throwable failure) {
            if (expected.isInstance(failure)) return;
            throw new AssertionError(reason + " wrong failure", failure);
        }
        throw new AssertionError(reason + " missing failure");
    }

    /** Different Java String wrappers and composition/range boundaries, same UTF-16 units. */
    private static String[] spellings(String source) {
        int half = source.length() >>> 1;
        return new String[] {
                new String(source.toCharArray()),
                new String(source.substring(0, half)).concat(new String(source.substring(half))),
                ("[" + source + "]").substring(1, source.length() + 1),
                new StringBuilder().append(source, 0, half)
                        .append(source, half, source.length()).toString()
        };
    }

    private static void checkKey(String source) {
        String[] keys = spellings(source);
        for (String key : keys) {
            equal(source, key, "logical text");
            verify(source.hashCode() == key.hashCode(), "M3 hash parity");
            verify(source.compareTo(key) == 0, "M3 UTF-16 comparison parity");
        }

        HashMap<String, Integer> hashed = new HashMap<>();
        hashed.put(keys[0], 1);
        for (String key : keys) equal(1, hashed.get(key), "HashMap lookup");
        equal(1, hashed.put(keys[1], 2), "HashMap replace");
        verify(hashed.keySet().iterator().next() == keys[0], "inserted key object preserved");
        equal(2, hashed.computeIfAbsent(keys[2], x -> {
            throw new AssertionError("existing key must not run callback");
        }), "HashMap callback contract");

        HashSet<String> hashSet = new HashSet<>(Arrays.asList(keys));
        verify(hashSet.size() == 1 && hashSet.contains(source), "HashSet content membership");

        ConcurrentHashMap<String, Integer> concurrent = new ConcurrentHashMap<>();
        concurrent.put(keys[0], 3);
        equal(3, concurrent.putIfAbsent(keys[1], 4), "concurrent duplicate key");
        equal(3, concurrent.get(keys[3]), "CHM M3 String lookup");
        verify(concurrent.keySet().iterator().next() == keys[0],
                "CHM key object identity preserved");

        TreeMap<String, Integer> sorted = new TreeMap<>();
        sorted.put(keys[0], 5);
        equal(5, sorted.put(keys[2], 6), "TreeMap comparator-equal update");
        verify(sorted.size() == 1 && sorted.firstKey() == keys[0],
                "TreeMap key reference retained");
        TreeSet<String> treeSet = new TreeSet<>(Arrays.asList(keys));
        verify(treeSet.size() == 1, "TreeSet UTF-16 uniqueness");

        LinkedHashMap<String, Integer> ordered = new LinkedHashMap<>(16, 0.75f, true);
        ordered.put(keys[0], 1);
        ordered.put("tail", 2);
        equal(1, ordered.get(keys[3]), "access-order lookup");
        equal(List.of("tail", source), new ArrayList<>(ordered.keySet()),
                "LinkedHashMap access order");

        WeakHashMap<String, Integer> weak = new WeakHashMap<>();
        weak.put(keys[0], 9);
        equal(9, weak.get(keys[2]), "weak key content lookup");

        IdentityHashMap<String, Integer> identity = new IdentityHashMap<>();
        verify(keys[0] != keys[1], "distinct Java wrappers cannot be identity-interned");
        identity.put(keys[0], 1);
        identity.put(keys[1], 2);
        verify(identity.size() == 2, "identity map must not collapse canonical payloads");

        Hashtable<String, Integer> synchronizedMap = new Hashtable<>();
        synchronizedMap.put(keys[0], 11);
        equal(11, synchronizedMap.get(keys[1]), "Hashtable lookup");
        Map<String, Integer> unmodifiable = Collections.unmodifiableMap(hashed);
        equal(2, unmodifiable.get(keys[3]), "unmodifiable backed read");
        hashed.put(keys[2], 10);
        equal(10, unmodifiable.get(keys[1]), "unmodifiable backing visibility");
        fails(UnsupportedOperationException.class,
                () -> unmodifiable.put("not-allowed", 1), "unmodifiable write");
        equal(6, Map.of(keys[0], 6).get(keys[1]), "Map.of content lookup");
    }

    private static void edges() {
        String left = new String("Aa"), right = new String("BB");
        verify(left.hashCode() == right.hashCode() && !left.equals(right),
                "deliberate Java 31-hash collision");
        for (Map<String, Integer> map :
                List.<Map<String, Integer>>of(new HashMap<>(),
                        new LinkedHashMap<>(), new ConcurrentHashMap<>(), new TreeMap<>())) {
            map.put(left, 1);
            map.put(right, 2);
            verify(map.size() == 2, "hash candidate never proves key equality");
            equal(1, map.get(spellings("Aa")[1]), "Aa collision must verify content");
            equal(2, map.get(spellings("BB")[2]), "BB collision must verify content");
        }

        NavigableMap<String, Integer> natural = new TreeMap<>();
        natural.put("AA", 1); natural.put("AB", 2);
        natural.put("B", 3); natural.put("aa", 4);
        equal(List.of("AA", "AB"), new ArrayList<>(
                natural.subMap("AA", true, "B", false).keySet()),
                "TreeMap half-open UTF-16 range");
        NavigableMap<String, Integer> caseFold = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        caseFold.put(new String("AA"), 1);
        equal(1, caseFold.put(new String("aa"), 2), "case-insensitive comparator");
        verify(caseFold.size() == 1, "comparator semantics differ from String.equals");

        HashMap<String, Integer> nullMap = new HashMap<>();
        nullMap.put(null, 1);
        equal(1, nullMap.get(null), "HashMap null key");
        fails(NullPointerException.class, () -> new ConcurrentHashMap<String, Integer>().get(null),
                "CHM rejects null key");

        char[] writable = {'k', 'e', 'y'};
        String immutable = new String(writable);
        nullMap.put(immutable, 3);
        writable[0] = 'x';
        equal(3, nullMap.get("key"), "mutable input must not mutate String key");
        verify(!nullMap.containsKey("xey"), "mutable char[] admission isolation");

        Properties defaults = new Properties();
        defaults.setProperty(new String("name"), "m3");
        Properties properties = new Properties(defaults);
        equal("m3", properties.getProperty(spellings("name")[1]),
                "Properties default chain keeps String semantics");
        verify(new String("word").intern() == "word".intern(),
                "String.intern object identity independent of M3 payload sharing");
    }

    public static void main(String[] args) {
        edges();
        for (String value : new String[] {
                "", "a", "Aa", "BB", "\u0000", "\u00ff", "\u0100",
                "\ud800", "\udc00", "\ud83d\ude00", "x\ud83d\ude00y", "\ud800\udc00\ud800"
        }) checkKey(value);
        Random random = new Random(0x4d335354524b4559L);
        for (int sample = 0; sample < 256; sample++) {
            char[] chars = new char[random.nextInt(145)];
            for (int i = 0; i < chars.length; i++) chars[i] = (char) random.nextInt(65536);
            checkKey(new String(chars));
        }
        System.out.println("M3_STRING_KEYED_COLLECTIONS_PASS checks=" + checks);
    }
}
