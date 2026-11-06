// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.indexdb;

import java.io.BufferedOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Random;

/**
 * Differential dump (gate 3 of the relocation, recipe/hamming-topk-20261106/differential.sh). Replays the oracle
 * inputs of {@link M3HammingTopKContract} (the 2,500 seeded trials with their seven k values, the eight all-tie
 * sizes with two k values each, the six refusals and the k=0 historical case) and then 100,000 seeded random
 * fingerprint sets (uniform fingerprints, near duplicates of the query with 0..8 or 0..64 flipped bits, and
 * distance-0 ties among noise; k fixed, random, or n) through the top-k class named by args[0], reached by
 * reflection so that the donor class, the relocated class and the tie-order mutant run the same driver. For every
 * call it prints k, n, the result length and the SHA-256 of the complete result records (index, distance,
 * fingerprint, in result order) of nearest and of fromDistances, or the exception class of a refused call. Two
 * top-k classes agree when their dumps are byte-identical; the final TOTAL line counts calls and records.
 * <p>With {@code --fingerprint FQN} it prints instead the 64-bit fingerprint of 20,000 seeded unit-weight feature
 * windows (0..64 features) through a fingerprint class: a static {@code fingerprint(long[])} (the donor SimHash) or
 * a static {@code fromFeatures(String, long[], int, int[], int, int)} followed by {@code simHash64()}
 * ({@link M3IndexDbChunkSimHash}), together with the driver's own tie mask (the bits whose vote sum is zero), so
 * the two tie rules can be compared mechanically (the NEC lane of the plan row).
 * Usage: {@code java -cp classes;test-classes jdk.m3.indexdb.M3HammingTopKDump FQN} or {@code --fingerprint FQN}.
 */
//FLLDCIM: TFG - dump driver from the template m3/arrays/recipe/csr-index-20261106 M3CsrIndexDump (reflective; serves donor, relocated and mutant class)
public final class M3HammingTopKDump {
    private static final long ORACLE_SEED = 0x4d33544845415053L;
    private static final int ORACLE_TRIALS = 2_500;
    private static final int[] TIE_SIZES = {0, 1, 2, 3, 64, 1024, 4096, 16384};
    private static final long SET_SEED = 0x4d3348414d4d494eL;
    private static final int SETS = 100_000;
    private static final long WINDOW_SEED = 0x4d3346494e475052L;
    private static final int WINDOWS = 20_000;
    private static final String DOMAIN = "m3:indexdb:hamming-topk-dump:v1";
    private static final HexFormat HEX = HexFormat.of();
    private final Method nearest;
    private final Method fromDistances;
    private final Method index;
    private final Method distance;
    private final Method fingerprint;
    private final MessageDigest sha;
    private final ByteBuffer record = ByteBuffer.allocate(16);
    private final PrintStream out;
    private long calls;
    private long records;

    private M3HammingTopKDump(Class<?> topK, PrintStream out) throws ReflectiveOperationException, NoSuchAlgorithmException {
        this.nearest = topK.getMethod("nearest", long.class, long[].class, int.class);
        this.fromDistances = topK.getMethod("fromDistances", int[].class, long[].class, int.class);
        Class<?> match = nearest.getReturnType().getComponentType();
        this.index = match.getMethod("index");
        this.distance = match.getMethod("distance");
        this.fingerprint = match.getMethod("fingerprint");
        this.sha = MessageDigest.getInstance("SHA-256");
        this.out = out;
    }

    public static void main(String[] args) throws ReflectiveOperationException, NoSuchAlgorithmException {
        PrintStream out = new PrintStream(new BufferedOutputStream(System.out, 1 << 16), false, StandardCharsets.UTF_8);
        if (args.length == 2 && args[0].equals("--fingerprint")) {
            fingerprints(Class.forName(args[1]), out);
        } else {
            M3HammingTopKDump dump = new M3HammingTopKDump(Class.forName(args[0]), out);
            dump.oracleInputs();
            dump.seededSets();
            out.print("TOTAL calls=" + dump.calls + " records=" + dump.records + " trials=" + ORACLE_TRIALS + " sets=" + SETS + "\n");
        }
        out.flush();
    }

    /** The exact input stream of M3HammingTopKContract.main (same seed, same draws, same k values). */
    private void oracleInputs() throws ReflectiveOperationException {
        Random random = new Random(ORACLE_SEED);
        for (int trial = 0; trial < ORACLE_TRIALS; trial++) {
            int n = trial % 20 == 0 ? 2048 : random.nextInt(256);
            long query = random.nextLong();
            long[] values = new long[n];
            for (int i = 0; i < n; i++) {
                values[i] = switch (trial % 6) {
                    case 0 -> query;
                    case 1 -> query ^ 1;
                    case 2 -> query ^ (1L << (i & 63));
                    case 3 -> -1L;
                    case 4 -> random.nextLong();
                    default -> (i & 1) == 0 ? query : ~query;
                };
            }
            for (int k : new int[] {0, 1, 2, 3, n / 2, n, n + 5}) {
                dump("T" + trial, query, values, k);
            }
        }
        for (int n : TIE_SIZES) {
            long[] values = new long[n];
            for (int i = 0; i < n; i++) values[i] = 1L << (i & 63);
            dump("A" + n, 0L, values, n);
            dump("A" + n, 0L, values, Math.min(n, 7));
        }
        refused("nearest null candidates", () -> nearest.invoke(null, 0L, null, 1));
        refused("nearest negative k", () -> nearest.invoke(null, 0L, new long[1], -1));
        refused("fromDistances null distances", () -> fromDistances.invoke(null, null, new long[0], 0));
        refused("fromDistances length mismatch", () -> fromDistances.invoke(null, new int[1], new long[0], 0));
        refused("fromDistances negative distance", () -> fromDistances.invoke(null, new int[] {-1}, new long[] {0L}, 1));
        refused("fromDistances distance above 64", () -> fromDistances.invoke(null, new int[] {65}, new long[] {0L}, 1));
        Object[] historical = (Object[]) fromDistances.invoke(null, new int[] {65}, new long[] {0L}, 0);
        calls++;
        out.print("H fromDistances k=0 unvalidated distance length=" + historical.length + "\n");
    }

