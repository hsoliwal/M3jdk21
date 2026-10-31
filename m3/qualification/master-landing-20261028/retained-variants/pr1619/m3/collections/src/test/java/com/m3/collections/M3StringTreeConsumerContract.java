/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Random;
import java.util.Set;

/** Existing AVL owners consuming ordinary, optionally M3-composed, String keys. */
public final class M3StringTreeConsumerContract {
    private static int checks;
    private M3StringTreeConsumerContract() { }

    private record Key(char[] units, String text, int value) { }
    private record Backing(Object body, Object owner, long coordinate) { }

    private static void check(boolean condition, String reason) {
        checks++;
        if (!condition) throw new AssertionError(reason);
    }

    private static int compare(char[] left, char[] right) {
        for (int i = 0; i < Math.min(left.length, right.length); i++) {
            int difference = left[i] - right[i];
            if (difference != 0) return difference;
        }
        return left.length - right.length;
    }

    private static boolean prefix(char[] source, char[] prefix) {
        if (prefix.length > source.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (source[i] != prefix[i]) return false;
        }
        return true;
    }

    private static String composed(char[] units, int salt) {
        char[] framed = new char[units.length + 2];
        framed[0] = '[';
        System.arraycopy(units, 0, framed, 1, units.length);
        framed[framed.length - 1] = ']';
        String result = "";
        for (int cursor = 0; cursor < framed.length;) {
            int end = Math.min(framed.length, cursor + 1 + Math.floorMod(cursor + salt, 23));
            result = result.concat(new String(framed, cursor, end - cursor));
            cursor = end;
        }
        return result.substring(1, result.length() - 1);
    }

    private static List<Key> corpus() {
        List<char[]> units = new ArrayList<>();
        for (String value : new String[] {"", "\0", "\0x", "A", "Aa", "BB", "a",
                "alpha", "alphabet", "alphanumeric", "alpine", "beta", "\u00ff",
                "\u0100", "\uD800", "\uD800x", "\uD83D\uDE03", "\uD83D\uDE03x",
                "\uDC00", "\uffff", "\uffff\0"}) units.add(value.toCharArray());
        char[] shared = new char[259];
        for (int i = 0; i < shared.length; i++) shared[i] = (char) ('a' + i % 23);
        shared[31] = '\uD83D';
        shared[32] = '\uDE03';
        shared[63] = '\0';
        shared[127] = '\uD800';
        shared[128] = '\uDC00';
        for (int length : new int[] {17, 31, 32, 33, 64, 128, 129, 200, 258, 259}) {
            units.add(Arrays.copyOf(shared, length));
        }
        char[] sibling = shared.clone();
        sibling[200] = 'z';
        units.add(sibling);
        List<Key> result = new ArrayList<>();
        for (int i = 0; i < units.size(); i++) {
            result.add(new Key(units.get(i), composed(units.get(i), i), i + 1));
        }
        return result;
    }

    private static void exactKeys(Iterable<String> actual, List<Key> expected, String context) {
        Iterator<String> iterator = actual.iterator();
        for (Key key : expected) {
            check(iterator.hasNext(), context + " missing key");
            String observed = iterator.next();
            check(observed == key.text(), context + " retained reference");
            check(observed.length() == key.units().length, context + " UTF16 length");
            for (int i = 0; i < key.units().length; i++) {
                check(observed.charAt(i) == key.units()[i], context + " UTF16 units");
            }
        }
        check(!iterator.hasNext(), context + " extra key");
    }

    private static void state(M3TreeMap<String, Integer> map, M3TreeSet<String> set,
                              List<Key> expected) {
        map.verifyInvariants();
        set.verifyInvariants();
        expected.sort((left, right) -> compare(left.units(), right.units()));
        check(map.size() == expected.size() && set.size() == expected.size(), "owner size");
        exactKeys(map.navigableKeySet(), expected, "map order");
        exactKeys(set, expected, "set order");
        List<Key> reversed = new ArrayList<>(expected);
        Collections.reverse(reversed);
        exactKeys(map.descendingKeySet(), reversed, "descending map order");
        exactKeys(set.descendingSet(), reversed, "descending set order");
        for (Key key : expected) {
            String alias = composed(key.units(), 67);
            check(map.get(alias).intValue() == key.value(), "equal key lookup");
            check(set.contains(alias), "equal set lookup");
        }
    }

