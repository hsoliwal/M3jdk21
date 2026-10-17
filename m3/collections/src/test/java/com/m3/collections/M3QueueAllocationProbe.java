// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.lang.management.ManagementFactory;

/** Prepared scalar mutation discriminator; no throughput or retained-heap claim. */
public final class M3QueueAllocationProbe {
    private M3QueueAllocationProbe() { }
    private static long exercise(M3LongQueue sequence, int count) {
        long total = 0;
        for (int index = 0; index < count; index++) {
            if (!sequence.offer(index)) throw new AssertionError("unexpected rejection");
            total += sequence.poll();
        }
        return total;
    }
    private static long exerciseBulk(M3LongQueue sequence, int count, long[] input, long[] target) {
        long total = 0;
        for (int index = 0; index < count; index++) {
            if (sequence.offerBatch(input, 0, 4) != 4 || sequence.drainTo(target, 0, 4) != 4)
                throw new AssertionError("bulk prefix lost");
            total += target[0] + target[1] + target[2] + target[3];
        }
        return total;
    }
    public static void main(String[] args) {
        var bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        if (!bean.isThreadAllocatedMemorySupported()) throw new IllegalStateException("allocation counter unavailable");
        bean.setThreadAllocatedMemoryEnabled(true);
        M3LongArrayDeque owner = new M3LongArrayDeque(8);
        M3LongQueue wrapped = new M3ForwardingLongQueue() {
            @Override protected M3LongQueue delegate() { return owner; }
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
        long[] input = {1, 2, 3, 4}, target = new long[4];
        exerciseBulk(owner, 200000, input, target);
        exerciseBulk(wrapped, 200000, input, target);
        long bulkBefore = bean.getThreadAllocatedBytes(thread);
        long nativeBulkSum = exerciseBulk(owner, 100000, input, target);
        long bulkMiddle = bean.getThreadAllocatedBytes(thread);
        long wrappedBulkSum = exerciseBulk(wrapped, 100000, input, target);
        long bulkAfter = bean.getThreadAllocatedBytes(thread);
        if (nativeBulkSum != 1000000L || wrappedBulkSum != nativeBulkSum) throw new AssertionError("bulk work omitted");
        long nativeBulkBytes = bulkMiddle - bulkBefore, wrappedBulkBytes = bulkAfter - bulkMiddle;
        if (nativeBulkBytes != 0 || wrappedBulkBytes != 0)
            throw new AssertionError("prepared bulk allocation: " + nativeBulkBytes + "/" + wrappedBulkBytes);
        System.out.println("prepared batches=100000 each; nativeBulkBytes=" + nativeBulkBytes
                + "; forwardingBulkBytes=" + wrappedBulkBytes + "; checksum=" + wrappedBulkSum);
    }
}