    private void seededSets() throws ReflectiveOperationException {
        Random random = new Random(SET_SEED);
        for (int set = 0; set < SETS; set++) {
            int n = set % 1000 == 0 ? 4096 : random.nextInt(257);
            long query = random.nextLong();
            long[] values = new long[n];
            for (int i = 0; i < n; i++) {
                values[i] = switch (set % 4) {
                    case 0 -> random.nextLong();
                    case 1 -> query ^ randomBits(random, random.nextInt(9));
                    case 2 -> query ^ randomBits(random, random.nextInt(65));
                    default -> random.nextInt(4) == 0 ? query : random.nextLong();
                };
            }
            int k = switch (set % 5) {
                case 0 -> 1;
                case 1 -> 8;
                case 2 -> random.nextInt(n + 6);
                case 3 -> n;
                default -> 16;
            };
            dump("S" + set, query, values, k);
        }
    }

    private static long randomBits(Random random, int count) {
        long mask = 0L;
        while (Long.bitCount(mask) < count) mask |= 1L << random.nextInt(64);
        return mask;
    }

    private void dump(String label, long query, long[] values, int k) throws ReflectiveOperationException {
        int[] distances = new int[values.length];
        for (int i = 0; i < values.length; i++) distances[i] = Long.bitCount(query ^ values[i]);
        long[] frozenValues = values.clone();
        int[] frozenDistances = distances.clone();
        Object[] stream = (Object[]) nearest.invoke(null, query, values, k);
        Object[] prepared = (Object[]) fromDistances.invoke(null, distances, values, k);
        calls += 2;
        boolean intact = Arrays.equals(frozenValues, values) && Arrays.equals(frozenDistances, distances);
        out.print(label + " k=" + k + " n=" + values.length + " nearest=" + stream.length + ":" + digest(stream)
                + " prepared=" + prepared.length + ":" + digest(prepared) + " inputs_intact=" + intact + "\n");
    }

    private String digest(Object[] matches) throws ReflectiveOperationException {
        sha.reset();
        for (Object match : matches) {
            record.clear();
            record.putInt((int) index.invoke(match)).putInt((int) distance.invoke(match)).putLong((long) fingerprint.invoke(match));
            sha.update(record.array());
        }
        records += matches.length;
        return HEX.formatHex(sha.digest());
    }

    private void refused(String label, Call call) throws ReflectiveOperationException {
        calls++;
        out.print("R " + label + " " + outcome(call) + "\n");
    }

    private static void fingerprints(Class<?> type, PrintStream out) throws ReflectiveOperationException {
        Method direct = find(type, "fingerprint", long[].class);
        Method fromFeatures = find(type, "fromFeatures", String.class, long[].class, int.class, int[].class, int.class, int.class);
        if (direct == null && fromFeatures == null) {
            throw new IllegalArgumentException("no fingerprint(long[]) or fromFeatures(...) on " + type.getName());
        }
        Method simHash64 = fromFeatures == null ? null : type.getMethod("simHash64");
        Random random = new Random(WINDOW_SEED);
        long tiedWindows = 0;
        long tiedBits = 0;
        for (int window = 0; window < WINDOWS; window++) {
            int n = random.nextInt(65);
            long[] features = new long[n];
            for (int i = 0; i < n; i++) features[i] = random.nextLong();
            long tieMask = tieMask(features);
            long value;
            if (direct != null) {
                value = (long) direct.invoke(null, features);
            } else {
                int[] ones = new int[n];
                Arrays.fill(ones, 1);
                value = (long) simHash64.invoke(fromFeatures.invoke(null, DOMAIN, features, 0, ones, 0, n));
            }
            if (tieMask != 0L) tiedWindows++;
            tiedBits += Long.bitCount(tieMask);
            out.print("F" + window + " n=" + n + " fp=" + Long.toHexString(value) + " tiemask=" + Long.toHexString(tieMask) + "\n");
        }
        out.print("TOTAL windows=" + WINDOWS + " tied_windows=" + tiedWindows + " tied_bits=" + tiedBits + "\n");
    }

    /** The bits whose unit-weight vote sum over the window is exactly zero (the two tie rules differ there). */
    private static long tieMask(long[] features) {
        long mask = 0L;
        for (int bit = 0; bit < 64; bit++) {
            int votes = 0;
            for (long feature : features) votes += ((feature >>> bit) & 1L) != 0L ? 1 : -1;
            if (votes == 0) mask |= 1L << bit;
        }
        return mask;
    }

    private static Method find(Class<?> type, String name, Class<?>... parameters) {
        try {
            return type.getMethod(name, parameters);
        } catch (NoSuchMethodException absent) {
            return null;
        }
    }

    private interface Call {
        Object run() throws ReflectiveOperationException;
    }

    private static String outcome(Call call) throws ReflectiveOperationException {
        try {
            call.run();
            return "ACCEPTED";
        } catch (InvocationTargetException refusal) {
            return refusal.getCause().getClass().getName();
        }
    }
}