    private static void prefixes(M3TreeMap<String, Integer> map, M3TreeSet<String> set,
                                 List<Key> expected) {
        char[] longest = expected.stream().map(Key::units)
                .max(Comparator.comparingInt(value -> value.length)).orElseThrow();
        for (int length : new int[] {0, 1, 17, 31, 32, 33, 63, 64, 127, 128, 129, 201, 258}) {
            char[] units = Arrays.copyOf(longest, length);
            String query = composed(units, 91);
            List<Key> selected = expected.stream().filter(key -> prefix(key.units(), units)).toList();
            NavigableMap<String, Integer> view = M3FilteredViews.filterEntries(map,
                    (key, value) -> key.startsWith(query));
            NavigableSet<String> setView = M3FilteredViews.filter((NavigableSet<String>) set,
                    key -> key.startsWith(query));
            exactKeys(view.navigableKeySet(), selected, "prefix map units=" + length);
            exactKeys(setView, selected, "prefix set units=" + length);
            check(view.size() == selected.size(), "prefix cardinality");
        }
    }

    private static void ranges(M3TreeMap<String, Integer> map, M3TreeSet<String> set,
                               List<Key> expected) {
        for (boolean lowerInclusive : new boolean[] {false, true}) {
            for (boolean upperInclusive : new boolean[] {false, true}) {
                Key lower = expected.get(5);
                Key upper = expected.get(expected.size() - 5);
                List<Key> selected = expected.stream().filter(key -> {
                    int left = compare(key.units(), lower.units());
                    int right = compare(key.units(), upper.units());
                    return (left > 0 || left == 0 && lowerInclusive)
                            && (right < 0 || right == 0 && upperInclusive);
                }).toList();
                exactKeys(map.subMap(composed(lower.units(), 33), lowerInclusive,
                        composed(upper.units(), 41), upperInclusive).navigableKeySet(), selected, "map range");
                exactKeys(set.subSet(composed(lower.units(), 33), lowerInclusive,
                        composed(upper.units(), 41), upperInclusive), selected, "set range");
                List<Key> reversed = new ArrayList<>(selected);
                Collections.reverse(reversed);
                exactKeys(map.descendingMap().subMap(upper.text(), upperInclusive,
                        lower.text(), lowerInclusive).navigableKeySet(), reversed, "descending range");
            }
        }
    }

    private static void natural(Storage storage) throws ReflectiveOperationException {
        M3TreeMap<String, Integer> map = new M3TreeMap<>();
        M3TreeSet<String> set = new M3TreeSet<>();
        List<Key> all = corpus();
        for (Key key : all) storage.capture(key.text());
        List<Key> insertion = new ArrayList<>(all);
        Collections.shuffle(insertion, new Random(0x4d33545245454cL));
        List<Key> current = new ArrayList<>();
        for (Key key : insertion) {
            check(map.put(key.text(), key.value()) == null, "new mapping");
            check(set.add(key.text()), "new member");
            current.add(key);
            state(map, set, current);
        }
        check(map.comparator() == null && set.comparator() == null, "natural order metadata");
        prefixes(map, set, current);
        ranges(map, set, current);
        String livePrefix = composed("alp".toCharArray(), 3);
        NavigableMap<String, Integer> live = M3FilteredViews.filterEntries(map,
                (key, value) -> key.startsWith(livePrefix));
        int before = live.size();
        for (int i = 0; i < insertion.size(); i += 2) {
            Key key = insertion.get(i);
            String alias = composed(key.units(), 121);
            check(map.remove(alias).intValue() == key.value(), "remove equal composed mapping");
            check(set.remove(alias), "remove equal composed member");
            current.remove(key);
            state(map, set, current);
        }
        Key liveKey = new Key("alpaca".toCharArray(), composed("alpaca".toCharArray(), 7), 1000);
        int remaining = live.size();
        map.put(liveKey.text(), liveKey.value());
        set.add(liveKey.text());
        current.add(liveKey);
        check(live.size() == remaining + 1 && before >= remaining, "live prefix insertion");
        for (int i = 0; i < insertion.size(); i += 2) {
            Key old = insertion.get(i);
            Key replacement = new Key(old.units(), composed(old.units(), 171), old.value());
            map.put(replacement.text(), replacement.value());
            set.add(replacement.text());
            current.add(replacement);
            storage.capture(replacement.text());
        }
        state(map, set, current);
        prefixes(map, set, current);
        storage.verify();
    }

