/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 * Adapted from the existing Synexia one-pass/COW regression; expanded owner,
 * exception, identity and differential coverage in 2026.
 * @test
 * @summary Filtered bulk admission captures each input occurrence before owner mutation
 * @run main/othervm FilteredBatchAdmissionTest
 * @run main/othervm -Xint FilteredBatchAdmissionTest
 */
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.CollectionViews;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Predicate;

public final class FilteredBatchAdmissionTest {
    private static int checks;
    private FilteredBatchAdmissionTest() { }

    private static void check(boolean result, String message) {
        checks++;
        if (!result) throw new AssertionError(message);
    }

    private static void equal(Object expected, Object actual, String message) {
        check(Objects.equals(expected, actual), message + ": " + expected + " != " + actual);
    }

    private static final class Once<E> extends AbstractCollection<E> {
        final List<E> values;
        int opens;
        Once(List<E> values) { this.values = values; }
        @Override public Iterator<E> iterator() {
            if (++opens != 1) throw new AssertionError("source traversed twice");
            return values.iterator();
        }
        @Override public int size() { throw new AssertionError("eager source size"); }
    }

    private static final class Owner<E> extends AbstractCollection<E> {
        final List<E> values = new ArrayList<>();
        final List<String> trace;
        RuntimeException failure;
        int bulkCalls;
        boolean failAfterFirst;
        Owner(List<String> trace) { this.trace = trace; }
        @Override public int size() { return values.size(); }
        @Override public Iterator<E> iterator() { return values.iterator(); }
        @Override public boolean add(E value) { throw new AssertionError("bypassed bulk policy"); }
        @Override public boolean addAll(Collection<? extends E> input) {
            bulkCalls++;
            trace.add("owner");
            if (failure != null && !failAfterFirst) throw failure;
            boolean changed = false;
            for (E value : input) {
                values.add(value);
                changed = true;
                if (failure != null) throw failure;
            }
            return changed;
        }
    }

    private static void onePass() {
        List<Integer> owner = new ArrayList<>(Arrays.asList(null, 0, 1, 2));
        Collection<Integer> view = CollectionViews.filtered((Collection<Integer>) owner,
                value -> value == null || value % 2 == 0);
        Once<Integer> accepted = new Once<>(Arrays.asList(4, null, 6));
        check(view.addAll(accepted), "accepted batch changed owner");
        equal(1, accepted.opens, "one iterator");
        equal(Arrays.asList(null, 0, 1, 2, 4, null, 6), owner, "order and nulls");
        Once<Integer> empty = new Once<>(List.of());
        check(!view.addAll(empty), "empty result");
        equal(1, empty.opens, "empty input opened once");

        Collection<Integer> changing = new AbstractCollection<>() {
            int traversals;
            @Override public int size() { return 1; }
            @Override public Iterator<Integer> iterator() {
                return List.of(++traversals == 1 ? 8 : 9).iterator();
            }
        };
        view.addAll(changing);
        equal(Arrays.asList(null, 0, 1, 2, 4, null, 6, 8), owner,
                "owner receives the validated occurrence, not a second traversal");
    }

    private static void failuresAndPolicy() {
        List<String> trace = new ArrayList<>();
        Owner<Integer> owner = new Owner<>(trace);
        Collection<Integer> view = CollectionViews.filtered((Collection<Integer>) owner,
                value -> { trace.add("predicate:" + value); return value % 2 == 0; });
        try {
            view.addAll(new Once<>(List.of(2, 3, 4)));
            throw new AssertionError("rejection missing");
        } catch (IllegalArgumentException expected) {
            equal("element rejected by view predicate", expected.getMessage(), "rejection message");
        }
        equal(List.of("predicate:2", "predicate:3"), trace, "short circuit before owner");
        equal(0, owner.bulkCalls, "no owner call for rejected batch");
        check(owner.values.isEmpty(), "no partial admission");
        trace.clear();
        view.addAll(new Once<>(List.of(4, 6)));
        equal(List.of("predicate:4", "predicate:6", "owner"), trace, "phase order");
        equal(1, owner.bulkCalls, "one owner bulk call");
        equal(List.of(4, 6), owner.values, "approved owner values");
        trace.clear();
        check(!view.addAll(new Once<>(List.of())), "empty result forwarded");
        equal(List.of("owner"), trace, "empty batch preserves owner dispatch");

        RuntimeException sentinel = new IllegalStateException("owner policy");
        owner.failure = sentinel;
        trace.clear();
        try { view.addAll(List.of(8, 10)); throw new AssertionError("owner policy bypass"); }
        catch (RuntimeException actual) { check(actual == sentinel, "owner exception identity"); }
        equal(List.of("predicate:8", "predicate:10", "owner"), trace, "all admission precedes refusal");
        equal(List.of(4, 6), owner.values, "owner refusal unchanged");
        owner.failAfterFirst = true;
        try { view.addAll(List.of(12, 14)); throw new AssertionError("partial owner failure missing"); }
        catch (RuntimeException actual) { check(actual == sentinel, "partial exception identity"); }
        equal(List.of(4, 6, 12), owner.values, "owner partial mutation is not rolled back");

        int previous = owner.bulkCalls;
        Collection<Integer> throwing = CollectionViews.filtered((Collection<Integer>) owner, value -> {
            if (value == 18) throw sentinel;
            return true;
        });
        try { throwing.addAll(List.of(16, 18, 20)); throw new AssertionError("predicate failure missing"); }
        catch (RuntimeException actual) { check(actual == sentinel, "predicate exception identity"); }
        equal(previous, owner.bulkCalls, "predicate failure has no owner call");
        Collection<Integer> broken = new AbstractCollection<>() {
            @Override public int size() { throw new AssertionError("size not needed"); }
            @Override public Iterator<Integer> iterator() {
                return new Iterator<>() {
                    int position;
                    @Override public boolean hasNext() { return position < 2; }
                    @Override public Integer next() {
                        if (position++ == 0) return 22;
                        throw sentinel;
                    }
                };
            }
        };
        try { view.addAll(broken); throw new AssertionError("source failure missing"); }
        catch (RuntimeException actual) { check(actual == sentinel, "source exception identity"); }
        equal(previous, owner.bulkCalls, "source failure has no owner call");
        try { view.addAll(null); throw new AssertionError("null missing"); }
        catch (NullPointerException expected) { equal(previous, owner.bulkCalls, "null precedes owner"); }
    }

