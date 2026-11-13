// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** Standalone deterministic Java21 differential oracle; target module/JUnit separately gated. */
public final class M3ListDiffRunsProof {
    private static long cases;
    private static long digest = 0x6a09e667f3bcc909L;
    private static void check(boolean pass, String what) {
        if (!pass) throw new AssertionError(what + " case=" + cases);
    }
    private static void mark(int x) { digest = (digest ^ x) * 0x9e3779b97f4a7c15L + (digest >>> 17); }

    private static int[][] oracle(boolean[] kept, boolean descending) {
        List<Integer> starts = new ArrayList<>(), lengths = new ArrayList<>();
        for (int i = 0; i < kept.length;) {
            if (kept[i]) { i++; continue; }
            int begin = i;
            while (i < kept.length && !kept[i]) i++;
            starts.add(begin); lengths.add(i - begin);
        }
        if (descending) {
            java.util.Collections.reverse(starts);
            java.util.Collections.reverse(lengths);
        }
        int[] a = starts.stream().mapToInt(Integer::intValue).toArray();
        int[] b = lengths.stream().mapToInt(Integer::intValue).toArray();
        return new int[][] { a, b };
    }

    private static void run(boolean[] kept, boolean descending) {
        int[][] got = M3ListDiff.runs(kept, descending);
        int[][] want = oracle(kept, descending);
        check(Arrays.deepEquals(got, want), "runs mismatch length=" + kept.length
                + " descending=" + descending + " want=" + Arrays.deepToString(want)
                + " got=" + Arrays.deepToString(got));
        check(got.length == 2 && got[0].length == got[1].length, "matched lanes");
        for (int i = 0; i < got[0].length; i++) {
            mark(got[0][i]); mark(got[1][i]);
        }
        mark(got[0].length);
        cases++;
    }

    private static long[] randomSnapshot(Random random, long[] universe) {
        long[] copy = universe.clone();
        for (int i = copy.length - 1; i > 0; i--) {
            int swap = random.nextInt(i + 1);
            long temp = copy[swap]; copy[swap] = copy[i]; copy[i] = temp;
        }
        return Arrays.copyOf(copy, random.nextInt(copy.length + 1));
    }

    private static void testDiff(Random random) {
        long[] universe = new long[64];
        universe[0] = Long.MIN_VALUE;
        universe[1] = Long.MAX_VALUE;
        universe[2] = 0L;
        universe[3] = -1L;
        for (int i = 4; i < 64; i++) universe[i] = (i & 1) == 0 ? i : -i;
        for (int round = 0; round < 12000; round++) {
            long[] before = randomSnapshot(random, universe);
            long[] after = randomSnapshot(random, universe);
            M3Patch patch = M3ListDiff.diff(before, after);
            check(Arrays.equals(after, patch.apply(before, after)), "patch apply");
            check(patch.keptCount() + patch.removedCount() == before.length, "remove cardinality");
            check(patch.keptCount() + patch.insertedCount() == after.length, "insert cardinality");
            check(patch.moveCount() <= patch.removedCount(), "move removal bound");
            check(patch.moveCount() <= patch.insertedCount(), "move insert bound");
            for (int i = 1; i < patch.removeRuns(); i++)
                check(patch.removeAt(i) < patch.removeAt(i - 1), "descending remove order");
            for (int i = 1; i < patch.insertRuns(); i++)
                check(patch.insertAt(i) > patch.insertAt(i - 1), "ascending insert order");
            for (int i = 0; i < patch.moveCount(); i++)
                check(before[patch.moveFrom(i)] == after[patch.moveTo(i)], "move equal keys");
            mark(patch.keptCount()); mark(patch.moveCount());
            mark(patch.removeRuns()); mark(patch.insertRuns());
            for(int i=0;i<patch.removeRuns();i++){mark(patch.removeAt(i));mark(patch.removeCount(i));}
            for(int i=0;i<patch.insertRuns();i++){mark(patch.insertAt(i));mark(patch.insertCount(i));}
            cases++;
        }
        try {
            M3ListDiff.diff(new long[] {1, 1}, new long[] {2, 2});
            throw new AssertionError("duplicate not rejected");
        } catch (IllegalArgumentException expected) {
            check("duplicate key in after: 2".equals(expected.getMessage()), "after first error");
        }
        try {
            M3ListDiff.diff(new long[] {1, 1}, new long[] {2, 3});
            throw new AssertionError("duplicate before not rejected");
        } catch (IllegalArgumentException expected) {
            check("duplicate key in before: 1".equals(expected.getMessage()), "before error");
        }
    }

    public static void main(String[] args) {
        for(int n=0;n<=16;n++){
            for(int bits=0;bits<(1<<n);bits++){
                boolean[] kept=new boolean[n];
                for(int i=0;i<n;i++)kept[i]=(bits&(1<<i))!=0;
                run(kept,false); run(kept,true);
            }
        }
        Random random = new Random(0x5EED2026L);
        for (int k=0;k<20000;k++){
            int n=random.nextInt(2049); boolean[] kept=new boolean[n];
            int style=random.nextInt(6);
            for(int i=0;i<n;i++){
                kept[i] = switch (style) {
                    case 0 -> true;
                    case 1 -> false;
                    case 2 -> (i&1)==0;
                    case 3 -> i< n/2;
                    case 4 -> random.nextInt(100)<5;
                    default -> random.nextBoolean();
                };
            }
            run(kept,false); run(kept,true);
        }
        testDiff(random);
        System.out.println("M3ListDiffRunsProof PASS cases="+cases+" hash="+Long.toUnsignedString(digest,16));
    }
}
