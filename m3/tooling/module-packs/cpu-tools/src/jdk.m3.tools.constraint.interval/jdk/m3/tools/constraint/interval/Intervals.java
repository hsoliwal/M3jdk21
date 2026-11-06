// Copyright 2026 Hitesh Soliwal and contributors
// Modified 2026-10-18: map the pinned Synexia CPU tool into its existing M3JDK 21 module name.
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.tools.constraint.interval;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
/** Half-open [start,end) time or offset intervals, no OS-specific clock dependency. */
public final class Intervals {
    private Intervals() { }
    public record Span(long start,long end) {
        public Span {if(end<start)throw new IllegalArgumentException("inverted interval");}
    }
    public static boolean intersects(Span a,Span b) {
        Objects.requireNonNull(a);Objects.requireNonNull(b);
        return a.start()<a.end()&&b.start()<b.end()&&a.start()<b.end()&&b.start()<a.end();
    }
    public static List<Integer> conflicting(List<Span> intervals,Span request){
        Objects.requireNonNull(intervals);Objects.requireNonNull(request);
        List<Integer> ids=new ArrayList<>();
        for(int i=0;i<intervals.size();i++)if(intersects(Objects.requireNonNull(intervals.get(i)),request))ids.add(i);
        return List.copyOf(ids);
    }
    /**
     * Canonical union of parallel primitive endpoint lanes. Inputs are borrowed
     * only for this call and are never mutated or retained. Empty spans disappear,
     * touching spans coalesce, and output is sorted and disjoint. At most one
     * million input or output intervals are admitted. The sort has primitive
     * index lanes, not one object/boxed comparator key per interval.
     */
    public static Packed union(long[] starts, long[] ends) {
        Objects.requireNonNull(starts, "starts");
        Objects.requireNonNull(ends, "ends");
        if (starts.length != ends.length || starts.length > Packed.MAX_INTERVALS)
            throw new IllegalArgumentException("interval lane geometry/budget");
        int n = starts.length;
        int[] order = new int[n], scratch = new int[n];
        for (int i = 0; i < n; i++) {
            if (ends[i] < starts[i]) throw new IllegalArgumentException("inverted interval");
            order[i] = i;
        }
        for (int width = 1; width < n; width *= 2) {
            for (int base = 0; base < n; base += 2 * width) {
                int middle = Math.min(n, base + width), limit = Math.min(n, base + 2 * width);
                int a = base, b = middle;
                for (int at = base; at < limit; at++) {
                    if (b == limit || a < middle && before(order[a], order[b], starts, ends))
                        scratch[at] = order[a++];
                    else scratch[at] = order[b++];
                }
            }
            int[] swap = order; order = scratch; scratch = swap;
        }
        PairLane result = new PairLane();
        for (int index : order) result.add(starts[index], ends[index]);
        return result.finish();
    }

    private static boolean before(int a, int b, long[] starts, long[] ends) {
        int compare = Long.compare(starts[a], starts[b]);
        return compare < 0 || compare == 0 && ends[a] <= ends[b];
    }

    /**
     * Immutable M3-style endpoint lane, not a retained List of Span objects.
     * Public projections copy primitive data. Every result is normalized;
     * methods never mutate either operand and use half-open signed-long ranges.
     */
    public static final class Packed {
        private static final int MAX_INTERVALS = 1_000_000;
        private final long[] pairs;
        private Packed(long[] pairs) { this.pairs = pairs; }
        public int size() { return pairs.length / 2; }
        public long start(int index) { return pairs[2 * Objects.checkIndex(index, size())]; }
        public long end(int index) { return pairs[2 * Objects.checkIndex(index, size()) + 1]; }
        public long[] starts() {
            long[] copy = new long[size()];
            for (int i = 0; i < copy.length; i++) copy[i] = pairs[2 * i];
            return copy;
        }
        public long[] ends() {
            long[] copy = new long[size()];
            for (int i = 0; i < copy.length; i++) copy[i] = pairs[2 * i + 1];
            return copy;
        }

        /** Linear merge of two normalized primitive lanes. */
        public Packed union(Packed other) {
            Objects.requireNonNull(other, "other");
            PairLane result = new PairLane();
            int a = 0, b = 0;
            while (a < size() || b < other.size()) {
                if (b == other.size() || a < size() && start(a) <= other.start(b)) {
                    result.add(start(a), end(a)); a++;
                } else {
                    result.add(other.start(b), other.end(b)); b++;
                }
            }
            return result.finish();
        }

        /** Linear exact intersection, with no arithmetic at the endpoint extremes. */
        public Packed intersection(Packed other) {
            Objects.requireNonNull(other, "other");
            PairLane result = new PairLane();
            int a = 0, b = 0;
            while (a < size() && b < other.size()) {
                long from = Math.max(start(a), other.start(b));
                long to = Math.min(end(a), other.end(b));
                if (from < to) result.add(from, to);
                if (end(a) < other.end(b)) a++;
                else b++;
            }
            return result.finish();
        }

