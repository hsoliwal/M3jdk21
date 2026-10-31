/*
 * @test
 * @summary Actual java.base CollectionViews occurrence and owner mutation contract
 * @run main CollectionViewsCowBulkTest
 */
// Modified 2026: execute the existing behavioral oracle against the java.base receiver.
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
import java.util.CollectionViews;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

/** Frozen behavioral oracle for the existing live filtered collection owner. */
public final class CollectionViewsCowBulkTest {
    private static int checks;

    private CollectionViewsCowBulkTest() { }

    private enum Operation { CLEAR, REMOVE_IF, REMOVE_ALL, RETAIN_ALL }

    /** Equality deliberately ignores visibility; assertions also check physical identity. */
    private static final class Item {
        final int key;
        final boolean visible;
        Item(int key, boolean visible) { this.key = key; this.visible = visible; }
        @Override public boolean equals(Object other) {
            return other instanceof Item item && key == item.key;
        }
        @Override public int hashCode() { return key; }
        @Override public String toString() { return key + ":" + visible; }
    }

    private static final class Failure extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    private static final class TrackingCow<E> extends CopyOnWriteArrayList<E> {
        private static final long serialVersionUID = 1L;
        int bulkCalls;
        TrackingCow(Collection<? extends E> values) { super(values); }
        @Override public boolean removeIf(Predicate<? super E> predicate) {
            bulkCalls++;
            return super.removeIf(predicate);
        }
    }

    private static final class Membership<E> extends AbstractCollection<E> {
        private final Predicate<Object> membership;
        Membership(Predicate<Object> membership) { this.membership = membership; }
        @Override public Iterator<E> iterator() { return List.<E>of().iterator(); }
        @Override public int size() { return 0; }
        @Override public boolean contains(Object element) { return membership.test(element); }
    }

    private static void check(boolean valid, String message) {
        checks++;
        if (!valid) throw new AssertionError(message);
    }

    private static void sameReferences(List<?> expected, List<?> actual, String message) {
        check(expected.size() == actual.size(), message + ": size");
        for (int i = 0; i < expected.size(); i++) {
            check(expected.get(i) == actual.get(i), message + ": physical occurrence " + i);
        }
    }

    private static void throwsSame(Failure expected, Runnable action, String message) {
        try {
            action.run();
            throw new AssertionError(message + ": no exception");
        } catch (Failure actual) {
            check(actual == expected, message + ": original exception identity");
        }
    }

    private static void throwsNull(Runnable action, String message) {
        try {
            action.run();
            throw new AssertionError(message + ": no exception");
        } catch (NullPointerException expected) {
            checks++;
        }
    }

    private static void deleteVisible(Operation operation, Collection<Integer> view) {
        switch (operation) {
            case CLEAR -> view.clear();
            case REMOVE_IF -> check(view.removeIf(value -> true), "removeIf changed");
            case REMOVE_ALL -> check(view.removeAll(new Membership<>(value -> true)), "removeAll changed");
            case RETAIN_ALL -> check(view.retainAll(new Membership<>(value -> false)), "retainAll changed");
        }
    }

    private static void selectedOccurrences(Operation operation) {
        Item hiddenOne = new Item(1, false);
        Item visibleOne = new Item(1, true);
        Item hiddenTwo = new Item(2, false);
        Item visibleTwo = new Item(2, true);
        Item duplicateOne = new Item(1, true);
        var owner = new TrackingCow<>(Arrays.asList(
                hiddenOne, visibleOne, hiddenTwo, visibleTwo, null, duplicateOne));
        Collection<Item> view = CollectionViews.filtered(owner,
                item -> item == null || item.visible);
        List<Item> expected;
        switch (operation) {
            case CLEAR -> {
                view.clear();
                expected = List.of(hiddenOne, hiddenTwo);
            }
            case REMOVE_IF -> {
                check(view.removeIf(item -> item == null || item.key == 1), "removeIf changed");
                expected = List.of(hiddenOne, hiddenTwo, visibleTwo);
            }
            case REMOVE_ALL -> {
                check(view.removeAll(Arrays.asList(new Item(1, true), null)), "removeAll changed");
                expected = List.of(hiddenOne, hiddenTwo, visibleTwo);
            }
            case RETAIN_ALL -> {
                check(view.retainAll(Arrays.asList(new Item(1, true))), "retainAll changed");
                expected = List.of(hiddenOne, visibleOne, hiddenTwo, duplicateOne);
            }
            default -> throw new AssertionError(operation);
        }
        sameReferences(expected, owner, operation + " preserves hidden/equal/null occurrences");
        check(owner.bulkCalls == 1, operation + " delegates once to actual owner bulk method");
        owner.add(new Item(9, true));
        check(view.stream().anyMatch(item -> item != null && item.key == 9), "view remains backed");
    }

    static void selectedOccurrences() {
        for (Operation operation : Operation.values()) selectedOccurrences(operation);
    }

