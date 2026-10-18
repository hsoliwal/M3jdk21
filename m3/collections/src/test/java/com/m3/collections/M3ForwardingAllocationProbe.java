// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.lang.management.ManagementFactory;

/** Prepared scalar mutation discriminator; no throughput or retained-heap claim. */
public final class M3ForwardingAllocationProbe {
    private M3ForwardingAllocationProbe() { }
    private static long exercise(M3LongList sequence, int count) {
        long total = 0;
        for (int index = 0; index < count; index++) {
            sequence.add(index);
            total += sequence.removeAt(0);
        }
        return total;
    }
    public static void main(String[] args) {
        var bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        if (!bean.isThreadAllocatedMemorySupported()) throw new IllegalStateException("allocation counter unavailable");
        bean.setThreadAllocatedMemoryEnabled(true);
        M3LongArrayList owner = new M3LongArrayList(8);
        M3LongList wrapped = new M3ForwardingLongList() {
            @Override protected M3LongList delegate() { return owner; }
        };
        exercise(owner, 200000);
        exercise(wrapped, 200000);
        long thread = Thread.currentThread().threadId();
        long before = bean.getThreadAllocatedBytes(thread);
        long nativeSum = exercise(owner, 100000);
        long middle = bean.getThreadAllocatedBytes(thread);
        long wrappedSum = exercise(wrapped, 100000);
        long after = bean.getThreadAllocatedBytes(thread);
        if (nativeSum != 4999950000L || wrappedSum != nativeSum) throw new AssertionError("work omitted");
        long nativeBytes = middle - before;
        long wrappedBytes = after - middle;
        if (nativeBytes != 0 || wrappedBytes != 0) throw new AssertionError("prepared allocation: " + nativeBytes + "/" + wrappedBytes);
        System.out.println("prepared operations=100000 each; nativeBytes=" + nativeBytes
                + "; forwardingBytes=" + wrappedBytes + "; checksum=" + wrappedSum);
    }
}
