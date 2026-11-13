/*
 * Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections.eclipse.impl.map.sorted.mutable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.function.BiConsumer;

/** Differential Map.forEach protocol and actual-owner projection-allocation tests. */
public final class IndexedSortedMapForEachTest {
    private static int checks;
    private static int scenarios;

    private IndexedSortedMapForEachTest() { }

    public static void main(String[] arguments) throws Exception {
        String mode = arguments.length == 0 ? "all" : arguments[0];
        if (mode.equals("all") || mode.equals("contract")) {
            releasedPositionIsRejected();
            callbackCorpus();
            comparatorCorpus();
            nullCorpus();
            nullKeysAndSerialization();
        }
        if (mode.equals("all") || mode.equals("storage")) noImplicitEntries();
        check(mode.equals("all") || mode.equals("contract") || mode.equals("storage"), "known mode");
        System.out.println("IndexedSortedMapForEachTest PASS mode=" + mode
                + " scenarios=" + scenarios + " checks=" + checks);
    }

    private static void releasedPositionIsRejected() {
        Context context = new Context(false, false, 0, 0, 0, false);
        context.root.clear();
        for (int key : new int[] {2, 1, 3}) context.root.put(key, "v" + key);
        context.view = context.root.tailMap(1);
        context.order.exactLeaf = true;
        context.order.armed = true;
        try {
            context.view.forEach((key, value) -> { throw new AssertionError("dead slot callback"); });
            throw new AssertionError("released position was accepted");
        } catch (IllegalStateException expected) {
            check(Objects.equals(expected.getMessage(), "arena slot is not live: 1"),
                    "entry-iterator dead-slot exception is preserved");
        }
        check(context.root.size() == 2 && !context.root.containsKey(1), "comparator removal remains applied");
    }

    private static void callbackCorpus() {
        for (boolean reverse : new boolean[] {false, true}) {
            for (int view = 0; view < 5; view++) {
                for (int action = 0; action < 17; action++) {
                    for (int trigger : new int[] {0, 1, 6}) {
                        compare(new Context(false, reverse, view, action, trigger, false),
                                new Context(true, reverse, view, action, trigger, false),
                                "callback " + reverse + "/" + view + "/" + action + "/" + trigger);
                    }
                }
            }
        }
    }

    private static void comparatorCorpus() {
        for (boolean reverse : new boolean[] {false, true}) {
            for (int view = 1; view < 5; view++) {
                for (int action = 0; action < 8; action++) {
                    for (int trigger : new int[] {1, 2, 3, 4}) {
                        Context actual = new Context(false, reverse, view, 0, 0, false);
                        Context reference = new Context(true, reverse, view, 0, 0, false);
                        actual.armComparator(action, trigger);
                        reference.armComparator(action, trigger);
                        compare(actual, reference, "comparator " + reverse + "/" + view + "/" + action + "/" + trigger);
                    }
                }
            }
        }
    }

    private static void nullCorpus() {
        for (boolean empty : new boolean[] {false, true}) {
            for (int view = 0; view < 5; view++) {
                Context actual = new Context(false, false, view, 0, 0, empty);
                Context reference = new Context(true, false, view, 0, 0, empty);
                actual.nullAction = true;
                reference.nullAction = true;
                compare(actual, reference, "null action " + empty + "/" + view);
                check(actual.events.size() == 1 && actual.events.get(0).equals("throw:java.lang.NullPointerException:null:false"),
                        "null action rejected before comparator callbacks");
            }
        }
    }

