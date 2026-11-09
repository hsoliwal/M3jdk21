// Copyright 2026 Hitesh Soliwal and contributors
// Modified 2026-10-18: map the pinned Synexia CPU tool into its existing M3JDK 21 module name.
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.tools.text.similarity;
import java.util.Objects;
/** Bounded UTF-16 Levenshtein oracle with O(min(length)) primitive workspace. */
public final class EditDistance {
    private EditDistance() { }
    public static int within(String left, String right, int bound) {
        Objects.requireNonNull(left); Objects.requireNonNull(right);
        if(bound<0||bound>1_000_000) throw new IllegalArgumentException("distance bound");
        if(left.length()<right.length()){String swap=left;left=right;right=swap;}
        int a=left.length(),b=right.length();
        if(a-b>bound)return bound+1;
        if((long)a*b>10_000_000L)throw new IllegalArgumentException("work budget");
        int[] previous=new int[b+1],current=new int[b+1];
        for(int j=0;j<=b;j++)previous[j]=j;
        for(int i=1;i<=a;i++){
            current[0]=i;int min=current[0];
            for(int j=1;j<=b;j++){
                int sub=previous[j-1]+(left.charAt(i-1)==right.charAt(j-1)?0:1);
                current[j]=Math.min(sub,Math.min(previous[j]+1,current[j-1]+1));
                min=Math.min(min,current[j]);
            }
            if(min>bound)return bound+1;
            int[] swap=previous;previous=current;current=swap;
        }
        return Math.min(previous[b],bound+1);
    }
    public static void main(String[] args) {
        if(args.length==1&&args[0].equals("--self-test")){
            if(within("kitten","sitting",3)!=3)throw new AssertionError("distance");
            if(within("abc","xyz",1)!=2)throw new AssertionError("bounded");
            if(within("","",0)!=0)throw new AssertionError("empty");
            System.out.println("M3_JMOD_TEST_PASS text.similarity");
        } else if(args.length==3)System.out.println(within(args[0],args[1],Integer.parseInt(args[2])));
        else throw new IllegalArgumentException("left right bound");
    }

    /**
     * Planned DP states, including initial-row/zero-column cells, for the new
     * compact-band methods. Length-only rejection and an empty operand cost zero.
     * This is admission geometry, not a claim about actual early-exit work.
     */
    public static long bandCells(int leftLength, int rightLength, int bound) {
        checkGeometry(leftLength, rightLength, bound);
        int a = Math.max(leftLength, rightLength), b = Math.min(leftLength, rightLength);
        if (a - b > bound || b == 0) return 0;
        long x = Math.max(0, a - bound), y = Math.max(0, b - bound);
        return (a + 1L) * (b + 1L) - x * (x + 1) / 2 - y * (y + 1) / 2;
    }

    /**
     * Unit-cost UTF-16 Levenshtein distance clipped to bound+1. Borrows immutable
     * M3 views without flattening, normalization or source retention. Lengths are
     * capped at 1 Mi unit, bound at one million, and planned states at 64 Mi.
     * Work admission precedes all character reads. The original String method
     * retains its original work budget and implementation unchanged.
     */
    public static int withinViews(CharSequence left, CharSequence right, int bound, long maxCells) {
        int a = viewLength(left), b = viewLength(right);
        admitWork(bandCells(a, b, bound), maxCells);
        return bandDistance(left, a, right, b, bound);
    }

    /**
     * Explicit JNI counterpart; no automatic loading or fallback. Input and work
     * checks are repeated natively. Result checks validate bounds, not semantic
     * correctness of an arbitrary malicious native provider.
     */
    public static int withinNative(String left, String right, int bound, long maxCells) {
        int a = viewLength(left), b = viewLength(right);
        admitWork(bandCells(a, b, bound), maxCells);
        int result = withinNative0(left, right, bound, maxCells);
        if (result < Math.min(Math.abs(a - b), bound + 1)
                || result > Math.min(Math.max(a, b), bound + 1))
            throw new IllegalStateException("invalid native distance bounds");
        return result;
    }

