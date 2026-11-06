/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
 */

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.SplittableRandom;

/**
 * Differential driver of leaf jbcoll-mask-compactor-20261106 (DIFF_MASK_COMPACTOR v1).
 *
 * <p>Runs M3CollectionMaskCompactor.compact(Object[], int, int, long[]) of the live master class (patched java.base,
 * jdk.internal.mindex, needs --add-exports) and of renamed copies of the census postimage and the retained variants
 * (classes jbcoll.MaskCompactor_LABEL, compiled outside java.base by verify.sh) over the same seeded cases. Cases cover
 * lengths 0..4100 around the 512 and 1024 dispatch thresholds, offsets, random-density, run, sparse, alternating,
 * word-block, boundary-bit, all-zero and all-one masks, garbage bits past the length and extra mask words, and the
 * refusal cases (mask too short, bad range). Per case and variant the outcome is the returned end and a hash of the
 * whole array (survivor prefix and untouched tail), or the exception class. Every variant is compared with the live
 * class; the output lists cases, mismatches and a sha256 of the outcome stream per variant.
 *
 * <p>Mode {@code probe} prints the live maskStatistics(long[], int) value for fragmented masks (needs --add-opens) and
 * the dispatch that value selects, plus a median nanosecond timing of live and postimage on those masks (INFO).
 *
 * <p>usage: java --patch-module java.base=CLASSES --add-exports java.base/jdk.internal.mindex=ALL-UNNAMED
 *            -cp DRIVER:VARIANTS CompactorDifferential diff SEED CASES LABEL... | probe LABEL
 */
public final class CompactorDifferential {
    private static final String LIVE = "jdk.internal.mindex.M3CollectionMaskCompactor";
    private static final int[] BOUNDARY = {0, 1, 62, 63, 64, 65, 127, 128, 511, 512, 513, 1023, 1024, 1025, 2047};
    private static final Integer[] POOL = new Integer[8192];

    static {
        for (int i = 0; i < POOL.length; i++) POOL[i] = i;
    }

    private CompactorDifferential() { }

    static MethodHandle compactor(String className) throws ReflectiveOperationException {
        return MethodHandles.lookup().findStatic(Class.forName(className), "compact",
                MethodType.methodType(int.class, Object[].class, int.class, int.class, long[].class));
    }

    static int length(SplittableRandom random) {
        int band = random.nextInt(10);
        if (band < 5) return random.nextInt(512);
        if (band < 8) return 512 + random.nextInt(600);
        return 1100 + random.nextInt(3001);
    }

    static long[] mask(SplittableRandom random, int length) {
        int words = length == 0 ? 0 : ((length - 1) >>> 6) + 1;
        long[] mask = new long[words + random.nextInt(3)];
        int kind = random.nextInt(10);
        switch (kind) {
            case 0 -> { }
            case 1 -> Arrays.fill(mask, 0, words, -1L);
            case 2 -> {
                int density = random.nextInt(257);
                for (int i = 0; i < length; i++) if (random.nextInt(256) < density) mask[i >>> 6] |= 1L << i;
            }
            case 3 -> {
                int i = 0;
                boolean on = random.nextBoolean();
                while (i < length) {
                    int run = 1 + random.nextInt(1 + random.nextInt(300));
                    if (on) for (int k = i; k < Math.min(length, i + run); k++) mask[k >>> 6] |= 1L << k;
                    i += run;
                    on = !on;
                }
            }
            case 4 -> {
                int bits = random.nextInt(6);
                for (int b = 0; b < bits && length > 0; b++) {
                    int i = random.nextInt(length);
                    mask[i >>> 6] |= 1L << i;
                }
            }
            case 5 -> {
                long pattern = random.nextBoolean() ? 0x5555555555555555L : 0xAAAAAAAAAAAAAAAAL;
                Arrays.fill(mask, 0, words, pattern);
            }
            case 6 -> {
                for (int w = 0; w < words; w++) mask[w] = random.nextInt(3) == 0 ? -1L : 0L;
            }
            case 7 -> {
                for (int b : BOUNDARY) if (b < length && random.nextBoolean()) mask[b >>> 6] |= 1L << b;
            }
            case 8 -> {
                Arrays.fill(mask, 0, words, -1L);
                int holes = random.nextInt(6);
                for (int h = 0; h < holes && length > 0; h++) {
                    int i = random.nextInt(length);
                    mask[i >>> 6] &= ~(1L << i);
                }
            }
            default -> {
                for (int w = 0; w < words; w++) mask[w] = random.nextLong();
            }
        }
        if (random.nextBoolean() && words > 0 && (length & 63) != 0) mask[words - 1] |= -1L << (length & 63);
        for (int w = words; w < mask.length; w++) mask[w] = random.nextLong();
        return mask;
    }

    static String outcome(MethodHandle compactor, Object[] source, int from, int to, long[] mask) {
        Object[] values = source.clone();
        try {
            int end = (int) compactor.invokeExact(values, from, to, mask.clone());
            long hash = 0xcbf29ce484222325L;
            for (Object value : values) hash = (hash ^ (value == null ? -1 : (Integer) value)) * 0x100000001b3L;
            return end + ":" + Long.toHexString(hash);
        } catch (Throwable failure) {
            return failure.getClass().getSimpleName();
        }
    }

