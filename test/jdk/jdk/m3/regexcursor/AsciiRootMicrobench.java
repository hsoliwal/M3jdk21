// SPDX-License-Identifier: Apache-2.0
package jdk.m3.regexcursor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/** Isolated descriptive throughput assay, not a JMH or HotSpot qualification. */
public final class AsciiRootMicrobench {
    private static volatile long sink;
    private static final int SAMPLES = 4096;
    private static final int WARMUP = 700_000;
    private static final int REPS = 1_500_000;
    private static final int ROUNDS = 5;
    private AsciiRootMicrobench() { }

    private static String[] makeInputs(int roots, String mode) {
        String[] samples = new String[SAMPLES];
        Random random = new Random(0x6d336b65796c6578L + roots + mode.hashCode());
        for (int i = 0; i < samples.length; i++) {
            int pos = random.nextInt(roots);
            char first = pos < 26 ? (char) ('a' + pos) : (char) ('A' + pos - 26);
            samples[i] = switch (mode) {
                case "hit" -> random.nextBoolean() ? Character.toString(first) : first + "_suffix";
                case "miss" -> random.nextBoolean() ? "!missing" : "#missing";
                case "unicode" -> random.nextBoolean() ? "₹" : "😀";
                case "deep" -> first + (random.nextBoolean() ? "_suffix" : "_suffixed");
                default -> throw new IllegalArgumentException("unknown mode " + mode);
            };
        }
        return samples;
    }

    private static long assay(StaticGrammarLexicon table, String[] samples,
            boolean prefix, int reps) {
        long total = 0;
        int mask = samples.length - 1;
        // Deterministic non-constant, no strings or arrays created during the loop.
        for (int i = 0; i < reps; i++) {
            String sample = samples[(i * 1637 + 41) & mask];
            total += prefix ? table.longest(sample, 0, sample.length())
                    : table.exact(sample, 0, sample.length());
        }
        sink = total;
        return total;
    }

    public static void main(String[] args) {
        if (args.length != 3) throw new IllegalArgumentException("roots / hit|miss|deep|unicode / exact|longest");
        int roots = Integer.parseInt(args[0]);
        if (roots < 1 || roots > 52) throw new IllegalArgumentException("roots 1..52");
        String mode = args[1];
        boolean prefix = switch (args[2]) {
            case "exact" -> false;
            case "longest" -> true;
            default -> throw new IllegalArgumentException("unknown search mode");
        };
        List<StaticGrammarLexicon.Literal> rows = new ArrayList<>();
        for (int i = 0; i < roots; i++) {
            char first = i < 26 ? (char) ('a' + i) : (char) ('A' + i - 26);
            rows.add(new StaticGrammarLexicon.Literal(Character.toString(first), i * 2 + 1, false));
            rows.add(new StaticGrammarLexicon.Literal(first + "_suffix", i * 2 + 2, false));
        }
        rows.add(new StaticGrammarLexicon.Literal("₹", roots * 2 + 1, false));
        rows.add(new StaticGrammarLexicon.Literal("😀", roots * 2 + 2, false));
        StaticGrammarLexicon table = StaticGrammarLexicon.compile("bench", "v1", rows);
        String[] inputs = makeInputs(roots, mode);
        assay(table, inputs, prefix, WARMUP);
        long[] ns = new long[ROUNDS];
        long check = Long.MIN_VALUE;
        for (int round = 0; round < ROUNDS; round++) {
            long before = System.nanoTime();
            long checksum = assay(table, inputs, prefix, REPS);
            ns[round] = System.nanoTime() - before;
            if (check != Long.MIN_VALUE && checksum != check) throw new AssertionError("checksum drift");
            check = checksum;
        }
        Arrays.sort(ns);
        System.out.printf("roots=%d mode=%s search=%s median_ns_per_op=%.3f "
                + "samples=%d reps_per_round=%d rounds=%d checksum=%d digest=%s%n",
                roots, mode, prefix ? "longest" : "exact", ns[ROUNDS / 2] / (double) REPS,
                SAMPLES, REPS, ROUNDS, check, table.grammarSha256());
    }
}
