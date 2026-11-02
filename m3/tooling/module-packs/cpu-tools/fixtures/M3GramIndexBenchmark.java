// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
import com.m3.tools.text.ngram.NGrams;
import java.util.Arrays;
import java.util.Map;
import java.util.SplittableRandom;

/** Repeatable diagnostic only: includes index preparation and materialization costs. */
public final class M3GramIndexBenchmark {
    private static volatile long sink;
    private M3GramIndexBenchmark() { }

    public static void main(String[] args) {
        String[] samples = {"abacabcbca".repeat(12_000), randomText(120_000)};
        String[] names = {"repetitive", "varied"};
        for (int sample = 0; sample < samples.length; sample++) {
            for (int size : new int[]{2, 5, 8}) {
                Map<String, Integer> expected = NGrams.count(samples[sample], size);
                Map<String, Integer> prepared = NGrams.prepare(samples[sample], size).counts();
                if (!expected.equals(prepared)) throw new AssertionError("different n-gram results");
                for (int warmup = 0; warmup < 3; warmup++) {
                    sink += NGrams.count(samples[sample], size).size();
                    sink += NGrams.prepare(samples[sample], size).counts().size();
                }
                long[] legacy = new long[7], positional = new long[7];
                for (int iteration = 0; iteration < legacy.length; iteration++) {
                    long start = System.nanoTime();
                    sink += NGrams.count(samples[sample], size).size();
                    legacy[iteration] = System.nanoTime() - start;
                    start = System.nanoTime();
                    sink += NGrams.prepare(samples[sample], size).counts().size();
                    positional[iteration] = System.nanoTime() - start;
                }
                Arrays.sort(legacy);
                Arrays.sort(positional);
                System.out.printf("gram_benchmark\t%s\tn=%d\tutf16=%d\tdistinct=%d"
                                + "\tlegacy_ms=%.3f\tindexed_ms=%.3f\tindexed_over_legacy=%.2f%n",
                        names[sample], size, samples[sample].length(), expected.size(),
                        legacy[3] / 1_000_000.0d, positional[3] / 1_000_000.0d,
                        positional[3] / (double) legacy[3]);
            }
        }
        if (sink == Long.MIN_VALUE) throw new AssertionError("prevent dead-code elimination");
    }

    private static String randomText(int units) {
        char[] alphabet = {'a', 'b', 'c', 'd', 'e', 'f', '0', '1', '\u00e9', '\u0301', '\ud83d', '\ude42'};
        SplittableRandom random = new SplittableRandom(20261024);
        char[] value = new char[units];
        for (int i = 0; i < units; i++) value[i] = alphabet[random.nextInt(alphabet.length)];
        return new String(value);
    }
}
