// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicInteger;

/** Simple operation counts, not a JMH speedup benchmark or concurrent proof. */
public final class M3FilteredListsWorkProof {
    private M3FilteredListsWorkProof() { }
    public static void main(String[] args) {
        int n = 65_537;
        ArrayList<Integer> backing = new ArrayList<>(n);
        for (int i = 0; i < n; i++) backing.add(i);
        AtomicInteger selections = new AtomicInteger();
        List<Integer> view = M3FilteredLists.filtered(backing, x -> {
            selections.incrementAndGet(); return (x & 1) == 0;
        });
        var iterator = view.iterator();
        selections.set(0);
        long observed = 0, checksum = 0;
        while (iterator.hasNext()) {
            if (!iterator.hasNext()) throw new AssertionError("repeated lookahead");
            Integer next = iterator.next();
            checksum += next; observed++;
        }
        if (observed != (n + 1L) / 2L || checksum != 1_073_774_592L) {
            throw new AssertionError("incorrect selected sequence");
        }
        if (selections.get() > n + 1) throw new AssertionError("superlinear predicate rescanning: " + selections);
        Spliterator<Integer> traversal = view.spliterator();
        if ((traversal.characteristics() & (Spliterator.SIZED | Spliterator.SORTED | Spliterator.DISTINCT)) != 0)
            throw new AssertionError("unsupported traversal characteristics");
        if (traversal.trySplit() != null) throw new AssertionError("mutable live view must not synthesize a cached split");
        for (Class<?> nested : M3FilteredLists.class.getDeclaredClasses()) {
            for (Field field : nested.getDeclaredFields()) {
                Class<?> type = field.getType();
                if (type == Object[].class || type == int[].class || type == long[].class
                        || Collection.class == type)
                    throw new AssertionError("new retained per-element payload owner: " + field);
            }
        }
        System.out.println("PASS M3FilteredListsWorkProof rows=" + n
                + " predicate_calls=" + selections.get() + " split_payload=0");
    }
}