    private record Value(int key, String tag) { }

    private static void identityAndRanges() {
        Value first = new Value(2, "same"), alias = new Value(2, "same");
        List<Value> owner = new ArrayList<>();
        CollectionViews.filtered((Collection<Value>) owner, value -> true)
                .addAll(new Once<>(Arrays.asList(first, alias, first, null)));
        check(owner.get(0) == first && owner.get(1) == alias && owner.get(2) == first
                && owner.get(3) == null, "reference identity and duplicates");
        Set<Value> identities = Collections.newSetFromMap(new IdentityHashMap<>());
        CollectionViews.filtered(identities, value -> true)
                .addAll(new Once<>(Arrays.asList(first, alias, first, null)));
        equal(3, identities.size(), "identity-set cardinality");
        check(identities.contains(first) && identities.contains(alias), "identity members");
        NavigableSet<Value> sorted = new TreeSet<>(Comparator.comparingInt(Value::key));
        CollectionViews.filtered(sorted, value -> true).addAll(new Once<>(List.of(first, alias)));
        check(sorted.first() == first && sorted.size() == 1, "comparator representative");

        NavigableSet<Integer> numbers = new TreeSet<>(List.of(0, 4, 8));
        NavigableSet<Integer> range = CollectionViews.filtered(numbers.subSet(0, true, 8, false), n -> n % 2 == 0);
        range.addAll(new Once<>(List.of(6, 2)));
        equal(List.of(0, 2, 4, 6, 8), new ArrayList<>(numbers), "range remains backed");
        try { range.addAll(List.of(1, 10)); throw new AssertionError("predicate first"); }
        catch (IllegalArgumentException expected) {
            equal("element rejected by view predicate", expected.getMessage(), "predicate before range rejection");
        }
        List<Integer> self = new ArrayList<>(List.of(1, 2, 3, 4));
        Collection<Integer> evens = CollectionViews.filtered((Collection<Integer>) self, n -> n % 2 == 0);
        evens.addAll(evens);
        equal(List.of(1, 2, 3, 4, 2, 4), self, "self-view captured before owner mutation");
        List<String> trace = new ArrayList<>();
        Collection<Integer> nested = CollectionViews.filtered(
                CollectionViews.filtered((Collection<Integer>) new ArrayList<Integer>(), n -> {
                    trace.add("inner:" + n); return n % 2 == 0;
                }), n -> { trace.add("outer:" + n); return n < 10; });
        nested.addAll(new Once<>(List.of(2, 4)));
        equal(List.of("inner:2", "outer:2", "inner:4", "outer:4"), trace, "nested callback order");
    }

    private static Collection<Integer> owner(int kind, List<Integer> seed) {
        return switch (kind) {
            case 0 -> new ArrayList<>(seed);
            case 1 -> new LinkedList<>(seed);
            case 2 -> new HashSet<>(seed);
            case 3 -> new TreeSet<>(seed);
            case 4 -> new CopyOnWriteArrayList<>(seed);
            default -> new CopyOnWriteArraySet<>(seed);
        };
    }

    private static void ordinary() {
        Random random = new Random(0x4d334241544348L);
        for (int trial = 0; trial < 2400; trial++) {
            int kind = trial % 6;
            List<Integer> seed = new ArrayList<>(), batch = new ArrayList<>();
            for (int n = random.nextInt(17); n > 0; n--) seed.add(random.nextInt(21) - 10);
            boolean accepted = true;
            for (int n = random.nextInt(25); n > 0; n--) {
                int value = random.nextInt(21) - 10;
                if (trial % 3 != 0) value *= 2;
                batch.add(value);
                accepted &= value % 2 == 0;
            }
            Collection<Integer> actual = owner(kind, seed), expected = owner(kind, seed);
            List<Integer> observed = new ArrayList<>();
            Predicate<Integer> predicate = n -> { observed.add(n); return n % 2 == 0; };
            Collection<Integer> view = CollectionViews.filtered(actual, predicate);
            if (accepted) equal(expected.addAll(batch), view.addAll(batch), "bulk return");
            else {
                try { view.addAll(batch); throw new AssertionError("bad batch accepted"); }
                catch (IllegalArgumentException correct) { checks++; }
            }
            equal(new ArrayList<>(expected), new ArrayList<>(actual), "owner contents");
            int visited = 0;
            while (visited < batch.size()) { if (batch.get(visited++) % 2 != 0) break; }
            equal(batch.subList(0, visited), observed, "predicate order and short circuit");
        }
    }

    public static void main(String[] args) {
        String mode = args.length == 0 ? "all" : args[0];
        if (!mode.equals("ordinary")) {
            onePass();
            failuresAndPolicy();
            identityAndRanges();
        }
        ordinary();
        System.out.println("BATCH_ADMISSION_PASS mode=" + mode + " checks=" + checks);
    }
}
