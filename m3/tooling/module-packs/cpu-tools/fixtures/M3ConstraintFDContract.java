// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors

import jdk.m3.tools.cpu.M3DifferenceConstraints;
import jdk.m3.tools.cpu.M3FunctionalDependency;
import java.util.Arrays;
import java.util.Random;

/** Independent finite reference/oracle for exact difference constraints and FDs. */
public final class M3ConstraintFDContract {
    private static int checks;

    private M3ConstraintFDContract() { }

    private static void require(boolean yes, String what) {
        checks++;
        if (!yes) { throw new AssertionError(what); }
    }

    private static void refuse(Class<? extends Throwable> type, Runnable call, String what) {
        checks++;
        try { call.run(); }
        catch (Throwable actual) {
            if (type.isInstance(actual)) { return; }
            throw new AssertionError("unexpected refusal in " + what, actual);
        }
        throw new AssertionError("expected refusal in " + what);
    }

    private static boolean oracleFeasible(int n, int[] from, int[] to, int[] upper) {
        final long inf = 1_000_000_000L;
        long[][] d = new long[n][n];
        for (int i = 0; i < n; i++) {
            Arrays.fill(d[i], inf);
            d[i][i] = 0;
        }
        for (int e = 0; e < from.length; e++) {
            d[from[e]][to[e]] = Math.min(d[from[e]][to[e]], upper[e]);
        }
        for (int k = 0; k < n; k++) {
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if (d[i][k] != inf && d[k][j] != inf) {
                        d[i][j] = Math.min(d[i][j], d[i][k] + d[k][j]);
                    }
                }
            }
        }
        for (int i = 0; i < n; i++) { if (d[i][i] < 0) { return false; } }
        return true;
    }

    private static int[] oracleFD(int rows, int columns, int[] data,
            int[] lhs, int[] rhs) {
        for (int a = 0; a < rows; a++) {
            for (int b = a + 1; b < rows; b++) {
                boolean same = true;
                for (int f : lhs) { if (data[a * columns + f] != data[b * columns + f]) {
                    same = false; break;
                } }
                if (!same) { continue; }
                for (int f : rhs) {
                    if (data[a * columns + f] != data[b * columns + f]) {
                        return new int[] {a, b};
                    }
                }
            }
        }
        return new int[] {-1, -1};
    }

    private static int[] chooseFields(Random random, int columns) {
        int[] tmp = new int[columns];
        int n = 0;
        for (int c = 0; c < columns; c++) {
            if (random.nextBoolean()) { tmp[n++] = c; }
        }
        return Arrays.copyOf(tmp, n);
    }

    public static void main(String[] args) {
        Random rng = new Random(0x4711f00dL);
        require(M3DifferenceConstraints.verify(1, new int[0], new int[0], new int[0],
                new long[] {Long.MIN_VALUE}), "empty witness verification");
        require(M3DifferenceConstraints.verify(2, new int[] {0}, new int[] {1},
                new int[] {1}, new long[] {Long.MAX_VALUE, Long.MAX_VALUE}),
                "positive add-overflow on external witness");
        require(!M3DifferenceConstraints.verify(2, new int[] {0}, new int[] {1},
                new int[] {-1}, new long[] {Long.MIN_VALUE, Long.MIN_VALUE}),
                "negative add-underflow on external witness");
        refuse(IllegalArgumentException.class, () -> M3DifferenceConstraints.solve(
                0, new int[0], new int[0], new int[0], 0), "zero variables");
        refuse(IllegalArgumentException.class, () -> M3DifferenceConstraints.solve(
                2, new int[] {0}, new int[] {2}, new int[] {1}, 1), "bad to endpoint");
        refuse(IllegalArgumentException.class, () -> M3DifferenceConstraints.solve(
                2, new int[] {0}, new int[0], new int[] {1}, 1), "misaligned edge lanes");
        refuse(IllegalArgumentException.class, () -> M3DifferenceConstraints.solve(
                1, new int[0], new int[0], new int[0], 50_000_001L), "oversize budget");
        refuse(IllegalStateException.class, () -> M3DifferenceConstraints.solve(
                2, new int[] {0}, new int[] {1}, new int[] {-2}, 0), "work exhausted");
        require(Arrays.equals(new long[] {0}, M3DifferenceConstraints.solve(
                1, new int[0], new int[0], new int[0], 0)), "empty feasible graph");
        require(M3DifferenceConstraints.solve(1, new int[] {0}, new int[] {0},
                new int[] {-1}, 1) == null, "negative self cycle");
        for (int n = 1; n <= 6; n++) {
            for (int trial = 0; trial < 15000; trial++) {
                int edges = rng.nextInt(17);
                int[] from = new int[edges];
                int[] to = new int[edges];
                int[] weight = new int[edges];
                for (int e = 0; e < edges; e++) {
                    from[e] = rng.nextInt(n);
                    to[e] = rng.nextInt(n);
                    weight[e] = rng.nextInt(13) - 6;
                }
                int[] frozenFrom = from.clone();
                int[] frozenTo = to.clone();
                int[] frozenWeight = weight.clone();
                boolean expected = oracleFeasible(n, from, to, weight);
                long[] got = M3DifferenceConstraints.solve(n, from, to, weight, 200_000L);
                require(expected == (got != null), "difference constraints oracle");
                if (got != null) {
                    require(M3DifferenceConstraints.verify(n, from, to, weight, got),
                            "candidate witness infeasible");
                    require(Arrays.equals(got, M3DifferenceConstraints.solve(
                            n, from, to, weight, 200_000L)), "non-repeatable potentials");
                }
                require(Arrays.equals(from, frozenFrom) && Arrays.equals(to, frozenTo)
                        && Arrays.equals(weight, frozenWeight), "constraint input mutation");
            }
        }
        require(Arrays.equals(new int[] {-1, -1}, M3FunctionalDependency.firstViolation(
                0, 0, new int[0], new int[0], new int[0], 0)), "zero rows and fields");
        refuse(IllegalArgumentException.class, () -> M3FunctionalDependency.firstViolation(
                2, 1, new int[] {1}, new int[0], new int[0], 0), "row-major shape");
        refuse(IllegalArgumentException.class, () -> M3FunctionalDependency.firstViolation(
                2, 2, new int[4], new int[] {1, 1}, new int[] {0}, 1),
                "duplicate fields");
        refuse(IllegalArgumentException.class, () -> M3FunctionalDependency.firstViolation(
                2, 2, new int[4], new int[] {1, 0}, new int[] {0}, 1),
                "unsorted field IDs");
        refuse(IllegalStateException.class, () -> M3FunctionalDependency.firstViolation(
                2, 2, new int[] {0, 0, 0, 1}, new int[] {0}, new int[] {1}, 0),
                "comparison work budget");
        for (int trial = 0; trial < 90000; trial++) {
            int rows = rng.nextInt(14);
            int columns = rng.nextInt(7);
            int[] data = new int[rows * columns];
            for (int i = 0; i < data.length; i++) { data[i] = rng.nextInt(9) - 4; }
            int[] lhs = chooseFields(rng, columns);
            int[] rhs = chooseFields(rng, columns);
            int[] frozen = data.clone();
            int[] expected = oracleFD(rows, columns, data, lhs, rhs);
            int[] got = M3FunctionalDependency.firstViolation(rows, columns,
                    data, lhs, rhs, 100_000L);
            require(Arrays.equals(expected, got), "functional dependency oracle");
            require(Arrays.equals(frozen, data), "functional dependency input mutation");
            require(Arrays.equals(got, M3FunctionalDependency.firstViolation(rows,
                    columns, data, lhs, rhs, 100_000L)), "non-repeatable FD witness");
        }
        int rows = 8192;
        int[] large = new int[rows * 2];
        large[large.length - 1] = 1;
        require(Arrays.equals(new int[] {0, rows - 1},
                M3FunctionalDependency.firstViolation(rows, 2, large,
                        new int[] {0}, new int[] {1}, 50_000_000L)),
                "large grouped dependency witness");
        System.out.println("M3_WAVE7_CONSTRAINT_FD_PASS checks=" + checks);
    }
}
