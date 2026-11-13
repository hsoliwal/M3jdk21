// SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
// Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
//
// LANE28_DIFFERENTIAL v1 (2026-11-09): drives jdk.internal.m3.collections.bits.SegmentedBitLane28 and
// jdk.m3.collections.BitLane28 with the same random operation stream and compares every public observation
// (values and exception classes) after each step. Prints the number of comparisons and the first mismatch per method.
// Usage (classes of master ab022da1246d, which still has SegmentedBitLane28): java -cp <classes>;. LaneDiff <seed>
// <steps> [mutant]. With a third argument the BitLane28 side skips flip, which must produce mismatches.
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.function.Supplier;
import jdk.internal.m3.collections.bits.SegmentedBitLane28;
import jdk.m3.collections.BitLane28;

public final class LaneDiff {
    static final Map<String, String> FIRST = new TreeMap<>();
    static final Map<String, Integer> COUNT = new TreeMap<>();

    static String run(Supplier<Object> s) {
        try {
            return String.valueOf(s.get());
        } catch (RuntimeException e) {
            return "THROW " + e.getClass().getName();
        }
    }

    static void cmp(String m, Supplier<Object> a, Supplier<Object> b) {
        String x = run(a), y = run(b);
        COUNT.merge(m, 1, Integer::sum);
        if (!x.equals(y)) FIRST.putIfAbsent(m, x + " vs " + y);
    }

    static int index(Random r) {
        int k = r.nextInt(10);
        if (k < 6) return r.nextInt(1 << 14);
        if (k < 8) return (1 << 28) - 1 - r.nextInt(1 << 13);
        if (k < 9) return r.nextInt(1 << 28);
        return r.nextBoolean() ? -1 - r.nextInt(3) : (1 << 28) + r.nextInt(3);
    }

    public static void main(String[] args) {
        long seed = Long.parseLong(args[0]);
        int steps = Integer.parseInt(args[1]);
        Random r = new Random(seed);
        SegmentedBitLane28 s = new SegmentedBitLane28();
        BitLane28 b = new BitLane28();
        for (int step = 0; step < steps; step++) {
            int i = index(r), j = index(r);
            switch (r.nextInt(5)) {
                case 0 -> cmp("set", () -> { s.set(i); return 0; }, () -> { b.set(i); return 0; });
                case 1 -> cmp("clear", () -> { s.clear(i); return 0; }, () -> { b.clear(i); return 0; });
                case 2 -> { boolean v = r.nextBoolean();
                    cmp("setBool", () -> { s.set(i, v); return 0; }, () -> { b.set(i, v); return 0; }); }
                case 3 -> cmp("flip", () -> { s.flip(i); return 0; }, () -> { if (args.length < 3) b.flip(i); return 0; });
                default -> { }
            }
            cmp("get", () -> s.get(j), () -> b.get(j));
            cmp("cardinality", s::cardinality, b::cardinality);
            cmp("allocatedRegionCount", s::allocatedRegionCount, b::allocatedRegionCount);
            cmp("allocatedBlockCount", s::allocatedBlockCount, b::allocatedBlockCount);
            cmp("payloadBytes", s::payloadBytes, b::payloadBytes);
            cmp("nextSetBit", () -> s.nextSetBit(j), () -> b.nextSetBit(j));
            cmp("previousSetBit", () -> s.previousSetBit(j), () -> b.previousSetBit(j));
            int lo = Math.min(i, j), hi = Math.max(i, j);
            cmp("cardinalityRange", () -> s.cardinality(lo, hi), () -> b.cardinality(lo, hi));
            cmp("cardinalityRangeReversed", () -> s.cardinality(hi, lo), () -> b.cardinality(hi, lo));
            cmp("rank", () -> s.rank(j), () -> b.rank(j));
            int k = r.nextInt(Math.max(1, s.cardinality() + 2)) - 1;
            cmp("select", () -> s.select(k), () -> b.select(k));
        }
        System.out.println("LANE28_DIFFERENTIAL seed " + seed + " steps " + steps + " comparisons "
                + COUNT.values().stream().mapToInt(Integer::intValue).sum() + " mismatching_methods " + FIRST.size());
        FIRST.forEach((m, v) -> System.out.println("MISMATCH " + m + " " + v));
    }
}
