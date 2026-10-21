/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Predicate;

/** Independent owner-state and callback oracles for filtered bulk mutation. */
public final class M3FilteredCowOwnerContract {
    private static int checks;
    private static final List<String> failures = new ArrayList<>();

    private M3FilteredCowOwnerContract() { }

    private enum Operation { CLEAR, REMOVE_IF, REMOVE_ALL, RETAIN_ALL }

    public static void main(String[] args) {
        checks = 0;
        failures.clear();
        for (boolean set : new boolean[] { false, true }) {
            for (boolean nested : new boolean[] { false, true }) {
                for (Operation operation : Operation.values()) {
                    run("builtin/" + set + "/" + nested + "/" + operation,
                            () -> mutation(set, nested, false, operation));
                    run("subclass/" + set + "/" + nested + "/" + operation,
                            () -> mutation(set, nested, true, operation));
                }
                for (boolean visibilityFailure : new boolean[] { false, true }) {
                    run("atomic-failure/" + set + "/" + nested + "/" + visibilityFailure,
                            () -> atomicFailure(set, nested, visibilityFailure));
                }
            }
            run("null/" + set, () -> nullValidation(set));
            for (List<Integer> values : List.of(List.<Integer>of(), List.of(1, 3, 5))) {
                for (Operation operation : Operation.values()) {
                    run("hidden/" + set + "/" + values + "/" + operation,
                            () -> hiddenOnly(set, values, operation));
                }
            }
        }
        for (boolean nested : new boolean[] { false, true }) {
            for (Operation operation : Operation.values()) {
                run("array-list/" + nested + "/" + operation,
                        () -> arrayListFallback(nested, operation));
            }
        }
        run("hidden-equal-reference", M3FilteredCowOwnerContract::equalReferences);
        System.out.println("M3FilteredCowOwnerContract checks=" + checks
                + " failures=" + failures.size());
        if (!failures.isEmpty()) throw new AssertionError(String.join("\n", failures));
    }

    private static void run(String label, Runnable action) {
        try {
            action.run();
        } catch (Throwable failure) {
            failures.add(label + ": " + failure);
        }
    }

    private static Collection<Integer> owner(boolean set, boolean subclass, List<Integer> values) {
        if (subclass) return set ? new SetSubclass(values) : new ListSubclass(values);
        return set ? new CopyOnWriteArraySet<>(values) : new CopyOnWriteArrayList<>(values);
    }

    private static Collection<Integer> view(Collection<Integer> owner, boolean nested) {
        Collection<Integer> view = M3CollectionViews.filtered(owner, value -> (value & 1) == 0);
        return nested ? M3CollectionViews.filtered(view, value -> value >= 2) : view;
    }

    private static boolean visible(int value, boolean nested) {
        return (value & 1) == 0 && (!nested || value >= 2);
    }

    private static boolean removal(Operation operation, int value) {
        return switch (operation) {
            case CLEAR -> true;
            case REMOVE_IF -> value >= 4;
            case REMOVE_ALL -> value == 0 || value == 2 || value == 6 || value == 99;
            case RETAIN_ALL -> value != 2 && value != 99;
        };
    }

    private static Boolean apply(Collection<Integer> view, Operation operation, boolean nested) {
        return switch (operation) {
            case CLEAR -> { view.clear(); yield null; }
            case REMOVE_IF -> view.removeIf(value -> {
                check(visible(value, nested), "condition evaluated on hidden owner value");
                return value >= 4;
            });
            case REMOVE_ALL -> view.removeAll(List.of(0, 2, 6, 99));
            case RETAIN_ALL -> view.retainAll(List.of(2, 99));
        };
    }

    private static List<Integer> expected(List<Integer> values, boolean nested, Operation operation) {
        List<Integer> expected = new ArrayList<>();
        for (int value : values) {
            if (!visible(value, nested) || !removal(operation, value)) expected.add(value);
        }
        return expected;
    }

    private static void mutation(boolean set, boolean nested, boolean subclass, Operation operation) {
        List<Integer> input = List.of(0, 1, 2, 3, 4, 5, 6);
        Collection<Integer> owner = owner(set, subclass, input);
        Iterator<Integer> snapshot = owner.iterator();
        Collection<Integer> filtered = view(owner, nested);
        List<Integer> expected = expected(input, nested, operation);
        Boolean changed = apply(filtered, operation, nested);
        check(new ArrayList<>(owner).equals(expected), "owner contents or order");
        if (changed != null) check(changed == !input.equals(expected), "mutation return value");
        List<Integer> retainedSnapshot = new ArrayList<>();
        snapshot.forEachRemaining(retainedSnapshot::add);
        check(retainedSnapshot.equals(input), "previous snapshot changed");
        if (owner instanceof ListSubclass list) check(list.bulkCalls == 0, "list subclass bulk callback");
        if (owner instanceof SetSubclass setOwner) check(setOwner.bulkCalls == 0, "set subclass bulk callback");
    }

