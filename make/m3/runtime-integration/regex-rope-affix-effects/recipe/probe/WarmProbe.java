// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
import java.util.Arrays;
import java.util.Locale;

public final class WarmProbe {
    private static volatile long blackhole;
    private static long run(M3String[] texts, String regex, int rounds, boolean next) {
        long begin = System.nanoTime();
        long sum = 0;
        for (int i = 0; i < rounds; i++) {
            M3String source = texts[i % texts.length];
            sum += next ? RegexAfter.tryMatch(source, regex) : RegexBefore.tryMatch(source, regex);
        }
        blackhole = sum;
        return System.nanoTime() - begin;
    }
    private static M3String make(String prefix, String suffix, boolean fail) {
        return M3String.join(M3String.join(M3String.text(fail ? "aXc" + prefix : prefix),
                M3String.text("M".repeat(2048))), M3String.text(suffix));
    }
    private static void measure(String label, String prefix, String suffix, boolean fail, boolean cold) {
        String regex = prefix + ".*" + suffix;
        M3String[] sources = new M3String[cold ? 1024 : 1];
        for (int i = 0; i < sources.length; i++) sources[i] = make(prefix, suffix, fail);
        int rounds = cold ? 2048 : 100000;
        for (int i = 0; i < 4; i++) {run(sources, regex, rounds, false);run(sources, regex, rounds, true);}
        long[] before = new long[7], after = new long[7];
        for (int i = 0; i < before.length; i++) {
            if ((i & 1) == 0) {
                before[i] = run(sources, regex, rounds, false);
                after[i] = run(sources, regex, rounds, true);
            } else {
                after[i] = run(sources, regex, rounds, true);
                before[i] = run(sources, regex, rounds, false);
            }
        }
        Arrays.sort(before); Arrays.sort(after);
        System.out.printf(Locale.ROOT, "AFFIX_WARM_PROBE case=%s before_ns=%.2f after_ns=%.2f ratio=%.3f source_fixture=true%n",
                label, before[3] / (double) rounds, after[3] / (double) rounds, after[3] / (double) before[3]);
    }
    public static void main(String[] args) {
        measure("warm-positive-short", "abc", "xyz", false, false);
        measure("warm-positive-long", "abcdefghijkl", "MNOPQRSTUVWX", false, false);
        measure("warm-prefix-negative", "abc", "xyz", true, false);
        measure("cache-churn-prefix-negative", "abc", "xyz", true, true);
    }
}