        /** Exact set difference this-minus-other in ordered primitive lanes. */
        public Packed difference(Packed other) {
            Objects.requireNonNull(other, "other");
            PairLane result = new PairLane();
            int b = 0;
            for (int a = 0; a < size(); a++) {
                long cursor = start(a), limit = end(a);
                while (b < other.size() && other.end(b) <= cursor) b++;
                int k = b;
                while (k < other.size() && other.start(k) < limit) {
                    if (other.start(k) > cursor)
                        result.add(cursor, Math.min(limit, other.start(k)));
                    if (other.end(k) >= limit) { cursor = limit; break; }
                    cursor = Math.max(cursor, other.end(k));
                    k++;
                }
                if (cursor < limit) result.add(cursor, limit);
                b = k;
            }
            return result.finish();
        }

        /** Complement clipped to [from,to); does not invent infinities or closed endpoints. */
        public Packed complement(long from, long to) {
            requireRange(from, to);
            Packed domain = new Packed(from == to ? new long[0] : new long[]{from, to});
            return domain.difference(this);
        }

        /** Binary lookup of a single coordinate. The interval's end remains excluded. */
        public boolean contains(long coordinate) {
            int i = firstEndAfter(coordinate);
            return i < size() && start(i) <= coordinate;
        }

        /** Binary range overlap lookup; empty requests never intersect. */
        public boolean intersects(long from, long to) {
            requireRange(from, to);
            if (from == to) return false;
            int i = firstEndAfter(from);
            return i < size() && start(i) < to;
        }

        /** Total covered length, refusing any subtraction or summation overflow. */
        public long coveredLength() {
            long total = 0;
            for (int i = 0; i < size(); i++)
                total = Math.addExact(total, Math.subtractExact(end(i), start(i)));
            return total;
        }

        /** Exact exclusive union, composing the existing normalized primitive owners. */
        public Packed symmetricDifference(Packed other) {
            Objects.requireNonNull(other, "other");
            return difference(other).union(other.difference(this));
        }

        /** Linear allocation-free subset check over normalized half-open spans. */
        public boolean isSubsetOf(Packed other) {
            Objects.requireNonNull(other, "other");
            int b = 0;
            for (int a = 0; a < size(); a++) {
                while (b < other.size() && other.end(b) <= start(a)) b++;
                if (b == other.size() || other.start(b) > start(a) || other.end(b) < end(a))
                    return false;
            }
            return true;
        }

        /** Retain only coverage within [from,to); empty ranges produce an empty lane. */
        public Packed clip(long from, long to) {
            requireRange(from, to);
            PairLane result = new PairLane();
            if (from != to) {
                for (int i = firstEndAfter(from); i < size() && start(i) < to; i++)
                    result.add(Math.max(from, start(i)), Math.min(to, end(i)));
            }
            return result.finish();
        }

        /** Exact clipped measure without allocating an intermediate result. */
        public long coveredLength(long from, long to) {
            requireRange(from, to);
            if (from == to) return 0;
            long total = 0;
            for (int i = firstEndAfter(from); i < size() && start(i) < to; i++)
                total = Math.addExact(total, Math.subtractExact(
                        Math.min(to, end(i)), Math.max(from, start(i))));
            return total;
        }

        /** Count intersecting normalized spans using two binary searches, not their measure. */
        public int overlapCount(long from, long to) {
            requireRange(from, to);
            if (from == to) return 0;
            int first = firstEndAfter(from), low = first, high = size();
            while (low < high) {
                int mid = low + ((high - low) >>> 1);
                if (start(mid) < to) low = mid + 1;
                else high = mid;
            }
            return low - first;
        }

        private int firstEndAfter(long coordinate) {
            int low = 0, high = size();
            while (low < high) {
                int mid = low + ((high - low) >>> 1);
                if (end(mid) <= coordinate) low = mid + 1;
                else high = mid;
            }
            return low;
        }
        private static void requireRange(long from, long to) {
            if (to < from) throw new IllegalArgumentException("inverted interval");
        }
        @Override public boolean equals(Object value) {
            return value instanceof Packed other && java.util.Arrays.equals(pairs, other.pairs);
        }
        @Override public int hashCode() { return java.util.Arrays.hashCode(pairs); }
    }

    /** Sorted append/coalesce owner; at most one primitive output lane is retained. */
    private static final class PairLane {
        private long[] pairs = new long[0];
        private int used;
        private void add(long from, long to) {
            if (from == to) return;
            if (used != 0 && from <= pairs[used - 1]) {
                pairs[used - 1] = Math.max(pairs[used - 1], to);
                return;
            }
            if (used / 2 == Packed.MAX_INTERVALS)
                throw new IllegalArgumentException("output interval budget exceeded");
            if (used == pairs.length) {
                int capacity = Math.min(2 * Packed.MAX_INTERVALS, Math.max(16, 2 * used));
                pairs = java.util.Arrays.copyOf(pairs, capacity);
            }
            pairs[used++] = from;
            pairs[used++] = to;
        }
        private Packed finish() { return new Packed(java.util.Arrays.copyOf(pairs, used)); }
    }

    public static void main(String[] args) {
        if(args.length==1&&args[0].equals("--self-test")){
            if(intersects(new Span(0,2),new Span(2,3)))throw new AssertionError("touching boundary");
            if(!intersects(new Span(0,3),new Span(2,4)))throw new AssertionError("overlap");
            if(!conflicting(List.of(new Span(0,3),new Span(4,8)),new Span(2,5))
                    .equals(List.of(0,1)))throw new AssertionError("conflicts");
            System.out.println("M3_JMOD_TEST_PASS constraint.interval");
        }else throw new IllegalArgumentException("--self-test");
    }
}
