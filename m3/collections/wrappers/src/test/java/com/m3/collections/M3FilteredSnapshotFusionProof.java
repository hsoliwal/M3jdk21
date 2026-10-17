// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

/** Java 21 public-API oracle for additive fused M3 collection snapshots. */
public final class M3FilteredSnapshotFusionProof {
    private static int checks;
    private M3FilteredSnapshotFusionProof() { }

    private static void check(boolean good, String description) {
        if (!good) throw new AssertionError(description);
        checks++;
    }

    private static void rejectMutation(List<?> values) {
        try {
            values.clear();
            throw new AssertionError("read-only snapshot allowed clear");
        } catch (UnsupportedOperationException expected) {
            checks++;
        }
        try {
            values.reversed().clear();
            throw new AssertionError("reversed snapshot allowed mutation");
        } catch (UnsupportedOperationException expected) {
            checks++;
        }
    }

    private static void rejectNull(Runnable action) {
        try {
            action.run();
            throw new AssertionError("null source/predicate accepted");
        } catch (NullPointerException expected) {
            checks++;
        }
    }

    private static Collection<Integer> owner(int kind, List<Integer> values) {
        return switch (kind) {
            case 0 -> new ArrayList<>(values);
            case 1 -> new LinkedList<>(values);
            case 2 -> new LinkedHashSet<>(values);
            case 3 -> new HashSet<>(values);
            default -> {
                TreeSet<Integer> sorted = new TreeSet<>(
                        Comparator.nullsFirst(Comparator.naturalOrder()));
                sorted.addAll(values);
                yield sorted;
            }
        };
    }

    private static void randomDifferentials() {
        Comparator<Integer> compare = Comparator.nullsLast(Comparator.naturalOrder());
        for (int kind = 0; kind < 5; kind++) {
            for (int seed = 0; seed < 128; seed++) {
                Random random = new Random(seed * 991L + kind * 31L);
                List<Integer> input = new ArrayList<>();
                for (int n = 0; n < seed % 51; n++) {
                    input.add(n % 9 == 0 ? null : random.nextInt(31) - 15);
                }
                Collection<Integer> backing = owner(kind, input);
                final int row = seed;
                Predicate<Integer> predicate =
                        element -> element == null || ((element + row) & 3) != 0;
                ArrayList<Integer> expected = new ArrayList<>();
                for (Integer value : backing) {
                    if (predicate.test(value)) expected.add(value);
                }
                ArrayList<Integer> sortedExpected = new ArrayList<>(expected);
                sortedExpected.sort(compare);

                List<Integer> plain = M3CollectionViews.filteredSnapshot(backing, predicate);
                List<Integer> sorted = M3CollectionViews.filteredSortedSnapshot(
                        backing, predicate, compare);
                check(expected.equals(plain), "filtered encounter order " + kind + "/" + seed);
                check(sortedExpected.equals(sorted), "stable sorting " + kind + "/" + seed);
                check(plain.getClass() != ArrayList.class, "mutable list escaped");
                check(sorted.getClass() != ArrayList.class, "mutable sorted list escaped");
                check(plain.reversed().equals(new ArrayList<>(plain.reversed())),
                        "reversed iteration changed");
                rejectMutation(plain);
                rejectMutation(sorted);
                backing.clear();
                check(expected.equals(plain), "snapshot changed with backing mutation");
                check(sortedExpected.equals(sorted), "sorted snapshot changed with source");
            }
        }
    }

    private static void metadataFreeSingleTraversal() {
        AtomicInteger sizes = new AtomicInteger();
        AtomicInteger iterations = new AtomicInteger();
        AtomicInteger predicateCalls = new AtomicInteger();
        Collection<Integer> owner = new AbstractCollection<>() {
            @Override public int size() {
                sizes.incrementAndGet();
                return 5;
            }
            @Override public Iterator<Integer> iterator() {
                iterations.incrementAndGet();
                return Arrays.asList(3, null, 1, 3, 2).iterator();
            }
        };
        List<Integer> snapshot = M3CollectionViews.filteredSortedSnapshot(
                owner,
                value -> {
                    predicateCalls.incrementAndGet();
                    return value == null || value >= 2;
                },
                Comparator.nullsFirst(Comparator.naturalOrder()));
        check(snapshot.equals(Arrays.asList(null, 2, 3, 3)), "single-pass nullable result");
        check(sizes.get() == 0, "filter/sort called source.size()");
        check(iterations.get() == 1, "filter/sort created more than one source iterator");
        check(predicateCalls.get() == 5, "predicate not evaluated once per occurrence");

        AtomicInteger plainCalls = new AtomicInteger();
        List<Integer> plain = M3CollectionViews.filteredSnapshot(owner, value -> {
            plainCalls.incrementAndGet();
            return value != null;
        });
        check(plain.equals(List.of(3, 1, 3, 2)), "single-pass plain values");
        check(sizes.get() == 0 && iterations.get() == 2,
                "plain snapshot performed a size prepass");
        check(plainCalls.get() == 5, "plain snapshot called predicate more than once");
    }

