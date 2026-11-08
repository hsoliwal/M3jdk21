// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import java.util.Objects;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.function.Supplier;

/**
 * Differential dump driver of recipe/progress-algorithm-20261108 (test scope, not a test): binds two lambdas to the
 * relocated progress contract and replays seeded inputs through them, with the no-op monitor and under thread
 * interruption, printing one line per case plus one TOTAL line. One source, two compiles: differential.sh compiles
 * it here (relocated) and, with the TYPE and MEMBER rows of RELOCATION_MAP.tsv reversed, against the donor classes;
 * the two dumps must be byte-identical.
 * usage: java jdk.m3.algorithm.M3ProgressAlgorithmDump --seed N --cases N
 */
//FLLDCIM: TOP - differential driver of recipe/progress-algorithm-20261108 (M3JDK-authored, test scope)
public final class M3ProgressAlgorithmDump {
    private static long lines;

    private M3ProgressAlgorithmDump() {
    }

    public static void main(String[] args) {
        long seed = Long.parseLong(args[1]);
        int cases = Integer.parseInt(args[3]);
        Random random = new Random(seed);
        StringBuilder out = new StringBuilder(1 << 16);
        M3ProgressAlgorithm<int[], Long> sum = M3ProgressAlgorithmDump::sum;
        M3ProgressAlgorithm<int[], Integer> count = (input, monitor) -> {
            M3ProgressChecks.checkCanceled(M3ProgressChecks.nonNull(monitor));
            return Objects.requireNonNull(input, "input").length;
        };
        for (int index = 0; index < cases; index++) {
            int[] values = values(random);
            line(out, "execute.sum", index, () -> sum.execute(values, M3ProgressMonitor.none()));
            line(out, "execute.sum#nullMonitor", index, () -> sum.execute(values, null));
            line(out, "execute.count", index, () -> count.execute(values, M3ProgressMonitor.none()));
        }
        line(out, "refusal.sum#nullInput", 0, () -> sum.execute(null, M3ProgressMonitor.none()));
        line(out, "interrupt.sum", 0, () -> interrupted(() -> sum.execute(new int[] {1, 2}, M3ProgressMonitor.none())));
        line(out, "interrupt.count", 0, () -> interrupted(() -> count.execute(new int[0], null)));
        out.append("TOTAL lines=").append(lines).append(" seed=").append(seed).append(" cases=").append(cases)
                .append('\n');
        System.out.print(out);
    }

    private static Long sum(int[] input, M3ProgressMonitor supplied) {
        M3ProgressMonitor monitor = M3ProgressChecks.nonNull(supplied);
        M3ProgressChecks.checkCanceled(monitor);
        long total = 0L;
        for (int value : Objects.requireNonNull(input, "input")) {
            M3ProgressChecks.checkCanceled(monitor);
            total += value;
            monitor.worked(1L);
        }
        return total;
    }

    private static int[] values(Random random) {
        int[] values = new int[random.nextInt(8)];
        for (int index = 0; index < values.length; index++) {
            values[index] = random.nextInt(21) - 10;
        }
        return values;
    }

    private static Object interrupted(Supplier<Object> call) {
        Thread.currentThread().interrupt();
        try {
            return call.get();
        } catch (CancellationException canceled) {
            return "CANCELED " + canceled.getMessage();
        } finally {
            Thread.interrupted();
        }
    }

    private static void line(StringBuilder out, String member, int index, Supplier<Object> call) {
        Object result;
        try {
            result = call.get();
        } catch (RuntimeException refused) {
            result = "EXC " + refused.getClass().getSimpleName() + ": " + refused.getMessage();
        }
        out.append(member).append('#').append(index).append(' ').append(result).append('\n');
        lines++;
    }
}
