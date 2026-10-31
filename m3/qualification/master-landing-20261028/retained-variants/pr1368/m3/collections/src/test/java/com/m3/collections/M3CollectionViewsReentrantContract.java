/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Spliterator;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Independent operation-result, callback-identity and lifetime regressions for
 * nested synchronous traversal of the real M3CollectionViews owner.
 * No backing collection is replaced by a test implementation.
 */
public final class M3CollectionViewsReentrantContract {
    private static long checks;

    private M3CollectionViewsReentrantContract() {
    }

    private static final class Holder<T> {
        T value;
    }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }

    private static void equal(Object expected, Object actual, String label) {
        checks++;
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(label + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void sameFailure(Throwable expected, Runnable action, String label) {
        checks++;
        try {
            action.run();
        } catch (Throwable actual) {
            if (actual == expected) return;
            throw new AssertionError(label + " changed failure identity", actual);
        }
        throw new AssertionError(label + " did not throw");
    }

    private static void nullFailure(Runnable action, String label) {
        checks++;
        try {
            action.run();
        } catch (NullPointerException expected) {
            equal("action", expected.getMessage(), label + " argument diagnostic");
            return;
        }
        throw new AssertionError(label + " accepted null");
    }

    private static Collection<Integer> source(int kind, List<Integer> values) {
        if (kind == 0) return new ArrayList<>(values);
        M3ArrayList<Integer> valuesOwner = new M3ArrayList<>();
        valuesOwner.addAll(values);
        return valuesOwner;
    }

    private static Spliterator<Integer> selected(
            Collection<Integer> source, Predicate<? super Integer> predicate) {
        return M3CollectionViews.filtered(source, predicate).spliterator();
    }

    /** Deterministic retained-callback inspection; does not force or assume GC. */
    private static Object retainedCallback(Spliterator<?> cursor) {
        try {
            Field field = cursor.getClass().getDeclaredField("action");
            field.setAccessible(true);
            return field.get(cursor);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("inspect existing operation callback", failure);
        }
    }

    private static void released(Spliterator<?> cursor, String label) {
        check(retainedCallback(cursor) == null, label + " retained operation callback");
    }

    static void consumerExhaustion() {
        for (int kind = 0; kind < 2; kind++) {
            Spliterator<Integer> cursor = selected(source(kind, List.of(1)), value -> true);
            List<Integer> seen = new ArrayList<>();
            boolean outer = cursor.tryAdvance(value -> {
                seen.add(value);
                check(!cursor.tryAdvance(ignored -> {
                    throw new AssertionError("exhausted nested callback");
                }), "nested exhaustion");
            });
            equal(List.of(1), seen, "one outer callback");
            check(outer, "outer delivery must remain true after nested exhaustion");
            check(!cursor.tryAdvance(seen::add), "remains exhausted");
            released(cursor, "consumer exhaustion");
        }
    }

    static void nestedConsumers() {
        for (int kind = 0; kind < 2; kind++) {
            for (int length : new int[] {0, 1, 2, 3, 16, 63, 128}) {
                List<Integer> values = new ArrayList<>();
                List<Integer> expected = new ArrayList<>();
                for (int i = 0; i < length; i++) {
                    values.add(i);
                    if ((i & 1) == 0) expected.add(i);
                }
                Spliterator<Integer> cursor = selected(source(kind, values),
                        value -> (value & 1) == 0);
                List<Integer> seen = new ArrayList<>();
                List<Boolean> nested = new ArrayList<>();
                final class Recurse implements Consumer<Integer> {
                    @Override public void accept(Integer value) {
                        seen.add(value);
                        nested.add(cursor.tryAdvance(this));
                    }
                }
                boolean outer = cursor.tryAdvance(new Recurse());
                equal(expected, seen, "nested consumer encounter order");
                equal(!expected.isEmpty(), outer, "outer presence result");
                equal(expected.size(), nested.size(), "one nested operation per callback");
                for (int i = 0; i < nested.size(); i++) {
                    equal(i != 0, nested.get(i), "unwinding invocation delivery " + i);
                }
                check(!cursor.tryAdvance(seen::add), "recursive drain exhausted");
                released(cursor, "recursive consumers");
            }
        }
    }

    static void nestedPredicates() {
        for (int kind = 0; kind < 2; kind++) {
            for (int length = 1; length <= 3; length++) {
                for (boolean admitFirst : new boolean[] {false, true}) {
                    List<Integer> values = new ArrayList<>();
                    for (int i = 1; i <= length; i++) values.add(i);
                    Holder<Spliterator<Integer>> holder = new Holder<>();
                    List<Integer> inner = new ArrayList<>();
                    List<Integer> outer = new ArrayList<>();
                    List<Integer> visited = new ArrayList<>();
                    final int count = length;
                    holder.value = selected(source(kind, values), value -> {
                        visited.add(value);
                        if (value == 1) {
                            equal(count > 1, holder.value.tryAdvance(inner::add),
                                    "predicate nested presence");
                        }
                        return value != 1 || admitFirst;
                    });
                    boolean present = holder.value.tryAdvance(outer::add);
                    List<Integer> expectedFirst = admitFirst ? List.of(1)
                            : length == 3 ? List.of(3) : List.of();
                    equal(expectedFirst, outer, "outer predicate delivery belongs to its invocation");
                    equal(!expectedFirst.isEmpty(), present, "predicate nested flag isolation");
                    equal(length > 1 ? List.of(2) : List.of(), inner, "nested predicate consumer");
                    holder.value.forEachRemaining(outer::add);
                    List<Integer> expectedOuter = new ArrayList<>();
                    if (admitFirst) expectedOuter.add(1);
                    if (length == 3) expectedOuter.add(3);
                    equal(expectedOuter, outer, "predicate continuation");
                    equal(values, visited, "one decision per consumed source occurrence");
                    released(holder.value, "nested predicates");
                }
            }
        }
    }

    static void callbackFailures() {
        for (int kind = 0; kind < 2; kind++) {
            for (boolean predicateFailure : new boolean[] {false, true}) {
                RuntimeException token = new IllegalStateException("nested callback");
                Holder<Spliterator<Integer>> holder = new Holder<>();
                List<Integer> seen = new ArrayList<>();
                holder.value = selected(source(kind, List.of(1, 2, 3)), value -> {
                    if (predicateFailure && value == 2) throw token;
                    return true;
                });
                check(holder.value.tryAdvance(value -> {
                    seen.add(value);
                    sameFailure(token, () -> holder.value.tryAdvance(inner -> {
                        if (!predicateFailure) throw token;
                        throw new AssertionError("throwing predicate admitted consumer");
                    }), "caught nested failure");
                    check(holder.value.tryAdvance(seen::add), "continue inside outer callback");
                }), "caught failure preserves outer presence");
                equal(List.of(1, 3), seen, "failed source occurrence consumed once");
                released(holder.value, "caught nested callback failure");
            }

            RuntimeException token = new IllegalArgumentException("uncaught nested callback");
            Spliterator<Integer> cursor = selected(source(kind, List.of(1, 2, 3)), value -> true);
            sameFailure(token, () -> cursor.tryAdvance(value ->
                    cursor.tryAdvance(inner -> { throw token; })), "uncaught nested failure");
            released(cursor, "uncaught nested failure");
            List<Integer> tail = new ArrayList<>();
            check(cursor.tryAdvance(tail::add), "resume after propagated failure");
            equal(List.of(3), tail, "no replay after failure");

            for (boolean predicateFailure : new boolean[] {false, true}) {
                Error failure = new AssertionError("operation error");
                Spliterator<Integer> failing = selected(source(kind, List.of(1, 2)), value -> {
                    if (predicateFailure && value == 1) throw failure;
                    return true;
                });
                sameFailure(failure, () -> failing.tryAdvance(value -> { throw failure; }),
                        "outer callback error");
                released(failing, "outer callback error");
                tail.clear();
                check(failing.tryAdvance(tail::add), "fresh consumer after error");
                equal(List.of(2), tail, "error continuation");
            }

            Holder<Spliterator<Integer>> inPredicate = new Holder<>();
            RuntimeException predicateToken = new IllegalStateException("inner predicate");
            List<Integer> output = new ArrayList<>();
            inPredicate.value = selected(source(kind, List.of(1, 2, 3)), value -> {
                if (value == 2) throw predicateToken;
                if (value == 1) sameFailure(predicateToken,
                        () -> inPredicate.value.tryAdvance(output::add),
                        "predicate catches nested predicate failure");
                return true;
            });
            check(inPredicate.value.tryAdvance(output::add), "outer predicate still delivers");
            inPredicate.value.forEachRemaining(output::add);
            equal(List.of(1, 3), output, "predicate failure does not replace outer action");
            released(inPredicate.value, "predicate catches nested failure");
        }
    }

    static void nullAdmission() {
        for (int kind = 0; kind < 2; kind++) {
            for (boolean empty : new boolean[] {false, true}) {
                int[] decisions = {0};
                Spliterator<Integer> cursor = selected(
                        source(kind, empty ? List.of() : List.of(1, 2)), value -> {
                            decisions[0]++;
                            return true;
                        });
                nullFailure(() -> cursor.tryAdvance(null), "advance null");
                nullFailure(() -> cursor.forEachRemaining(null), "bulk null");
                equal(0, decisions[0], "null admission before predicates");
                released(cursor, "null admission");
                List<Integer> output = new ArrayList<>();
                equal(!empty, cursor.tryAdvance(value -> {
                    output.add(value);
                    nullFailure(() -> cursor.tryAdvance(null), "nested advance null");
                    nullFailure(() -> cursor.forEachRemaining(null), "nested bulk null");
                }), "null refusal preserves outer delivery");
                cursor.forEachRemaining(output::add);
                equal(empty ? List.of() : List.of(1, 2), output, "null refusal preserves cursor");
                released(cursor, "nested null admission");
            }

            Holder<Spliterator<Integer>> holder = new Holder<>();
            holder.value = selected(source(kind, List.of(1)), value -> {
                nullFailure(() -> holder.value.tryAdvance(null), "predicate advance null");
                return true;
            });
            List<Integer> seen = new ArrayList<>();
            check(holder.value.tryAdvance(seen::add), "predicate null refusal leaves action");
            equal(List.of(1), seen, "predicate null refusal delivery");
            released(holder.value, "predicate null admission");
        }
    }

    private static boolean keep(Integer value) {
        return value == null || (value & 1) != 0;
    }

    private static void splitDrain(Spliterator<Integer> cursor, List<Integer> seen,
            boolean sorted, Comparator<? super Integer> comparator) {
        int flags = cursor.characteristics();
        check((flags & Spliterator.ORDERED) != 0, "ordered cursor");
        check((flags & (Spliterator.SIZED | Spliterator.SUBSIZED | Spliterator.CONCURRENT)) == 0,
                "selection does not invent exact size or concurrency");
        equal(-1L, cursor.getExactSizeIfKnown(), "unsized selection");
        if (sorted) {
            check((flags & (Spliterator.SORTED | Spliterator.DISTINCT))
                    == (Spliterator.SORTED | Spliterator.DISTINCT), "sorted metadata");
            check(cursor.getComparator() == comparator, "split comparator identity");
        }
        Spliterator<Integer> prefix = cursor.trySplit();
        if (prefix != null) {
            splitDrain(prefix, seen, sorted, comparator);
            splitDrain(cursor, seen, sorted, comparator);
        } else {
            boolean present;
            do {
                int previous = seen.size();
                present = cursor.tryAdvance(value -> {
                    seen.add(value);
                    cursor.tryAdvance(seen::add);
                });
                equal(seen.size() != previous, present, "leaf operation reports own delivery");
                released(cursor, "split leaf");
            } while (present);
        }
    }

    static void splitConformance() {
        for (int kind = 0; kind < 3; kind++) {
            List<Integer> values = new ArrayList<>();
            for (int i = 0; i < 257; i++) values.add(i % 17 == 0 ? null : (i * 37) % 71);
            Comparator<Integer> comparator = Comparator.nullsFirst(Comparator.reverseOrder());
            Collection<Integer> owner;
            if (kind == 2) {
                TreeSet<Integer> tree = new TreeSet<>(comparator);
                tree.addAll(values);
                owner = tree;
            } else owner = source(kind, values);
            List<Integer> expected = new ArrayList<>();
            for (Integer value : owner) if (keep(value)) expected.add(value);
            int[] decisions = {0};
            Spliterator<Integer> cursor = selected(owner, value -> {
                decisions[0]++;
                return keep(value);
            });
            cursor.estimateSize();
            cursor.characteristics();
            equal(0, decisions[0], "metadata has no predicate prepass");
            List<Integer> actual = new ArrayList<>();
            splitDrain(cursor, actual, kind == 2, kind == 2 ? comparator : null);
            equal(expected, actual, "recursively split order, nulls and duplicates");
            equal(owner.size(), decisions[0], "every source occurrence tested once");
        }

        List<Integer> values = new ArrayList<>();
        for (int i = 0; i < 16; i++) values.add(i);
        equal(crossSplit(new ArrayList<>(values).spliterator()),
                crossSplit(selected(new ArrayList<>(values), value -> true)),
                "cross-child nesting follows actual owner traversal");
    }

    private static List<String> crossSplit(Spliterator<Integer> cursor) {
        List<String> tape = new ArrayList<>();
        Spliterator<Integer> prefix = cursor.trySplit();
        check(prefix != null, "actual ArrayList prefix split");
        boolean outer = cursor.tryAdvance(value -> {
            tape.add("outer:" + value);
            boolean child = prefix.tryAdvance(item -> {
                tape.add("child:" + item);
                tape.add("nested-present:" + cursor.tryAdvance(
                        nested -> tape.add("nested:" + nested)));
            });
            tape.add("child-present:" + child);
        });
        tape.add("outer-present:" + outer);
        prefix.forEachRemaining(value -> tape.add("prefix-tail:" + value));
        cursor.forEachRemaining(value -> tape.add("outer-tail:" + value));
        return tape;
    }

    private static List<String> bulkTrace(Spliterator<Integer> cursor) {
        List<String> tape = new ArrayList<>();
        cursor.forEachRemaining(value -> {
            tape.add("bulk:" + value);
            tape.add("nested:" + cursor.tryAdvance(inner -> tape.add("inner:" + inner)));
        });
        tape.add("after:" + cursor.tryAdvance(value -> tape.add("after-value:" + value)));
        return tape;
    }

    static void bulkConformance() {
        for (int kind = 0; kind < 2; kind++) {
            List<Integer> values = Arrays.asList(null, 1, 2, 3, null, 4, 5);
            List<Integer> expected = new ArrayList<>();
            for (Integer value : values) if (keep(value)) expected.add(value);
            Spliterator<Integer> cursor = selected(source(kind, values),
                    M3CollectionViewsReentrantContract::keep);
            List<Integer> actual = new ArrayList<>();
            check(cursor.tryAdvance(value -> {
                actual.add(value);
                cursor.forEachRemaining(actual::add);
            }), "advance containing bulk remains delivered");
            equal(expected, actual, "advance then nested bulk");
            check(!cursor.tryAdvance(actual::add), "bulk exhausted");
            released(cursor, "nested bulk");

            equal(bulkTrace(source(kind, values).spliterator()),
                    bulkTrace(selected(source(kind, values), value -> true)),
                    "bulk nested advance follows real owner");

            Holder<Spliterator<Integer>> holder = new Holder<>();
            holder.value = selected(source(kind, List.of(1, 2, 3)), value -> {
                if (value == 1) check(!holder.value.tryAdvance(ignored -> {
                    throw new AssertionError("bulk owner already reserved its remaining range");
                }), "nested advance during actual owner bulk");
                return true;
            });
            actual.clear();
            holder.value.forEachRemaining(actual::add);
            equal(List.of(1, 2, 3), actual, "bulk predicate reentry");
            released(holder.value, "bulk predicate reentry");
        }
    }

    static void callbackLifetime() {
        for (int kind = 0; kind < 2; kind++) {
            Spliterator<Integer> cursor = selected(source(kind, List.of(1, 2)), value -> true);
            Object[] capture = new Object[1024];
            Consumer<Integer> inner = value -> check(capture.length == 1024, "live capture");
            Holder<Consumer<Integer>> outer = new Holder<>();
            outer.value = value -> {
                check(retainedCallback(cursor) == outer.value, "outer callback active");
                check(cursor.tryAdvance(inner), "inner capture delivered");
                check(retainedCallback(cursor) == outer.value, "outer callback restored");
                check(!cursor.tryAdvance(inner), "inner capture exhausted");
                check(retainedCallback(cursor) == outer.value, "outer restored after exhaustion");
            };
            check(cursor.tryAdvance(outer.value), "lifetime outer result");
            released(cursor, "successful captured callback");
            check(!cursor.tryAdvance(inner), "captured callback not called on exhaustion");
            released(cursor, "exhausted captured callback");

            Spliterator<Integer> failing = selected(source(kind, List.of(1)), value -> true);
            RuntimeException token = new IllegalStateException("captured failure");
            Consumer<Integer> throwsWithCapture = value -> {
                check(capture.length == 1024, "throwing live capture");
                throw token;
            };
            sameFailure(token, () -> failing.tryAdvance(throwsWithCapture), "captured exception");
            released(failing, "exceptional captured callback");
        }
    }

    public static void main(String[] args) {
        if (args.length == 1 && args[0].equals("--baseline-witness")) {
            consumerExhaustion();
        } else if (args.length == 0) {
            consumerExhaustion();
            nestedConsumers();
            nestedPredicates();
            callbackFailures();
            nullAdmission();
            splitConformance();
            bulkConformance();
            callbackLifetime();
        } else {
            throw new IllegalArgumentException("no arguments or --baseline-witness required");
        }
        System.out.println("PASS M3_COLLECTION_VIEWS_REENTRANT checks=" + checks);
    }
}