    static void noMatchAndValidation() {
        for (Operation operation : Operation.values()) {
            var owner = new TrackingCow<>(List.of(1, 3, 5));
            AtomicInteger conditions = new AtomicInteger();
            Collection<Integer> view = CollectionViews.filtered(owner, value -> value % 2 == 0);
            switch (operation) {
                case CLEAR -> view.clear();
                case REMOVE_IF -> check(!view.removeIf(value -> { conditions.incrementAndGet(); return true; }), "no-match removeIf");
                case REMOVE_ALL -> check(!view.removeAll(new Membership<>(value -> { conditions.incrementAndGet(); return true; })), "no-match removeAll");
                case RETAIN_ALL -> check(!view.retainAll(new Membership<>(value -> { conditions.incrementAndGet(); return false; })), "no-match retainAll");
            }
            check(conditions.get() == 0, "hidden elements never reach removal/membership callback");
            check(owner.equals(List.of(1, 3, 5)), "no-match owner unchanged");
            check(owner.bulkCalls == 1, "no-match owner receives one bulk call");
        }
        for (List<Integer> input : List.of(List.<Integer>of(), List.of(1, 3))) {
            AtomicInteger filters = new AtomicInteger();
            var owner = new TrackingCow<>(input);
            Collection<Integer> view = CollectionViews.filtered(owner,
                    value -> { filters.incrementAndGet(); return false; });
            throwsNull(() -> view.removeIf(null), "null removeIf");
            throwsNull(() -> view.removeAll(null), "null removeAll");
            throwsNull(() -> view.retainAll(null), "null retainAll");
            check(filters.get() == 0 && owner.bulkCalls == 0, "validation precedes filters and owner writes");
        }
        var empty = new CopyOnWriteArrayList<Integer>();
        Collection<Integer> view = CollectionViews.filtered(empty, Objects::nonNull);
        view.clear();
        check(!view.removeIf(value -> true), "empty removeIf");
        check(!view.removeAll(List.of(1)), "empty removeAll");
        check(!view.retainAll(List.of()), "empty retainAll");
    }

    static void failureAtomicity() {
        for (Operation operation : Operation.values()) {
            List<Integer> input = List.of(1, 2, 3, 4);
            var owner = new CopyOnWriteArrayList<>(input);
            Failure failure = new Failure();
            Collection<Integer> view = CollectionViews.filtered(owner, value -> {
                if (value == 4) throw failure;
                return value % 2 == 0;
            });
            throwsSame(failure, () -> deleteVisible(operation, view), operation + " filter failure");
            sameReferences(input, owner, operation + " filter failure has no owner publication");
        }
        for (Operation operation : List.of(Operation.REMOVE_IF, Operation.REMOVE_ALL, Operation.RETAIN_ALL)) {
            List<Integer> input = List.of(1, 2, 3, 4);
            var owner = new CopyOnWriteArrayList<>(input);
            Failure failure = new Failure();
            AtomicInteger calls = new AtomicInteger();
            Collection<Integer> view = CollectionViews.filtered(owner, value -> value % 2 == 0);
            Predicate<Object> callback = value -> {
                if (calls.incrementAndGet() == 2) throw failure;
                return operation != Operation.RETAIN_ALL;
            };
            throwsSame(failure, () -> {
                switch (operation) {
                    case REMOVE_IF -> view.removeIf(callback);
                    case REMOVE_ALL -> view.removeAll(new Membership<>(callback));
                    case RETAIN_ALL -> view.retainAll(new Membership<>(callback));
                    default -> throw new AssertionError(operation);
                }
            }, operation + " operation callback failure");
            check(calls.get() == 2, "failure follows an earlier selected occurrence");
            sameReferences(input, owner, operation + " callback failure has no owner publication");
        }
    }

    static void regularListPartialFailure() {
        for (boolean linked : new boolean[] {false, true}) {
            for (Operation operation : Operation.values()) {
                List<Integer> owner = linked ? new LinkedList<>(List.of(1, 2, 3, 4))
                        : new ArrayList<>(List.of(1, 2, 3, 4));
                Failure failure = new Failure();
                Collection<Integer> view = CollectionViews.filtered(owner, value -> {
                    if (value == 4) throw failure;
                    return value % 2 == 0;
                });
                throwsSame(failure, () -> deleteVisible(operation, view), "regular-list original failure");
                check(owner.equals(List.of(1, 3, 4)), "regular-list earlier deletion remains published");
            }
        }
    }

    static void nestedAndAliasedViews() {
        var owner = new TrackingCow<>(List.of(1, 2, 3, 4, 5, 6));
        List<Integer> calls = new ArrayList<>();
        Collection<Integer> inner = CollectionViews.filtered(owner, value -> {
            calls.add(value);
            return value % 2 == 0;
        });
        Collection<Integer> outer = CollectionViews.filtered(inner, value -> {
            calls.add(-value);
            return value >= 4;
        });
        outer.clear();
        check(owner.equals(List.of(1, 2, 3, 5)), "nested filters use original owner");
        check(calls.equals(List.of(1, 2, -2, 3, 4, -4, 5, 6, -6)), "nested filter order and short-circuiting");
        check(owner.bulkCalls == 1, "nested filter has one owner dispatch");
        Collection<Integer> even = CollectionViews.filtered(owner, value -> value % 2 == 0);
        check(!even.retainAll(even), "self retain is no-op");
        check(even.removeAll(even), "self remove deletes visible occurrences");
        check(owner.equals(List.of(1, 3, 5)), "self removal preserves hidden occurrences");
    }

    public static void main(String[] args) {
        checks = 0;
        if (args.length == 2 && args[0].equals("--case")) {
            selectedOccurrences(Operation.valueOf(args[1]));
        } else {
            selectedOccurrences();
            noMatchAndValidation();
            failureAtomicity();
            regularListPartialFailure();
            nestedAndAliasedViews();
        }
        System.out.println("CollectionViewsCowBulkTest PASS checks=" + checks);
    }
}
