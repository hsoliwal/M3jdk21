/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NavigableSet;
import java.util.SequencedSet;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

/** Endpoint refusal must precede observation of a strict filtered owner. */
public final class M3FilteredFacadeEndpointContract {
    private static int checks;
    private static final List<String> failures = new ArrayList<>();

    private M3FilteredFacadeEndpointContract() { }
    private record View(String name, SequencedSet<Integer> value) { }

    public static void main(String[] args) {
        checks = 0;
        failures.clear();
        for (List<Integer> input : List.of(List.<Integer>of(), List.of(1, 3, 5), List.of(1, 2, 3, 4, 5))) {
            for (boolean hideAll : new boolean[] { false, true }) exercise(input, hideAll);
        }
        System.out.println("M3FilteredFacadeEndpointContract checks=" + checks
                + " failures=" + failures.size());
        if (!failures.isEmpty()) throw new AssertionError(String.join("\n", failures));
    }

    private static void exercise(List<Integer> input, boolean hideAll) {
        AtomicInteger compares = new AtomicInteger();
        AtomicInteger predicates = new AtomicInteger();
        TreeSet<Integer> owner = new TreeSet<>((left, right) -> {
            compares.incrementAndGet();
            return Integer.compare(left, right);
        });
        owner.addAll(input);
        Predicate<Integer> predicate = value -> {
            predicates.incrementAndGet();
            return !hideAll && (value & 1) == 0;
        };
        NavigableSet<Integer> nav = M3FilteredNavigableSet.view(owner, predicate);
        SortedSet<Integer> sortedOwner = Collections.unmodifiableSortedSet(owner);
        SortedSet<Integer> sorted = M3FilteredViews.filter(sortedOwner, predicate);
        List<View> views = List.of(
                new View("facade", nav),
                new View("facade-descending", nav.descendingSet()),
                new View("facade-head", nav.headSet(10, false)),
                new View("facade-tail", nav.tailSet(0, true)),
                new View("facade-range", nav.subSet(0, true, 10, false)),
                new View("sorted", sorted),
                new View("sorted-reversed", sorted.reversed()),
                new View("sorted-head", sorted.headSet(10)),
                new View("sorted-tail", sorted.tailSet(0)),
                new View("sorted-range", sorted.subSet(0, 10)));
        for (View view : views) {
            for (boolean first : new boolean[] { false, true }) {
                predicates.set(0);
                compares.set(0);
                Throwable observed = null;
                try {
                    if (first) view.value.removeFirst(); else view.value.removeLast();
                } catch (Throwable failure) {
                    observed = failure;
                }
                String label = view.name + "/" + input + "/" + hideAll + "/" + first;
                verify(observed instanceof UnsupportedOperationException, label + ": refusal=" + observed);
                verify(predicates.get() == 0, label + ": predicate observed before refusal");
                verify(compares.get() == 0, label + ": comparator observed before refusal");
                verify(new ArrayList<>(owner).equals(input), label + ": backing owner changed");
            }
        }
    }

    private static void verify(boolean condition, String label) {
        checks++;
        if (!condition) failures.add(label);
    }
}

