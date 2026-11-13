/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
 */

import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SplittableRandom;

/**
 * Timing probe of leaf jbcoll-mask-compactor-perf-20261106 (MASK_COMPACTOR_TIMING v1; not JMH).
 *
 * <p>Times compact(Object[], int, int, long[]) of renamed compactor copies (jbcoll.MaskCompactor_LABEL, written by
 * variants.py) on fixed mask classes x lengths (default 64/512/1024/4096, -Dtiming.lengths=a,b,...). All variants run in one JVM and are interleaved per
 * sample (start variant rotates), so machine-load drift hits every variant alike. Each variant is reached through one
 * interface call site (megamorphic, the same for all) and every repetition first restores the work array from the
 * source with System.arraycopy; the pseudo-variant {@code restore} measures exactly that overhead (no compaction), so
 * net = variant - restore. Warm-up: 3 passes over every cell and variant before any sample. Per cell and variant the
 * output is the median ns/op over SAMPLES samples of max(32, 131072 / length) repetitions.
 *
 * <p>Mask classes (seeded from SEED and the cell index): dense = Bernoulli 3/4, sparse = Bernoulli 1/32, fragmented =
 * alternating (0x5555...), every8th = 0x0101..., block = [n/4, 3n/4), prefix = clean first half + alternating second
 * half, tail = only bit n-1, random50 = Bernoulli 1/2, bernK = Bernoulli 1/K, periodK = every K-th entry,
 * clusterK = 3 entries out of every K (-Dtiming.classes=a,b,... picks the classes; default the eight above). Each line also carries the true run/removed counts and the first deletion;
 * timing_table.py derives each variant's path from (length, runs, removed).
 *
 * <p>usage: java -cp DRIVER:VARIANTS CompactorTiming SEED RUN SAMPLES LABEL...
 */
public final class CompactorTiming {
    /** One compactor variant behind one call site. */
    public interface Compactor {
        int compact(Object[] values, int from, int to, long[] deleted);
    }

    private static final String[] CLASSES = System.getProperty("timing.classes",
            "dense,sparse,fragmented,every8th,block,prefix,tail,random50").split(",");
    private static final int[] LENGTHS = Arrays.stream(System.getProperty("timing.lengths", "64,512,1024,4096")
            .split(",")).mapToInt(Integer::parseInt).toArray();
    private static long sink;

    private CompactorTiming() { }

    static Compactor compactor(String label) throws Throwable {
        if (label.equals("restore")) return (values, from, to, deleted) -> to;
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodType type = MethodType.methodType(int.class, Object[].class, int.class, int.class, long[].class);
        MethodHandle target = lookup.findStatic(Class.forName("jbcoll.MaskCompactor_" + label), "compact", type);
        return (Compactor) LambdaMetafactory.metafactory(lookup, "compact", MethodType.methodType(Compactor.class),
                type, target, type).getTarget().invokeExact();
    }

    static long[] mask(String kind, int n, SplittableRandom random) {
        long[] mask = new long[(n + 63) >>> 6];
        for (int i = 0; i < n; i++) {
            boolean deleted = switch (kind) {
                case "dense" -> random.nextInt(4) != 0;
                case "sparse" -> random.nextInt(32) == 0;
                case "fragmented" -> (i & 1) == 0;
                case "every8th" -> (i & 7) == 0;
                case "block" -> i >= n / 4 && i < 3 * n / 4;
                case "prefix" -> i >= n / 2 && (i & 1) == 0;
                case "tail" -> i == n - 1;
                case "random50" -> random.nextBoolean();
                default -> kind.startsWith("period") ? i % Integer.parseInt(kind.substring("period".length())) == 0
                        : kind.startsWith("cluster") ? i % Integer.parseInt(kind.substring("cluster".length())) < 3
                        : random.nextInt(Integer.parseInt(kind.substring("bern".length()))) == 0;
            };
            if (deleted) mask[i >>> 6] |= 1L << i;
        }
        return mask;
    }

    static long sample(Compactor c, Object[] source, Object[] work, int n, long[] mask, int reps) {
        long acc = 0;
        long start = System.nanoTime();
        for (int r = 0; r < reps; r++) {
            System.arraycopy(source, 0, work, 0, n);
            acc += c.compact(work, 0, n, mask);
        }
        long elapsed = System.nanoTime() - start;
        sink += acc;
        return elapsed;
    }

    public static void main(String[] args) throws Throwable {
        long seed = Long.decode(args[0]);
        String run = args[1];
        int samples = Integer.parseInt(args[2]);
        List<String> labels = new ArrayList<>(List.of("restore"));
        labels.addAll(Arrays.asList(args).subList(3, args.length));
        Compactor[] variants = new Compactor[labels.size()];
        for (int v = 0; v < variants.length; v++) variants[v] = compactor(labels.get(v));
        Object[] pool = new Object[4096];
        for (int i = 0; i < pool.length; i++) pool[i] = Integer.valueOf(i);
        int cells = CLASSES.length * LENGTHS.length;
        long[][] masks = new long[cells][];
        for (int c = 0; c < cells; c++) {
            masks[c] = mask(CLASSES[c / LENGTHS.length], LENGTHS[c % LENGTHS.length], new SplittableRandom(seed + c));
        }
        Object[] work = new Object[4096];
        for (int pass = 0; pass < 3; pass++) {
            for (int c = 0; c < cells; c++) {
                int n = LENGTHS[c % LENGTHS.length];
                for (Compactor variant : variants) sample(variant, pool, work, n, masks[c], 2 * Math.max(32, 131072 / n));
            }
        }
        System.out.println("TIMING_BEGIN run=" + run + " seed=0x" + Long.toHexString(seed) + " samples=" + samples
                + " labels=" + labels + " java=" + System.getProperty("java.version"));
        for (int c = 0; c < cells; c++) {
            int n = LENGTHS[c % LENGTHS.length];
            int reps = Math.max(32, 131072 / n);
            long[] mask = masks[c];
            long[][] nanos = new long[variants.length][samples];
            for (int s = 0; s < samples; s++) {
                for (int k = 0; k < variants.length; k++) {
                    int v = (s + k) % variants.length;
                    nanos[v][s] = sample(variants[v], pool, work, n, mask, reps);
                }
            }
            int runs = 0;
            int removed = 0;
            int first = n;
            for (int i = 0; i < n; i++) {
                boolean bit = (mask[i >>> 6] & (1L << i)) != 0;
                boolean previous = i > 0 && (mask[(i - 1) >>> 6] & (1L << (i - 1))) != 0;
                if (bit) removed++;
                if (bit && first == n) first = i;
                if (bit && !previous) runs++;
            }
            StringBuilder line = new StringBuilder("TIMING run=" + run + " class=" + CLASSES[c / LENGTHS.length]
                    + " length=" + n + " runs=" + runs + " removed=" + removed + " first=" + first + " reps=" + reps);
            for (int v = 0; v < variants.length; v++) {
                Arrays.sort(nanos[v]);
                long median = nanos[v][samples / 2];
                line.append(' ').append(labels.get(v)).append('=').append(String.format(java.util.Locale.ROOT, "%.1f", median / (double) reps));
            }
            System.out.println(line);
        }
        System.out.println("TIMING_END run=" + run + " sink=" + sink);
    }
}
