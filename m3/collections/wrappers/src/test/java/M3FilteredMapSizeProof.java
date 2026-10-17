// Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
import com.m3.collections.M3FilteredMaps;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/** Bounded-storage virtual map tests saturated cardinality and the complete callback traversal. */
public final class M3FilteredMapSizeProof {
    private static final long COUNT = (long) Integer.MAX_VALUE + 1;
    private static long visits;
    private M3FilteredMapSizeProof() { }
    private static final class VirtualMap extends AbstractMap<Long, Integer> {
        private final long count;
        VirtualMap(long count) { this.count = count; }
        @Override public Set<Entry<Long, Integer>> entrySet() {
            return new AbstractSet<>() {
                @Override public int size() { return (int) Math.min(count, Integer.MAX_VALUE); }
                @Override public Iterator<Entry<Long, Integer>> iterator() {
                    return new Iterator<>() {
                        private long next;
                        @Override public boolean hasNext() { return next < count; }
                        @Override public Entry<Long, Integer> next() {
                            if (!hasNext()) throw new NoSuchElementException();
                            return new SimpleImmutableEntry<>(next++, 0);
                        }
                    };
                }
            };
        }
    }
    public static void main(String[] args) {
        boolean expectOldOverflow = args.length == 1 && args[0].equals("old");
        Map<Long, Integer> view = M3FilteredMaps.filtered(new VirtualMap(COUNT), (key, value) -> {
            visits++;
            return true;
        });
        int actual = view.size();
        int expected = expectOldOverflow ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        if (actual != expected || visits != COUNT) throw new AssertionError("size=" + actual + " visits=" + visits);
        if (!expectOldOverflow) {
            visits = 0;
            Map<Long, Integer> failing = M3FilteredMaps.filtered(new VirtualMap(COUNT), (key, value) -> {
                if (++visits == COUNT) throw new IllegalArgumentException("late predicate");
                return true;
            });
            try {
                failing.size();
                throw new AssertionError("saturation skipped the late predicate failure");
            } catch (IllegalArgumentException expectedFailure) {
                if (!expectedFailure.getMessage().equals("late predicate") || visits != COUNT) throw new AssertionError(expectedFailure);
            }
        }
        System.out.println("M3_FILTERED_MAP_SIZE_PROOF mode=" + (expectOldOverflow ? "old-overflow" : "candidate")
                + " size=" + actual + " entries=" + COUNT + " complete_scan=true late_failure_checked=" + !expectOldOverflow);
    }
}
