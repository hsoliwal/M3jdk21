/*
 * @test
 * @summary Problem-category-qualified XOR singleton and all-lonely primitive array algorithms
 * @modules java.base/jdk.internal.m3
 * @run main/othervm M3IntAlgorithmsTest
 */

import jdk.internal.m3.M3IntAlgorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class M3IntAlgorithmsTest {
    public static void main(String[] args) {
        xorContract();
        lonelyExamples();
        edgeCases();
        randomizedDifferential();
        randomizedXorProof();
        System.out.println("M3IntAlgorithmsTest PASS");
    }

    private static void xorContract() {
        equal(1, M3IntAlgorithms.singleNumberOfPairs(new int[]{2, 1, 2}), "single 1");
        equal(4, M3IntAlgorithms.singleNumberOfPairs(new int[]{4, 1, 2, 1, 2}), "single 4");
        equal(7, M3IntAlgorithms.singleNumberOfPairs(new int[]{7}), "single only");
        equal(Integer.MIN_VALUE, M3IntAlgorithms.singleNumberOfPairs(
                new int[]{99, -23, Integer.MIN_VALUE, -23, 19, 19, 99}), "single boundary");
        equal(-9, M3IntAlgorithms.singleNumberOfPairs(new int[]{0, 9, -9, 9, 0, 11, 11, 0}, 1, 7),
                "single offset");
        fails(IllegalArgumentException.class,
                () -> M3IntAlgorithms.singleNumberOfPairs(new int[]{}), "no singleton");
        fails(IllegalArgumentException.class,
                () -> M3IntAlgorithms.singleNumberOfPairs(new int[]{1, 1}), "even length");
        fails(IndexOutOfBoundsException.class,
                () -> M3IntAlgorithms.singleNumberOfPairs(new int[]{1, 2, 1}, 2, 3), "bad offset");
        fails(NullPointerException.class,
                () -> M3IntAlgorithms.singleNumberOfPairs(null), "null XOR array");
    }

    private static void lonelyExamples() {
        equal(new int[]{10, 8}, M3IntAlgorithms.lonelyNumbers(new int[]{10, 6, 5, 8}), "2150 example one");
        equal(new int[]{1, 5}, M3IntAlgorithms.lonelyNumbers(new int[]{1, 3, 5, 3}), "2150 example two");
        equal(new int[]{}, M3IntAlgorithms.lonelyNumbers(new int[]{1, 1, 2, 2}), "no lonely");
        equal(new int[]{}, M3IntAlgorithms.lonelyNumbers(new int[]{1, 2, 3, 4}), "all adjacent");
        equal(new int[]{10}, M3IntAlgorithms.lonelyNumbers(new int[]{3, 10, 1, 2}), "singleton no neighbors");
        equal(new int[]{}, M3IntAlgorithms.lonelyNumbers(new int[]{}), "empty input");
        equal(new int[]{1, 5}, M3IntAlgorithms.lonelyNumbers(new int[]{91, 1, 3, 5, 3, 91}, 1, 4),
                "offset and bound");
        fails(IndexOutOfBoundsException.class,
                () -> M3IntAlgorithms.lonelyNumbers(new int[]{1}, Integer.MAX_VALUE, 1), "overflow offset");
        fails(NullPointerException.class,
                () -> M3IntAlgorithms.lonelyNumbers(null), "null array");
    }

    private static void edgeCases() {
        equal(new int[]{Integer.MIN_VALUE, 0, Integer.MAX_VALUE},
                M3IntAlgorithms.lonelyNumbers(new int[]{Integer.MIN_VALUE, 0, Integer.MAX_VALUE}),
                "wide domain must not overflow neighbor checks");
        equal(new int[]{0}, M3IntAlgorithms.lonelyNumbers(new int[]{Integer.MIN_VALUE,
                Integer.MIN_VALUE + 1, 0, Integer.MAX_VALUE - 1, Integer.MAX_VALUE}),
                "neighbors at both boundaries");
        equal(new int[]{42}, M3IntAlgorithms.lonelyNumbers(new int[]{42}), "one alone");
        equal(new int[]{Integer.MIN_VALUE}, M3IntAlgorithms.lonelyNumbers(new int[]{Integer.MIN_VALUE}),
                "minimum alone");
        equal(new int[]{Integer.MAX_VALUE}, M3IntAlgorithms.lonelyNumbers(new int[]{Integer.MAX_VALUE}),
                "maximum alone");
        int[] dense = new int[100_000];
        for (int i = 0; i < dense.length; i++) {
            dense[i] = i;
        }
        equal(new int[0], M3IntAlgorithms.lonelyNumbers(dense), "long contiguous domain");
        int[] sparse = new int[10_000];
        for (int i = 0; i < sparse.length; i++) {
            sparse[i] = Integer.MIN_VALUE + i * 8191;
        }
        equal(sparse, M3IntAlgorithms.lonelyNumbers(sparse), "long sparse domain");
    }

    private static void randomizedDifferential() {
        final Random random = new Random(0x3A1FEEDL);
        for (int run = 0; run < 4000; run++) {
            int count = random.nextInt(200);
            int prefix = random.nextInt(6);
            int suffix = random.nextInt(6);
            int[] source = new int[prefix + count + suffix];
            for (int i = 0; i < source.length; i++) {
                source[i] = randomValue(random);
            }
            int[] copy = source.clone();
            int[] expected = oracle(source, prefix, count);
            int[] actual = M3IntAlgorithms.lonelyNumbers(source, prefix, count);
            equal(expected, actual, "differential run " + run);
            equal(copy, source, "must not mutate input " + run);
        }
    }

    private static int[] oracle(int[] source, int offset, int length) {
        // Boxed baseline is used only as a differential testing oracle.
        final Map<Integer, Integer> counts = new HashMap<>();
        for (int i = offset; i < offset + length; i++) {
            counts.merge(source[i], 1, Integer::sum);
        }
        final ArrayList<Integer> unique = new ArrayList<>();
        for (int i = offset; i < offset + length; i++) {
            final int value = source[i];
            if (counts.get(value) == 1
                    && (value == Integer.MIN_VALUE || !counts.containsKey(value - 1))
                    && (value == Integer.MAX_VALUE || !counts.containsKey(value + 1))) {
                unique.add(value);
            }
        }
        return unique.stream().mapToInt(Integer::intValue).toArray();
    }

    private static int randomValue(Random random) {
        return switch (random.nextInt(8)) {
            case 0 -> random.nextInt(65) - 32;
            case 1 -> random.nextInt(10001) - 5000;
            case 2 -> random.nextInt();
            case 3 -> Integer.MIN_VALUE;
            case 4 -> Integer.MAX_VALUE;
            case 5 -> 0;
            case 6 -> Integer.MIN_VALUE + random.nextInt(3);
            default -> Integer.MAX_VALUE - random.nextInt(3);
        };
    }

    private static void randomizedXorProof() {
        final Random random = new Random(0x5EEDEEDL);
        for (int run = 0; run < 800; run++) {
            int pairs = random.nextInt(120);
            final int[] source = new int[2 * pairs + 1];
            final Set<Integer> used = new HashSet<>();
            int singleton = random.nextInt();
            used.add(singleton);
            source[0] = singleton;
            for (int i = 0; i < pairs; i++) {
                int value;
                do {
                    value = random.nextInt();
                } while (!used.add(value));
                source[2 * i + 1] = value;
                source[2 * i + 2] = value;
            }
            for (int i = source.length - 1; i > 0; i--) {
                int j = random.nextInt(i + 1);
                int swap = source[i];
                source[i] = source[j];
                source[j] = swap;
            }
            equal(singleton, M3IntAlgorithms.singleNumberOfPairs(source), "XOR proof run " + run);
        }
    }

    private static void fails(Class<? extends Throwable> type, Runnable task, String explanation) {
        try {
            task.run();
            throw new AssertionError(explanation + " should throw " + type.getSimpleName());
        } catch (Throwable thrown) {
            if (!type.isInstance(thrown)) {
                throw new AssertionError(explanation, thrown);
            }
        }
    }

    private static void equal(int expected, int actual, String explanation) {
        if (expected != actual) {
            throw new AssertionError(explanation + ": expected=" + expected + ", actual=" + actual);
        }
    }

    private static void equal(int[] expected, int[] actual, String explanation) {
        if (!Arrays.equals(expected, actual)) {
            throw new AssertionError(explanation + ": expected=" + Arrays.toString(expected)
                    + ", actual=" + Arrays.toString(actual));
        }
    }
}