    private static void arrayListFallback(boolean nested, Operation operation) {
        List<Integer> input = List.of(0, 1, 2, 3, 4, 5, 6);
        Collection<Integer> owner = new ArrayList<>(input);
        Boolean changed = apply(view(owner, nested), operation, nested);
        List<Integer> expected = expected(input, nested, operation);
        check(new ArrayList<>(owner).equals(expected), "ordinary writable iterator fallback");
        if (changed != null) check(changed == !input.equals(expected), "ordinary return value");
    }

    private static void atomicFailure(boolean set, boolean nested, boolean visibilityFailure) {
        List<Integer> input = List.of(0, 1, 2, 3, 4, 5, 6);
        Collection<Integer> owner = owner(set, false, input);
        RuntimeException marker = new RuntimeException("intentional predicate failure");
        Collection<Integer> filtered = M3CollectionViews.filtered(owner, value -> {
            if (visibilityFailure && value == 4) throw marker;
            return (value & 1) == 0;
        });
        if (nested) filtered = M3CollectionViews.filtered(filtered, value -> value >= 2);
        RuntimeException observed = null;
        try {
            filtered.removeIf(value -> {
                if (!visibilityFailure && value == 4) throw marker;
                return true;
            });
        } catch (RuntimeException failure) {
            observed = failure;
        }
        check(observed == marker, "original predicate exception identity");
        check(new ArrayList<>(owner).equals(input), "COW owner partially published after failure");
    }

    private static void nullValidation(boolean set) {
        Collection<Integer> owner = owner(set, false, List.of(0, 1, 2));
        int[] calls = { 0 };
        Collection<Integer> filtered = M3CollectionViews.filtered(owner, value -> {
            calls[0]++;
            return true;
        });
        expectNull(() -> filtered.removeIf(null));
        expectNull(() -> filtered.removeAll(null));
        expectNull(() -> filtered.retainAll(null));
        check(calls[0] == 0, "null argument traversed owner");
        check(new ArrayList<>(owner).equals(List.of(0, 1, 2)), "null argument changed owner");
    }

    private static void expectNull(Runnable action) {
        try {
            action.run();
            throw new AssertionError("missing NullPointerException");
        } catch (NullPointerException expected) {
            checks++;
        }
    }

    private static void hiddenOnly(boolean set, List<Integer> input, Operation operation) {
        Collection<Integer> owner = owner(set, false, input);
        Boolean changed = apply(view(owner, false), operation, false);
        check(new ArrayList<>(owner).equals(input), "hidden owner changed");
        if (changed != null) check(!changed, "hidden-only operation reports a change");
    }

    private static void equalReferences() {
        Entry hidden = new Entry(7, false);
        Entry selected = new Entry(7, true);
        CopyOnWriteArrayList<Entry> owner = new CopyOnWriteArrayList<>(List.of(hidden, selected));
        Collection<Entry> filtered = M3CollectionViews.filtered(owner, entry -> entry.visible);
        check(filtered.removeAll(List.of(new Entry(7, true))), "equal visible entry not removed");
        check(owner.size() == 1 && owner.get(0) == hidden, "hidden equal reference was removed");
    }

    private static final class Entry {
        final int key;
        final boolean visible;
        Entry(int key, boolean visible) { this.key = key; this.visible = visible; }
        @Override public boolean equals(Object other) {
            return other instanceof Entry entry && key == entry.key;
        }
        @Override public int hashCode() { return key; }
    }

    private static Iterator<Integer> writableSnapshot(Collection<Integer> owner) {
        Iterator<Integer> snapshot = new ArrayList<>(owner).iterator();
        return new Iterator<>() {
            private Integer current;
            private boolean removable;
            @Override public boolean hasNext() { return snapshot.hasNext(); }
            @Override public Integer next() {
                current = snapshot.next();
                removable = true;
                return current;
            }
            @Override public void remove() {
                if (!removable) throw new IllegalStateException();
                owner.remove(current);
                removable = false;
            }
        };
    }

    private static final class ListSubclass extends CopyOnWriteArrayList<Integer> {
        private static final long serialVersionUID = 1L;
        int bulkCalls;
        ListSubclass(Collection<Integer> values) { super(values); }
        @Override public Iterator<Integer> iterator() { return writableSnapshot(this); }
        @Override public boolean removeIf(Predicate<? super Integer> filter) {
            bulkCalls++;
            throw new UnsupportedOperationException("subclass bulk refusal");
        }
    }

    private static final class SetSubclass extends CopyOnWriteArraySet<Integer> {
        private static final long serialVersionUID = 1L;
        int bulkCalls;
        SetSubclass(Collection<Integer> values) { super(values); }
        @Override public Iterator<Integer> iterator() { return writableSnapshot(this); }
        @Override public boolean removeIf(Predicate<? super Integer> filter) {
            bulkCalls++;
            throw new UnsupportedOperationException("subclass bulk refusal");
        }
    }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }
}

