// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.compression;

import java.io.ByteArrayOutputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.function.UnaryOperator;
import java.util.zip.Deflater;

/**
 * Reference-model oracle of {@link M3CompressedChunkCache}: seeded single-thread workloads (skewed reuse of a chunk
 * pool with near-duplicates and identical copies, three wire profiles, window offsets, rare trims) run against the
 * cache and against an independent model, and every operation must agree on hit or miss, returned bytes, hit / miss /
 * eviction counters, retained entries, retained payload bytes and encoder calls.
 *
 * <p>The model keeps slot contents in a {@link HashMap} and recency in an access-ordered {@link LinkedHashMap} (the
 * classic LRU): a hit touches its slot; a miss whose raw + encoded weight fits the budget first evicts the occupant of
 * its slot, then the least-recently-used slots until the weight fits, then inserts as most recent; an oversized miss
 * changes nothing; trim empties the model and keeps the counters. The direct mapping (profile hash folded with the
 * SHA-256 words) is the one line of the owner the model repeats, because slot collisions are part of the observable
 * contract.
 *
 * <p>The cache class is reached by name through method handles and a {@link Proxy} of its nested
 * {@code DeterministicEncoder}, so the same driver replays the donor class in the differential; the report carries a
 * SHA-256 over the per-operation trace and no class name.
 * usage: {@code M3CompressedChunkCacheModel [cacheClass [operationsPerConfiguration]]}
 */
public final class CompressedChunkCacheModel {
    static final int DEFAULT_OPERATIONS = 40_000;
    private static final long SEED = 0x4d33_4348_554e_4b43L;
    private static final int[][] CONFIGURATIONS = {{1, 1300}, {2, 4096}, {4, 6000}, {6, 1 << 16}, {12, 1 << 20}};
    private static final int POOL = 96;
    private static final String[] PROFILES = {"deflate/raw:level=1:dictionary=none", "deflate/raw:level=1:variant",
            "identity/reversed:tag=1"};

    private CompressedChunkCacheModel() {}

    public static void main(String[] args) throws Throwable {
        String cacheClass = args.length > 0 ? args[0] : CompressedChunkCache.class.getName();
        int operations = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_OPERATIONS;
        System.out.print(run(cacheClass, operations));
    }

    /** Runs the model oracle over the relocated cache with the default operation count. */
    public static String runAll() throws Throwable {
        return run(CompressedChunkCache.class.getName(), DEFAULT_OPERATIONS);
    }

    static String run(String cacheClass, int operations) throws Throwable {
        MessageDigest trace = MessageDigest.getInstance("SHA-256");
        long[] totals = new long[6]; // operations, hits, misses, evictions, trims, field checks
        StringBuilder out = new StringBuilder();
        for (int c = 0; c < CONFIGURATIONS.length; c++) {
            long[] figures = workload(cacheClass, c, operations, trace);
            for (int i = 0; i < totals.length; i++) {
                totals[i] += figures[i];
            }
            out.append(String.format("M3_CHUNK_CACHE_CONFIG_%d=tableBits=%d budget=%d ops=%d hits=%d misses=%d "
                    + "evictions=%d trims=%d%n", c, CONFIGURATIONS[c][0], CONFIGURATIONS[c][1], figures[0], figures[1],
                    figures[2], figures[3], figures[4]));
        }
        String[] keys = {"OPS", "HITS", "MISSES", "EVICTIONS", "TRIMS", "MODEL_FIELD_CHECKS"};
        for (int i = 0; i < keys.length; i++) {
            out.append("M3_CHUNK_CACHE_").append(keys[i]).append('=').append(totals[i]).append('\n');
        }
        out.append("M3_CHUNK_CACHE_MODEL_MISMATCHES=0\n");
        out.append("M3_CHUNK_CACHE_TRACE_SHA256=").append(HexFormat.of().formatHex(trace.digest())).append('\n');
        out.append("M3_CHUNK_CACHE_RESULT=PASS\n");
        return out.toString();
    }

