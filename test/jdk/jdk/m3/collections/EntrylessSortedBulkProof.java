// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableMap;
import java.util.Random;
import java.util.SortedMap;

/** Independent old-entry-path versus new direct-slot-path differential proof. */
public final class EntrylessSortedBulkProof {
    private static long checks;
    private EntrylessSortedBulkProof() { }
    private static void same(Object expected, Object actual, String label) {
        checks++;
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError(label + ": expected=" + expected + " actual=" + actual);
        }
    }
    private static void trial(int seed, int viewKind) {
        List<String> events = new ArrayList<>();
        Comparator<Integer> comparator = (a, b) -> {
            events.add("C" + a + ":" + b);
            return Integer.compare(a, b);
        };
        M3TreeMap<Integer, Integer> original = new M3TreeMap<>(comparator);
        Random random = new Random(seed * 0x9e3779b97f4a7c15L);
        for (int i = 0; i < seed % 130; i++) {
            original.put(random.nextInt(400) - 200,
                    random.nextInt(7) == 0 ? null : random.nextInt());
        }
        SortedMap<Integer, Integer> source = switch (viewKind) {
            case 0 -> original;
            case 1 -> original.headMap(50);
            case 2 -> original.tailMap(-50);
            default -> original.subMap(-80, 80);
        };
        // UnmodifiableSortedMap preserves source entry traversal but is not M3TreeMap,
        // so it exercises the exact original bulk path as the independent reference.
        SortedMap<Integer, Integer> oldAdapter = Collections.unmodifiableSortedMap(source);
        M3TreeMap<Integer, Integer> baseline = new M3TreeMap<>(source.comparator());
        M3TreeMap<Integer, Integer> candidate = new M3TreeMap<>(source.comparator());
        events.clear();
        baseline.putAll(oldAdapter);
        List<String> oldEvents = List.copyOf(events);
        events.clear();
        candidate.putAll(source);
        List<String> newEvents = List.copyOf(events);
        same(oldEvents, newEvents, "comparator callbacks " + seed + "/" + viewKind);
        same(new ArrayList<>(baseline.entrySet()), new ArrayList<>(candidate.entrySet()),
                "logical entries " + seed + "/" + viewKind);
        same(baseline.size(), candidate.size(), "size " + seed + "/" + viewKind);
        baseline.verifyInvariants();
        candidate.verifyInvariants();
        checks += 2;
        if (source.size() > 0 && viewKind == 0) {
            candidate.put(999999, seed);
            same(false, original.containsKey(999999), "source independence");
        }
    }
    public static void main(String[] args) {
        for (int seed = 0; seed < 350; seed++) {
            for (int view = 0; view < 4; view++) trial(seed, view);
        }
        System.out.println("PASS entryless M3TreeMap bulk oracle checks=" + checks);
    }
}
