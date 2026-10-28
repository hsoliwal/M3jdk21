// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** Differential proof that the original trim contract and primitive removal mask coexist. */
public final class M3ArrayListSupersetContract {
    private static long checks;
    private M3ArrayListSupersetContract() { }

    private static void check(boolean valid, String reason) {
        checks++;
        if (!valid) throw new AssertionError(reason);
    }

    @FunctionalInterface private interface Probe { void invoke(); }
    private static void refuses(Class<? extends Throwable> expected, Probe operation) {
        checks++;
        try { operation.invoke(); }
        catch (Throwable failure) {
            if (expected.isInstance(failure)) return;
            throw new AssertionError("Unexpected refusal", failure);
        }
        throw new AssertionError("Expected " + expected.getName());
    }

    private static void existingTrimContract() {
        M3ArrayList<Integer> owner = new M3ArrayList<>(3);
        owner.add(7); owner.add(8); owner.add(9);
        check(owner.capacity() == 3, "initial array capacity exact");
        Iterator<Integer> stable = owner.iterator();
        owner.trimToSize();
        check(stable.next() == 7 && stable.next() == 8,
                "no-arg no-op trim preserves existing iterator");
        owner.trimToSize(false);
        check(stable.next() == 9, "explicit false has same storage-only epoch");
        Iterator<Integer> invalidated = owner.iterator();
        owner.trimToSize(true);
        refuses(ConcurrentModificationException.class, invalidated::next);
        check(owner.capacity() == 3, "explicit epoch never reallocates full capacity");

        M3ArrayList<Integer> spare = new M3ArrayList<>(16);
        spare.add(3); spare.add(4);
        Iterator<Integer> old = spare.iterator();
        spare.trimToSize();
        check(spare.capacity() == 2, "original trim releases spare capacity");
        refuses(ConcurrentModificationException.class, old::next);
        check(spare.equals(List.of(3, 4)), "exact trimmed values");
    }

    private static void firstRejectRelativeBitMasks() {
        M3ArrayList<Integer> owner = new M3ArrayList<>();
        List<Integer> oracle = new ArrayList<>();
        int length = 769;
        for (int i = 0; i < length; i++) {
            owner.add(i);
            oracle.add(i);
        }
        List<Integer> observed = new ArrayList<>();
        boolean changed = owner.removeIf(value -> {
            observed.add(value);
            return value >= 65 && value % 65 == 0;
        });
        oracle.removeIf(value -> value >= 65 && value % 65 == 0);
        check(changed, "first removal at offset 65");
        check(observed.size() == length, "predicate callback evaluated once per original row");
        check(observed.equals(java.util.stream.IntStream.range(0, length).boxed().toList()),
                "callback order equals original physical encounter order");
        check(owner.equals(oracle), "stable mask compaction matches independent JDK ArrayList");
        check(owner.size() == length - 11, "exact bit lane cardinality");
        boolean changedAgain = owner.removeIf(value -> false);
        check(!changedAgain && owner.equals(oracle), "no-match input does not modify original list");

        M3ArrayList<Object> identities = new M3ArrayList<>();
        Object a = new Object(), b = new Object();
        identities.add(a); identities.add(null); identities.add(a); identities.add(b);
        identities.add(null); identities.add(b);
        check(identities.removeIf(value -> value == null), "null filtering");
        check(identities.size() == 4, "all nulls removed");
        check(identities.get(0) == a && identities.get(1) == a
                && identities.get(2) == b && identities.get(3) == b,
                "duplicate object reference identity and original order preserved");
    }

    private static void callbackSemantics() {
        M3ArrayList<Integer> owner = new M3ArrayList<>();
        for (int i = 0; i < 12; i++) owner.add(i);
        AtomicInteger evaluated = new AtomicInteger();
        check(owner.removeIf(value -> {
            evaluated.incrementAndGet();
            if (value == 8) owner.set(0, 99); // nonstructural write
            return value == 4;
        }), "removal after nonstructural callback");
        check(evaluated.get() == 12, "single original callback pass");
        check(owner.get(0) == 99 && owner.size() == 11
                && owner.get(3) == 3 && owner.get(4) == 5,
                "retain nonstructural write while compacting stable order");

        M3ArrayList<Integer> nested = new M3ArrayList<>();
        nested.add(1); nested.add(2); nested.add(3);
        refuses(ConcurrentModificationException.class, () ->
                nested.removeIf(value -> {
                    if (value == 2) nested.add(100);
                    return value == 1;
                }));
        check(nested.equals(Arrays.asList(1, 2, 3, 100)),
                "structural reentrancy refuses compaction but keeps callback side effect");
    }

    public static void main(String[] args) {
        existingTrimContract();
        firstRejectRelativeBitMasks();
        callbackSemantics();
        System.out.println("M3_ARRAY_SUPERSET_PASS checks=" + checks);
    }
}
