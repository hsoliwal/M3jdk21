/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * @test
 * @summary Commons HashedMap takes its key equality as a strategy (an Equator), as TreeMap
 *          takes its ordering as a Comparator; maps without one are unchanged
 * @modules jdk.m3.collections
 * @run main HashedMapEquatorTest
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.TreeMap;
import jdk.m3.collections.apache.Equator;
import jdk.m3.collections.apache.map.HashedMap;
import jdk.m3.collections.apache.map.ReferenceIdentityMap;

public class HashedMapEquatorTest {

    static int checks;

    static void check(boolean condition, String what) {
        checks++;
        if (!condition) {
            throw new AssertionError(what);
        }
    }

    /** Strings equal ignoring case; never accepts null; serializable. */
    static final class CaseInsensitive implements Equator<String>, Serializable {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean equate(String a, String b) {
            return Objects.requireNonNull(a).equalsIgnoreCase(Objects.requireNonNull(b));
        }

        @Override
        public int hash(String s) {
            return Objects.requireNonNull(s).toLowerCase(Locale.ROOT).hashCode();
        }
    }

    /** The same, not serializable. */
    static final class CaseInsensitiveLocal implements Equator<String> {
        @Override
        public boolean equate(String a, String b) {
            return a.equalsIgnoreCase(b);
        }

        @Override
        public int hash(String s) {
            return s.toLowerCase(Locale.ROOT).hashCode();
        }
    }

    public static void main(String[] args) throws Exception {
        againstTreeMap(20261117L, 200_000);
        nullKey();
        serialization();
        cloneKeepsEquator();
        unchangedWithoutEquator(5L, 100_000);
        subclassesUnaffected();
        System.out.println("HashedMapEquatorTest: " + checks + " checks passed");
    }

    /** A case-insensitive HashedMap behaves as a TreeMap ordered case-insensitively. */
    static void againstTreeMap(long seed, int steps) {
        Random random = new Random(seed);
        HashedMap<String, Integer> map = new HashedMap<>(new CaseInsensitive());
        TreeMap<String, Integer> oracle = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (int i = 0; i < steps; i++) {
            String key = word(random);
            Integer value = random.nextInt(1_000);
            switch (random.nextInt(6)) {
                case 0, 1 ->
                    check(Objects.equals(map.put(key, value), oracle.put(key, value)), "put");
                case 2 -> check(Objects.equals(map.remove(key), oracle.remove(key)), "remove");
                case 3, 4 -> check(Objects.equals(map.get(key), oracle.get(key)), "get");
                default -> check(map.containsKey(key) == oracle.containsKey(key), "containsKey");
            }
            check(map.size() == oracle.size(), "size at step " + i);
        }
        for (Map.Entry<String, Integer> e : map.entrySet()) {
            check(Objects.equals(oracle.get(e.getKey()), e.getValue()), "entry " + e);
        }
    }

    /** The null key is the map's; the equator, which rejects null, never sees it. */
    static void nullKey() {
        HashedMap<String, Integer> map = new HashedMap<>(new CaseInsensitive());
        check(map.put(null, 1) == null && map.put("A", 2) == null, "puts");
        check(map.get(null) == 1 && map.get("a") == 2 && map.containsKey(null), "gets");
        check(map.remove(null) == 1 && !map.containsKey(null) && map.size() == 1, "remove null");
        try {
            new HashedMap<String, Integer>((Equator<String>) null);
            check(false, "a null equator must throw");
        } catch (NullPointerException expected) {
            checks++;
        }
    }

    /** The equator travels with the map; with a non-serializable one the map does not serialize. */
    static void serialization() throws Exception {
        HashedMap<String, Integer> map = new HashedMap<>(new CaseInsensitive());
        for (int i = 0; i < 1_000; i++) {
            map.put("Key" + i, i);
        }
        map.put(null, -1);
        HashedMap<String, Integer> back = copy(map);
        check(back.size() == map.size(), "size after copy");
        check(back.get("KEY500") == 500 && back.get("key999") == 999 && back.get(null) == -1,
                "lookups");
        back.put("KEY0", 7);
        check(back.size() == map.size() && back.get("key0") == 7, "still case-insensitive");
        try {
            HashedMap<String, Integer> local = new HashedMap<>(new CaseInsensitiveLocal());
            local.put("x", 1);
            copy(local);
            check(false, "a map with a non-serializable equator must not serialize");
        } catch (NotSerializableException expected) {
            checks++;
        }
    }

    static void cloneKeepsEquator() {
        HashedMap<String, Integer> map = new HashedMap<>(new CaseInsensitive());
        map.put("Pear", 1);
        HashedMap<String, Integer> clone = map.clone();
        clone.put("PEAR", 2);
        check(clone.size() == 1 && clone.get("pear") == 2 && map.get("pear") == 1, "clone");
    }

    /** Without an equator the map behaves as java.util.HashMap and serializes as before. */
    static void unchangedWithoutEquator(long seed, int steps) throws Exception {
        Random random = new Random(seed);
        HashedMap<String, Integer> map = new HashedMap<>();
        HashMap<String, Integer> oracle = new HashMap<>();
        for (int i = 0; i < steps; i++) {
            String key = random.nextInt(20) == 0 ? null : word(random);
            Integer value = random.nextInt(1_000);
            if (random.nextInt(3) == 0) {
                check(Objects.equals(map.remove(key), oracle.remove(key)), "remove");
            } else {
                check(Objects.equals(map.put(key, value), oracle.put(key, value)), "put");
            }
        }
        check(map.equals(oracle) && oracle.equals(map), "same mappings");
        HashedMap<String, Integer> back = copy(map);
        check(back.equals(oracle) && back.containsKey("ab") == oracle.containsKey("ab"), "copy");
        check(Objects.equals(back.get("AB"), oracle.get("AB")),
                "case still matters without an equator");
    }

    /** A subclass that overrides the hooks keeps its own equality. */
    static void subclassesUnaffected() {
        ReferenceIdentityMap<String, Integer> identity = new ReferenceIdentityMap<>();
        String a = new String("same");
        String b = new String("same");
        identity.put(a, 1);
        identity.put(b, 2);
        check(identity.size() == 2 && identity.get(a) == 1 && identity.get(b) == 2, "identity map");
    }

    @SuppressWarnings("unchecked")
    static <T> T copy(T value) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(value);
        }
        ByteArrayInputStream input = new ByteArrayInputStream(bytes.toByteArray());
        try (ObjectInputStream in = new ObjectInputStream(input)) {
            return (T) in.readObject();
        }
    }

    static String word(Random random) {
        char[] chars = new char[1 + random.nextInt(3)];
        for (int i = 0; i < chars.length; i++) {
            char c = (char) ('a' + random.nextInt(4));
            chars[i] = random.nextBoolean() ? Character.toUpperCase(c) : c;
        }
        return new String(chars);
    }
}
