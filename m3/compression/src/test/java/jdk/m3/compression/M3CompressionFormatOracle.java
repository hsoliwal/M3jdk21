// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.compression;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.SplittableRandom;
import java.util.TreeMap;

/**
 * Format oracle of the two raw encoders: {@value #DEFAULT_INPUTS} seeded inputs (empty, 1 byte, incompressible,
 * highly repetitive, periodic, low-alphabet, text-like, copy-rich, 64 KiB offset and block edges, 4 MiB blocks) are
 * encoded under the standard worst-case output bounds and decoded by the independent strict reference decoders of
 * {@link M3CompressionSpecDecoders}; every decoded block must equal its input and every LZ4 block must keep the
 * end-of-block rules. Every 97th input also replays the exact output size as the cap (same bytes) and that size minus
 * one (refused). The SHA-256 of all LZ4 and of all Snappy wire bytes is printed, so the same driver run over the donor
 * classes (encoder class names as arguments, reached through method handles) proves byte identity.
 *
 * <p>Test scope only; prints {@code M3_COMPRESSION_FORMAT_*} lines and throws {@link AssertionError} on any failure.
 * usage: {@code M3CompressionFormatOracle [lz4EncoderClass snappyEncoderClass [inputs]]}
 */
public final class M3CompressionFormatOracle {
    static final int DEFAULT_INPUTS = 100_000;
    private static final long SEED = 0x4d33_434f_4d50_5253L;
    private static final long GOLDEN = 0x9e37_79b9_7f4a_7c15L;
    private static final int TABLE_BITS = 12;
    private static final int REPLAY_STRIDE = 97;
    private static final int FOUR_MIB = 4 << 20;
    private static final int[] FIXED_SIZES = {0, 1, 2, 3, 4, 5, 6, 7, 8, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 31, 32,
            33, 59, 60, 61, 62, 63, 64, 65, 66, 67, 127, 128, 129, 255, 256, 257, 269, 270, 271, 1023, 1024, 1025,
            4095, 4096, 4097, 65535, 65536, 65537, 131071, 131072, 131073};
    private static final int FIXED = FIXED_SIZES.length * 4;
    private static final int[] EDGE_DISTANCES = {1, 2, 3, 4, 65533, 65534, 65535, 65536};
    private static final int[] EDGE_TAILS = {0, 4, 5, 11, 12, 13, 64, 4096};
    private static final int EDGES = EDGE_DISTANCES.length * EDGE_TAILS.length;
    private static final int LARGE = 8;
    private static final String[] WORDS = {"the", "block", "match", "literal", "offset", "window", "encoder", "raw",
            "format", "length", "token", "copy", " ", " ", ", ", ".\n"};

    private M3CompressionFormatOracle() {}

    public static void main(String[] args) throws Throwable {
        String lz4 = args.length > 0 ? args[0] : M3Lz4RawBlockEncoder.class.getName();
        String snappy = args.length > 1 ? args[1] : M3SnappyRawEncoder.class.getName();
        int inputs = args.length > 2 ? Integer.parseInt(args[2]) : DEFAULT_INPUTS;
        long started = System.nanoTime();
        System.out.print(run(lz4, snappy, inputs));
        System.err.printf("M3_COMPRESSION_FORMAT_SECONDS=%.1f%n", (System.nanoTime() - started) / 1e9);
    }

    /** Runs the oracle over the relocated encoders with the default input count. */
    public static String runAll() throws Throwable {
        return run(M3Lz4RawBlockEncoder.class.getName(), M3SnappyRawEncoder.class.getName(), DEFAULT_INPUTS);
    }

    static String run(String lz4Class, String snappyClass, int inputs) throws Throwable {
        Codec lz4 = new Codec(lz4Class);
        Codec snappy = new Codec(snappyClass);
        MessageDigest lz4Digest = MessageDigest.getInstance("SHA-256");
        MessageDigest snappyDigest = MessageDigest.getInstance("SHA-256");
        long[] lz4Stats = new long[3];
        long[] snappyStats = new long[3];
        long[] totals = new long[6]; // input bytes, LZ4 bytes, Snappy bytes, replays, refusals, immutability checks
        TreeMap<String, Integer> categories = new TreeMap<>();
        for (int index = 0; index < inputs; index++) {
            byte[] data = input(index);
            byte[] before = data.clone();
            byte[] packedLz4 = lz4.encode(data, lz4Bound(data.length));
            byte[] packedSnappy = snappy.encode(data, snappyBound(data.length));
            check(Arrays.equals(before, data), index, "encoder mutated its input");
            check(Arrays.equals(M3CompressionSpecDecoders.lz4Block(packedLz4, data.length, lz4Stats), data), index,
                    "LZ4 round trip");
            check(Arrays.equals(M3CompressionSpecDecoders.snappyRaw(packedSnappy, snappyStats), data), index,
                    "Snappy round trip");
            update(lz4Digest, packedLz4);
            update(snappyDigest, packedSnappy);
            totals[0] += data.length;
            totals[1] += packedLz4.length;
            totals[2] += packedSnappy.length;
            totals[5]++;
            if (index % REPLAY_STRIDE == 0) {
                totals[3] += replay(lz4, data, packedLz4, index) + replay(snappy, data, packedSnappy, index);
                totals[4] += refuse(lz4, data, packedLz4.length - 1) + refuse(snappy, data, packedSnappy.length - 1);
            }
            categories.merge(category(index), 1, Integer::sum);
        }
        return report(inputs, totals, lz4Stats, snappyStats, lz4Digest, snappyDigest, categories);
    }