    private static final class Row {
        final int rank;
        final String name;
        Row(int rank, String name) {
            this.rank = rank;
            this.name = name;
        }
    }

    private static void stableTiesAndIdentity() {
        Row a = new Row(1, "a");
        Row b = new Row(0, "b");
        Row c = new Row(1, "c");
        Row d = new Row(0, "d");
        List<Row> input = Arrays.asList(a, b, c, a, d);
        List<Row> sorted = M3CollectionViews.filteredSortedSnapshot(
                input, element -> true, Comparator.comparingInt(item -> item.rank));
        check(sorted.size() == 5, "comparator ties were deduplicated");
        check(sorted.get(0) == b && sorted.get(1) == d, "low-rank ties not stable");
        check(sorted.get(2) == a && sorted.get(3) == c && sorted.get(4) == a,
                "high-rank ties or duplicate reference lost");
        input.set(0, new Row(9, "other"));
        check(sorted.get(2) == a, "snapshot should not replace element reference");
        check("a".equals(sorted.get(2).name), "element reference changed");
    }

    private static void nullNaturalOrderAndExceptions() {
        rejectNull(() -> M3CollectionViews.filteredSnapshot(null, null));
        rejectNull(() -> M3CollectionViews.filteredSnapshot(List.of(1), null));
        rejectNull(() -> M3CollectionViews.filteredSortedSnapshot(null, null, null));
        rejectNull(() -> M3CollectionViews.filteredSortedSnapshot(List.of(1), null, null));
        check(M3CollectionViews.filteredSortedSnapshot(
                Arrays.asList(3, 1, 2), x -> true, null).equals(List.of(1, 2, 3)),
                "null comparator did not mean natural order");
        List<String> strings = new ArrayList<>(List.of("b", "a", "c"));
        try {
            M3CollectionViews.filteredSortedSnapshot(strings, x -> true,
                    (left, right) -> { throw new IllegalStateException("bad order"); });
            throw new AssertionError("comparator exception swallowed");
        } catch (IllegalStateException expected) {
            check(Objects.equals(expected.getMessage(), "bad order"),
                    "comparator failure wrapped");
        }
        check(strings.equals(List.of("b", "a", "c")), "sort mutated input collection");
        try {
            M3CollectionViews.filteredSnapshot(strings, value -> {
                if (value.equals("a")) throw new IllegalArgumentException("bad predicate");
                return true;
            });
            throw new AssertionError("predicate exception swallowed");
        } catch (IllegalArgumentException expected) {
            check("bad predicate".equals(expected.getMessage()), "predicate failure wrapped");
        }
        check(strings.equals(List.of("b", "a", "c")), "failed filtering changed source");
        check(M3CollectionViews.filteredSnapshot(List.<String>of(), x -> true).isEmpty(),
                "empty snapshot had data");
    }

    private static void retainedLegacyJdkCollectionApi() {
        Collection<Integer> backing = new LinkedList<>(Arrays.asList(1, 2, 2, 3, null, 4));
        Collection<Integer> view = M3CollectionViews.filtered(
                backing, value -> value != null && (value & 1) == 0);
        Integer[] exact = view.toArray(new Integer[0]);
        check(Arrays.equals(new Integer[] {2, 2, 4}, exact),
                "legacy typed Collection.toArray changed");
        Integer[] oversized = {99, 99, 99, 99, 99};
        check(view.toArray(oversized) == oversized,
                "oversized legacy array identity changed");
        check(Arrays.equals(new Integer[] {2, 2, 4, null, 99}, oversized),
                "typed array termination changed");
        check(new ArrayList<>(view).equals(List.of(2, 2, 4)),
                "original backed filter changed");
        backing.add(6);
        check(new ArrayList<>(view).equals(List.of(2, 2, 4, 6)),
                "original view is no longer live");
    }

    public static void main(String[] arguments) {
        check(arguments.length == 0, "unexpected proof arguments");
        randomDifferentials();
        metadataFreeSingleTraversal();
        stableTiesAndIdentity();
        nullNaturalOrderAndExceptions();
        retainedLegacyJdkCollectionApi();
        System.out.println("M3_FILTERED_SNAPSHOT_FUSION_PASS checks=" + checks);
    }
}
