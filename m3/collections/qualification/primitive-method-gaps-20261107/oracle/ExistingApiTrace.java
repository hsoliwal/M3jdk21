/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import java.util.SplittableRandom;

/**
 * Before/after differential of the EXISTING API of the six owners touched by primitive-method-gaps-20261107.
 * Compiles against the master owners (it never names an absorbed member), so the same class file runs on the
 * master classes and on the head classes. Every observation (results, sizes, capacities, payload bytes,
 * iteration order, exception types) is folded into one 64-bit FNV-1a digest per owner. A seeded trace must
 * print identical digests on master and head, under the JIT and under -Xint.
 *
 * <p>usage: java -cp HEAD_OR_MASTER_CLASSES:ORACLE jdk.m3.collections.ExistingApiTrace [steps]</p>
 */
public final class ExistingApiTrace {
    private static final long SEED = 0x4558495354415049L;
    private static final long FNV_OFFSET = 0xcbf29ce484222325L;
    private static final long FNV_PRIME = 0x100000001b3L;
    private static final int DEFAULT_STEPS = 200_000;
    private static final int DENSE = 2_048;

    private long digest = FNV_OFFSET;
    private long observations;

    private ExistingApiTrace() {}

    private void see(long value) {
        long x = value;
        for (int b = 0; b < Long.BYTES; b++) {
            digest = (digest ^ (x & 0xFF)) * FNV_PRIME;
            x >>>= Byte.SIZE;
        }
        observations++;
    }

    private void see(boolean value) { see(value ? 1L : 0L); }

    private void see(long[] values) {
        see(values.length);
        for (long v : values) see(v);
    }

    private void see(int[] values) {
        see(values.length);
        for (int v : values) see(v);
    }

    private void fail(RuntimeException failure) {
        see(failure.getClass().getName().hashCode());
    }

    private static long value(SplittableRandom random) {
        return switch (random.nextInt(10)) {
            case 0 -> Long.MIN_VALUE;
            case 1 -> Long.MAX_VALUE;
            case 2 -> 0L;
            case 3 -> random.nextLong();
            default -> random.nextInt(DENSE) - DENSE / 2L;
        };
    }

    private void treeSet(int steps, SplittableRandom random) {
        M3LongTreeSet set = new M3LongTreeSet(random.nextInt(64), 4 + random.nextInt(8));
        for (int step = 0; step < steps; step++) {
            long v = value(random);
            switch (random.nextInt(14)) {
                case 0, 1, 2 -> see(set.add(v));
                case 3, 4 -> see(set.remove(v));
                case 5 -> see(set.contains(v));
                case 6 -> { see(set.floor(v, 7)); see(set.ceiling(v, 7)); see(set.lower(v, 7)); see(set.higher(v, 7)); }
                case 7 -> { try { see(set.first()); see(set.last()); } catch (RuntimeException e) { fail(e); } }
                case 8 -> { if ((step & 1023) == 0) see(set.filterInPlace(x -> (x & 3) != 1)); }
                case 9 -> { if ((step & 511) == 0) set.trimToSize(); }
                case 10 -> { if ((step & 8191) == 0) set.clear(); }
                case 11 -> { see(set.size()); see(set.capacity()); see(set.pageCount()); see(set.payloadBytes()); }
                case 12 -> { if ((step & 255) == 0) { see(set.toArray()); see(set.longSpliterator().estimateSize()); } }
                default -> {
                    M3LongIterator it = set.iterator();
                    try { if (it.hasNext()) see(it.nextLong()); set.add(v + 1); see(it.hasNext()); } catch (RuntimeException e) { fail(e); }
                }
            }
        }
        see(set.toArray());
    }

