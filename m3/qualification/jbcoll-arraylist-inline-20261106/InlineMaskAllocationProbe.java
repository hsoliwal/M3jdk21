/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
 */

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.function.Predicate;

/**
 * INFO probe of leaf jbcoll-arraylist-inline-20261106: bytes allocated by ArrayList.removeIf on spans of 2..128
 * elements (the inline-mask range) versus 129..511 (deathRow kept), measured with com.sun.management.ThreadMXBean
 * after warm-up. The list is refilled element by element from a prebuilt array (no allocation once capacity is there),
 * and the odd-value predicate rejects from index 1, so span N removes over N - 1 elements (span129 is still inline).
 *
 * <p>usage: java [--patch-module java.base=CLASSES] InlineMaskAllocationProbe
 */
public final class InlineMaskAllocationProbe {
    private InlineMaskAllocationProbe() { }

    private static long run(int span, int rounds, Object[] source, ArrayList<Object> list, Predicate<Object> odd) {
        com.sun.management.ThreadMXBean bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        long before = bean.getCurrentThreadAllocatedBytes();
        for (int round = 0; round < rounds; round++) {
            list.clear();
            for (int i = 0; i < span; i++) list.add(source[i]);
            list.removeIf(odd);
        }
        return bean.getCurrentThreadAllocatedBytes() - before;
    }

    public static void main(String[] args) {
        Object[] source = new Object[512];
        for (int i = 0; i < source.length; i++) source[i] = Integer.valueOf(i);
        ArrayList<Object> list = new ArrayList<>(1024);
        Predicate<Object> odd = value -> (((Integer) value) & 1) != 0;
        for (int warm = 0; warm < 20; warm++) {
            run(100, 10_000, source, list, odd);
            run(300, 10_000, source, list, odd);
        }
        int rounds = 100_000;
        StringBuilder line = new StringBuilder("INLINE_MASK_ALLOCATION rounds=" + rounds);
        for (int span : new int[] {64, 128, 129, 300}) {
            long bytes = run(span, rounds, source, list, odd);
            line.append(" span").append(span).append("=").append(bytes).append("B");
        }
        System.out.println(line);
    }
}
