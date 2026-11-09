// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;
import jdk.internal.m3.collections.packed.PackedLongInlineList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

/** Independent mutable-list oracle for the real, complete collection owner. */
//FLLDCIM: RELOCATE - donor blob ce54b87a83c7 at d5ad5090d87e; recipe m3/collections/qualification/census2-t2-20261106/relocate.py v6
public final class InlineOwnerBehaviorProbe {
    private static long trace = 0xcbf29ce484222325L;
    private static long assertions;

    private static void record(long value) {
        trace = (trace ^ value) * 0x100000001b3L;
    }

    private static void verify(PackedLongInlineList list, List<Long> model,
                               long query, int phase, int step) {
        if (list.size() != model.size()) throw bad("size", phase, step);
        long[] expected = new long[model.size()];
        for (int i = 0; i < expected.length; i++) {
            expected[i] = model.get(i);
            if (list.get(i) != expected[i]) throw bad("get", phase, step);
            assertions++;
        }
        if (!Arrays.equals(list.toArray(), expected)) throw bad("toArray", phase, step);
        assertions++;
        for (long key : new long[]{query, 0L, Long.MIN_VALUE, Long.MAX_VALUE,
                                  expected.length == 0 ? 123456789L : expected[expected.length / 2]}) {
            if (list.indexOf(key) != model.indexOf(key)) throw bad("indexOf", phase, step);
            if (list.lastIndexOf(key) != model.lastIndexOf(key)) throw bad("lastIndexOf", phase, step);
            if (list.contains(key) != model.contains(key)) throw bad("contains", phase, step);
            assertions += 3;
        }
        long[] scanned = new long[expected.length];
        var cursor = list.iterator();
        for (int i = 0; i < expected.length; i++) {
            if (!cursor.hasNext()) throw bad("iterator early end", phase, step);
            scanned[i] = cursor.nextLong();
        }
        if (cursor.hasNext() || !Arrays.equals(scanned, expected)) {
            throw bad("iterator", phase, step);
        }
        assertions++;
        if ((step % 31) == 0) {
            if (!Arrays.equals(list.longStream().toArray(), expected)) {
                throw bad("longStream", phase, step);
            }
            if (!Arrays.equals(list.parallelLongStream().toArray(), expected)) {
                throw bad("parallelLongStream", phase, step);
            }
            assertions += 2;
        }
        record(list.size());
        // Heap-pressure-aware capacity can vary between separate JVM processes;
        // assert its contract without folding residency into the behavior digest.
        if (list.capacity() < list.size() || (list.spilled() && list.capacity() < 5)) {
            throw bad("capacity/spill invariant", phase, step);
        }
        assertions++;
        for (long v : expected) record(v);
        record(list.indexOf(query));
    }

    private static AssertionError bad(String reason, int phase, int step) {
        return new AssertionError(reason + " phase=" + phase + " step=" + step);
    }

    public static void main(String[] args) {
        int iterations = Integer.parseInt(args[0]);
        Random random = new Random(0x4d33496e6c696e65L);
        for (int phase = 0; phase < 3; phase++) {
            PackedLongInlineList list = new PackedLongInlineList(phase == 0 ? 0 : phase == 1 ? 4 : 12);
            ArrayList<Long> model = new ArrayList<>();
            for (int step = 0; step < iterations; step++) {
                int action = random.nextInt(17);
                if (model.size() > 96) action = 7;
                long value = switch (random.nextInt(9)) {
                    case 0 -> Long.MIN_VALUE;
                    case 1 -> Long.MAX_VALUE;
                    case 2 -> 0L;
                    case 3 -> -1L;
                    default -> random.nextInt(41) - 20L;
                };
                switch (action) {
                    case 0, 1 -> { list.add(value); model.add(value); }
                    case 2 -> {
                        int count = random.nextInt(7);
                        long[] added = new long[count];
                        for (int i = 0; i < count; i++) added[i] = random.nextInt(31) - 15L;
                        list.addAll(added);
                        for (long v : added) model.add(v);
                    }
                    case 3 -> {
                        int at = random.nextInt(model.size() + 1);
                        list.insert(at, value); model.add(at, value);
                    }
                    case 4 -> {
                        boolean actual = list.removeValue(value);
                        boolean expected = model.remove(Long.valueOf(value));
                        if (actual != expected) throw bad("removeValue", phase, step);
                    }
                    case 5 -> {
                        if (model.isEmpty()) break;
                        int at = random.nextInt(model.size());
                        long actual = list.removeAt(at), expected = model.remove(at);
                        if (actual != expected) throw bad("removeAt", phase, step);
                    }
                    case 6 -> {
                        if (model.isEmpty()) break;
                        int at = random.nextInt(model.size());
                        long actual = list.set(at, value), expected = model.set(at, value);
                        if (actual != expected) throw bad("set", phase, step);
                    }
                    case 7 -> { list.clear(); model.clear(); }
                    case 8 -> {
                        list.sort(); model.sort(null);
                        int before = model.size();
                        int actual = list.deduplicateSorted();
                        ArrayList<Long> dedup = new ArrayList<>();
                        for (long v : model) {
                            if (dedup.isEmpty() || dedup.get(dedup.size() - 1) != v) dedup.add(v);
                        }
                        model = dedup;
                        if (actual != before - model.size()) throw bad("dedup", phase, step);
                    }
                    case 9 -> {
                        int before = model.size();
                        int actual = list.filterInPlace(v -> (v & 3L) != 0L);
                        model.removeIf(v -> (v & 3L) == 0L);
                        if (actual != before - model.size()) throw bad("filter", phase, step);
                    }
                    case 10 -> list.trimToSize();
                    case 11 -> {
                        boolean actual = list.addIf(value, v -> (v & 1L) == 0L);
                        boolean expected = (value & 1L) == 0L;
                        if (expected) model.add(value);
                        if (actual != expected) throw bad("addIf", phase, step);
                    }
                    case 12 -> { list.push(value); model.add(value); }
                    case 13 -> {
                        if (model.isEmpty()) {
                            try { list.pop(); throw bad("empty pop did not throw", phase, step); }
                            catch (NoSuchElementException expected) { assertions++; }
                            break;
                        }
                        long actual = list.pop(), expected = model.remove(model.size() - 1);
                        if (actual != expected) throw bad("pop", phase, step);
                    }
                    case 14 -> {
                        long[] payload = new long[model.size()];
                        for (int i = 0; i < payload.length; i++) payload[i] = model.get(i);
                        PackedLongInlineList copy = PackedLongInlineList.copyOf(payload);
                        if (!Arrays.equals(copy.toArray(), payload)) throw bad("copyOf", phase, step);
                        assertions++;
                    }
                    case 15 -> list.ensureCapacity(model.size() + random.nextInt(5));
                    case 16 -> {
                        if (!model.isEmpty() && list.peek() != model.get(model.size() - 1)) {
                            throw bad("peek", phase, step);
                        }
                    }
                    default -> throw bad("action", phase, step);
                }
                verify(list, model, value, phase, step);
            }
        }
        System.out.println("M3_FULL_OWNER_PARITY_PASS assertions=" + assertions
                + " trace=" + Long.toUnsignedString(trace, 16));
    }
}
