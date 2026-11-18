/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * @test
 * @summary Primitive delay heap uses dynamic offset lanes, recycled slots and live bit masks
 * @modules jdk.m3.collections/jdk.internal.m3.collections
 * @run main/othervm --add-opens=jdk.m3.collections/jdk.internal.m3.collections=ALL-UNNAMED DelayLongQueueArrayHeapTest
 */
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.Random;
import jdk.internal.m3.collections.DelayLongQueue;

public final class DelayLongQueueArrayHeapTest {
    private record Entry(long id, long deadline, long order) { }

    private static long checks;

    public static void main(String[] args) throws Exception {
        emptyAndBoundaries();
        ordinalBoundaries();
        randomizedDifferential();
        recyclingAndMasks();
        System.out.println("DelayLongQueueArrayHeapTest PASS " + checks + " assertions");
    }

    private static void emptyAndBoundaries() throws Exception {
        DelayLongQueue q = new DelayLongQueue();
        check(q.size() == 0, "initial size");
        check(q.pollOrDefault(0L, 17L) == 17L, "empty default");
        expect(NoSuchElementException.class, q::peekDeadline);
        check(!q.remove(2), "missing removal");
        check(q.offer(77, Long.MAX_VALUE), "offer max");
        check(q.peekDeadline() == Long.MAX_VALUE, "max deadline");
        check(q.pollOrDefault(Long.MAX_VALUE - 1L, 123L) == 123L, "not due");
        check(q.size() == 1, "not due size");
        check(q.pollOrDefault(Long.MAX_VALUE, 123L) == 77L, "due max");
        check(q.offer(Long.MIN_VALUE, Long.MIN_VALUE), "offer min");
        check(q.pollOrDefault(Long.MIN_VALUE, 9L) == Long.MIN_VALUE, "due min");
        check(q.offer(1, 1), "tie 1");
        check(q.offer(2, 1), "tie 2");
        check(q.offer(3, 1), "tie 3");
        check(q.pollOrDefault(1, -1) == 1, "first tie");
        check(q.pollOrDefault(1, -1) == 2, "second tie");
        check(q.pollOrDefault(1, -1) == 3, "third tie");
        check(q.size() == 0, "drained");
        check(DelayLongQueue.scaffoldDescriptor().equals(
                "M3DelayLongQueue|DELAY_LONG_QUEUE|LONG|EXPIRED|long[] payloadLane;long[] deadlineLane"),
                "existing descriptor");
        verifyStorage(q);
    }

    /** Compress a reachable long-running history without executing 2^63 offers. */
    private static void ordinalBoundaries() throws Exception {
        Field ordinal = field(DelayLongQueue.class, "nextInsertion");
        DelayLongQueue q = new DelayLongQueue();
        // An old equal-deadline item remains live while newer offers approach overflow.
        q.offer(1, 7);
        ordinal.setLong(q, Long.MAX_VALUE);
        q.offer(2, 7);
        q.offer(3, 7);
        q.offer(4, 7);
        for (int expected = 1; expected <= 4; expected++) {
            check(q.pollOrDefault(7, -1) == expected, "stable old and rollover ordinals");
        }
        verifyStorage(q);

        // A drained queue can reset without allocating a rank array.
        ordinal.setLong(q, Long.MAX_VALUE);
        q.offer(5, Long.MIN_VALUE);
        q.offer(6, Long.MIN_VALUE);
        check(q.pollOrDefault(Long.MIN_VALUE, -1) == 5, "empty rollover first");
        check(q.pollOrDefault(Long.MIN_VALUE, -1) == 6, "empty rollover second");

        // Holes, free-slot reuse and mixed deadlines must survive repeated normalization.
        q.offer(10, 20);
        q.offer(11, 10);
        q.offer(12, 20);
        q.offer(13, 10);
        check(q.remove(11), "make free slot before ordinal normalization");
        ordinal.setLong(q, Long.MAX_VALUE - 1);
        q.offer(14, 10);
        q.offer(15, 20);
        verifyStorage(q);
        ordinal.setLong(q, Long.MAX_VALUE);
        q.offer(16, 20);
        verifyStorage(q);
        for (long expected : new long[]{13, 14, 10, 12, 15, 16}) {
            check(q.pollOrDefault(Long.MAX_VALUE, -1) == expected,
                    "normalization preserves deadlines and relative offer order");
        }
        verifyStorage(q);

        // Normalize at live-mask word boundaries without changing slot/heap geometry.
        for (int count : new int[]{31, 32, 33, 63, 64, 65, 127, 128, 129}) {
            q.clear();
            for (int i = 0; i < count; i++) q.offer(i, Long.MAX_VALUE);
            for (int i = 0; i < count; i += 3) check(q.remove(i), "remove before rank packing");
            ordinal.setLong(q, Long.MAX_VALUE);
            q.offer(count, Long.MAX_VALUE);
            verifyStorage(q);
            for (int i = 0; i < count; i++) {
                if (i % 3 != 0) check(q.pollOrDefault(Long.MAX_VALUE, -1) == i, "ranked live order");
            }
            check(q.pollOrDefault(Long.MAX_VALUE, -1) == count, "normalized newest item last");
            verifyStorage(q);
        }
    }

