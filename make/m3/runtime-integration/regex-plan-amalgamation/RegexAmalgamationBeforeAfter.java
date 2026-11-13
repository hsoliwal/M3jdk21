// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Pattern;

/**
 * Exact Java21 source before/after regex engines with an immutable owner fixture.
 * Pattern.matches is the independent semantic oracle, not a substitute engine.
 * This cannot qualify HotSpot/JNI, nor is it a JMH end-to-end benchmark.
 */
public final class RegexAmalgamationBeforeAfter {
    private static final Random RANDOM = new Random(0x4d33414d414c4741L);
    private static final String[] TOKENS =
            {"a", "b", "c", "Q", "Z", "A", ".", "a*", "b*", "c*", ".*"};
    private static final String[] CHUNKS =
            {"a", "b", "c", "ab", "bc", "aa", "", "\n", "\r",
                    "\u0085", "\u2028", "\ud83d", "\ude42"};
    private static long checks;
    private static volatile long blackhole;

    private record Workload(String name, M3String source, String expression,
                            int iterations) {}

    public static void main(String[] args) throws Exception {
        deterministic();
        randomDifferential();
        retainedPlanBound();
        benchmarks();
        System.out.println("M3_REGEX_AMALGAMATION_DIFFERENTIAL_PASS checks=" + checks);
    }

    private static M3String make(String text, int parts) {
        if (parts <= 1) return M3String.text(text);
        int width = Math.max(1, text.length() / parts);
        M3String composite = M3String.text("");
        for (int start = 0; start < text.length(); start += width) {
            composite = M3String.join(composite,
                    M3String.text(text.substring(start, Math.min(text.length(), start + width))));
        }
        return composite;
    }

    private static void deterministic() {
        compare(make("a".repeat(300) + "b".repeat(300), 12),
                "a*Q.*", true, true);
        compare(make("a".repeat(300) + "b".repeat(300), 12),
                "a*Q.*", false, true);
        compare(make("a", 1), "a*b*c*QRTS", true, true);
        compare(make("b".repeat(100), 4), "Qb*", true, true);
        compare(make("a".repeat(100), 4), "a*Q", true, true);
        compare(make("a".repeat(80) + "b".repeat(80), 8), "a*b*", true, true);
        compare(make("a".repeat(80) + "b".repeat(80), 8), "a.*b", false, true);
        compare(make("a\ud83d\ude42b", 4), "a.*b", true, true);
        compare(make("a\ud83d\ude42b", 4), "a*Q.*", true, true);
        compare(make("abccba", 5).slice(1, 5), "a*b*c*b*a*", true, true);
        compare(make("a".repeat(40) + "b".repeat(40), 8),
                "a*".repeat(36) + "Q.*", true, true);
        compare(make("abc".repeat(40), 8),
                "a*".repeat(36) + ".*", false, true);
    }

    private static void compare(M3String source, String regex,
                                boolean prepareFacts, boolean fullOracle) {
        if (prepareFacts) source.prepareFacts();
        int before = RegexBefore.tryMatch(source, regex);
        int after = RegexAfter.tryMatch(source, regex);
        checks++;
        boolean truth = Pattern.matches(regex, source.toString());
        if ((before >= 0 && (before == 1) != truth)
                || (after >= 0 && (after == 1) != truth)) {
            throw new AssertionError("Pattern disagreement text=" + source
                    + " regex=" + regex + " before=" + before
                    + " after=" + after + " truth=" + truth);
        }
        if (before >= 0 && after < 0) {
            throw new AssertionError("amalgamation lost an exact admission");
        }
        if (fullOracle) {
            checks++;
            if (before >= 0 && after >= 0 && before != after) {
                throw new AssertionError("admitted decisions differ");
            }
        }
    }

    private static void randomDifferential() {
        for (int trial = 0; trial < 12_288; trial++) {
            StringBuilder expression = new StringBuilder("a*");
            int count = 1 + RANDOM.nextInt(54);
            for (int i = 0; i < count; i++) {
                expression.append(TOKENS[RANDOM.nextInt(TOKENS.length)]);
            }
            int leaves = 1 + RANDOM.nextInt(12);
            M3String source = M3String.text("");
            for (int i = 0; i < leaves; i++) {
                source = M3String.join(source,
                        M3String.text(CHUNKS[RANDOM.nextInt(CHUNKS.length)]));
            }
            if ((trial & 7) == 0 && source.length() > 2) {
                int left = RANDOM.nextInt(source.length());
                int right = left + RANDOM.nextInt(source.length() - left + 1);
                source = source.slice(left, right);
            }
            compare(source, expression.toString(), (trial & 1) == 0, true);
        }
    }