    /**
     * Independent comparisons against a borrowed immutable reference. Detaches
     * candidate references, admits every row and total band work before reading
     * contents, and retains only primitive distances. At most 100,000 candidates.
     * Above-threshold values mean only 'greater than bound', not exact distances.
     */
    public static Distances compareViews(CharSequence reference, CharSequence[] candidates,
                                         int bound, long maxCells) {
        int length = viewLength(reference);
        Objects.requireNonNull(candidates, "candidates");
        checkGeometry(length, 0, bound);
        admitWork(0, maxCells);
        if (candidates.length > 100_000) throw new IllegalArgumentException("candidate row budget");
        CharSequence[] owners = candidates.clone();
        int[] lengths = new int[owners.length];
        long cells = 0;
        for (int i = 0; i < owners.length; i++) {
            lengths[i] = viewLength(owners[i]);
            cells += bandCells(length, lengths[i], bound);
            admitWork(cells, maxCells);
        }
        int[] values = new int[owners.length];
        for (int i = 0; i < owners.length; i++)
            values[i] = bandDistance(reference, length, owners[i], lengths[i], bound);
        return new Distances(values, bound);
    }

    /** Caller-owned primitive result. No source, view or match object is retained. */
    public static final class Distances {
        private final int[] values;
        private final int bound;
        private Distances(int[] values, int bound) { this.values = values; this.bound = bound; }
        public int size() { return values.length; }
        public int bound() { return bound; }
        public int distance(int row) { return values[Objects.checkIndex(row, values.length)]; }
        public int[] distances() { return values.clone(); }
        public long[] withinMask() {
            long[] mask = new long[(values.length + 63) >>> 6];
            for (int row = 0; row < values.length; row++)
                if (values[row] <= bound) mask[row >>> 6] |= 1L << (row & 63);
            return mask;
        }
    }

    private static int viewLength(CharSequence text) {
        int length = Objects.requireNonNull(text, "text").length();
        if (length < 0 || length > (1 << 20)) throw new IllegalArgumentException("distance UTF-16 budget");
        return length;
    }
    private static void checkGeometry(int a, int b, int bound) {
        if (a < 0 || b < 0 || a > (1 << 20) || b > (1 << 20)
                || bound < 0 || bound > 1_000_000)
            throw new IllegalArgumentException("distance geometry");
    }
    private static void admitWork(long cells, long maxCells) {
        if (maxCells < 0 || maxCells > (64L << 20) || cells > maxCells)
            throw new IllegalArgumentException("distance band work budget");
    }
    private static native int withinNative0(String left, String right, int bound, long maxCells);

    /** Existing recurrence restricted to a threshold band; no per-state objects. */
    private static int bandDistance(CharSequence left, int a, CharSequence right, int b, int bound) {
        if (a < b) {
            CharSequence owner = left; left = right; right = owner;
            int length = a; a = b; b = length;
        }
        int infinity = bound + 1;
        if (a - b > bound) return infinity;
        if (b == 0) return Math.min(a, infinity);
        int width = Math.min(b + 1, 2 * bound + 1);
        int[] previous = new int[width], current = new int[width];
        int previousLow = 0, previousHigh = Math.min(b, bound);
        for (int j = 0; j <= previousHigh; j++) previous[j] = j;
        for (int i = 1; i <= a; i++) {
            int low = Math.max(0, i - bound), high = Math.min(b, i + bound);
            int minimum = infinity;
            char unit = left.charAt(i - 1);
            for (int j = low; j <= high; j++) {
                int value;
                if (j == 0) value = i;
                else {
                    int up = bandValue(previous, previousLow, previousHigh, j, infinity);
                    int diagonal = bandValue(previous, previousLow, previousHigh, j - 1, infinity);
                    int before = j == low ? infinity : current[j - low - 1];
                    int substitution = diagonal + (unit == right.charAt(j - 1) ? 0 : 1);
                    value = Math.min(infinity, Math.min(substitution, Math.min(up + 1, before + 1)));
                }
                current[j - low] = value;
                minimum = Math.min(minimum, value);
            }
            if (minimum > bound) return infinity;
            int[] swap = previous; previous = current; current = swap;
            previousLow = low; previousHigh = high;
        }
        return previous[b - previousLow];
    }
    private static int bandValue(int[] row, int low, int high, int column, int infinity) {
        return column < low || column > high ? infinity : row[column - low];
    }
}
