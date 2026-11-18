// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and Synexia contributors
package jdk.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.Spliterator;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

/** Direct executable, Java 21 set-algebra differential and contract oracle. */
public final class SetOperatorViewsSelfTest {
    private static int checks;

    private SetOperatorViewsSelfTest() { }

    public static void main(String[] args) {
        boundariesAndEmptySets();
        liveViewsAndReadOnly();
        identityAndComparatorOptIn();
        maskWordEdges();
        overrideAndFailureModes();
        randomizedOracle();
        System.out.println("PASS M3SetOperatorViewsSelfTest checks=" + checks);
    }

    private static void boundariesAndEmptySets() {
        Set<Integer> empty = new LinkedHashSet<>();
        Set<Integer> left = new LinkedHashSet<>(Arrays.asList(null, -4, 2, 7));
        Set<Integer> right = new LinkedHashSet<>(Arrays.asList(null, 2, 9));
        checkOperator(left, right);
        checkOperator(empty, left);
        checkOperator(left, empty);
        checkOperator(empty, empty);
        Set<Integer> union = SetOperatorViews.unionByEquals(left, right);
        eq(List.of(-4, 2, 7, 9), nonNullOrder(union), "first-source order");
        eq(5, union.size(), "null occupies one equality slot");
        eq(0, SetOperatorViews.differenceByEquals(left, left).size(), "self-difference empty");
        eq(0, SetOperatorViews.symmetricDifferenceByEquals(left, left).size(),
                "self-xor empty");
        eq(left.size(), SetOperatorViews.intersectionByEquals(left, left).size(),
                "self-intersection");
    }

    private static void liveViewsAndReadOnly() {
        LinkedHashSet<String> left = new LinkedHashSet<>(List.of("alpha", "beta"));
        LinkedHashSet<String> right = new LinkedHashSet<>(List.of("beta", "gamma"));
        Set<String> union = SetOperatorViews.asEqualsOperator(left).or(right);
        Set<String> difference = SetOperatorViews.asEqualsOperator(left).minus(right);
        Iterator<String> firstSnapshot = union.iterator();
        Spliterator<String> late = union.spliterator();
        eq(List.of("alpha", "beta", "gamma"), List.copyOf(union), "ordered projection");
        eq(Set.of("alpha"), difference, "difference before changes");
        left.add("delta");
        right.add("epsilon");
        right.remove("beta");
        eq(List.of("alpha", "beta", "gamma"), consume(firstSnapshot),
                "bound iterator is a fixed snapshot");
        eq(Set.of("alpha", "beta", "delta"), difference,
                "later reads reflect mutation");
        eq(List.of("alpha", "beta", "delta", "gamma", "epsilon"), List.copyOf(union),
                "live backing with left then right ordering");
        List<String> traversed = new ArrayList<>();
        late.forEachRemaining(traversed::add);
        eq(List.copyOf(union), traversed, "spliterator snapshots on first touch");
        check((late.characteristics() & Spliterator.DISTINCT) != 0, "DISTINCT characteristic");
        check((late.characteristics() & Spliterator.ORDERED) != 0, "ORDERED characteristic");
        check((late.characteristics() & Spliterator.SIZED) == 0, "no stale SIZED characteristic");

        expect(UnsupportedOperationException.class, () -> union.add("z"), "add");
        expect(UnsupportedOperationException.class, () -> union.addAll(Set.of()), "addAll empty");
        expect(UnsupportedOperationException.class, () -> union.remove("absent"), "remove absent");
        expect(UnsupportedOperationException.class, union::clear, "clear");
        expect(UnsupportedOperationException.class, () -> union.removeAll(Set.of()), "removeAll empty");
        expect(UnsupportedOperationException.class, () -> union.retainAll(Set.of()), "retainAll");
        expect(UnsupportedOperationException.class, () -> union.removeIf(s -> false), "removeIf");
        Iterator<String> iterator = union.iterator();
        iterator.next();
        expect(UnsupportedOperationException.class, iterator::remove, "iterator remove");
        eq(new LinkedHashSet<>(List.of("alpha", "beta", "delta")), left,
                "sources not modified by view");
    }

    private static void identityAndComparatorOptIn() {
        // Distinct identities of equal strings are intentionally merged by Java equality.
        Set<String> identity = Collections.newSetFromMap(new IdentityHashMap<>());
        String a = new String("same");
        String b = new String("same");
        identity.add(a);
        identity.add(b);
        eq(2, identity.size(), "identity source holds both references");
        Set<String> byEquals = SetOperatorViews.unionByEquals(identity, Set.of());
        eq(Set.of("same"), byEquals, "explicit equals projection is not identity semantics");
        eq(1, byEquals.size(), "identity equal pair deduplicated");

        TreeSet<String> caseInsensitive = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        caseInsensitive.add("a");
        Set<String> normalized = SetOperatorViews.unionByEquals(caseInsensitive, Set.of("A"));
        eq(Set.of("a", "A"), normalized, "comparator not implicitly inherited");
        check(!(normalized instanceof java.util.SortedSet<?>), "no unproved comparator marker");
        eq(2, normalized.size(), "equals projection retains case-distinct values");
    }

