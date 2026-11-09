// SPDX-License-Identifier: GPL-2.0-only
/*
 * @test
 * @summary M3 array primitive JIT interpreted, C1 and C2 parity with scalar oracle
 * @run main/othervm -Xint -Xcheck:jni TestM3AtomJitParity
 * @run main/othervm -Xbatch -XX:TieredStopAtLevel=1 -Xcheck:jni TestM3AtomJitParity
 * @run main/othervm -Xbatch -XX:-TieredCompilation -XX:CompileThreshold=100 -Xcheck:jni TestM3AtomJitParity
 */
import java.util.Arrays;

/** Deterministic Java/JIT oracle for array-fill/copy/mismatch and exception contracts. */
public final class TestM3AtomJitParity {
    private static final int LENGTH = 64;
    private static final int ITERATIONS = 35_000;
    private static final int SEED_MULTIPLIER = 37;
    private static final int INDEX_MULTIPLIER = 23;
    private static final int EMPTY_FILL_VALUE = -7;
    private static final long INITIAL_DIGEST = 0x23c95e4a6178b0d1L;

    private TestM3AtomJitParity() { }

    /** HotSpot should compile this method in C1/C2 modes. */
    private static int kernel(int seed) {
        int[] source = new int[LENGTH];
        int[] actual = new int[LENGTH];
        for (int index = 0; index < LENGTH; index++) {
            source[index] = seed * SEED_MULTIPLIER + index * INDEX_MULTIPLIER;
        }
        Arrays.fill(actual, EMPTY_FILL_VALUE);
        System.arraycopy(source, 0, actual, 0, LENGTH);
        System.arraycopy(actual, 0, actual, 1, LENGTH - 1);
        int difference = Arrays.mismatch(source, actual);
        int hash = difference;
        for (int value : actual) {
            hash = 31 * hash + value;
        }
        return hash;
    }

    /** Independent scalar oracle; no Arrays.fill, Arrays.mismatch or arraycopy. */
    private static int reference(int seed) {
        int[] source = new int[LENGTH];
        int[] expected = new int[LENGTH];
        for (int index = 0; index < LENGTH; index++) {
            source[index] = seed * SEED_MULTIPLIER + index * INDEX_MULTIPLIER;
            expected[index] = source[index];
        }
        for (int index = LENGTH - 1; index > 0; index--) {
            expected[index] = expected[index - 1];
        }
        int mismatch = -1;
        for (int index = 0; index < LENGTH; index++) {
            if (source[index] != expected[index]) {
                mismatch = index;
                break;
            }
        }
        int hash = mismatch;
        for (int value : expected) {
            hash = 31 * hash + value;
        }
        return hash;
    }

    private static void expect(Class<? extends Throwable> expected, Runnable action) {
        try {
            action.run();
        } catch (Throwable actual) {
            if (expected.isInstance(actual)) {
                return;
            }
            throw new AssertionError("Wrong exception: " + actual, actual);
        }
        throw new AssertionError("Expected " + expected.getName());
    }

    private static void checkBoundaryContracts() {
        int[] values = {1, 2, 3, 4};
        System.arraycopy(values, 0, values, 1, 3);
        if (!Arrays.equals(values, new int[] {1, 1, 2, 3})) {
            throw new AssertionError("overlapping forward copy");
        }
        System.arraycopy(values, 1, values, 0, 3);
        if (!Arrays.equals(values, new int[] {1, 2, 3, 3})) {
            throw new AssertionError("overlapping backward copy");
        }
        Arrays.fill(values, 1, 3, 9);
        if (!Arrays.equals(values, new int[] {1, 9, 9, 3})) {
            throw new AssertionError("ranged fill");
        }
        expect(NullPointerException.class,
                () -> System.arraycopy(null, 0, new int[1], 0, 1));
        expect(NullPointerException.class,
                () -> Arrays.fill((int[]) null, 0));
        expect(ArrayIndexOutOfBoundsException.class,
                () -> System.arraycopy(new int[1], -1, new int[1], 0, 1));
        expect(ArrayStoreException.class,
                () -> System.arraycopy(new Object[] {"x"}, 0, new Integer[1], 0, 1));
        if (Arrays.mismatch(new int[0], new int[0]) != -1) {
            throw new AssertionError("empty mismatch");
        }
    }

    public static void main(String[] args) {
        if (args.length != 0) {
            throw new IllegalArgumentException("No arguments are accepted");
        }
        checkBoundaryContracts();
        long digest = INITIAL_DIGEST;
        for (int seed = 0; seed < ITERATIONS; seed++) {
            int observed = kernel(seed);
            int expected = reference(seed);
            if (observed != expected) {
                throw new AssertionError("Java/JIT parity failed at seed " + seed);
            }
            digest = Long.rotateLeft(digest ^ (Integer.toUnsignedLong(observed) + seed), 7);
        }
        System.out.println("PROBE_OK\tdigest=" + Long.toUnsignedString(digest, 16)
                + "\titerations=" + ITERATIONS + "\tboundary_contracts=8");
    }
}