    private static String report(int inputs, long[] totals, long[] lz4Stats, long[] snappyStats,
            MessageDigest lz4Digest, MessageDigest snappyDigest, TreeMap<String, Integer> categories) {
        StringBuilder out = new StringBuilder();
        line(out, "INPUTS", inputs);
        line(out, "INPUT_BYTES", totals[0]);
        line(out, "LZ4_ROUNDTRIP", totals[5]);
        line(out, "SNAPPY_ROUNDTRIP", totals[5]);
        line(out, "LZ4_BYTES", totals[1]);
        line(out, "SNAPPY_BYTES", totals[2]);
        line(out, "LZ4_SEQUENCES", lz4Stats[0]);
        line(out, "LZ4_MATCHES", lz4Stats[1]);
        line(out, "LZ4_MATCH_BYTES", lz4Stats[2]);
        line(out, "SNAPPY_ELEMENTS", snappyStats[0]);
        line(out, "SNAPPY_COPIES", snappyStats[1]);
        line(out, "SNAPPY_COPY_BYTES", snappyStats[2]);
        line(out, "EXACT_CAP_REPLAYS", totals[3]);
        line(out, "CAP_MINUS_ONE_REFUSALS", totals[4]);
        categories.forEach((name, count) -> line(out, "CATEGORY_" + name, count));
        line(out, "LZ4_SHA256", HexFormat.of().formatHex(lz4Digest.digest()));
        line(out, "SNAPPY_SHA256", HexFormat.of().formatHex(snappyDigest.digest()));
        line(out, "RESULT", "PASS");
        return out.toString();
    }

    private static void line(StringBuilder out, String key, Object value) {
        out.append("M3_COMPRESSION_FORMAT_").append(key).append('=').append(value).append('\n');
    }

    /** LZ4 worst case of the reference implementation (LZ4_COMPRESSBOUND): n + n / 255 + 16. */
    static int lz4Bound(int length) {
        return length + length / 255 + 16;
    }

    /** Snappy worst case of the reference implementation (MaxCompressedLength): 32 + n + n / 6. */
    static int snappyBound(int length) {
        return 32 + length + length / 6;
    }

    private static int replay(Codec codec, byte[] data, byte[] packed, int index) throws Throwable {
        check(Arrays.equals(codec.encode(data, packed.length), packed), index, "exact-cap replay differs");
        return 1;
    }

    private static int refuse(Codec codec, byte[] data, int cap) throws Throwable {
        if (cap < 0) {
            return 0;
        }
        try {
            codec.encode(data, cap);
        } catch (IllegalArgumentException refused) {
            return 1;
        }
        throw new AssertionError("cap " + cap + " below the exact output size was accepted");
    }

    private static void update(MessageDigest digest, byte[] wire) {
        int length = wire.length;
        digest.update(new byte[] {(byte) (length >>> 24), (byte) (length >>> 16), (byte) (length >>> 8),
                (byte) length});
        digest.update(wire);
    }

    private static void check(boolean condition, int index, String message) {
        if (!condition) {
            throw new AssertionError(message + " at input " + index + " (" + category(index) + ")");
        }
    }

    /** The category of input {@code index}, a pure function of the index. */
    public static String category(int index) {
        if (index < FIXED) {
            return "FIXED_" + new String[] {"ZERO", "RANDOM", "PERIOD7", "ASCENDING"}[index % 4];
        }
        if (index < FIXED + EDGES) {
            return "EDGE_64K";
        }
        if (index < FIXED + EDGES + LARGE) {
            return "LARGE_4MIB";
        }
        return "SEEDED_" + new String[] {"RANDOM", "FILL", "PERIODIC", "LOW_ALPHABET", "TEXT", "COPIES", "ASCENDING",
                "RUNS"}[random(index).nextInt(8)];
    }

    /** Input {@code index}, a pure function of the index (so any reference decoder can regenerate it). */
    public static byte[] input(int index) {
        if (index < FIXED) {
            return fill(new byte[FIXED_SIZES[index / 4]], index % 4, random(index));
        }
        if (index < FIXED + EDGES) {
            return edge(index - FIXED, random(index));
        }
        if (index < FIXED + EDGES + LARGE) {
            return large(index - FIXED - EDGES, random(index));
        }
        SplittableRandom random = random(index);
        int mode = random.nextInt(8);
        double shape = random.nextDouble();
        int size = shape < 0.85 ? random.nextInt(1025)
                : shape < 0.99 ? 1024 + random.nextInt(7169) : 8192 + random.nextInt(65536 + 4097);
        return seeded(new byte[size], mode, random);
    }

