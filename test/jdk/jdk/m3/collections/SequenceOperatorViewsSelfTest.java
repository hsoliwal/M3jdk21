// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and Synexia contributors
package jdk.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.Random;
import java.util.Spliterator;
import java.util.stream.StreamSupport;

/** Executable contract oracle for explicit Java 21 generic-list plus defaults. */
public final class SequenceOperatorViewsSelfTest {
    private static int checks;
    private SequenceOperatorViewsSelfTest() { }

    public static void main(String[] args) {
        orderedAndNullableLiveBacking();
        randomAccessAndNestedSegments();
        defaultOverrideAndFailurePaths();
        eagerModificationRefusalAndSourceFailFast();
        randomizedDifferentialLists();
        System.out.println("PASS M3SequenceOperatorViewsSelfTest checks=" + checks);
    }

    private static void orderedAndNullableLiveBacking() {
        LinkedList<String> a = new LinkedList<>(Arrays.asList("a", null, "a"));
        ArrayList<String> b = new ArrayList<>(Arrays.asList(null, "b"));
        List<String> composed = SequenceOperatorViews.asSequenceOperator(a).plus(b);
        equal(composed, Arrays.asList("a", null, "a", null, "b"));
        check(!(composed instanceof RandomAccess));
        a.addFirst("first");
        b.add("last");
        equal(composed, Arrays.asList("first", "a", null, "a", null, "b", "last"));
        check(composed.indexOf(null) == 2);
        check(composed.lastIndexOf(null) == 4);
        check(composed.get(0).equals("first"));
        throwsType(IndexOutOfBoundsException.class, () -> composed.get(-1));
        throwsType(IndexOutOfBoundsException.class, () -> composed.get(composed.size()));
        var stream = composed.stream();
        a.add("late");
        equal(stream.toList(), Arrays.asList("first", "a", null, "a", "late", null, "b", "last"));
    }

    private static void randomAccessAndNestedSegments() {
        List<Integer> composed = new ArrayList<>(List.of(0));
        for (int i = 1; i < 1_500; i++) {
            composed = SequenceOperatorViews.asSequenceOperator(composed).plus(List.of(i));
        }
        check(composed instanceof RandomAccess);
        check(composed.size() == 1_500);
        check(composed.get(0) == 0);
        check(composed.get(749) == 749);
        check(composed.get(1_499) == 1_499);
        check(composed.stream().mapToInt(Integer::intValue).sum() == 1_124_250);
        check(SequenceOperatorViews.concat(List.<Integer>of(), List.of(1)) instanceof RandomAccess);
        check(!(SequenceOperatorViews.concat(new LinkedList<Integer>(), List.of(1))
                instanceof RandomAccess));
    }

    private static void defaultOverrideAndFailurePaths() {
        List<Integer> source = List.of(1);
        List<Integer> replacement = List.of(3);
        SequenceOperatorViews.SequenceOperator<Integer> override =
                new SequenceOperatorViews.SequenceOperator<>() {
                    @Override public List<Integer> source() { return source; }
                    @Override public List<Integer> plus(List<? extends Integer> ignored) {
                        return replacement;
                    }
                };
        check(override.plus(List.of(9)) == replacement);
        throwsType(NullPointerException.class,
                () -> SequenceOperatorViews.asSequenceOperator(null));
        throwsType(NullPointerException.class,
                () -> SequenceOperatorViews.concat(null, source));
        throwsType(NullPointerException.class,
                () -> SequenceOperatorViews.concat(source, null));
        throwsType(NullPointerException.class,
                () -> SequenceOperatorViews.asSequenceOperator(source).plus(null));
        SequenceOperatorViews.SequenceOperator<Integer> invalid = () -> null;
        throwsType(NullPointerException.class, () -> invalid.plus(source));
        equal(SequenceOperatorViews.concat(List.of(), List.of()), List.of());
    }

    private static void eagerModificationRefusalAndSourceFailFast() {
        ArrayList<String> a = new ArrayList<>(List.of("one", "two"));
        ArrayList<String> b = new ArrayList<>(List.of("three"));
        List<String> combined = SequenceOperatorViews.concat(a, b);
        throwsType(UnsupportedOperationException.class, combined::clear);
        throwsType(UnsupportedOperationException.class, () -> combined.add("x"));
        throwsType(UnsupportedOperationException.class, () -> combined.set(0, "x"));
        throwsType(UnsupportedOperationException.class, () -> combined.remove("not found"));
        throwsType(UnsupportedOperationException.class, () -> combined.removeIf(x -> false));
        throwsType(UnsupportedOperationException.class, () -> combined.replaceAll(String::toUpperCase));
        throwsType(UnsupportedOperationException.class, () -> combined.sort(String::compareTo));
        Iterator<String> iterator = combined.iterator();
        check(iterator.next().equals("one"));
        a.add("modified");
        throwsType(ConcurrentModificationException.class, iterator::next);
        Spliterator<String> beforeTraversal = combined.spliterator();
        b.add("after acquisition");
        equal(StreamSupport.stream(beforeTraversal, false).toList(),
                List.of("one", "two", "modified", "three", "after acquisition"));
    }

    private static void randomizedDifferentialLists() {
        Random random = new Random(0x4D334C4953544F50L);
        for (int trial = 0; trial < 200; trial++) {
            ArrayList<Integer> left = new ArrayList<>();
            LinkedList<Integer> right = new LinkedList<>();
            for (int i = 0, n = random.nextInt(30); i < n; i++) {
                left.add(random.nextInt(7) == 0 ? null : random.nextInt(17) - 8);
            }
            for (int i = 0, n = random.nextInt(30); i < n; i++) {
                right.add(random.nextInt(7) == 0 ? null : random.nextInt(17) - 8);
            }
            List<Integer> expected = new ArrayList<>(left);
            expected.addAll(right);
            List<Integer> actual = SequenceOperatorViews.asSequenceOperator(left).plus(right);
            equal(actual, expected);
            check(actual.hashCode() == expected.hashCode());
            check(Arrays.equals(actual.toArray(), expected.toArray()));
            check(actual.indexOf(null) == expected.indexOf(null));
            check(actual.lastIndexOf(null) == expected.lastIndexOf(null));
            if (trial % 2 == 0) {
                right.add(null);
                expected.add(null);
                equal(actual, expected);
            }
            if (!actual.isEmpty()) {
                int index = random.nextInt(actual.size());
                equal(actual.get(index), expected.get(index));
            }
        }
    }

    private static void equal(Object actual, Object expected) {
        checks++;
        if (!Objects.equals(actual, expected)) {
            throw new AssertionError("expected=" + expected + " got=" + actual);
        }
    }

    private static void check(boolean condition) {
        checks++;
        if (!condition) throw new AssertionError("generic sequence contract");
    }

    private static void throwsType(Class<? extends Throwable> type, Runnable call) {
        checks++;
        try {
            call.run();
        } catch (Throwable failure) {
            if (type.isInstance(failure)) return;
            throw new AssertionError("expected=" + type.getName(), failure);
        }
        throw new AssertionError("expected exception " + type.getName());
    }
}
