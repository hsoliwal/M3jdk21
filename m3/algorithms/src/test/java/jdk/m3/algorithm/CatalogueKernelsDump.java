// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.function.Supplier;

/**
 * Differential dump driver of recipe/catalogue-kernels-20261107 (test scope, not a test): replays seeded inputs
 * through every kept public member of the relocated kernels and prints one line per case (scalar results as is,
 * arrays as length and Arrays.hashCode, refusals as exception class and message) plus one TOTAL line. One source,
 * two compiles: differential.sh compiles it here (relocated) and, with the TYPE and MEMBER rows of
 * RELOCATION_MAP.tsv reversed, against the donor classes; the two dumps must be byte-identical.
 * usage: java jdk.m3.algorithm.CatalogueKernelsDump --seed N --cases N
 */
//FLLDCIM: TOP - differential driver of recipe/catalogue-kernels-20261107 (M3JDK-authored, test scope)
public final class CatalogueKernelsDump {
    private static final char[] SMALL = {'a', 'b', '$', '\u0000', '\ud800', '\udfff'};
    private static final int[] INTS = {-7, 0, 7, Integer.MIN_VALUE, Integer.MAX_VALUE};
    private static long lines;

    private CatalogueKernelsDump() {
    }

    public static void main(String[] args) {
        long seed = Long.parseLong(args[1]);
        int cases = Integer.parseInt(args[3]);
        Random random = new Random(seed);
        StringBuilder out = new StringBuilder(1 << 20);
        for (int index = 0; index < cases; index++) {
            String text = text(random, random.nextInt(48));
            String pattern = pattern(random, text);
            zCases(out, index, text, pattern);
            mismatchCases(out, index, text, pattern, random);
            lcsCases(out, index, random);
        }
        refusalCases(out);
        out.append("TOTAL lines=").append(lines).append(" seed=").append(seed).append(" cases=").append(cases)
                .append('\n');
        System.out.print(out);
    }

    private static void zCases(StringBuilder out, int index, String text, String pattern) {
        line(out, "ZAlgorithm.zArray", index, () -> ZAlgorithm.zArray(text));
        line(out, "ZAlgorithm.zArray#monitor", index, () -> ZAlgorithm.zArray(text, ProgressMonitor.none()));
        line(out, "ZAlgorithm.zSearch", index, () -> ZAlgorithm.zSearch(text, pattern));
        line(out, "ZAlgorithm.zSearch#nullText", index, () -> ZAlgorithm.zSearch(null, pattern));
        line(out, "ZAlgorithm.zSearch#monitor", index,
                () -> ZAlgorithm.zSearch(text, pattern, ProgressMonitor.none()));
        line(out, "ZPrefixMatch.prefixMatchLengths", index, () -> ZPrefixMatch.prefixMatchLengths(text, pattern));
        line(out, "ZPrefixMatch.prefixMatchLengths#monitor", index,
                () -> ZPrefixMatch.prefixMatchLengths(text, pattern, ProgressMonitor.none()));
    }

    private static void mismatchCases(StringBuilder out, int index, String text, String pattern, Random random) {
        line(out, "OneMismatchWindowSearch.firstIndex", index,
                () -> OneMismatchWindowSearch.firstIndex(text, pattern));
        line(out, "OneMismatchWindowSearch.allIndices", index,
                () -> OneMismatchWindowSearch.allIndices(text, pattern));
        line(out, "OneMismatchWindowSearch.allIndices#monitor", index,
                () -> OneMismatchWindowSearch.allIndices(text, pattern, ProgressMonitor.none()));
        line(out, "OneMismatchWindowSearch.firstIndex#query", index,
                () -> OneMismatchWindowSearch.firstIndex(
                        new OneMismatchWindowSearch.Query(text, pattern), ProgressMonitor.none()));
        int start = random.nextInt(text.length() + 3) - 1;
        line(out, "OneMismatchWindowSearch.matchesAt", index,
                () -> OneMismatchWindowSearch.matchesAt(text, pattern, start));
    }

