// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project: Synexia-to-M3 package/type adaptation.
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.lang.management.ManagementFactory;
import java.util.PriorityQueue;
import java.util.function.BiConsumer;
import com.sun.management.ThreadMXBean;

/** Standalone prepared-payload allocation measurement; not a throughput benchmark. */
public final class M3AllocationProbe {
    private static volatile long sink;
    private static final class Payload implements Comparable<Payload> {
        private final int id;
        Payload(int id) { this.id = id; }
        @Override public int hashCode() { return id; }
        @Override public int compareTo(Payload other) { return Integer.compare(id, other.id); }
    }
    private static final BiConsumer<Payload, Payload> VISIT = (k, v) -> sink += k.id + v.id;
    private M3AllocationProbe() { }
    /** Run with a warmed Java 21 JVM and thread allocation accounting enabled. */
    public static void main(String[] arguments) {
        ThreadMXBean bean = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        if (!bean.isThreadAllocatedMemorySupported()) { throw new IllegalStateException("allocation accounting unavailable"); }
        bean.setThreadAllocatedMemoryEnabled(true);
        Payload key = new Payload(1), value = new Payload(2);
        var hash = new M3LinkedHashMap<Payload, Payload>(); var tree = new M3TreeMap<Payload, Payload>();
        var set = new M3LinkedHashSet<Payload>(); var list = new M3LinkedList<Payload>();
        var ring = new M3BlockingDeque<Payload>(16); var transfer = new M3TransferQueue<Payload>();
        var locked = new M3LockedMap<Payload, Payload>(); var weak = new M3WeakHashMap<Payload, Payload>();
        var heap = new PriorityQueue<Payload>(); weak.put(key, value);
        measure(bean, "hash-put-get-remove", () -> { hash.put(key, value); sink += hash.get(key).id; hash.remove(key); });
        measure(bean, "avl-put-get-remove", () -> { tree.put(key, value); sink += tree.get(key).id; tree.remove(key); });
        measure(bean, "ordered-set-endpoints", () -> { set.add(key); sink += set.getLast().id; set.removeFirst(); });
        measure(bean, "linked-add-poll", () -> { list.addLast(value); sink += list.pollFirst().id; });
        measure(bean, "blocking-ring-offer-poll", () -> { ring.offer(value); sink += ring.poll().id; });
        measure(bean, "transfer-buffer-offer-poll", () -> { transfer.offer(value); sink += transfer.poll().id; });
        measure(bean, "locked-map-put-get-remove", () -> { locked.put(key, value); sink += locked.get(key).id; locked.remove(key); });
        measure(bean, "weak-existing-get", () -> sink += weak.get(key).id);
        measure(bean, "jdk-heap-offer-poll", () -> { heap.offer(value); sink += heap.poll().id; });
        for (int i = 0; i < 16; i++) { Payload p = new Payload(i); hash.put(p, p); tree.put(p, p); }
        var treeKeys = tree.sequencedKeySet(); var treeValues = tree.sequencedValues();
        measure(bean, "avl-sequenced-key-value-endpoints", () -> { sink += treeKeys.getFirst().id; sink += treeValues.getLast().id; });
        measure(bean, "hash-direct-forEach-16", () -> hash.forEach(VISIT));
        measure(bean, "avl-direct-forEach-16", () -> tree.forEach(VISIT));
        System.out.println("sink=" + sink);
    }
    private static void measure(ThreadMXBean bean, String label, Runnable action) {
        for (int i = 0; i < 300000; i++) { action.run(); }
        long id = Thread.currentThread().threadId(); long best = Long.MAX_VALUE;
        for (int trial = 1; trial <= 3; trial++) {
            long before = bean.getThreadAllocatedBytes(id);
            for (int i = 0; i < 1000000; i++) { action.run(); }
            long bytes = bean.getThreadAllocatedBytes(id) - before; best = Math.min(best, bytes);
            System.out.println(label + "\t" + trial + "\t1000000\t" + bytes);
        }
        if (best != 0) { throw new AssertionError(label + " steady-state allocation: " + best); }
    }
}
