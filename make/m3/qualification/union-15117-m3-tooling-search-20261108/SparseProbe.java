import jdk.m3.tooling.search.M3SortedIntBounds;

/** Sparse sorted batch: 128 ascending keys over a 16M-element range; median of 9 timed calls after warmup. */
public final class SparseProbe {
    public static void main(String[] args) {
        int[] sorted = new int[1 << 24];
        for (int i = 0; i < sorted.length; i++) {
            sorted[i] = i * 2;
        }
        int[] keys = new int[128];
        for (int i = 0; i < keys.length; i++) {
            keys[i] = i * ((1 << 25) / 128);
        }
        long sum = 0;
        for (int i = 0; i < 20; i++) {
            sum += M3SortedIntBounds.bounds(sorted, keys)[255];
        }
        long[] t = new long[9];
        for (int i = 0; i < t.length; i++) {
            long s = System.nanoTime();
            sum += M3SortedIntBounds.bounds(sorted, keys)[255];
            t[i] = System.nanoTime() - s;
        }
        java.util.Arrays.sort(t);
        System.out.println("SPARSE_SORTED_BATCH n=16777216 keys=128 median_ns=" + t[4] + " checksum=" + sum);
    }
}
