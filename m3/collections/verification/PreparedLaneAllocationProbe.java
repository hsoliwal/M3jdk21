/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0 */
import com.m3.collections.M3ConcurrentIntLane28;
import com.m3.collections.M3ConcurrentLongLane28;
import com.m3.collections.M3LongLane28;
import java.lang.management.ManagementFactory;

/** Measures only warmed, prepared scalar CAS and page-copy loops on the current JVM. */
public final class PreparedLaneAllocationProbe {
    private static volatile long sink;
    private PreparedLaneAllocationProbe() { }
    public static void main(String[] args) {
        var bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        if (!bean.isThreadAllocatedMemorySupported()) throw new IllegalStateException("allocation counters unavailable");
        bean.setThreadAllocatedMemoryEnabled(true);
        M3ConcurrentLongLane28 longs = new M3ConcurrentLongLane28();
        M3ConcurrentIntLane28 ints = new M3ConcurrentIntLane28();
        M3LongLane28 bulk = new M3LongLane28();
        longs.prepare(0, 8192); ints.prepare(0, 8192); bulk.fill(0, 8192, 7);
        for (int i = 0; i < 12; i++) exercise(longs, ints, bulk);
        long thread = Thread.currentThread().threadId();
        for (int trial = 0; trial < 5; trial++) {
            long before = bean.getThreadAllocatedBytes(thread);
            exercise(longs, ints, bulk);
            long bytes = bean.getThreadAllocatedBytes(thread) - before;
            System.out.println("prepared trial=" + trial + " bytes=" + bytes);
            if (bytes != 0) throw new AssertionError("prepared loop allocated " + bytes);
        }
        System.out.println("allocation PASS; applies only to this prepared loop/JVM, not streams or page growth");
    }
    private static void exercise(M3ConcurrentLongLane28 longs, M3ConcurrentIntLane28 ints, M3LongLane28 bulk) {
        long sum = 0;
        for (int i = 0; i < 200000; i++) {
            int slot = i & 8191;
            sum += longs.getAndAdd(slot, 1);
            sum += ints.getAndAdd(slot, 1);
            sum += longs.getAcquire(slot);
            longs.compareAndSet(slot, -1, 0);
        }
        for (int i = 0; i < 5000; i++) bulk.copyFrom(bulk, 0, 1, 8191);
        sink = sum + bulk.get(8191);
    }
}