    private static void retainedPlanBound() throws Exception {
        Method old = RegexBefore.class.getDeclaredMethod("maximumRetainedPrimitiveBytes");
        Method next = RegexAfter.class.getDeclaredMethod("maximumRetainedPrimitiveBytes");
        old.setAccessible(true);
        next.setAccessible(true);
        long older = (long) old.invoke(null);
        long newer = (long) next.invoke(null);
        long difference = newer - older;
        checks++;
        if (difference != (64L + 256L) * (3L * Integer.BYTES + Long.BYTES)) {
            throw new AssertionError("unaccounted plan memory delta=" + difference);
        }
        System.out.println("M3_REGEX_AMALGAMATION_PLAN_BYTES before=" + older
                + " after=" + newer + " max_extra=" + difference);
    }

    private static long execute(Workload workload, boolean amalgamation) {
        long checksum = 0;
        for (int n = 0; n < workload.iterations; n++) {
            checksum += amalgamation
                    ? RegexAfter.tryMatch(workload.source, workload.expression)
                    : RegexBefore.tryMatch(workload.source, workload.expression);
        }
        blackhole = checksum;
        return checksum;
    }

    private static long median(long[] values) {
        long[] copy = values.clone();
        Arrays.sort(copy);
        return copy[copy.length / 2];
    }

    private static void benchmarks() {
        M3String absent = make("a".repeat(1024) + "b".repeat(1024), 16);
        absent.prepareFacts();
        M3String compatible = make("a".repeat(700) + "b".repeat(700), 16);
        compatible.prepareFacts();
        M3String edge = make("b".repeat(1000), 12);
        M3String shortText = make("abc", 2);

        Workload[] workloads = {
            new Workload("missing-middle-prepared", absent, "a*Q.*", 5000),
            new Workload("minimum-length", shortText, "a*b*c*QRST", 5000),
            new Workload("fixed-first", edge, "Qb*", 5000),
            new Workload("matching-prepared", compatible, "a*b*", 5000)
        };
        for (Workload workload : workloads) {
            int baseline = RegexBefore.tryMatch(workload.source, workload.expression);
            int candidate = RegexAfter.tryMatch(workload.source, workload.expression);
            if (baseline != candidate) {
                // This fixture has no surrogates, so exact Boolean decisions must agree.
                throw new AssertionError("benchmark source parity");
            }
            for (int j = 0; j < 4; j++) {
                if (execute(workload, false) != execute(workload, true)) {
                    throw new AssertionError("warmup mismatch");
                }
            }
            long[] before = new long[9];
            long[] after = new long[9];
            for (int round = 0; round < before.length; round++) {
                if ((round & 1) == 0) {
                    long start = System.nanoTime();
                    long a = execute(workload, false);
                    before[round] = System.nanoTime() - start;
                    start = System.nanoTime();
                    long b = execute(workload, true);
                    after[round] = System.nanoTime() - start;
                    if (a != b) throw new AssertionError("round parity");
                } else {
                    long start = System.nanoTime();
                    long b = execute(workload, true);
                    after[round] = System.nanoTime() - start;
                    start = System.nanoTime();
                    long a = execute(workload, false);
                    before[round] = System.nanoTime() - start;
                    if (a != b) throw new AssertionError("round parity");
                }
            }
            double oldNs = (double) median(before) / workload.iterations;
            double newNs = (double) median(after) / workload.iterations;
            System.out.printf(Locale.ROOT,
                    "M3_REGEX_AMALGAMATION_AB case=%s before_ns=%.3f after_ns=%.3f"
                    + " speedup=%.3fx delta_pct=%.2f%% iterations=%d samples=9"
                    + " stock_java21_fixture=true%n",
                    workload.name, oldNs, newNs, oldNs / newNs,
                    (newNs / oldNs - 1.0) * 100, workload.iterations);
            System.out.println("M3_REGEX_AMALGAMATION_AB_RAW case=" + workload.name
                    + " before=" + Arrays.toString(before)
                    + " after=" + Arrays.toString(after));
        }
    }
}