    private static void randomizedDifferential() throws Exception {
        for (int seed = 0; seed < 32; seed++) {
            Random r = new Random(0x5EED1234L ^ (seed * 0x9E3779B97F4A7C15L));
            DelayLongQueue q = new DelayLongQueue();
            PriorityQueue<Entry> expected = new PriorityQueue<>((a, b) -> {
                int byDeadline = Long.compare(a.deadline, b.deadline);
                return byDeadline != 0 ? byDeadline : Long.compare(a.order, b.order);
            });
            Map<Long, Entry> live = new HashMap<>();
            long nextId = 1;
            for (int step = 0; step < 6_000; step++) {
                int operation = r.nextInt(100);
                if (operation < 53 || live.isEmpty()) {
                    long id = nextId++;
                    long deadline = switch (r.nextInt(9)) {
                        case 0 -> Long.MIN_VALUE;
                        case 1 -> Long.MAX_VALUE;
                        case 2 -> 0;
                        case 3 -> -1;
                        default -> r.nextInt(200) - 100;
                    };
                    Entry entry = new Entry(id, deadline, id);
                    expected.add(entry);
                    live.put(id, entry);
                    check(q.offer(id, deadline), "offer");
                } else if (operation < 75) {
                    long now = switch (r.nextInt(5)) {
                        case 0 -> Long.MAX_VALUE;
                        case 1 -> Long.MIN_VALUE;
                        default -> r.nextInt(300) - 150;
                    };
                    Entry head = expected.peek();
                    if (head == null || head.deadline > now) {
                        check(q.pollOrDefault(now, -1L) == -1L, "unexpired fallback");
                    } else {
                        check(q.pollOrDefault(now, -1L) == head.id, "expired ordering");
                        expected.remove();
                        live.remove(head.id);
                    }
                } else if (operation < 88) {
                    long key = 1 + r.nextInt((int) nextId + 3);
                    Entry found = live.remove(key);
                    check(q.remove(key) == (found != null), "remove presence");
                    if (found != null) check(expected.remove(found), "oracle remove");
                } else if (operation < 95) {
                    if (expected.isEmpty()) expect(NoSuchElementException.class, q::peekDeadline);
                    else check(q.peekDeadline() == expected.peek().deadline, "peek deadline");
                } else {
                    q.clear();
                    expected.clear();
                    live.clear();
                }
                check(q.size() == live.size(), "live cardinality");
                if ((step & 127) == 0) verifyStorage(q);
            }
            while (!expected.isEmpty()) {
                check(q.pollOrDefault(Long.MAX_VALUE, -1L) == expected.remove().id,
                        "drain after randomized operations");
            }
            check(q.size() == 0, "final size");
            verifyStorage(q);
        }
    }

    private static void recyclingAndMasks() throws Exception {
        DelayLongQueue q = new DelayLongQueue();
        for (int round = 0; round < 4; round++) {
            for (int i = 0; i < 65536; i++) check(q.offer(i, i % 257), "large offer");
            check(q.size() == 65536, "growth count");
            verifyStorage(q);
            for (int i = 0; i < 65536; i += 2) check(q.remove(i), "remove even");
            verifyStorage(q);
            check(q.size() == 32768, "half live");
            q.clear();
            verifyStorage(q);
        }
        // Duplicate values may be independently removed; no per-element object identity exists.
        for (int i = 0; i < 65; i++) q.offer(7, 100 + i);
        verifyStorage(q);
        for (int i = 0; i < 65; i++) check(q.remove(7), "duplicate remove");
        check(!q.remove(7), "duplicates exhausted");
        verifyStorage(q);
    }

    private static void verifyStorage(DelayLongQueue q) throws Exception {
        Class<?> owner = DelayLongQueue.class;
        long[] mask = (long[]) field(owner, "stateMaskWords").get(q);
        int[] heap = (int[]) field(owner, "heapOffsets").get(q);
        int[] positions = (int[]) field(owner, "heapPosition").get(q);
        long[] deadlines = (long[]) field(owner, "deadlineLane").get(q);
        long[] ordinals = (long[]) field(owner, "insertionLane").get(q);
        long[] payloads = (long[]) field(owner, "payloadLane").get(q);
        check(heap.length == positions.length && heap.length == deadlines.length
                && heap.length == ordinals.length && heap.length == payloads.length,
                "aligned dynamic lanes");
        check(mask.length == ((long) heap.length + 63) / 64, "one bit per slot");
        int bits = 0;
        for (long word : mask) bits += Long.bitCount(word);
        check(bits == q.size(), "bit cardinality");
        boolean[] seen = new boolean[heap.length];
        for (int i = 0; i < q.size(); i++) {
            int slot = heap[i];
            check(slot >= 0 && slot < heap.length && !seen[slot], "unique heap offset");
            seen[slot] = true;
            check((mask[slot >>> 6] & (1L << (slot & 63))) != 0, "live bit");
            check(positions[slot] == i, "reverse slot offset");
            if (i > 0) {
                int parent = heap[(i - 1) >>> 1];
                check(Long.compare(deadlines[parent], deadlines[slot]) <= 0,
                        "heap priority invariant");
                if (deadlines[parent] == deadlines[slot]) {
                    check(Long.compare(ordinals[parent] - ordinals[slot], 0L) <= 0,
                            "heap stable tie invariant");
                }
            }
        }
        for (Field field : owner.getDeclaredFields()) {
            if (!java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                check(field.getType().isArray() || field.getType().isPrimitive(),
                        "no retained object per entry");
            }
        }
    }

    private static Field field(Class<?> owner, String name) throws Exception {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static void check(boolean good, String label) {
        checks++;
        if (!good) throw new AssertionError(label);
    }

    private static void expect(Class<? extends Throwable> kind, Runnable action) {
        checks++;
        try { action.run(); }
        catch (Throwable problem) {
            if (kind.isInstance(problem)) return;
            throw new AssertionError("wrong exception: " + problem, problem);
        }
        throw new AssertionError("expected " + kind.getName());
    }
}
