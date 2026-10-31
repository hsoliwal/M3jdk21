/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
import com.m3.collections.apache.trie.PatriciaTrie;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

/** Relocated Commons donor evidence only: this provider retains TrieEntry nodes. */
public final class M3StringPatriciaDonorProof {
    private static int checks;
    private record Key(char[] units, String text, int value) { }
    private M3StringPatriciaDonorProof() { }

    private static void check(boolean condition, String reason) {
        checks++;
        if (!condition) throw new AssertionError(reason);
    }

    private static String composed(char[] units, int salt) {
        String result = "";
        for (int cursor = 0; cursor < units.length;) {
            int end = Math.min(units.length, cursor + 1 + Math.floorMod(cursor + salt, 13));
            result = result.concat(new String(units, cursor, end - cursor));
            cursor = end;
        }
        return result;
    }

    private static int compare(char[] left, char[] right) {
        for (int i = 0; i < Math.min(left.length, right.length); i++) {
            if (left[i] != right[i]) return left[i] - right[i];
        }
        return left.length - right.length;
    }

    private static boolean startsWith(char[] key, char[] prefix) {
        if (key.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) if (key[i] != prefix[i]) return false;
        return true;
    }

    private static void contents(SortedMap<String, Integer> map, List<Key> expected, String context) {
        List<Key> sorted = new ArrayList<>(expected);
        sorted.sort((left, right) -> compare(left.units(), right.units()));
        check(map.size() == sorted.size(), context + " size");
        Iterator<Map.Entry<String, Integer>> iterator = map.entrySet().iterator();
        for (Key key : sorted) {
            check(iterator.hasNext(), context + " missing entry");
            Map.Entry<String, Integer> entry = iterator.next();
            check(entry.getKey() == key.text(), context + " provider representative");
            check(entry.getValue() == key.value(), context + " value");
            check(entry.getKey().length() == key.units().length, context + " UTF16 length");
            for (int i = 0; i < key.units().length; i++) {
                check(entry.getKey().charAt(i) == key.units()[i], context + " UTF16 unit");
            }
            check(map.get(composed(key.units(), 29)) == key.value(), context + " alias lookup");
        }
        check(!iterator.hasNext(), context + " extra entry");
    }

    private static void prefixes(PatriciaTrie<Integer> trie, List<Key> current, char[] shared) {
        List<char[]> queries = new ArrayList<>();
        for (String prefix : new String[] {"", "r", "ro", "rom", "roma", "roman", "romane",
                "rub", "rubi", "rubic", "rubico", "ruz", "s", "s\uD83D", "s\uD83D\uDE03",
                "s\uD800", "s\uDC00", "embedded\0"}) queries.add(prefix.toCharArray());
        // Prefixes end inside common paths, inside String pieces and between surrogate units.
        for (int cut : new int[] {1, 7, 12, 13, 31, 32, 33, 127, 128, 129, 200}) {
            queries.add(Arrays.copyOf(shared, cut));
        }
        for (char[] query : queries) {
            SortedMap<String, Integer> view = trie.prefixMap(composed(query, 3));
            List<Key> expected = current.stream().filter(key -> startsWith(key.units(), query)).toList();
            contents(view, expected, "prefix length " + query.length);
            check(view.comparator() == trie.comparator(), "prefix comparator identity");
        }
    }