    private static void maskWordEdges() {
        for (int size : new int[] {1, 2, 7, 8, 9, 63, 64, 65, 66, 127, 128, 129, 1025}) {
            LinkedHashSet<Integer> left = new LinkedHashSet<>();
            LinkedHashSet<Integer> right = new LinkedHashSet<>();
            for (int i = 0; i < size; i++) {
                if ((i & 1) == 0) left.add(i);
                if (i % 3 == 0) right.add(i);
            }
            checkOperator(left, right);
        }
    }

    private static void overrideAndFailureModes() {
        SetOperatorViews.SetOperator<String> override = new SetOperatorViews.SetOperator<>() {
            @Override public Set<String> source() { return Set.of("source"); }
            @Override public Set<String> or(Set<? extends String> right) {
                return Set.of("overridden");
            }
        };
        eq(Set.of("overridden"), override.or(Set.of("unused")), "default overridable");
        eq(Set.of("source"), override.and(Set.of("source")), "other defaults still work");
        expect(NullPointerException.class, () -> SetOperatorViews.asEqualsOperator(null),
                "null adapter");
        expect(NullPointerException.class, () -> SetOperatorViews.unionByEquals(null, Set.of()),
                "null left");
        expect(NullPointerException.class, () -> SetOperatorViews.unionByEquals(Set.of(), null),
                "null right");
        Iterator<Integer> exhausted = SetOperatorViews.unionByEquals(Set.<Integer>of(),
                Set.of()).iterator();
        expect(NoSuchElementException.class, exhausted::next, "exhausted iterator");
        check(!SetOperatorViews.unionByEquals(Set.<Integer>of(), Set.of()).spliterator()
                .tryAdvance(ignored -> { }), "empty spliterator");
    }

    private static void randomizedOracle() {
        Random random = new Random(20261118L);
        for (int trial = 0; trial < 350; trial++) {
            LinkedHashSet<Integer> left = new LinkedHashSet<>();
            LinkedHashSet<Integer> right = new LinkedHashSet<>();
            for (int i = 0, size = random.nextInt(80); i < size; i++) {
                left.add(random.nextInt(100) - 50);
            }
            for (int i = 0, size = random.nextInt(80); i < size; i++) {
                right.add(random.nextInt(100) - 50);
            }
            if ((trial & 1) == 0) left.add(null);
            if (trial % 3 == 0) right.add(null);
            checkOperator(left, right);
        }
    }

    private static <E> void checkOperator(Set<E> left, Set<E> right) {
        Set<E> union = new LinkedHashSet<>(left);
        union.addAll(right);
        Set<E> intersection = new LinkedHashSet<>(left);
        intersection.retainAll(right);
        Set<E> difference = new LinkedHashSet<>(left);
        difference.removeAll(right);
        Set<E> symmetric = new LinkedHashSet<>(union);
        Set<E> overlap = new HashSet<>(intersection);
        symmetric.removeAll(overlap);

        SetOperatorViews.SetOperator<E> adapter = SetOperatorViews.asEqualsOperator(left);
        assertView(union, adapter.or(right), "or");
        assertView(intersection, adapter.and(right), "and");
        assertView(difference, adapter.minus(right), "minus");
        assertView(symmetric, adapter.xor(right), "xor");
    }

    private static <E> void assertView(Set<E> expected, Set<E> actual, String operation) {
        eq(expected, actual, operation + " equality");
        eq(actual, expected, operation + " reverse equality");
        eq(expected.hashCode(), actual.hashCode(), operation + " hash");
        eq(expected.size(), actual.size(), operation + " size");
        eq(expected.isEmpty(), actual.isEmpty(), operation + " empty");
        eq(expected.containsAll(actual), actual.containsAll(expected),
                operation + " containsAll");
        eq(new LinkedHashSet<>(actual), expected, operation + " iterator distinct");
        for (E value : expected) check(actual.contains(value), operation + " membership");
        eq((long) expected.size(), actual.stream().count(), operation + " stream count");
    }

    private static List<Integer> nonNullOrder(Set<Integer> values) {
        List<Integer> result = new ArrayList<>();
        for (Integer value : values) if (value != null) result.add(value);
        return result;
    }

    private static <E> List<E> consume(Iterator<E> iterator) {
        List<E> values = new ArrayList<>();
        iterator.forEachRemaining(values::add);
        return values;
    }

    private static void expect(Class<? extends Throwable> type, Runnable operation,
                               String message) {
        try {
            operation.run();
            throw new AssertionError("Expected " + type.getSimpleName() + ": " + message);
        } catch (Throwable actual) {
            check(type.isInstance(actual), message + " wrong exception: " + actual);
        }
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static void eq(Object expected, Object actual, String message) {
        check(Objects.equals(expected, actual),
                message + " expected=" + expected + " actual=" + actual);
    }
}