    private void treeMap(int steps, SplittableRandom random) {
        M3LongLongTreeMap map = new M3LongLongTreeMap(random.nextInt(32));
        for (int step = 0; step < steps; step++) {
            long k = value(random);
            long v = random.nextLong();
            switch (random.nextInt(14)) {
                case 0, 1, 2 -> see(map.put(k, v));
                case 3, 4 -> see(map.remove(k));
                case 5 -> { see(map.containsKey(k)); see(map.getOrDefault(k, 9)); }
                case 6 -> { try { see(map.getOrThrow(k)); } catch (RuntimeException e) { fail(e); } }
                case 7 -> { see(map.floorKey(k, 7)); see(map.ceilingKey(k, 7)); see(map.lowerKey(k, 7)); see(map.higherKey(k, 7)); }
                case 8 -> { try { see(map.firstKey()); see(map.lastKey()); see(map.pollFirstKey()); see(map.pollLastKey()); } catch (RuntimeException e) { fail(e); } }
                case 9 -> putSorted(map, random);
                case 10 -> { if ((step & 511) == 0) map.trimToSize(); if ((step & 8191) == 0) map.clear(); }
                case 11 -> { see(map.size()); see(map.isEmpty()); see(map.capacity()); }
                case 12 -> { if ((step & 255) == 0) { see(map.keysToArray()); see(map.valuesToArray()); map.forEach((a, b) -> see(a ^ b)); } }
                default -> {
                    map.forEachInRange(k, k + 64, (a, b) -> see(a + b));
                    M3LongIterator it = map.keyIterator();
                    try { if (it.hasNext()) see(it.nextLong()); map.put(v, k); see(it.hasNext()); } catch (RuntimeException e) { fail(e); }
                }
            }
        }
        see(map.keysToArray());
        see(map.valuesToArray());
    }

    private void putSorted(M3LongLongTreeMap map, SplittableRandom random) {
        int n = random.nextInt(24);
        long[] keys = new long[n];
        long[] values = new long[n];
        for (int i = 0; i < n; i++) {
            keys[i] = value(random);
            values[i] = random.nextLong();
        }
        if (random.nextInt(4) != 0) java.util.Arrays.sort(keys);
        try { see(map.putAllSorted(keys, random.nextInt(16) == 0 ? new long[n + 1] : values)); } catch (RuntimeException e) { fail(e); }
        see(map.capacity());
    }

    private void intInt(int steps, SplittableRandom random) {
        M3IntIntHashMap map = new M3IntIntHashMap(random.nextInt(32));
        for (int step = 0; step < steps; step++) {
            int k = random.nextInt(8) == 0 ? 0 : random.nextInt(DENSE) - DENSE / 2;
            int v = random.nextInt();
            switch (random.nextInt(12)) {
                case 0, 1, 2 -> see(map.put(k, v));
                case 3 -> see(map.remove(k));
                case 4 -> see(map.remove(k, map.getOrDefault(k, 0)));
                case 5 -> { see(map.getOrDefault(k, 3)); see(map.containsKey(k)); see(map.containsValue(v)); }
                case 6 -> { see(map.replace(k, map.getOrDefault(k, 1), v)); see(map.addTo(k, v)); }
                case 7 -> { see(map.size()); see(map.isEmpty()); see(map.capacity()); see(map.payloadBytes()); }
                case 8 -> { if ((step & 255) == 0) { see(map.keysToArray()); map.forEach((a, b) -> see(a * 31L + b)); } }
                case 9 -> { if ((step & 8191) == 0) map.clear(); }
                default -> {
                    M3IntIterator it = map.keyIterator();
                    try { if (it.hasNext()) see(it.nextInt()); map.put(v, k); see(it.hasNext()); } catch (RuntimeException e) { fail(e); }
                }
            }
        }
        see(map.keysToArray());
    }

    private void intLong(int steps, SplittableRandom random) {
        M3IntLongHashMap map = new M3IntLongHashMap(random.nextInt(32));
        see(M3IntLongHashMap.payloadBytesFor(random.nextInt(1_000)));
        for (int step = 0; step < steps; step++) {
            int k = random.nextInt(8) == 0 ? 0 : random.nextInt(DENSE) - DENSE / 2;
            long v = random.nextLong();
            switch (random.nextInt(12)) {
                case 0, 1, 2 -> see(map.put(k, v));
                case 3 -> see(map.remove(k));
                case 4 -> see(map.remove(k, map.getOrDefault(k, 0L)));
                case 5 -> { see(map.getOrDefault(k, 3)); see(map.containsKey(k)); see(map.containsValue(v)); }
                case 6 -> { see(map.replace(k, map.getOrDefault(k, 1L), v)); see(map.addTo(k, v)); }
                case 7 -> { see(map.size()); see(map.isEmpty()); see(map.capacity()); see(map.payloadBytes()); }
                case 8 -> { if ((step & 255) == 0) { see(map.keysToArray()); map.forEach((a, b) -> see(a * 31L + b)); } }
                case 9 -> { if ((step & 8191) == 0) map.clear(); }
                default -> {
                    M3IntIterator it = map.keyIterator();
                    try { if (it.hasNext()) see(it.nextInt()); map.put((int) v, k); see(it.hasNext()); } catch (RuntimeException e) { fail(e); }
                }
            }
        }
        see(map.keysToArray());
    }

