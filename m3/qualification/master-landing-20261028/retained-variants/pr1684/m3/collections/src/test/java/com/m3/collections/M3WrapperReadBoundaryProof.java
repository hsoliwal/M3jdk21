/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/** Counterexamples to candidate wrapper shortcuts, not a wrapper acceptance suite. */
public final class M3WrapperReadBoundaryProof {
    private static int checks;
    private M3WrapperReadBoundaryProof() { }

    private static void check(boolean value, String label) {
        checks++;
        if (!value) throw new AssertionError(label);
    }

    private static final class ObservedTreeSet extends TreeSet<String> {
        private static final long serialVersionUID = 1L;
        final transient List<String> calls = new ArrayList<>();
        RuntimeException tailFailure;
        ObservedTreeSet() { super(String.CASE_INSENSITIVE_ORDER); }
        @Override public boolean contains(Object value) {
            calls.add("contains");
            return super.contains(value);
        }
        @Override public SortedSet<String> tailSet(String value) {
            calls.add("tailSet");
            if (tailFailure != null) throw tailFailure;
            return super.tailSet(value);
        }
        @Override public String ceiling(String value) {
            calls.add("ceiling");
            return super.ceiling(value);
        }
    }

    public static void main(String[] args) {
        checks = 0;
        ObservedTreeSet owner = new ObservedTreeSet();
        String stored = new String("Alpha");
        owner.add(stored);
        NavigableSet<String> view = M3CollectionViews.filtered(
                (NavigableSet<String>) owner, value -> value == stored);

        owner.calls.clear();
        check(view.contains("alpha"), "existing sorted wrapper membership");
        check(owner.calls.equals(List.of("contains", "tailSet")), "existing owner dispatch");
        owner.calls.clear();
        boolean proposed = owner.contains("alpha") && owner.ceiling("alpha") == stored;
        check(proposed, "ceiling proposal value agrees for ordinary lookup");
        check(owner.calls.equals(List.of("contains", "ceiling")), "proposal changes virtual call trace");

        RuntimeException marker = new RuntimeException("intentional owner tail failure");
        owner.tailFailure = marker;
        Throwable observed = null;
        try { view.contains("alpha"); } catch (RuntimeException failure) { observed = failure; }
        check(observed == marker, "existing owner exception identity");
        check(owner.contains("alpha") && owner.ceiling("alpha") == stored,
                "ceiling shortcut would suppress an original exception");
        owner.tailFailure = null;

        List<Integer> mutable = new ArrayList<>(List.of(1, 2));
        Collection<Integer> live = M3CollectionViews.filtered(mutable, value -> (value & 1) == 0);
        int cachedSize = live.size();
        mutable.add(4);
        check(cachedSize == 1 && live.size() == 2, "cached cardinality loses live owner mutation");

        check(view.contains(new String("alpha")), "stored representative remains visible");
        check(new String("alpha") != stored, "predicate-on-probe proposal would hide stored member");

        Set<String> readOnlySet = M3FilteredViews.filter((Set<String>) owner, value -> value == stored);
        check(owner.contains("alpha"), "sorted owner admits alias");
        check(!readOnlySet.contains("alpha"), "current Set-overload comparator-alias defect reproduced");
        check(readOnlySet.contains(stored), "same-reference baseline remains visible");
        System.out.println("WRAPPER_BOUNDARY_OBSERVATIONS checks=" + checks
                + " rejected=tailSet-to-ceiling,live-size-cache,predicate-on-probe"
                + " known_defect=set-overload-comparator-alias");
    }
}