    private static void comparator(Storage storage) throws ReflectiveOperationException {
        Comparator<String> order = Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER);
        M3TreeMap<String, Integer> map = new M3TreeMap<>(order);
        M3TreeSet<String> set = new M3TreeSet<>(order);
        String alpha = composed("Alpha".toCharArray(), 1);
        String alias = composed("aLPHa".toCharArray(), 9);
        String bravo = composed("Bravo".toCharArray(), 3);
        String charlie = composed("charlie".toCharArray(), 5);
        for (String key : new String[] {alpha, alias, bravo, charlie}) storage.capture(key);
        map.put(null, -1);
        set.add(null);
        map.put(alpha, 1);
        map.put(bravo, 2);
        map.put(charlie, 3);
        set.add(alpha);
        set.add(bravo);
        set.add(charlie);
        check(map.put(alias, 7) == 1 && !set.add(alias), "comparator duplicate");
        check(map.ceilingKey(alias) == alpha && set.ceiling(alias) == alpha, "retained comparator key");
        check(map.get(alias) == 7 && map.get(null) == -1, "comparator values including null key");
        Object[] seen = {null};
        Set<String> generic = M3FilteredViews.filter((Set<String>) set, key -> {
            seen[0] = key;
            return true;
        });
        check(generic.contains(alias), "actual M3TreeSet comparator alias lost by generic filter");
        check(seen[0] == alpha, "generic filter retained representative");
        NavigableMap<String, Integer> range = map.subMap(null, true, charlie, false);
        NavigableMap<String, Integer> filtered = M3FilteredViews.filterEntries(range, (key, value) -> true);
        NavigableSet<String> filteredSet = M3FilteredViews.filter((NavigableSet<String>) set,
                key -> key == null || key == alpha || key == bravo);
        check(range.comparator() == order && filtered.comparator() == order
                && filteredSet.comparator() == order, "custom comparator identity");
        check(filtered.containsKey(null) && filtered.get(null) == -1, "nullable range mapping");
        check(filtered.firstEntry().getKey() == null && filtered.lastKey() == bravo, "range endpoints");
        check(filtered.descendingMap().firstKey() == bravo, "descending range endpoint");
        check(filtered.descendingMap().comparator().compare(alpha, bravo) > 0, "descending comparator");
        check(filteredSet.spliterator().getComparator() == order, "filtered sorted spliterator comparator");
        check(filtered.get(alias) == 7, "filtered comparator value");
        map.remove(alias);
        set.remove(alias);
        check(!generic.contains(alias) && !filtered.containsKey(alias), "live comparator removal");
        map.put(alias, 8);
        set.add(alias);
        check(generic.contains(alpha) && seen[0] == alias, "replacement representative is live");
        check(filtered.ceilingKey(alpha) == alias && filtered.get(alpha) == 8, "range replacement reference");
        map.verifyInvariants();
        set.verifyInvariants();
        storage.verify();
    }

    private static final class Storage {
        private final Field m3;
        private final Field owner;
        private final Field coordinate;
        private final Map<String, Backing> snapshots = new IdentityHashMap<>();
        private int tuples;

        Storage(boolean required) throws ReflectiveOperationException {
            if (!required) { m3 = null; owner = null; coordinate = null; return; }
            m3 = String.class.getDeclaredField("m3");
            Class<?> type = Class.forName("java.lang.M3String");
            owner = type.getDeclaredField("owner");
            coordinate = type.getDeclaredField("value");
            for (Field field : new Field[] {m3, owner, coordinate}) field.setAccessible(true);
        }

        void capture(String value) throws ReflectiveOperationException {
            if (m3 == null || value.isEmpty()) return;
            value.length();
            Object body = m3.get(value);
            check(body != null, "explicit M3 mode requires canonical String storage");
            Object backingOwner = owner.get(body);
            if (backingOwner.getClass().getName().equals("java.lang.M3StringTuple")) tuples++;
            snapshots.put(value, new Backing(body, backingOwner, coordinate.getLong(body)));
        }

        void verify() throws ReflectiveOperationException {
            if (m3 == null) return;
            check(tuples > 0, "composed tuple keys were actually exercised");
            for (Map.Entry<String, Backing> entry : snapshots.entrySet()) {
                Object body = m3.get(entry.getKey());
                Backing before = entry.getValue();
                check(body == before.body() && owner.get(body) == before.owner()
                        && coordinate.getLong(body) == before.coordinate(), "consumer preserves String owner/range");
            }
        }
    }

    public static int run(boolean requireM3) throws ReflectiveOperationException {
        checks = 0;
        Storage storage = new Storage(requireM3);
        natural(storage);
        comparator(storage);
        return checks;
    }

    public static void main(String[] arguments) throws ReflectiveOperationException {
        boolean requireM3 = arguments.length != 0 && arguments[0].equals("requireM3");
        System.out.println("PASS tree consumers checks=" + run(requireM3) + " composed=" + requireM3);
    }
}