    private static SplittableRandom random(int index) {
        return new SplittableRandom(SEED ^ (index * GOLDEN));
    }

    private static byte[] fill(byte[] data, int mode, SplittableRandom random) {
        switch (mode) {
            case 0 -> { }
            case 1 -> random.nextBytes(data);
            case 2 -> period(data, 7, random);
            default -> {
                for (int i = 0; i < data.length; i++) {
                    data[i] = (byte) i;
                }
            }
        }
        return data;
    }

    /** A random head of {@code distance} bytes followed by its first {@code tail} bytes: one match at that distance. */
    private static byte[] edge(int edge, SplittableRandom random) {
        int distance = EDGE_DISTANCES[edge % EDGE_DISTANCES.length];
        int tail = EDGE_TAILS[edge / EDGE_DISTANCES.length];
        byte[] data = new byte[distance + tail];
        byte[] head = new byte[distance];
        random.nextBytes(head);
        System.arraycopy(head, 0, data, 0, distance);
        for (int i = 0; i < tail; i++) {
            data[distance + i] = head[i % distance];
        }
        return data;
    }

    private static byte[] large(int mode, SplittableRandom random) {
        byte[] data = new byte[FOUR_MIB];
        switch (mode) {
            case 0 -> { }
            case 1 -> random.nextBytes(data);
            case 2 -> period(data, 7, random);
            case 3 -> period(data, 65535, random);
            case 4 -> period(data, 65536, random);
            default -> seeded(data, mode == 5 ? 3 : mode == 6 ? 4 : 5, random);
        }
        return data;
    }

    private static byte[] seeded(byte[] data, int mode, SplittableRandom random) {
        switch (mode) {
            case 0 -> random.nextBytes(data);
            case 1 -> Arrays.fill(data, (byte) random.nextInt(256));
            case 2 -> period(data, 1 + random.nextInt(300), random);
            case 3 -> {
                for (int i = 0; i < data.length; i++) {
                    data[i] = (byte) random.nextInt(4);
                }
            }
            case 4 -> text(data, random);
            case 5 -> copies(data, random);
            case 6 -> {
                int step = 1 + random.nextInt(255);
                for (int i = 0; i < data.length; i++) {
                    data[i] = (byte) (i * step);
                }
            }
            default -> runs(data, random);
        }
        return data;
    }

    private static void period(byte[] data, int period, SplittableRandom random) {
        byte[] unit = new byte[Math.min(period, Math.max(1, data.length))];
        random.nextBytes(unit);
        for (int i = 0; i < data.length; i++) {
            data[i] = unit[i % unit.length];
        }
    }

    private static void text(byte[] data, SplittableRandom random) {
        int at = 0;
        while (at < data.length) {
            String word = WORDS[random.nextInt(WORDS.length)];
            for (int i = 0; i < word.length() && at < data.length; i++) {
                data[at++] = (byte) word.charAt(i);
            }
        }
    }

    /** Random bytes with copies of earlier segments at distances up to 70,000 (in and beyond the LZ window). */
    private static void copies(byte[] data, SplittableRandom random) {
        random.nextBytes(data);
        for (int at = 1; at < data.length; ) {
            int distance = 1 + random.nextInt(Math.min(at, 70_000));
            int length = Math.min(data.length - at, 4 + random.nextInt(200));
            for (int i = 0; i < length; i++) {
                data[at + i] = data[at + i - distance];
            }
            at += length + random.nextInt(64);
        }
    }

    /** Alternating zero runs and random runs. */
    private static void runs(byte[] data, SplittableRandom random) {
        for (int at = 0; at < data.length; ) {
            int length = Math.min(data.length - at, 1 + random.nextInt(512));
            if (random.nextBoolean()) {
                for (int i = 0; i < length; i++) {
                    data[at + i] = (byte) random.nextInt(256);
                }
            }
            at += length;
        }
    }

    /** One encoder instance reached by class name: {@code new C(12)} and {@code byte[] encode(byte[], int)}. */
    private static final class Codec {
        private static final MethodType ENCODE = MethodType.methodType(byte[].class, byte[].class, int.class);
        private final Object encoder;
        private final MethodHandle encode;

        Codec(String className) throws Throwable {
            Class<?> type = Class.forName(className);
            MethodHandles.Lookup lookup = MethodHandles.publicLookup();
            encoder = lookup.findConstructor(type, MethodType.methodType(void.class, int.class)).invoke(TABLE_BITS);
            encode = lookup.findVirtual(type, "encode", ENCODE);
        }

        byte[] encode(byte[] data, int cap) throws Throwable {
            return (byte[]) encode.invoke(encoder, data, cap);
        }
    }
}
