/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/** Standalone deterministic differential against JDK equality/count map semantics. */
public final class M3CountedBagSelfTest {
    private static long checks;
    private M3CountedBagSelfTest() { }
    private static void check(boolean result, String info) {
        checks++;
        if (!result) throw new AssertionError(info);
    }
    private static void rejects(Class<? extends Throwable> kind, Runnable task) {
        checks++;
        try { task.run(); } catch (Throwable problem) {
            if (kind.isInstance(problem)) return;
            throw new AssertionError("unexpected exception", problem);
        }
        throw new AssertionError("expected " + kind);
    }
    private static void match(M3CountedBag<Integer> owner, LinkedHashMap<Integer,Integer> oracle) {
        long total = 0;
        for (int n : oracle.values()) total += n;
        check(owner.totalOccurrences() == total, "total");
        check(owner.size() == Math.min(total, Integer.MAX_VALUE), "size");
        check(owner.distinctSize() == oracle.size(), "distinct");
        check(owner.distinctElements().equals(oracle.keySet()), "distinct keys");
        for (int key = -12; key <= 12; key++) {
            check(owner.count(key) == oracle.getOrDefault(key, 0), "count key " + key);
        }
        ArrayList<Integer> observed = new ArrayList<>();
        for (Integer value : owner) observed.add(value);
        ArrayList<Integer> expected = new ArrayList<>();
        for (Map.Entry<Integer,Integer> entry : oracle.entrySet()) {
            for (int j = 0; j < entry.getValue(); j++) expected.add(entry.getKey());
        }
        check(observed.equals(expected), "encounter and multiplicities");
    }
    public static void main(String[] args) {
        M3CountedBag<Integer> bag = new M3CountedBag<>();
        LinkedHashMap<Integer,Integer> model = new LinkedHashMap<>();
        Random random = new Random(0x1F10A20261017L);
        for (int step = 0; step < 5300; step++) {
            Integer key = random.nextInt(25) - 12;
            int delta = random.nextInt(5);
            int old = model.getOrDefault(key, 0);
            switch (random.nextInt(6)) {
                case 0, 1 -> {
                    check(bag.addOccurrences(key, delta) == old, "add old");
                    if (delta != 0) model.put(key, old + delta);
                }
                case 2, 3 -> {
                    check(bag.removeOccurrences(key, delta) == old, "remove old");
                    if (delta >= old && delta != 0) model.remove(key);
                    else if (delta != 0 && old != 0) model.put(key, old - delta);
                }
                case 4 -> {
                    int replacement = random.nextInt(8);
                    check(bag.setOccurrences(key, replacement) == old, "set old");
                    if (replacement == 0) model.remove(key);
                    else model.put(key, replacement);
                }
                default -> {
                    check(bag.setOccurrences(key, old + 1, 9) == false, "CAS wrong expected");
                    check(bag.setOccurrences(key, old, old), "CAS equal");
                }
            }
            match(bag, model);
        }
        // Valid API's explicit zero, overflow and failure boundaries.
        rejects(IllegalArgumentException.class, () -> bag.addOccurrences(0, -1));
        rejects(IllegalArgumentException.class, () -> bag.removeOccurrences(0, -1));
        rejects(IllegalArgumentException.class, () -> bag.setOccurrences(0, -1));
        M3CountedBag<String> large = new M3CountedBag<>();
        large.addOccurrences("a", Integer.MAX_VALUE);
        large.addOccurrences("b", Integer.MAX_VALUE);
        check(large.size() == Integer.MAX_VALUE, "saturated public size");
        check(large.totalOccurrences() == 2L * Integer.MAX_VALUE, "exact long total");
        rejects(IllegalArgumentException.class, () -> large.addOccurrences("a", 1));
        M3CountedBag<String> nullable = new M3CountedBag<>();
        nullable.add(null);
        nullable.add(null);
        check(nullable.count(null) == 2, "null multiplicity");
        byte[] states = {(byte) 0x81};
        check(nullable.selectedDistinct(states, 0x81, M3IndexMaskCompactor.Backend.JAVA).count(null) == 2,
                "mask selection of null key");
        rejects(IllegalArgumentException.class, () -> nullable.selectedDistinct(new byte[2], 1,
                M3IndexMaskCompactor.Backend.JAVA));
        if (M3IndexMaskCompactor.nativeAvailable()) {
            check(nullable.selectedDistinct(states, 0x81,
                    M3IndexMaskCompactor.Backend.NATIVE_REQUIRED).count(null) == 2,
                    "real JNI count-index mask parity");
        } else {
            rejects(UnsatisfiedLinkError.class, () -> nullable.selectedDistinct(states, 0x81,
                    M3IndexMaskCompactor.Backend.NATIVE_REQUIRED));
        }
        M3CountedBag<Integer> sorted = new M3CountedBag<>();
        sorted.addOccurrences(3, 2);
        sorted.addOccurrences(1, 4);
        sorted.addOccurrences(2, 5);
        var ordered = sorted.sortedCounts(Integer::compare);
        check(ordered.distinctSize() == 3, "sorted distinct count");
        check(ordered.elementAt(0) == 1 && ordered.countAt(0) == 4, "sorted first count");
        check(ordered.elementAt(1) == 2 && ordered.countAt(1) == 5, "sorted second count");
        check(ordered.elementAt(2) == 3 && ordered.countAt(2) == 2, "sorted third count");
        check(ordered.slice(1, 2).totalOccurrences() == 7, "offset slice count");
        java.util.concurrent.atomic.AtomicInteger calls = new java.util.concurrent.atomic.AtomicInteger();
        var filtered = ordered.filtered(value -> { calls.incrementAndGet(); return value >= 2; });
        check(calls.get() == 3, "once per distinct value");
        check(filtered.distinctSize() == 2 && filtered.countAt(0) == 5, "filtered index order");
        sorted.setOccurrences(1, 0);
        check(ordered.elementAt(0) == 1 && ordered.countAt(0) == 4, "detached sorted snapshot");
        check(filtered.slice(0, filtered.distinctSize()).totalOccurrences() == 7,
                "repeat sorted and filtered without re-evaluation");
        check(calls.get() == 3, "memoized predicate fact");
        M3CountedBag<Integer> iterBag = new M3CountedBag<>();
        iterBag.addOccurrences(3, 3);
        iterBag.addOccurrences(4, 2);
        Iterator<Integer> it = iterBag.iterator();
        check(it.next() == 3, "cursor first");
        it.remove();
        check(iterBag.count(3) == 2, "cursor one occurrence remove");
        check(it.next() == 3, "cursor next after remove");
        it.remove();
        check(it.next() == 3 && it.next() == 4, "boundary after count updates");
        iterBag.addOccurrences(4, 1);
        rejects(ConcurrentModificationException.class, it::hasNext);
        Iterator<Integer> dis = iterBag.distinctElements().iterator();
        check(dis.next() == 3, "distinct next");
        dis.remove();
        check(iterBag.count(3) == 0, "distinct removes all counts");
        rejects(IllegalStateException.class, dis::remove);
        System.out.println("PASS M3CountedBagSelfTest checks=" + checks);
    }
}