    private static long[] workload(String cacheClass, int configuration, int operations, MessageDigest trace)
            throws Throwable {
        int tableBits = CONFIGURATIONS[configuration][0];
        long budget = CONFIGURATIONS[configuration][1];
        SplittableRandom random = new SplittableRandom(SEED ^ configuration);
        byte[][] pool = pool(random);
        Subject subject = new Subject(cacheClass, tableBits, budget);
        Model model = new Model(tableBits, budget);
        long trims = 0;
        long checks = 0;
        for (int op = 0; op < operations; op++) {
            if (random.nextInt(1000) < 2) {
                subject.trim();
                model.trim();
                trims++;
                checks += compare(subject, model, op, null, null);
                record(trace, op, 'T', model, 0);
                continue;
            }
            double skew = random.nextDouble();
            byte[] raw = pool[(int) (POOL * skew * skew)];
            int profile = random.nextInt(PROFILES.length);
            int offset = random.nextInt(8);
            byte[] window = new byte[offset + raw.length + random.nextInt(8)];
            random.nextBytes(window);
            System.arraycopy(raw, 0, window, offset, raw.length);
            long callsBefore = subject.encoderCalls[0];
            byte[] got = subject.encode(window, offset, raw.length, profile);
            boolean hit = model.encode(raw, PROFILES[profile], encoder(profile));
            long calls = subject.encoderCalls[0] - callsBefore;
            if (calls != (hit ? 0 : 1)) {
                throw new AssertionError("op " + op + ": encoder calls " + calls + ", model " + (hit ? "hit" : "miss"));
            }
            checks += 1 + compare(subject, model, op, got, model.last);
            record(trace, op, hit ? 'H' : 'M', model, got.length);
        }
        return new long[] {operations, model.hits, model.misses, model.evictions, trims, checks};
    }

    private static int compare(Subject subject, Model model, int op, byte[] got, byte[] expected) throws Throwable {
        long[] actual = {subject.hits(), subject.misses(), subject.evictions(), subject.entries(), subject.payload()};
        long[] predicted = {model.hits, model.misses, model.evictions, model.content.size(), model.payload};
        String[] names = {"hits", "misses", "evictions", "entries", "payload"};
        for (int i = 0; i < actual.length; i++) {
            if (actual[i] != predicted[i]) {
                throw new AssertionError("op " + op + ": " + names[i] + " " + actual[i] + " != model " + predicted[i]);
            }
        }
        if (got != null && !Arrays.equals(got, expected)) {
            throw new AssertionError("op " + op + ": returned bytes differ from the model");
        }
        return actual.length + (got != null ? 1 : 0);
    }

    private static void record(MessageDigest trace, int op, char kind, Model model, int length) {
        trace.update((op + " " + kind + " " + model.hits + " " + model.misses + " " + model.evictions + " "
                + model.content.size() + " " + model.payload + " " + length + "\n").getBytes(StandardCharsets.US_ASCII));
    }

    /** 96 chunks of 0..899 bytes: random, filled, near-duplicates (one byte changed) and identical copies. */
    private static byte[][] pool(SplittableRandom random) {
        byte[][] pool = new byte[POOL][];
        for (int i = 0; i < POOL; i++) {
            int kind = random.nextInt(8);
            if (kind == 0 && i > 0) {
                pool[i] = pool[random.nextInt(i)].clone();
            } else if (kind == 1 && i > 0 && pool[i - 1].length > 0) {
                pool[i] = pool[i - 1].clone();
                pool[i][random.nextInt(pool[i].length)] ^= 1;
            } else {
                pool[i] = new byte[random.nextInt(900)];
                if (kind < 4) {
                    random.nextBytes(pool[i]);
                } else {
                    Arrays.fill(pool[i], (byte) random.nextInt(16));
                }
            }
        }
        return pool;
    }

    private static UnaryOperator<byte[]> encoder(int profile) {
        return profile == 2 ? CompressedChunkCacheModel::reversed : CompressedChunkCacheModel::deflate;
    }

    private static byte[] reversed(byte[] source) {
        byte[] out = new byte[source.length + 1];
        out[0] = 1;
        for (int i = 0; i < source.length; i++) {
            out[i + 1] = source[source.length - 1 - i];
        }
        return out;
    }

    private static byte[] deflate(byte[] source) {
        Deflater deflater = new Deflater(1, true);
        try {
            deflater.setInput(source);
            deflater.finish();
            byte[] scratch = new byte[256];
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            while (!deflater.finished()) {
                output.write(scratch, 0, deflater.deflate(scratch));
            }
            return output.toByteArray();
        } finally {
            deflater.end();
        }
    }

