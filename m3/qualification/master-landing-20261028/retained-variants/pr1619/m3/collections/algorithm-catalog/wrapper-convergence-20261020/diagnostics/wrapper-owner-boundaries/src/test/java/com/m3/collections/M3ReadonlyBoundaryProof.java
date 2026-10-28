// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

/** Positive observations of unresolved defects; not a replacement acceptance oracle. */
public final class M3ReadonlyBoundaryProof {
    private M3ReadonlyBoundaryProof() { }

    private static Object outcome(Supplier<?> operation) {
        try { return operation.get(); }
        catch (RuntimeException | Error failure) { return failure; }
    }

    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }

    private static void demonstrateMissingOwnerGeneration(boolean linked, boolean restoredContents) {
        List<Integer> owner = linked ? new LinkedList<>(List.of(1, 2, 3, 4))
                : new ArrayList<>(List.of(1, 2, 3, 4));
        Iterator<Integer> physical = owner.iterator();
        Iterator<Integer> selected = M3FilteredLists.readOnlyFiltered(owner, value -> (value & 1) == 0)
                .iterator();
        check(selected.next() == 2, "first selected member");
        if (restoredContents) {
            owner.remove(0);
            owner.add(0, 1);
        } else {
            owner.add(6);
        }
        Object physicalResult = outcome(physical::next);
        Object selectedResult = outcome(selected::next);
        check(physicalResult instanceof ConcurrentModificationException,
                "the physical owner detects its structural change");
        check(Integer.valueOf(4).equals(selectedResult),
                "DEFECT witness: current read-only indexed iterator misses the owner's generation change");
        System.out.println("KNOWN-DEFECT readonly-owner-generation owner=" + owner.getClass().getSimpleName()
                + " restored-contents=" + restoredContents + " physical=CME filtered=" + selectedResult);
    }

    private static void demonstrateDelegateSwitchChangesConstructionEffects() {
        ArrayList<Integer> strictTape = new ArrayList<>();
        ArrayList<Integer> delegateTape = new ArrayList<>();
        List<Integer> strict = M3FilteredLists.readOnlyFiltered(new ArrayList<>(List.of(1, 2, 3, 4)), value -> {
            strictTape.add(value);
            return (value & 1) == 0;
        });
        List<Integer> delegate = M3FilteredLists.filtered(new ArrayList<>(List.of(1, 2, 3, 4)), value -> {
            delegateTape.add(value);
            return (value & 1) == 0;
        });
        strict.iterator();
        delegate.iterator();
        check(strictTape.isEmpty(), "current strict iterator construction remains predicate-lazy");
        check(delegateTape.equals(List.of(1, 2)), "delegate cursor construction evaluates first physical prefix");
        System.out.println("REJECTED-DELEGATE-SWITCH constructor-tapes strict=" + strictTape
                + " delegate=" + delegateTape);
    }

    private static void demonstrateRequiredRetryEffects() {
        IllegalStateException marker = new IllegalStateException("caller predicate marker");
        ArrayList<Integer> tape = new ArrayList<>();
        Iterator<Integer> selected = M3FilteredLists.readOnlyFiltered(
                new ArrayList<>(List.of(1, 2, 4, 6)), value -> {
                    tape.add(value);
                    if (value == 4) throw marker;
                    return (value & 1) == 0;
                }).iterator();
        check(outcome(selected::hasNext) == marker, "size scan propagates same caller failure");
        check(tape.equals(List.of(1, 2, 4)), "first failing size scan tape");
        check(Integer.valueOf(2).equals(outcome(selected::next)), "indexed next still succeeds after failed size scan");
        check(tape.equals(List.of(1, 2, 4, 1, 2)), "frozen retry callback order");
        System.out.println("RETAINED-READ-EFFECT failed-hasNext=same-marker next=2 tape=" + tape);
    }

    public static void main(String[] args) {
        for (boolean linked : new boolean[] {false, true}) {
            for (boolean restoredContents : new boolean[] {false, true}) {
                demonstrateMissingOwnerGeneration(linked, restoredContents);
            }
        }
        demonstrateDelegateSwitchChangesConstructionEffects();
        demonstrateRequiredRetryEffects();
        System.out.println("DIAGNOSTIC OBSERVATIONS REPRODUCED read-only defects and preserved effects; not acceptance");
    }
}