    static void diff(long seed, int cases, List<String> labels) throws Exception {
        List<MethodHandle> handles = new ArrayList<>();
        handles.add(compactor(LIVE));
        for (String label : labels) handles.add(compactor("jbcoll.MaskCompactor_" + label));
        int variants = handles.size();
        long[] mismatches = new long[variants];
        String[] firstMismatch = new String[variants];
        MessageDigest[] sha = new MessageDigest[variants];
        for (int v = 0; v < variants; v++) sha[v] = MessageDigest.getInstance("SHA-256");
        SplittableRandom random = new SplittableRandom(seed);
        long elements = 0;
        long refusals = 0;
        for (int c = 0; c < cases; c++) {
            int length = length(random);
            int from = random.nextInt(8);
            int tail = random.nextInt(8);
            Object[] source = Arrays.copyOf(POOL, from + length + tail, Object[].class);
            long[] mask = mask(random, length);
            int to = from + length;
            int refusal = random.nextInt(200);
            if (refusal == 0 && length > 0) mask = Arrays.copyOf(mask, (length - 1) >>> 6);
            if (refusal == 1) to = source.length + 1;
            if (refusal == 2) from = to + 1;
            elements += length;
            String reference = null;
            for (int v = 0; v < variants; v++) {
                String result = outcome(handles.get(v), source, from, to, mask);
                sha[v].update((c + " " + result + "\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
                if (v == 0) {
                    reference = result;
                    if (!result.contains(":")) refusals++;
                } else if (!result.equals(reference)) {
                    mismatches[v]++;
                    if (firstMismatch[v] == null) {
                        firstMismatch[v] = "case=" + c + " length=" + length + " live=" + reference + " got=" + result;
                    }
                }
            }
        }
        StringBuilder line = new StringBuilder("DIFF_MASK_COMPACTOR seed=0x" + Long.toHexString(seed) + " cases=" + cases
                + " elements=" + elements + " refusals=" + refusals);
        for (int v = 0; v < variants; v++) {
            String label = v == 0 ? "live" : labels.get(v - 1);
            line.append(" | ").append(label).append(" mismatches=").append(mismatches[v]).append(" sha256=")
                    .append(HexFormat.of().formatHex(sha[v].digest()));
            if (firstMismatch[v] != null) line.append(" first=").append(firstMismatch[v]);
        }
        System.out.println(line);
    }

    static void probe(String label) throws Throwable {
        Method statistics = Class.forName(LIVE).getDeclaredMethod("maskStatistics", long[].class, int.class);
        statistics.setAccessible(true);
        MethodHandle live = compactor(LIVE);
        MethodHandle other = compactor("jbcoll.MaskCompactor_" + label);
        int length = 4096;
        long[][] masks = {new long[length / 64], new long[length / 64], new long[length / 64]};
        Arrays.fill(masks[0], 0x5555555555555555L);
        Arrays.fill(masks[1], 0x0101010101010101L);
        masks[2][0] = 1L;
        masks[2][32] = 1L << 7;
        String[] names = {"alternating", "every8th", "two_bits"};
        Object[] source = Arrays.copyOf(POOL, length, Object[].class);
        for (int m = 0; m < masks.length; m++) {
            long value = (long) statistics.invoke(null, masks[m], length);
            int runs = (int) (value >>> 32);
            int removed = (int) value;
            int threshold = Math.max(4, length >>> 7);
            String dispatch = removed == 0 ? "no-op" : runs <= threshold ? "compactRuns"
                    : removed <= length / 8 || removed >= length - length / 64 ? "compactWords" : "compactScalar";
            int trueRuns = 0;
            for (int i = 0; i < length; i++) {
                boolean bit = (masks[m][i >>> 6] & (1L << i)) != 0;
                boolean previous = i > 0 && (masks[m][(i - 1) >>> 6] & (1L << (i - 1))) != 0;
                if (bit && !previous) trueRuns++;
            }
            long[] nanos = new long[2];
            MethodHandle[] pair = {live, other};
            for (int h = 0; h < 2; h++) {
                long[] samples = new long[9];
                for (int s = 0; s < samples.length; s++) {
                    long start = System.nanoTime();
                    for (int r = 0; r < 2000; r++) {
                        Object[] values = source.clone();
                        int end = (int) pair[h].invokeExact(values, 0, length, masks[m]);
                        if (end < 0) throw new AssertionError();
                    }
                    samples[s] = (System.nanoTime() - start) / 2000;
                }
                Arrays.sort(samples);
                nanos[h] = samples[samples.length / 2];
            }
            System.out.println("MASK_STATS mask=" + names[m] + " length=" + length + " threshold=" + threshold
                    + " runs_true=" + trueRuns + " maskStatistics=0x" + Long.toHexString(value) + " runs_field=" + runs
                    + " removed_field=" + removed + " live_dispatch=" + dispatch + " median_ns live=" + nanos[0] + " "
                    + label + "=" + nanos[1]);
        }
    }

    public static void main(String[] args) throws Throwable {
        if (args.length >= 3 && args[0].equals("diff")) {
            diff(Long.decode(args[1]), Integer.parseInt(args[2]), Arrays.asList(args).subList(3, args.length));
        } else if (args.length == 2 && args[0].equals("probe")) {
            probe(args[1]);
        } else {
            throw new IllegalArgumentException("usage: diff SEED CASES LABEL... | probe LABEL");
        }
    }
}
