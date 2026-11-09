// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
import java.util.Arrays;
import java.util.Locale;
import java.util.Random;

/**
 * A/B of the exact Geometry method compiled from M3 master and rank cursor PR.
 * Isolated warmed offset method only: no whole java.lang.String/HotSpot timing.
 */
public final class CodePointRankBeforeAfterBench {
    private static final int QUERY_COUNT = 1024;
    private static final int REPEATS = 64;
    private static final int WARMUP = 5;
    private static final int ROUNDS = 9;
    private static volatile long sink;
    private static long verified;

    private record Case(String name, int length, int pairedLows,
                        RankGeometryBaseline.Geometry oldGeometry,
                        RankGeometryCandidate.Geometry newGeometry,
                        int[] positions, int[] deltas) { }

    public static void main(String[] args) {
        for (Case test : new Case[]{
                prepare("ascii-512", 512, 0),
                prepare("dense-pairs-512", 512, 1),
                prepare("sparse-pairs-4096", 4096, 2),
                prepare("dense-pairs-4096", 4096, 1),
                prepare("sparse-pairs-32768", 32768, 2),
                prepare("dense-pairs-32768", 32768, 1)}) {
            verify(test);
            measure(test);
        }
        System.out.println("M3_BA_DIFFERENTIAL_PASS checks=" + verified);
    }

    private static Case prepare(String name, int length, int pattern) {
        char[] source = new char[length];
        Arrays.fill(source, 'x');
        if (pattern != 0) {
            int stride = pattern == 1 ? 2 : 16;
            for (int i = 0; i + 1 < length; i += stride) {
                source[i] = '\ud83d';
                source[i + 1] = '\ude42';
            }
        }
        int[] buffer = new int[length >>> 1];
        int count = 0;
        for (int i = 1; i < length; i++) {
            if (Character.isHighSurrogate(source[i - 1])
                    && Character.isLowSurrogate(source[i])) {
                buffer[count++] = i;
            }
        }
        int[] pairedLows = Arrays.copyOf(buffer, count);
        int[] positions = new int[QUERY_COUNT];
        int[] deltas = new int[QUERY_COUNT];
        Random random = new Random((long) length * 0x4d334250L + pattern);
        for (int q = 0; q < QUERY_COUNT; q++) {
            int position = random.nextInt(length + 1);
            int availableForward = Character.codePointCount(
                    source, position, length - position);
            int availableBackward = Character.codePointCount(source, 0, position);
            boolean forward = (q & 1) == 0;
            if (forward && availableForward == 0) forward = false;
            if (!forward && availableBackward == 0) forward = true;
            int available = forward ? availableForward : availableBackward;
            // Include some large jumps; other queries are typical small jumps.
            int limit = (q % 17 == 0) ? 2048 : 32;
            int magnitude = 1 + random.nextInt(Math.min(available, limit));
            positions[q] = position;
            deltas[q] = forward ? magnitude : -magnitude;
        }
        return new Case(name, length, count,
                new RankGeometryBaseline.Geometry(length, pairedLows),
                new RankGeometryCandidate.Geometry(length, pairedLows),
                positions, deltas);
    }

    private static void verify(Case test) {
        for (int i = 0; i < QUERY_COUNT; i++) {
            int index = test.positions[i];
            int delta = test.deltas[i];
            int before = test.oldGeometry.offsetByCodePoints(index, delta);
            int after = test.newGeometry.offsetByCodePoints(index, delta);
            // Independently exercise UTF-16 oracle on the actual generated
            // text shape using paired-low geometry.
            char[] text = new char[test.length];
            Arrays.fill(text, 'x');
            int[] lows = pairedLows(test);
            for (int pos : lows) {
                text[pos - 1] = '\ud83d';
                text[pos] = '\ude42';
            }
            int expected = Character.offsetByCodePoints(
                    new StringBuilder(new String(text)), index, delta);
            if (before != after || before != expected) {
                throw new AssertionError(test.name + " position=" + index
                        + " delta=" + delta + " before=" + before
                        + " after=" + after + " oracle=" + expected);
            }
            verified++;
        }
    }