    private static void nullKeysAndSerialization() throws Exception {
        IndexedSortedMap<Integer, String> map = new IndexedSortedMap<>(Comparator.nullsFirst(Comparator.naturalOrder()));
        map.put(2, "two");
        map.put(null, "null-key");
        map.put(1, null);
        List<SortedMap<Integer, String>> views = List.of(map, map.tailMap(null), map.headMap(2), map.subMap(null, 2));
        for (SortedMap<Integer, String> view : views) {
            List<String> expected = new ArrayList<>();
            new AbstractMap<Integer, String>() {
                @Override public Set<Map.Entry<Integer, String>> entrySet() { return view.entrySet(); }
            }.forEach((key, value) -> expected.add(key + "=" + value));
            List<String> actual = new ArrayList<>();
            view.forEach((key, value) -> actual.add(key + "=" + value));
            check(actual.equals(expected), "null keys and values preserve root/range order");
        }
        byte[] before = serialized(map);
        map.forEach((key, value) -> { });
        check(Arrays.equals(before, serialized(map)), "forEach leaves serialized state unchanged");
        Object restored;
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(before))) {
            restored = input.readObject();
        }
        List<String> restoredValues = new ArrayList<>();
        ((Map<?, ?>) restored).forEach((key, value) -> restoredValues.add(key + "=" + value));
        check(restoredValues.equals(List.of("null=null-key", "1=null", "2=two")),
                "deserialized map dispatches in the original null-capable order");
    }

    private static byte[] serialized(Object value) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) { output.writeObject(value); }
        return bytes.toByteArray();
    }

    private static void compare(Context actual, Context reference, String description) {
        List<String> observed = actual.run();
        List<String> expected = reference.run();
        scenarios++;
        check(observed.equals(expected), description + " actual=" + observed + " expected=" + expected);
        check(actual.state().equals(reference.state()), description + " final state");
    }

    private static final class Context {
        final boolean reference;
        final List<String> events = new ArrayList<>();
        final Order order;
        final IndexedSortedMap<Integer, String> root;
        final int action;
        final int trigger;
        final RuntimeException comparatorFailure = new IllegalStateException("comparator-marker");
        final Throwable marker;
        SortedMap<Integer, String> view;
        boolean nullAction;
        int callbacks;

        Context(boolean reference, boolean reverse, int selection, int action, int trigger, boolean empty) {
            this.reference = reference;
            this.action = action;
            this.trigger = trigger;
            marker = switch (action) {
                case 9 -> new IllegalStateException("action-marker");
                case 10 -> new NullPointerException("action-marker");
                case 11 -> new ConcurrentModificationException("action-marker");
                default -> new AssertionError("action-marker");
            };
            order = new Order(this, reverse);
            root = new IndexedSortedMap<>(order);
            if (!empty) for (int key : new int[] {4, 2, 6, 1, 3, 5, 7}) root.put(key, key == 3 ? null : "v" + key);
            view = switch (selection) {
                case 0 -> root;
                case 1 -> root.headMap(4);
                case 2 -> root.tailMap(4);
                case 3 -> reverse ? root.subMap(6, 2) : root.subMap(2, 6);
                case 4 -> root.subMap(4, 4);
                default -> throw new AssertionError("view");
            };
            events.clear();
        }

        void armComparator(int operation, int comparison) {
            order.operation = operation;
            order.trigger = comparison;
            order.armed = true;
        }

        List<String> run() {
            try {
                traverse(view, nullAction ? null : this::accept);
                events.add("return");
            } catch (Throwable failure) {
                events.add("throw:" + failure.getClass().getName() + ":" + failure.getMessage()
                        + ":" + (failure == marker || failure == comparatorFailure));
            }
            order.armed = false;
            return List.copyOf(events);
        }

        void traverse(SortedMap<Integer, String> selected, BiConsumer<? super Integer, ? super String> consumer) {
            if (reference) {
                // Inherit the actual JDK Map default over the real owner's entry set.
                // This is the effective implementation before the new override.
                new AbstractMap<Integer, String>() {
                    @Override public Set<Map.Entry<Integer, String>> entrySet() { return selected.entrySet(); }
                }.forEach(consumer);
            } else {
                selected.forEach(consumer);
            }
        }

        void accept(Integer key, String value) {
            events.add("visit:" + key + ":" + value);
            if (callbacks++ != trigger) return;
            switch (action) {
                case 0 -> { }
                case 1 -> root.put(key, "current");
                case 2 -> root.put(7, "next");
                case 3 -> root.remove(key);
                case 4 -> root.remove(1);
                case 5 -> root.remove(7);
                case 6 -> root.put(0, "low");
                case 7 -> root.put(8, "high");
                case 8 -> root.clear();
                case 9, 10, 11 -> throw (RuntimeException) marker;
                case 12 -> throw (AssertionError) marker;
                case 13 -> {
                    Map.Entry<Integer, String> first = findEntry(root, key);
                    check(findEntry(root, key) == first, "explicit projection identity");
                    first.setValue("entry-written");
                }
                case 14 -> traverse(view, (nestedKey, nestedValue) -> events.add("nested:" + nestedKey + ":" + nestedValue));
                case 15 -> { root.remove(key); root.put(key, "reinserted"); }
                case 16 -> { root.clear(); root.put(9, "rebuilt"); }
                default -> throw new AssertionError("action");
            }
        }

        List<String> state() {
            List<String> state = new ArrayList<>();
            state.add("size:" + root.size());
            for (Map.Entry<Integer, String> entry : root.entrySet()) state.add(entry.getKey() + "=" + entry.getValue());
            return state;
        }
    }

    private static final class Order implements Comparator<Integer> {
        final Context context;
        final boolean reverse;
        boolean armed;
        boolean exactLeaf;
        int comparisons;
        int operation;
        int trigger;

        Order(Context context, boolean reverse) { this.context = context; this.reverse = reverse; }

        @Override public int compare(Integer first, Integer second) {
            context.events.add("compare:" + first + ":" + second);
            if (armed && (exactLeaf ? Objects.equals(first, 1) && Objects.equals(second, 1) : ++comparisons == trigger)) {
                armed = false;
                if (exactLeaf) context.root.remove(1);
                else switch (operation) {
                    case 0 -> context.root.remove(second);
                    case 1 -> context.root.remove(1);
                    case 2 -> context.root.remove(7);
                    case 3 -> context.root.clear();
                    case 4 -> context.root.put(0, "low");
                    case 5 -> context.root.put(8, "high");
                    case 6 -> context.root.put(second, "changed");
                    case 7 -> throw context.comparatorFailure;
                    default -> throw new AssertionError("comparator action");
                }
            }
            // Nulls-first remains well-defined even if a comparator callback has
            // released a positioned payload before a later fence comparison.
            int result = first == null ? second == null ? 0 : -1 : second == null ? 1 : Integer.compare(first, second);
            return reverse ? -result : result;
        }
    }

    private static Map.Entry<Integer, String> findEntry(SortedMap<Integer, String> map, Integer key) {
        for (Map.Entry<Integer, String> entry : map.entrySet()) if (Objects.equals(entry.getKey(), key)) return entry;
        throw new AssertionError("missing explicit entry");
    }

    private static void noImplicitEntries() throws Exception {
        IndexedSortedMap<Integer, String> map = new IndexedSortedMap<>();
        for (int i = 0; i < 4096; i++) map.put(i, "value");
        IndexedTreeArena<Integer, String> arena = arena(map);
        Object[] references = arena.entryViews;
        check(referenceCount(arena) == 0, "unexposed map has no entry wrappers");
        long[] count = {0};
        map.forEach((key, value) -> { check(value == "value", "exact callback value reference"); count[0]++; });
        check(count[0] == 4096, "one callback per mapping");
        check(referenceCount(arena) == 0 && arena.entryViews == references,
                "Map.forEach creates no implicit entry projections or cache lane");
        Map.Entry<Integer, String> held = map.entrySet().iterator().next();
        Object weak = arena.entryViews[0];
        held.setValue("held");
        map.tailMap(0).forEach((key, value) -> { if (key == 0) check(value == "held", "retained entry update observed"); });
        check(referenceCount(arena) == 1 && arena.entryViews[0] == weak,
                "range traversal preserves the sole explicitly requested projection");
        check(map.entrySet().iterator().next() == held, "forEach preserves retained projection identity");
    }

    @SuppressWarnings("unchecked")
    private static IndexedTreeArena<Integer, String> arena(IndexedSortedMap<Integer, String> map) throws Exception {
        Field storageField = IndexedSortedMap.class.getDeclaredField("storage");
        storageField.setAccessible(true);
        Object storage = storageField.get(map);
        Field backendField = storage.getClass().getDeclaredField("backend");
        backendField.setAccessible(true);
        return ((IndexedRedBlackMap<Integer, String>) backendField.get(storage)).tree.arena;
    }

    private static int referenceCount(IndexedTreeArena<?, ?> arena) {
        int count = 0;
        for (Object reference : arena.entryViews) if (reference != null) count++;
        return count;
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