    private static byte[] sha256(byte[] input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    /** The cache under test, reached by class name. */
    private static final class Subject {
        private final Object cache;
        private final MethodHandle encode;
        private final MethodHandle trim;
        private final MethodHandle[] counters = new MethodHandle[5];
        private final Object[] encoders = new Object[PROFILES.length];
        private final long[] encoderCalls = new long[1];

        Subject(String className, int tableBits, long budget) throws Throwable {
            Class<?> type = Class.forName(className);
            Class<?> encoderType = Class.forName(className + "$DeterministicEncoder");
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            cache = lookup.findConstructor(type, MethodType.methodType(void.class, int.class, long.class))
                    .invoke(tableBits, budget);
            encode = lookup.findVirtual(type, "encode", MethodType.methodType(byte[].class, byte[].class, int.class,
                    int.class, String.class, encoderType));
            trim = lookup.findVirtual(type, "trim", MethodType.methodType(void.class));
            String[] names = {"cacheHits", "cacheMisses", "evictions", "retainedEntries", "retainedPayloadBytes"};
            Class<?>[] results = {long.class, long.class, long.class, int.class, long.class};
            for (int i = 0; i < names.length; i++) {
                counters[i] = lookup.findVirtual(type, names[i], MethodType.methodType(results[i]));
            }
            for (int p = 0; p < PROFILES.length; p++) {
                UnaryOperator<byte[]> function = encoder(p);
                encoders[p] = Proxy.newProxyInstance(encoderType.getClassLoader(), new Class<?>[] {encoderType},
                        (proxy, method, arguments) -> {
                            if (!method.getName().equals("encode")) {
                                throw new UnsupportedOperationException(method.getName());
                            }
                            encoderCalls[0]++;
                            return function.apply((byte[]) arguments[0]);
                        });
            }
        }

        byte[] encode(byte[] window, int offset, int length, int profile) throws Throwable {
            return (byte[]) encode.invoke(cache, window, offset, length, PROFILES[profile], encoders[profile]);
        }

        void trim() throws Throwable {
            trim.invoke(cache);
        }

        long hits() throws Throwable {
            return (long) counters[0].invoke(cache);
        }

        long misses() throws Throwable {
            return (long) counters[1].invoke(cache);
        }

        long evictions() throws Throwable {
            return (long) counters[2].invoke(cache);
        }

        long entries() throws Throwable {
            return (int) counters[3].invoke(cache);
        }

        long payload() throws Throwable {
            return (long) counters[4].invoke(cache);
        }
    }

    /** Independent model: slot contents in a HashMap, recency in an access-ordered LinkedHashMap (LRU). */
    private static final class Model {
        private final int mask;
        private final long budget;
        private final Map<Integer, Entry> content = new HashMap<>();
        private final LinkedHashMap<Integer, Integer> recency = new LinkedHashMap<>(16, 0.75f, true);
        private long payload;
        private long hits;
        private long misses;
        private long evictions;
        private byte[] last;

        private record Entry(byte[] raw, String profile, byte[] encoded) {}

        Model(int tableBits, long budget) {
            this.mask = (1 << tableBits) - 1;
            this.budget = budget;
        }

        /** Returns true on a hit; {@link #last} holds the bytes the cache must return. */
        boolean encode(byte[] raw, String profile, UnaryOperator<byte[]> encoder) {
            int slot = slot(sha256(raw), profile);
            Entry entry = content.get(slot);
            if (entry != null && entry.profile().equals(profile) && Arrays.equals(entry.raw(), raw)) {
                hits++;
                recency.get(slot);
                last = entry.encoded();
                return true;
            }
            misses++;
            last = encoder.apply(raw.clone());
            long weight = (long) raw.length + last.length;
            if (weight <= budget) {
                if (content.containsKey(slot)) {
                    evict(slot);
                }
                while (payload > budget - weight) {
                    evict(recency.keySet().iterator().next());
                }
                content.put(slot, new Entry(raw.clone(), profile, last.clone()));
                recency.put(slot, slot);
                payload += weight;
            }
            return false;
        }

        void trim() {
            content.clear();
            recency.clear();
            payload = 0;
        }

        private void evict(int slot) {
            Entry entry = content.remove(slot);
            recency.remove(slot);
            payload -= (long) entry.raw().length + entry.encoded().length;
            evictions++;
        }

        /** The owner's direct mapping (the one repeated line of the owner; see the class comment). */
        private int slot(byte[] digest, String profile) {
            int hash = profile.hashCode();
            for (int i = 0; i < digest.length; i += 4) {
                int word = (digest[i] & 0xff) | ((digest[i + 1] & 0xff) << 8) | ((digest[i + 2] & 0xff) << 16)
                        | ((digest[i + 3] & 0xff) << 24);
                hash = (hash * 0x9e3779b1) ^ word;
            }
            hash ^= hash >>> 16;
            hash *= 0x7feb352d;
            return (hash ^ (hash >>> 15)) & mask;
        }
    }
}