    private static int[] pairedLows(Case test) {
        // Single construction for each query proof is deliberate but
        // excluded from the benchmark timing.
        int[] result = new int[test.pairedLows];
        int at = 0;
        int stride = test.name.startsWith("dense") ? 2 : 16;
        if (test.name.startsWith("ascii")) return result;
        for (int pos = 1; pos < test.length && at < result.length; pos += stride) {
            result[at++] = pos;
        }
        return result;
    }

    private static long useBefore(Case test) {
        long result = 0;
        for (int repeat = 0; repeat < REPEATS; repeat++) {
            for (int i = 0; i < QUERY_COUNT; i++) {
                result += test.oldGeometry.offsetByCodePoints(
                        test.positions[i], test.deltas[i]);
            }
        }
        sink = result;
        return result;
    }

    private static long useAfter(Case test) {
        long result = 0;
        for (int repeat = 0; repeat < REPEATS; repeat++) {
            for (int i = 0; i < QUERY_COUNT; i++) {
                result += test.newGeometry.offsetByCodePoints(
                        test.positions[i], test.deltas[i]);
            }
        }
        sink = result;
        return result;
    }

    private static void measure(Case test) {
        for (int i = 0; i < WARMUP; i++) {
            if (useBefore(test) != useAfter(test)) {
                throw new AssertionError("mismatched checksum for " + test.name);
            }
        }
        long[] oldTime = new long[ROUNDS];
        long[] newTime = new long[ROUNDS];
        for (int round = 0; round < ROUNDS; round++) {
            // Alternating order reduces fixed A-first measurement bias.
            if ((round & 1) == 0) {
                long a = System.nanoTime();
                long av = useBefore(test);
                oldTime[round] = System.nanoTime() - a;
                long b = System.nanoTime();
                long bv = useAfter(test);
                newTime[round] = System.nanoTime() - b;
                if (av != bv) throw new AssertionError("round checksum differs");
            } else {
                long b = System.nanoTime();
                long bv = useAfter(test);
                newTime[round] = System.nanoTime() - b;
                long a = System.nanoTime();
                long av = useBefore(test);
                oldTime[round] = System.nanoTime() - a;
                if (av != bv) throw new AssertionError("round checksum differs");
            }
        }
        long oldMedian = median(oldTime);
        long newMedian = median(newTime);
        double operations = (double) QUERY_COUNT * REPEATS;
        double oldNs = oldMedian / operations;
        double newNs = newMedian / operations;
        System.out.printf(Locale.ROOT,
                "M3_CODEPOINT_BA_RESULT dataset=%s utf16=%d pairs=%d"
                + " before_ns_per_op=%.3f after_ns_per_op=%.3f"
                + " speedup=%.3fx after_change_pct=%.1f%%"
                + " sample_count=%d ops_per_sample=%d"
                + " max_min_noise_before=%.3fx max_min_noise_after=%.3fx%n",
                test.name, test.length, test.pairedLows,
                oldNs, newNs, oldNs / newNs,
                100.0 * (newNs / oldNs - 1.0),
                ROUNDS, QUERY_COUNT * REPEATS,
                (double) max(oldTime) / min(oldTime),
                (double) max(newTime) / min(newTime));
        System.out.println("M3_CODEPOINT_BA_RAW dataset=" + test.name
                + " before_nanos=" + Arrays.toString(oldTime)
                + " after_nanos=" + Arrays.toString(newTime));
    }

    private static long median(long[] values) {
        long[] sorted = values.clone();
        Arrays.sort(sorted);
        return sorted[sorted.length >>> 1];
    }
    private static long min(long[] values) {
        long v = Long.MAX_VALUE;
        for (long n : values) v = Math.min(n, v);
        return v;
    }
    private static long max(long[] values) {
        long v = 0;
        for (long n : values) v = Math.max(n, v);
        return v;
    }
}
