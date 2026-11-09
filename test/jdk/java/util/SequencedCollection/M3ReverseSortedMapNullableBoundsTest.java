/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * GPLv2 with the Classpath exception.
 */

/*
 * @test
 * @summary M3 reversed sorted-map views preserve nullable endpoints and one-shot iterator removal
 * @build SimpleSortedMap
 * @run main/othervm M3ReverseSortedMapNullableBoundsTest
 */

import java.util.*;

public final class M3ReverseSortedMapNullableBoundsTest {
    private static long checks;

    private static void require(boolean trueValue, String context) {
        checks++;
        if (!trueValue)
            throw new AssertionError(context);
    }

    private static <T extends Throwable> void throwsType(
            Class<T> expected, Runnable code, String context) {
        checks++;
        try {
            code.run();
        } catch (Throwable thrown) {
            if (expected.isInstance(thrown))
                return;
            throw new AssertionError(context + ": wrong exception " + thrown, thrown);
        }
        throw new AssertionError(context + ": missing " + expected.getSimpleName());
    }

    private static SortedMap<String, Integer> map(boolean nullFirst) {
        Comparator<String> comparator = nullFirst
                ? Comparator.nullsFirst(Comparator.naturalOrder())
                : Comparator.nullsLast(Comparator.naturalOrder());
        SortedMap<String, Integer> base = new SimpleSortedMap<>(comparator);
        base.put(null, null);
        base.put("a", 1);
        base.put("b", 2);
        base.put("c", 3);
        return base;
    }

    private static void keys(SortedMap<String, Integer> view,
            String label, String... expected) {
        require(new ArrayList<>(view.keySet()).equals(Arrays.asList(expected)),
                label + ": " + new ArrayList<>(view.keySet()));
    }

    private static void nullAtReversedFront() {
        SortedMap<String, Integer> source = map(false);
        SortedMap<String, Integer> reversed = source.reversed();

        keys(reversed, "null-first reversed", null, "c", "b", "a");
        require(reversed.firstKey() == null, "nullable first key");
        require("a".equals(reversed.lastKey()), "non-null last key");
        require(reversed.reversed() == source, "reversing twice retains original owner");

        keys(reversed.headMap(null), "exclusive null head");
        keys(reversed.tailMap(null), "inclusive null tail", null, "c", "b", "a");
        keys(reversed.subMap(null, "b"), "nullable inclusive lower", null, "c");
        keys(reversed.subMap(null, "b").headMap("b"),
                "nested head equal exclusive upper", null, "c");
        keys(reversed.subMap(null, "b").tailMap(null),
                "nested tail equal inclusive lower", null, "c");
        keys(reversed.headMap("b"), "head without null sentinel", null, "c");
        keys(reversed.tailMap("b"), "tail without null sentinel", "b", "a");

        SortedMap<String, Integer> range = reversed.subMap(null, "b");
        Iterator<Map.Entry<String, Integer>> entries = range.entrySet().iterator();
        require(entries.hasNext(), "range first hasNext");
        require(entries.next().getKey() == null, "entry uses nullable stored key");
        require(entries.hasNext(), "hasNext may prefetch before remove");
        entries.remove();
        require(!source.containsKey(null), "range entry.remove deletes underlying null key");
        throwsType(IllegalStateException.class, entries::remove, "one-shot range remove");
        keys(range, "range remains backed", "c");
        source.put(null, 7);
        keys(range, "backed range sees reinserted null", null, "c");

        Iterator<String> keys = reversed.keySet().iterator();
        require(keys.next() == null, "root cursor yields nullable key");
        keys.remove();
        require(!source.containsKey(null), "root key.remove deletes null");
        throwsType(IllegalStateException.class, keys::remove, "one-shot root remove");

        source.put(null, null);
        Iterator<Integer> values = reversed.values().iterator();
        require(values.next() == null, "null key/null value is a stored mapping");
        values.remove();
        require(!source.containsKey(null), "values iterator removes null key");
        throwsType(IllegalStateException.class, values::remove, "one-shot values remove");
    }

    private static void nullAtReversedTail() {
        SortedMap<String, Integer> source = map(true);
        SortedMap<String, Integer> reversed = source.reversed();

        keys(reversed, "null-last reversed", "c", "b", "a", null);
        keys(reversed.headMap(null), "null-exclusive upper endpoint", "c", "b", "a");
        keys(reversed.tailMap(null), "null-inclusive lower endpoint", (String) null);
        keys(reversed.subMap("c", null), "nullable upper bound", "c", "b", "a");
        keys(reversed.subMap("c", null).headMap(null),
                "nested equal upper null endpoint", "c", "b", "a");

        SortedMap<String, Integer> tail = reversed.tailMap(null);
        Iterator<Map.Entry<String, Integer>> entries = tail.entrySet().iterator();
        require(entries.hasNext() && entries.next().getKey() == null,
                "tail iterator starts with stored null");
        entries.remove();
        throwsType(IllegalStateException.class, entries::remove, "null tail one-shot");
        require(!source.containsKey(null), "tail map removal is backed");

        throwsType(IllegalArgumentException.class,
                () -> reversed.subMap("a", "c"), "reversed comparator range ordering");
        source.put(null, 0);
        keys(tail, "tail tracks reinsertion", (String) null);
    }

    private static void ordinaryNonNullDifferential() {
        for (boolean nullFirst : new boolean[] {true, false}) {
            SortedMap<String, Integer> source = map(nullFirst);
            SortedMap<String, Integer> reversed = source.reversed();
            TreeMap<String, Integer> expected = new TreeMap<>(
                    Collections.reverseOrder(source.comparator()));
            expected.putAll(source);
            require(new ArrayList<>(reversed.keySet()).equals(new ArrayList<>(expected.keySet())),
                    "reversed order comparator parity");
            source.remove(null);
            expected.remove(null);
            require(new ArrayList<>(reversed.keySet()).equals(new ArrayList<>(expected.keySet())),
                    "backed map mutation parity");
            source.put("bb", 22);
            expected.put("bb", 22);
            require(new ArrayList<>(reversed.keySet()).equals(new ArrayList<>(expected.keySet())),
                    "live map insertion order parity");
            require(reversed.equals(expected) && expected.equals(reversed),
                    "map equality preserved");
            require(reversed.hashCode() == expected.hashCode(), "map hashCode preserved");
        }
    }

    public static void main(String[] args) {
        nullAtReversedFront();
        nullAtReversedTail();
        ordinaryNonNullDifferential();
        System.out.println("M3ReverseSortedMapNullableBoundsTest PASS checks=" + checks);
    }
}
