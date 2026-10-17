// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

/** Source-exact, Java21/JDK behavior proof for the selected first-party purpose owners. */
public final class M3PurposeReceiverProof {
    private static int checks;
    private M3PurposeReceiverProof() { }

    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
        checks++;
    }

    private static void reject(Class<? extends Throwable> expected, Runnable op, String why) {
        try { op.run(); throw new AssertionError("accepted " + why); }
        catch (Throwable failure) {
            if (!expected.isInstance(failure)) throw new AssertionError(why, failure);
            checks++;
        }
    }

    private static void saturatingConcatAndNoFalseSizing() {
        Collection<Integer> virtual = new AbstractCollection<>() {
            @Override public int size() { return Integer.MAX_VALUE; }
            @Override public Iterator<Integer> iterator() { return Collections.<Integer>emptyList().iterator(); }
        };
        Collection<Integer> concat = M3CollectionWrappers.concatenatedView(virtual, List.of(42));
        check(concat.size() == Integer.MAX_VALUE, "JDK Collection.size must saturate");
        check((concat.spliterator().characteristics() & java.util.Spliterator.SIZED) == 0,
                "saturated collection cannot advertise SIZED");
        check(concat.iterator().next() == 42, "concat iteration still source ordered");
        Collection<Integer> ordinary = M3CollectionWrappers.concatenatedView(
                List.of(1, 2), List.of(3, 4));
        check(List.copyOf(ordinary).equals(List.of(1, 2, 3, 4)), "normal concat traversal");
        check(ordinary.size() == 4, "normal concat size");
    }

    private static void boundedEarlyAdmissionDoesNotMaterializeRejectedInput() {
        List<Integer> source = new ArrayList<>(List.of(1, 2));
        List<Integer> bounded = M3CollectionWrappers.bounded(source, 2);
        AtomicInteger traversal = new AtomicInteger();
        Collection<Integer> oversized = new AbstractCollection<>() {
            @Override public int size() { return 1; }
            @Override public Iterator<Integer> iterator() {
                traversal.incrementAndGet();
                throw new AssertionError("rejected source must not materialize");
            }
        };
        reject(IllegalStateException.class, () -> bounded.addAll(1, oversized),
                "early over-capacity rejection");
        check(traversal.get() == 0, "rejected source iterated");
        check(source.equals(List.of(1, 2)), "invalid write mutated source");
        reject(IllegalStateException.class, () -> bounded.add(3), "bounded single addition");
    }

    private static void purposeCompositionsAndActualStoredComparators() {
        Comparator<String> comparator = String.CASE_INSENSITIVE_ORDER;
        NavigableSet<String> owner = new TreeSet<>(comparator);
        owner.addAll(List.of("Alpha", "Bravo", "charlie"));
        NavigableSet<String> filtered = M3CollectionWrappers.filtered(owner,
                value -> !value.equals("Bravo"));
        NavigableSet<String> readOnly = M3CollectionWrappers.readOnly(filtered);
        check(readOnly.comparator() == comparator, "read-only comparator identity");
        check(readOnly.floor("braVo").equals("Alpha"), "filtered navigation");
        check(readOnly.ceiling("alpha").equals("Alpha"), "stored comparator representative");
        reject(UnsupportedOperationException.class, () -> readOnly.pollFirst(),
                "read-only navigable poll");
        NavigableSet<String> checked = M3CollectionWrappers.checked(filtered, String.class);
        check(checked.comparator() == comparator, "checked sorted comparator");
        NavigableSet<String> synchronizedView = M3CollectionWrappers.synchronizedView(checked);
        synchronized (synchronizedView) {
            check(new ArrayList<>(synchronizedView).equals(List.of("Alpha", "charlie")),
                    "synchronized filter iteration");
        }
        owner.add("Delta");
        check(readOnly.contains("DELTA"), "purpose wrapper lost backing liveness");
        reject(IllegalArgumentException.class, () -> filtered.add("Bravo"),
                "filtered view reject invalid admission");
    }

    private static void checkedMapsAndSequencedViews() {
        TreeMap<Integer, Integer> source = new TreeMap<>();
        source.put(1, 10); source.put(2, 20);
        Map<Integer, Integer> checked = M3CollectionWrappers.checked(source,
                Integer.class, Integer.class);
        Map<Integer, Integer> readOnly = M3CollectionWrappers.readOnly(checked);
        reject(UnsupportedOperationException.class, () -> readOnly.put(3, 30),
                "readOnly checked map insertion");
        source.put(3, 30);
        check(readOnly.get(3) == 30, "read-only map backing liveness");
        List<Integer> list = new LinkedList<>(Arrays.asList(1, null, 1, 2));
        List<Integer> reversed = M3CollectionWrappers.reversedView(list);
        check(reversed.equals(Arrays.asList(2, 1, null, 1)), "Java21 reversed list");
        check(M3CollectionWrappers.snapshot(list).equals(list), "snapshot values");
        list.add(4);
        check(reversed.getFirst() == 4, "reversed list remains live");
    }

    public static void main(String[] ignored) {
        saturatingConcatAndNoFalseSizing();
        boundedEarlyAdmissionDoesNotMaterializeRejectedInput();
        purposeCompositionsAndActualStoredComparators();
        checkedMapsAndSequencedViews();
        System.out.println("M3_PURPOSE_WRAPPER_RECEIVER_PASS checks=" + checks);
    }
}
