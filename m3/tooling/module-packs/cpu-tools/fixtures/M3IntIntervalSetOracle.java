// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
import com.m3.tools.cpu.M3IntIntervalSet;
import java.util.Arrays;
import java.util.Random;

/** Independent exhaustive point-mask oracle for normalized signed half-open intervals. */
public final class M3IntIntervalSetOracle {
    private static long checks;
    private static void check(boolean truth, String label) {
        checks++;
        if (!truth) throw new AssertionError(label + " @" + checks);
    }
    private static boolean brute(int[] input, int point) {
        for (int i = 0; i < input.length; i += 2) {
            if (input[i] <= point && point < input[i + 1]) return true;
        }
        return false;
    }
    private static long bruteSpan(int[] input, int first, int last) {
        long count = 0;
        for (int point = first; point < last; ++point) if (brute(input, point)) count++;
        return count;
    }
    private static void validate(int[] input, int low, int high) {
        int[] copy = input.clone();
        M3IntIntervalSet index = M3IntIntervalSet.copyOf(input);
        check(Arrays.equals(input, copy), "input changed");
        int[] pairs = index.toPairs();
        check((pairs.length & 1) == 0 && pairs.length == 2 * index.intervalCount(), "pair size");
        check(pairs != index.toPairs(), "detached output");
        for (int i = 0; i < pairs.length; i += 2) {
            check(pairs[i] < pairs[i + 1], "nonempty range");
            if (i >= 2) check(pairs[i - 1] < pairs[i], "not normalized/adjacent");
        }
        for (int point = low; point <= high; point++) {
            check(index.contains(point) == brute(input, point), "point parity");
        }
        check(index.coveredLength() == bruteSpan(input, low, high + 1), "covered length");
        for (int start = low; start <= high; start++) {
            for (int end = start; end <= high + 1; end++) {
                check(index.coveredLengthIn(start, end) == bruteSpan(input, start, end),
                        "query length at " + start + ".." + end);
            }
        }
        if (pairs.length > 0) {
            int original = pairs[0];
            pairs[0] = Integer.MAX_VALUE;
            check(index.toPairs()[0] == original, "detached content");
        }
    }
    private static void rejects(Runnable task, String label) {
        try { task.run(); throw new AssertionError("accepted " + label); }
        catch (IllegalArgumentException | NullPointerException expected) { checks++; }
    }
    public static void main(String[] args) {
        for (int mask = 0; mask < 1 << 8; mask++) {
            int[] input = new int[16];
            int used = 0;
            for (int i = 0; i < 8; i++) {
                if ((mask & (1 << i)) != 0) {
                    input[used++] = i - 4;
                    input[used++] = i - 3;
                }
            }
            validate(Arrays.copyOf(input, used), -5, 5);
        }
        Random rng = new Random(0x456372119L);
        for (int test = 0; test < 1200; test++) {
            int pairs = rng.nextInt(25);
            int[] input = new int[pairs * 2];
            for (int i = 0; i < input.length; i += 2) {
                int a = rng.nextInt(18) - 9;
                int b = a + rng.nextInt(6);
                input[i] = a;
                input[i + 1] = b;
            }
            validate(input, -10, 14);
        }
        validate(new int[]{-10,-1,0,2,2,5,5,5}, -11, 9);
        validate(new int[]{-2,4,1,3,-7,-5,4,8}, -10, 10);
        M3IntIntervalSet extreme = M3IntIntervalSet.copyOf(new int[]{
                Integer.MIN_VALUE,Integer.MAX_VALUE});
        check(extreme.coveredLength() == 4_294_967_295L, "overflow-free signed span");
        check(extreme.contains(Integer.MIN_VALUE), "min inclusive");
        check(!extreme.contains(Integer.MAX_VALUE), "max exclusive");
        check(extreme.coveredLengthIn(-2, 4) == 6, "extreme query");
        check(extreme.coveredLengthIn(Integer.MIN_VALUE, Integer.MAX_VALUE) == 4_294_967_295L,
                "max span query");
        rejects(() -> M3IntIntervalSet.copyOf(null), "null source");
        rejects(() -> M3IntIntervalSet.copyOf(new int[]{1}), "odd source");
        rejects(() -> M3IntIntervalSet.copyOf(new int[]{2,1}), "reversed source");
        rejects(() -> extreme.coveredLengthIn(9,-9), "reversed query");
        rejects(() -> M3IntIntervalSet.copyOf(new int[2_000_002]), "too many pairs");
        int[] large = new int[200_000];
        for (int i = 0; i < large.length; i += 2) {
            large[i] = i;
            large[i + 1] = i + 1;
        }
        M3IntIntervalSet runs = M3IntIntervalSet.copyOf(large);
        check(runs.intervalCount() == 100_000, "large pair count");
        check(runs.coveredLength() == 100_000, "large coverage");
        check(runs.contains(199_998) && !runs.contains(199_999), "large lookup");
        System.out.println("M3_INTERVAL_JAVA_PASS checks=" + checks);
    }
}