    public static void main(String[] arguments) throws ReflectiveOperationException {
        boolean requireM3 = arguments.length != 0 && arguments[0].equals("requireM3");
        List<char[]> units = new ArrayList<>();
        for (String key : new String[] {"", "romane", "romanus", "romulus", "rubens", "ruber",
                "rubicon", "rubicundus", "s\uD83D\uDE03a", "s\uD83D\uDE03b", "s\uD800x",
                "s\uDC00x", "embedded\0alpha", "embedded\0beta"}) units.add(key.toCharArray());
        char[] shared = new char[257];
        for (int i = 0; i < shared.length; i++) shared[i] = (char) ('a' + i % 19);
        shared[31] = '\uD83D';
        shared[32] = '\uDE03';
        shared[127] = '\uD800';
        shared[128] = '\uDC00';
        units.add(Arrays.copyOf(shared, 200));
        units.add(shared);
        char[] sibling = shared.clone();
        sibling[220] = 'z';
        units.add(sibling);
        PatriciaTrie<Integer> trie = new PatriciaTrie<>();
        List<Key> current = new ArrayList<>();
        for (int i = units.size() - 1; i >= 0; i--) {
            Key key = new Key(units.get(i), composed(units.get(i), i + 1), i + 1);
            check(trie.put(key.text(), key.value()) == null, "new donor branch");
            current.add(key);
            contents(trie, current, "insertion");
        }
        String longKey = current.get(0).text();
        Field m3 = null;
        Field owner = null;
        Field coordinate = null;
        Object body = null;
        Object retainedOwner = null;
        long retainedCoordinate = 0;
        if (requireM3) {
            m3 = String.class.getDeclaredField("m3");
            Class<?> type = Class.forName("java.lang.M3String");
            owner = type.getDeclaredField("owner");
            coordinate = type.getDeclaredField("value");
            for (Field field : new Field[] {m3, owner, coordinate}) field.setAccessible(true);
            body = m3.get(longKey);
            check(body != null, "explicit donor M3 mode requires canonical storage");
            retainedOwner = owner.get(body);
            retainedCoordinate = coordinate.getLong(body);
            check(retainedOwner.getClass().getName().equals("java.lang.M3StringTuple"),
                    "donor actually consumes composed tuple key");
        }
        prefixes(trie, current, shared);
        SortedMap<String, Integer> live = trie.prefixMap(composed("rubi".toCharArray(), 9));
        check(live.size() == 2, "live prefix initial cardinality");
        Key added = new Key("rubidium".toCharArray(), composed("rubidium".toCharArray(), 11), 100);
        live.put(added.text(), added.value());
        current.add(added);
        check(live.size() == 3 && trie.containsKey(added.text()), "prefix insertion reaches owner");
        try {
            live.put(composed("outside".toCharArray(), 8), 0);
            throw new AssertionError("out-of-prefix insertion admitted");
        } catch (IllegalArgumentException expected) { checks++; }
        SortedMap<String, Integer> range = trie.subMap(composed("roma".toCharArray(), 4),
                composed("s".toCharArray(), 6));
        contents(range, current.stream().filter(key -> compare(key.units(), "roma".toCharArray()) >= 0
                && compare(key.units(), "s".toCharArray()) < 0).toList(), "range");
        check(range.comparator() == trie.comparator(), "range comparator identity");
        Key original = current.stream().filter(key -> Arrays.equals(key.units(), "romane".toCharArray()))
                .findFirst().orElseThrow();
        Key replacement = new Key(original.units(), composed(original.units(), 41), 200);
        check(trie.put(replacement.text(), replacement.value()) == original.value(), "donor replacement value");
        current.remove(original);
        current.add(replacement);
        // Commons explicitly setKeyValue on replacement; its retained key rule differs from TreeMap.
        contents(trie, current, "donor replacement representative");
        for (Key key : new ArrayList<>(current)) {
            if (key.value() % 2 != 0) {
                check(trie.remove(composed(key.units(), 17)) == key.value(), "delete donor branch");
                current.remove(key);
                contents(trie, current, "deletion");
            }
        }
        prefixes(trie, current, shared);
        live.clear();
        current.removeIf(key -> startsWith(key.units(), "rubi".toCharArray()));
        contents(trie, current, "live prefix clear");
        check(live.isEmpty(), "live prefix clears owner");
        if (requireM3) {
            check(m3.get(longKey) == body && owner.get(body) == retainedOwner
                    && coordinate.getLong(body) == retainedCoordinate, "donor preserves String owner/range");
        }
        System.out.println("PASS relocated Commons donor only checks=" + checks + " composed=" + requireM3);
    }
}
