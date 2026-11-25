// SPDX-License-Identifier: Apache-2.0
/*
 * @test
 * @summary Primitive range-add maxima use compact sparse endpoints without contract drift
 * @modules jdk.m3.collections
 * @run main IntLaneSparseRangeJtreg --huge
 */
import java.util.Arrays;
import java.util.Random;
import jdk.m3.arrays.IntLaneAlgorithms;

public final class IntLaneSparseRangeJtreg {
    private static final Random RANDOM = new Random(0x1EAFB33DL);
    private static int scenarios;
    private static int checks;
    private static long digest;
    private static void require(boolean success, String message) {
        checks++;
        if (!success) throw new AssertionError(message + " #" + scenarios);
    }
    private static long oracle(int n, int[] start, int[] end, long[] delta) {
        if (n == 0) return 0L;
        int[] coords = new int[1 + 2 * start.length];
        int count = 1;
        for (int i = 0; i < start.length; i++) {
            coords[count++] = start[i];
            if (end[i] + 1 < n) coords[count++] = end[i] + 1;
        }
        Arrays.sort(coords, 0, count);
        long maximum = Long.MIN_VALUE;
        for (int i = 0; i < count; i++) {
            int at = coords[i];
            long sum = 0;
            for (int j = 0; j < start.length; j++) {
                if (start[j] <= at && at <= end[j]) sum += delta[j];
            }
            if (sum > maximum) maximum = sum;
        }
        return maximum;
    }
    private static void valid(int n, int[] start, int[] end, long[] delta) {
        scenarios++;
        long actual = IntLaneAlgorithms.rangeAddMaximum(n, start, end, delta);
        long expected = oracle(n, start, end, delta);
        require(actual == expected, "range maxima mismatch expected=" + expected + " actual=" + actual);
        digest = digest * 1000003L + actual;
    }
    private static void refusal(Class<? extends RuntimeException> klass, int n,
            int[] start, int[] end, long[] delta) {
        scenarios++;
        try {
            IntLaneAlgorithms.rangeAddMaximum(n, start, end, delta);
            throw new AssertionError("missing required exception " + klass.getName());
        } catch (RuntimeException failure) {
            require(klass.isInstance(failure), "wrong exception " + failure.getClass());
            digest = digest * 1000003L + failure.getClass().getName().hashCode();
        }
    }
    public static void main(String[] args) {
        if (args.length > 1 || (args.length == 1 && !args[0].equals("--huge")))
            throw new IllegalArgumentException("only --huge is allowed");
        valid(0, new int[0], new int[0], new long[0]);
        for (int n : new int[]{1, 63, 64, 65, 65535, 65536, 65537, 200000}) {
            valid(n, new int[0], new int[0], new long[0]);
            valid(n, new int[]{0}, new int[]{n-1}, new long[]{-5});
            // A negative tail update must not erase the untouched zero prefix.
            valid(n, new int[]{n/2}, new int[]{n-1}, new long[]{-5});
            valid(n, new int[]{0}, new int[]{n-1}, new long[]{5});
        }
        refusal(IllegalArgumentException.class, -1, new int[0], new int[0], new long[0]);
        refusal(IllegalArgumentException.class, 65536, new int[]{0}, new int[0], new long[0]);
        refusal(IndexOutOfBoundsException.class, 65536, new int[]{-1}, new int[]{1}, new long[]{1});
        refusal(IndexOutOfBoundsException.class, 65536, new int[]{0}, new int[]{65536}, new long[]{1});
        refusal(ArithmeticException.class, 65536, new int[]{0,0}, new int[]{65535,65535},
                new long[]{Long.MAX_VALUE,1});
        refusal(ArithmeticException.class, 65536, new int[]{0}, new int[]{0},
                new long[]{Long.MIN_VALUE});
        for (int trial = 0; trial < 5500; trial++) {
            int n = trial < 4500 ? 1 + RANDOM.nextInt(1000) : 65536 + RANDOM.nextInt(230000);
            int k = RANDOM.nextInt(trial < 4500 ? 15 : 40);
            int[] start = new int[k], end = new int[k];
            long[] delta = new long[k];
            for (int i = 0; i < k; i++) {
                start[i] = RANDOM.nextInt(n);
                end[i] = start[i] + RANDOM.nextInt(n - start[i]);
                delta[i] = RANDOM.nextInt(201) - 100;
            }
            valid(n,start,end,delta);
        }
        if (args.length == 1) {
            int n = Integer.MAX_VALUE;
            require(IntLaneAlgorithms.rangeAddMaximum(n,new int[0],new int[0],new long[0]) == 0L,
                    "huge empty universe");
            require(IntLaneAlgorithms.rangeAddMaximum(n,new int[]{n-1},new int[]{n-1},
                    new long[]{17}) == 17L,"huge last offset");
            require(IntLaneAlgorithms.rangeAddMaximum(n,new int[]{n-1},new int[]{n-1},
                    new long[]{-17}) == 0L,"huge untouched prefix");
        }
        System.out.println("M3_SPARSE_RANGE_JTREG_PASS cases=" + scenarios + " checks=" + checks
                + " digest=" + digest + " huge=" + (args.length == 1));
    }
}