    private static void lcsCases(StringBuilder out, int index, Random random) {
        int[] a = ints(random, random.nextInt(40));
        int[] b = ints(random, random.nextInt(40));
        line(out, "IntLcsWitness.javaWitness", index, () -> IntLcsWitness.javaWitness(a, b));
        line(out, "IntLcsWitness.referenceLength", index, () -> IntLcsWitness.referenceLength(a, b));
        int[] witness = IntLcsWitness.javaWitness(a, b);
        line(out, "IntLcsWitness.isSubsequence", index,
                () -> IntLcsWitness.isSubsequence(witness, a) && IntLcsWitness.isSubsequence(witness, b)
                        && !IntLcsWitness.isSubsequence(new int[] {1, 2, 3, 4, 5}, new int[] {5, 4, 3, 2, 1}));
    }

    private static void refusalCases(StringBuilder out) {
        line(out, "refusal.zSearch#nullPattern", 0, () -> ZAlgorithm.zSearch("x", null));
        line(out, "refusal.firstIndex#emptyPattern", 0, () -> OneMismatchWindowSearch.firstIndex("abc", ""));
        line(out, "refusal.allIndices#nullText", 0, () -> OneMismatchWindowSearch.allIndices(null, "a"));
        line(out, "refusal.query#nullText", 0, () -> new OneMismatchWindowSearch.Query(null, "a"));
        line(out, "refusal.prefixMatchLengths#nullPattern", 0, () -> ZPrefixMatch.prefixMatchLengths("a", null));
        line(out, "refusal.javaWitness#null", 0, () -> IntLcsWitness.javaWitness(null, new int[] {1}));
        line(out, "refusal.javaWitness#length", 0, () -> IntLcsWitness.javaWitness(new int[4097], new int[] {1}));
        line(out, "refusal.javaWitness#cells", 0, () -> IntLcsWitness.javaWitness(new int[4096], new int[4096]));
        line(out, "refusal.referenceLength#null", 0, () -> IntLcsWitness.referenceLength(new int[0], null));
        line(out, "interrupt.allIndices", 0, () -> interrupted(() -> OneMismatchWindowSearch.allIndices("abc", "a")));
        line(out, "interrupt.zSearch#monitor", 0,
                () -> interrupted(() -> ZAlgorithm.zSearch("abc", "b", ProgressMonitor.none())));
        line(out, "interrupt.prefixMatchLengths", 0, () -> interrupted(() -> ZPrefixMatch.prefixMatchLengths("ab", "a")));
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
        out.append(member).append('#').append(index).append(' ');
        if (result instanceof int[] values) {
            out.append("int[").append(values.length).append("] ").append(Arrays.hashCode(values));
        } else {
            out.append(result);
        }
        out.append('\n');
        lines++;
    }

    private static String text(Random random, int length) {
        char[] units = new char[length];
        boolean small = random.nextInt(4) != 0;
        for (int index = 0; index < length; index++) {
            units[index] = small ? SMALL[random.nextInt(random.nextBoolean() ? 2 : SMALL.length)]
                    : (char) random.nextInt(Character.MAX_VALUE + 1);
        }
        return new String(units);
    }

    private static String pattern(Random random, String text) {
        if (!text.isEmpty() && random.nextInt(3) != 0) {
            int from = random.nextInt(text.length());
            int to = from + random.nextInt(Math.min(12, text.length() - from) + 1);
            char[] units = text.substring(from, to).toCharArray();
            if (units.length > 0 && random.nextBoolean()) {
                units[random.nextInt(units.length)] = SMALL[random.nextInt(SMALL.length)];
            }
            return new String(units);
        }
        return text(random, random.nextInt(6));
    }

    private static int[] ints(Random random, int length) {
        int[] values = new int[length];
        int alphabet = 1 + random.nextInt(INTS.length);
        for (int index = 0; index < length; index++) {
            values[index] = INTS[random.nextInt(alphabet)];
        }
        return values;
    }
}
