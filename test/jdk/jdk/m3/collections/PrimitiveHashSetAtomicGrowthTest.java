/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * @test
 * @summary PrimitiveHashSet avoids zero-only growth and retains exact-width membership
 * @modules jdk.m3.collections/jdk.m3.collections
 * @run main/othervm PrimitiveHashSetAtomicGrowthTest
 */
import java.lang.reflect.Array;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import jdk.m3.collections.PrimitiveHashSet;
import jdk.m3.collections.PrimitiveKind;

/** Actual-M3-owner differential; standalone main and jtreg-compatible. */
public class PrimitiveHashSetAtomicGrowthTest {
    private static long assertions;
    private static void check(boolean condition, String reason) {
        assertions++;
        if (!condition) throw new AssertionError(reason);
    }
    private static void verify(PrimitiveHashSet actual, Set<Long> model, PrimitiveKind kind) {
        check(actual.size() == model.size(), kind + " size");
        Set<Long> fromArray = new HashSet<>();
        Object values = actual.toPrimitiveArray();
        check(Array.getLength(values) == model.size(), kind + " exact array");
        for (int i = 0; i < Array.getLength(values); i++) {
            long bits = kind.normalizeBits(kind.readBits(values, i));
            check(fromArray.add(bits), kind + " duplicate array element");
        }
        check(fromArray.equals(model), kind + " contents");
        Set<Long> iterated = new HashSet<>();
        actual.forEachBits(value ->
                check(iterated.add(kind.normalizeBits(value)), kind + " duplicate iteration"));
        check(iterated.equals(model), kind + " forEach content");
    }
    private static void zeroDoesNotGrow(PrimitiveKind kind) {
        PrimitiveHashSet set = new PrimitiveHashSet(kind, 0, 0.65f);
        int starting = set.capacity();
        if (kind == PrimitiveKind.BOOLEAN) {
            check(set.addBits(1), "boolean one");
            check(set.addBits(0), "boolean zero");
            check(set.capacity() == starting, "boolean occupancy");
        } else {
            for (int i = 1; i <= 5; i++) check(set.addBits(i), kind + " initial slot " + i);
            check(set.capacity() == starting, kind + " before zero");
            check(set.addBits(0), kind + " zero insert");
            check(!set.addBits(0), kind + " zero duplicate");
            check(set.capacity() == starting, kind + " independent zero caused growth");
            for (int i = 1; i <= 5; i++) check(set.containsBits(i), kind + " preserved " + i);
            check(set.addBits(6), kind + " nonzero threshold crossing");
            check(set.capacity() > starting, kind + " real key grew table");
        }
        check(set.containsBits(0), kind + " zero present");
        check(set.removeBits(0), kind + " zero removed");
        check(!set.containsBits(0), kind + " zero absent");
        check(!set.removeBits(0), kind + " zero duplicate removal");
    }
    private static void randomized(PrimitiveKind kind, int seed) {
        PrimitiveHashSet actual = new PrimitiveHashSet(kind, 0, 0.65f);
        Set<Long> model = new HashSet<>();
        Random random = new Random(0x72a9f0cL + seed * 0x61c88647L);
        long[] edges = {0L, 1L, 2L, -1L, Long.MIN_VALUE, Long.MAX_VALUE,
                0x80000000L, 0x7fffffffL, 0x7fc00001L,
                0x7fc00002L, 0x7ff8000000000001L, 0x7ff8000000000002L};
        for (int step = 0; step < 12_000; step++) {
            long bits = switch (random.nextInt(4)) {
                case 0 -> edges[random.nextInt(edges.length)];
                case 1 -> random.nextInt(256) - 128;
                case 2 -> random.nextInt(2048);
                default -> random.nextLong();
            };
            long canonical = kind.normalizeBits(bits);
            switch (random.nextInt(4)) {
                case 0, 1 -> check(actual.addBits(bits) == model.add(canonical),
                        kind + " add step " + step);
                case 2 -> check(actual.removeBits(bits) == model.remove(canonical),
                        kind + " remove step " + step);
                default -> check(actual.containsBits(bits) == model.contains(canonical),
                        kind + " contains step " + step);
            }
            check(actual.size() == model.size(), kind + " size step " + step);
            if (step % 127 == 0) verify(actual, model, kind);
        }
        verify(actual, model, kind);
        Object detached = actual.toPrimitiveArray();
        if (Array.getLength(detached) > 0) {
            long retained = kind.readBits(detached, 0);
            kind.writeBits(detached, 0, 0xabcdefL);
            verify(actual, model, kind);
            check(actual.containsBits(retained), kind + " detached copy");
        }
        actual.clear();
        check(actual.size() == 0 && actual.isEmpty(), kind + " cleared");
        check(actual.addBits(0) && actual.containsBits(0), kind + " reuse after clear");
    }
    public static void main(String[] args) {
        int seed = 0;
        for (PrimitiveKind kind : PrimitiveKind.values()) {
            zeroDoesNotGrow(kind);
            randomized(kind, seed++);
        }
        System.out.println("PASS PrimitiveHashSet zero/staged-growth exact-kind oracle: "
                + assertions + " assertions");
    }
}
