/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
import com.m3.collections.M3BitMask;
import com.m3.collections.M3PermutationIndex;
import com.m3.collections.M3RadixKey;
import java.util.Random;

/** A filter mask over 10^6 rows (popcount rank, lowest-set-bit compaction) and a two-key radix sort of the kept rows. */
public final class MaskAndSortExample {
    public static void main(String[] args) {
        int n = 1_000_000;
        int[] price = new Random(1).ints(n, 0, 100_000).toArray();
        int[] region = new Random(2).ints(n, 0, 8).toArray();
        long[] mask = M3BitMask.ensure(new long[0], n);
        for (int row = 0; row < n; row++) M3BitMask.set(mask, row, price[row] % 3 == 0);   // the filter
        int words = M3BitMask.words(n);
        int[] kept = new int[n], rank = new int[words];
        int visible = M3BitMask.compact(mask, words, kept, rank);
        int firstKept = kept[0];
        System.out.println("visible=" + visible + " row " + firstKept + " is view row " + M3BitMask.rank(mask, rank, firstKept));
        M3PermutationIndex sort = new M3PermutationIndex();
        M3RadixKey byRegion = key(region), byPrice = key(price);
        long start = System.nanoTime();
        sort.sort(kept, visible, new M3RadixKey[] {byRegion, byPrice}, new boolean[] {false, true}, 2,
                (a, b) -> region[a] != region[b] ? Integer.compare(region[a], region[b]) : Integer.compare(price[b], price[a]));
        System.out.println("sorted " + visible + " rows in " + (System.nanoTime() - start) / 1_000_000 + " ms; first row region "
                + region[kept[0]] + " price " + price[kept[0]]);
        for (int i = 1; i < visible; i++) {
            int a = kept[i - 1], b = kept[i];
            if (region[a] > region[b] || region[a] == region[b] && price[a] < price[b]) throw new AssertionError("sort example");
        }
    }

    private static M3RadixKey key(int[] values) {
        return new M3RadixKey() {
            @Override public int radixKeys(int[] rows, int count, M3PermutationIndex lanes) {
                long max = 0;
                for (int i = 0; i < count; i++) max = Math.max(max, lanes.keys[i] = values[rows[i]]);
                return M3PermutationIndex.bytesFor(max);
            }
        };
    }
}