    private void longLong(int steps, SplittableRandom random) {
        M3LongLongHashMap map = new M3LongLongHashMap(random.nextInt(32));
        for (int step = 0; step < steps; step++) {
            long k = value(random);
            long v = random.nextLong();
            switch (random.nextInt(12)) {
                case 0, 1, 2 -> see(map.put(k, v));
                case 3 -> see(map.remove(k));
                case 4 -> see(map.remove(k, map.getOrDefault(k, 0L)));
                case 5 -> { see(map.getOrDefault(k, 3)); see(map.containsKey(k)); see(map.containsValue(v)); }
                case 6 -> { see(map.replace(k, map.getOrDefault(k, 1L), v)); see(map.addTo(k, v)); }
                case 7 -> { see(map.size()); see(map.isEmpty()); see(map.capacity()); }
                case 8 -> { if ((step & 255) == 0) { see(map.keysToArray()); map.forEach((a, b) -> see(a * 31L + b)); } }
                case 9 -> { if ((step & 8191) == 0) map.clear(); }
                default -> {
                    M3LongIterator it = map.keyIterator();
                    try { if (it.hasNext()) see(it.nextLong()); map.put(v, k); see(it.hasNext()); } catch (RuntimeException e) { fail(e); }
                }
            }
        }
        see(map.keysToArray());
    }

    private void deque(int steps, SplittableRandom random) {
        M3IntArrayDeque deque = new M3IntArrayDeque(random.nextInt(16) - 2);
        for (int step = 0; step < steps; step++) {
            int v = random.nextInt();
            try {
                switch (random.nextInt(13)) {
                    case 0, 1 -> deque.addLast(v);
                    case 2 -> deque.addFirst(v);
                    case 3 -> deque.push(v);
                    case 4 -> see(deque.removeFirst());
                    case 5 -> see(deque.removeLast());
                    case 6 -> see(deque.pop());
                    case 7 -> { see(deque.peekFirst()); see(deque.peekLast()); see(deque.getFirst()); see(deque.getLast()); see(deque.element()); }
                    case 8 -> see(deque.remove());
                    case 9 -> { int[] batch = random.ints(random.nextInt(9)).toArray(); see(deque.addAll(batch, 0, batch.length)); }
                    case 10 -> { see(deque.size()); see(deque.isEmpty()); see(deque.capacity()); see(deque.payloadBytes()); }
                    case 11 -> { if ((step & 127) == 0) { see(deque.toArray()); deque.forEach(x -> see(x)); } }
                    default -> { if ((step & 4095) == 0) deque.clear(); }
                }
            } catch (RuntimeException e) {
                fail(e);
            }
        }
        see(deque.toArray());
    }

    private interface Owner { void run(ExistingApiTrace trace, int steps, SplittableRandom random); }

    public static void main(String[] args) {
        int steps = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_STEPS;
        String[] names = {"M3LongTreeSet", "M3LongLongTreeMap", "M3IntIntHashMap", "M3IntLongHashMap",
            "M3LongLongHashMap", "M3IntArrayDeque"};
        Owner[] owners = {ExistingApiTrace::treeSet, ExistingApiTrace::treeMap, ExistingApiTrace::intInt,
            ExistingApiTrace::intLong, ExistingApiTrace::longLong, ExistingApiTrace::deque};
        for (int i = 0; i < owners.length; i++) {
            ExistingApiTrace trace = new ExistingApiTrace();
            owners[i].run(trace, steps, new SplittableRandom(SEED + i));
            System.out.printf("%s\t%d\t%d\t%016x%n", names[i], steps, trace.observations, trace.digest);
        }
    }
}
