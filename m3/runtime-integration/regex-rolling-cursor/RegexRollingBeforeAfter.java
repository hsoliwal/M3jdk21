// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
import java.util.Arrays;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Pattern;

/** Isolated stock-Java source-extracted rolling methods over a tuple-DAG fixture. */
public final class RegexRollingBeforeAfter {
    private static volatile int blackhole;
    private static long checks;
    private static final int STAR = 1 << 16;
    private static final int WARMUP = 5;
    private static final int ROUNDS = 7;

    private record Case(String name, M3String source, String raw,
                        LegacyRegexRolling.Plan oldPlan,
                        CursorRegexRolling.Plan newPlan, int iterations) { }

    public static void main(String[] args) {
        M3String nested = M3String.text("abc");
        String full = "abc";
        for (int i = 0; i < 8; i++) {
            nested = M3String.join(nested, nested);
            full = full + full;
        }
        String pattern = "a*".repeat(34) + ".*";
        int[] atoms = compile(pattern);
        Case[] samples = {
                create("flat-768", M3String.text(full), full, atoms, 125),
                create("tuple-dag-768", nested, full, atoms, 125),
                create("sliced-tuple-766", nested.slice(1, nested.length() - 1),
                        full.substring(1, full.length() - 1), atoms, 125)
        };
        for (Case sample : samples) verify(sample, pattern);
        fuzz();
        for (Case sample : samples) measure(sample);
        System.out.println("M3_REGEX_ROLLING_DIFFERENTIAL_PASS checks=" + checks);
    }

    private static Case create(String name, M3String source, String raw,
                               int[] atoms, int iterations) {
        return new Case(name, source, raw,
                new LegacyRegexRolling.Plan(atoms),
                new CursorRegexRolling.Plan(atoms), iterations);
    }

    private static int[] compile(String regex) {
        int[] data = new int[regex.length()];
        int count = 0;
        for (int i = 0; i < regex.length(); i++) {
            char unit = regex.charAt(i);
            if (unit == '*') {
                if (count == 0 || (data[count - 1] & STAR) != 0)
                    throw new IllegalArgumentException("unsupported");
                data[count - 1] |= STAR;
            } else {
                data[count++] = unit;
            }
        }
        return Arrays.copyOf(data, count);
    }

    private static void verify(Case sample, String regex) {
        int oldValue = LegacyRegexRolling.match(sample.source, sample.oldPlan);
        int newValue = CursorRegexRolling.match(sample.source, sample.newPlan);
        checks++;
        if (oldValue != newValue) throw new AssertionError(sample.name + " parity");
        if (oldValue >= 0) {
            boolean expected = Pattern.matches(regex, sample.raw);
            checks++;
            if ((oldValue == 1) != expected)
                throw new AssertionError(sample.name + " Pattern oracle");
        }
    }

    private static void fuzz() {
        Random random = new Random(0x4d33524547455837L);
        String[] tokens = {"a*", "b*", "c*", ".*", "a", "b", "c"};
        String[] leaves = {"a", "b", "c", "ab", "", "\n", "\r",
                "\u0085", "\u2028", "\ud83d", "\ude42"};
        for (int trial = 0; trial < 5_000; trial++) {
            StringBuilder pattern = new StringBuilder("a*");
            int count = 33 + random.nextInt(22);
            for (int j = 0; j < count; j++) {
                pattern.append(tokens[random.nextInt(tokens.length)]);
            }
            StringBuilder raw = new StringBuilder();
            M3String view = M3String.text("");
            int parts = 1 + random.nextInt(12);
            for (int j = 0; j < parts; j++) {
                String piece = leaves[random.nextInt(leaves.length)];
                raw.append(piece);
                view = M3String.join(view, M3String.text(piece));
            }
            if (view.length() != raw.length()) throw new AssertionError("fixture size");
            int[] atoms = compile(pattern.toString());
            int before = LegacyRegexRolling.match(view, new LegacyRegexRolling.Plan(atoms));
            int after = CursorRegexRolling.match(view, new CursorRegexRolling.Plan(atoms));
            checks++;
            if (before != after) {
                throw new AssertionError("old/new trial=" + trial);
            }
            if (before >= 0) {
                boolean expected = Pattern.matches(pattern.toString(), raw.toString());
                checks++;
                if ((before == 1) != expected) {
                    throw new AssertionError("Pattern parity trial=" + trial);
                }
            }
            int begin = random.nextInt(view.length() + 1);
            int end = begin + random.nextInt(view.length() - begin + 1);
            M3String sliced = view.slice(begin, end);
            int oldSlice = LegacyRegexRolling.match(sliced,
                    new LegacyRegexRolling.Plan(atoms));
            int newSlice = CursorRegexRolling.match(sliced,
                    new CursorRegexRolling.Plan(atoms));
            checks++;
            if (oldSlice != newSlice) {
                throw new AssertionError("slice cursor trial=" + trial);
            }
            if (oldSlice >= 0) {
                boolean expected = Pattern.matches(pattern.toString(),
                        raw.substring(begin, end));
                checks++;
                if ((oldSlice == 1) != expected) {
                    throw new AssertionError("slice Pattern parity trial=" + trial);
                }
            }
        }
    }

    private static int execute(Case sample, boolean cursor) {
        int result = 0;
        for (int i = 0; i < sample.iterations; i++) {
            result += cursor
                    ? CursorRegexRolling.match(sample.source, sample.newPlan)
                    : LegacyRegexRolling.match(sample.source, sample.oldPlan);
        }
        blackhole = result;
        return result;
    }

    private static long median(long[] source) {
        long[] ordered = source.clone();
        Arrays.sort(ordered);
        return ordered[ordered.length >>> 1];
    }

    private static void measure(Case sample) {
        for (int i = 0; i < WARMUP; i++) {
            if (execute(sample, false) != execute(sample, true))
                throw new AssertionError("warmup mismatch");
        }
        long[] old = new long[ROUNDS];
        long[] updated = new long[ROUNDS];
        for (int r = 0; r < ROUNDS; r++) {
            if ((r & 1) == 0) {
                long start = System.nanoTime();
                int a = execute(sample, false);
                old[r] = System.nanoTime() - start;
                start = System.nanoTime();
                int b = execute(sample, true);
                updated[r] = System.nanoTime() - start;
                if (a != b) throw new AssertionError("round mismatch");
            } else {
                long start = System.nanoTime();
                int b = execute(sample, true);
                updated[r] = System.nanoTime() - start;
                start = System.nanoTime();
                int a = execute(sample, false);
                old[r] = System.nanoTime() - start;
                if (a != b) throw new AssertionError("round mismatch");
            }
        }
        double baseline = (double) median(old) / sample.iterations;
        double candidate = (double) median(updated) / sample.iterations;
        System.out.printf(Locale.ROOT,
                "M3_REGEX_ROLLING_AB_RESULT case=%s before_ns=%.3f"
                + " after_ns=%.3f speedup=%.3fx change_pct=%.2f%%"
                + " samples=%d iterations=%d simulated_owner=true%n",
                sample.name, baseline, candidate, baseline / candidate,
                (candidate / baseline - 1.0) * 100.0,
                ROUNDS, sample.iterations);
        System.out.println("M3_REGEX_ROLLING_AB_RAW case=" + sample.name
                + " before=" + Arrays.toString(old)
                + " after=" + Arrays.toString(updated));
    }
}
