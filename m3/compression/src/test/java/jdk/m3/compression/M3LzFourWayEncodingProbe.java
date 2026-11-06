// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.compression;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Locale;

/**
 * Four-way encoding probe of the four-way leaf (test scope). Neither encoder takes its candidate index as a parameter,
 * so the probe carries an encoder twin: the parse loops of {@link M3Lz4RawBlockEncoder} and {@link M3SnappyRawEncoder},
 * statement for statement, over a candidate index given as a parameter.
 *
 * <ol>
 *   <li>Twin identity: over the direct-mapped {@link M3LzMatchCandidateIndex} the twin reproduces both encoders' wire
 *       bytes for every input, so the twin is the encoders' parse and not an approximation of it.</li>
 *   <li>Four-way round trip: over {@link M3LzFourWayCandidateIndex} at the same table bits every wire decodes back to
 *       its input through the donor decoder pair and through the strict reference decoders of
 *       {@link M3CompressionSpecDecoders} (so every LZ4 block keeps the end-of-block rules).</li>
 *   <li>Figures: wire bytes of both indexes (the ratio is informational), the SHA-256 of the four-way wires and a
 *       64-bit digest of the four-way candidate trace, so the same driver over the donor four-way index (class name
 *       argument) proves the two indexes return the same candidates. Timing goes to stderr only.</li>
 * </ol>
 *
 * <p>The inputs are the format oracle's seeded inputs ({@link M3CompressionFormatOracle#input(int)}). Prints
 * {@code M3_LZ_FOURWAY_*} lines and throws {@link AssertionError} on any failure.
 * usage: {@code M3LzFourWayEncodingProbe [fourWayIndexClass [inputs]]}
 */
public final class M3LzFourWayEncodingProbe {
    private static final int TABLE_BITS = 12;
    private static final int KEY_BYTES = Integer.BYTES;
    private static final int MAX_DISTANCE = 0xffff;
    private static final int LZ4_LAST_LITERALS = 5;
    private static final int LZ4_LAST_MATCH_START_GUARD = 12;
    private static final int NIBBLE_MAX = 15;
    private static final int SNAPPY_MAX_COPY_LENGTH = 64;
    private static final long TRACE_MULTIPLIER = 0x9e37_79b9_7f4a_7c15L;
    private static final String PREFIX = "M3_LZ_FOURWAY_";

    private M3LzFourWayEncodingProbe() {}

    /** The candidate index the twin parses with: begin, findAndRecord, clear (the shape of both index classes). */
    interface Candidates {
        void begin(byte[] input);

        int findAndRecord(int position, int maximumDistance);

        void clear();
    }

    public static void main(String[] args) throws Throwable {
        String fourWay = args.length > 0 ? args[0] : M3LzFourWayCandidateIndex.class.getName();
        int inputs = args.length > 1 ? Integer.parseInt(args[1]) : M3CompressionFormatOracle.DEFAULT_INPUTS;
        System.out.print(run(fourWay, inputs));
    }

    /** Runs the probe over the relocated four-way index with the default input count. */
    public static String runAll() throws Throwable {
        return run(M3LzFourWayCandidateIndex.class.getName(), M3CompressionFormatOracle.DEFAULT_INPUTS);
    }

    /** One LZ4 block (or Snappy stream) of {@code input} parsed over a fresh relocated four-way index (scratch use). */
    public static byte[] encodeFourWay(byte[] input, boolean lz4) {
        Candidates index = direct(new M3LzFourWayCandidateIndex(TABLE_BITS));
        return lz4 ? lz4(input, index) : snappy(input, index);
    }

    static String run(String fourWayClass, int inputs) throws Throwable {
        Tracing fourWay = new Tracing(byName(fourWayClass));
        Candidates direct = direct(new M3LzMatchCandidateIndex(TABLE_BITS));
        Candidates relocatedFourWay = direct(new M3LzFourWayCandidateIndex(TABLE_BITS));
        M3Lz4RawBlockEncoder lz4Encoder = new M3Lz4RawBlockEncoder(TABLE_BITS);
        M3SnappyRawEncoder snappyEncoder = new M3SnappyRawEncoder(TABLE_BITS);
        MessageDigest lz4Digest = MessageDigest.getInstance("SHA-256");
        MessageDigest snappyDigest = MessageDigest.getInstance("SHA-256");
        long[] totals = new long[5]; // input, LZ4 direct, LZ4 four-way, Snappy direct, Snappy four-way bytes
        long[] nanos = new long[4]; // encoder, twin direct, twin four-way (named class, traced), twin four-way (direct)
        for (int index = 0; index < inputs; index++) {
            byte[] data = M3CompressionFormatOracle.input(index);
            long t0 = System.nanoTime();
            byte[] lz4 = lz4Encoder.encode(data, M3CompressionFormatOracle.lz4Bound(data.length));
            byte[] snappy = snappyEncoder.encode(data, M3CompressionFormatOracle.snappyBound(data.length));
            long t1 = System.nanoTime();
            check(Arrays.equals(lz4(data, direct), lz4), index, "LZ4 twin over the direct index differs");
            check(Arrays.equals(snappy(data, direct), snappy), index, "Snappy twin over the direct index differs");
            long t2 = System.nanoTime();
            byte[] lz4FourWay = lz4(data, fourWay);
            byte[] snappyFourWay = snappy(data, fourWay);
            long t3 = System.nanoTime();
            check(Arrays.equals(lz4(data, relocatedFourWay), lz4FourWay), index,
                    "LZ4 twin over the named four-way index differs from the relocated four-way index");
            check(Arrays.equals(snappy(data, relocatedFourWay), snappyFourWay), index,
                    "Snappy twin over the named four-way index differs from the relocated four-way index");
            long t4 = System.nanoTime();
            roundTrip(data, lz4FourWay, snappyFourWay, index);
            update(lz4Digest, lz4FourWay);
            update(snappyDigest, snappyFourWay);
            totals[0] += data.length;
            totals[1] += lz4.length;
            totals[2] += lz4FourWay.length;
            totals[3] += snappy.length;
            totals[4] += snappyFourWay.length;
            nanos[0] += t1 - t0;
            nanos[1] += t2 - t1;
            nanos[2] += t3 - t2;
            nanos[3] += t4 - t3;
        }
        System.err.printf(Locale.ROOT, "M3_LZ_FOURWAY_NS_PER_INPUT_BYTE encoders=%.3f twin_direct=%.3f "
                + "twin_fourway=%.3f twin_fourway_named_traced=%.3f (both codecs, one pass each; informational)%n",
                nanos[0] / (double) totals[0], nanos[1] / (double) totals[0], nanos[3] / (double) totals[0],
                nanos[2] / (double) totals[0]);
        return report(inputs, totals, lz4Digest, snappyDigest, fourWay.trace);
    }

    private static void roundTrip(byte[] data, byte[] lz4, byte[] snappy, int index) {
        check(Arrays.equals(M3Lz4RawBlockDecoder.decode(lz4, data.length, data.length), data), index,
                "four-way LZ4 through the donor decoder");
        check(Arrays.equals(M3CompressionSpecDecoders.lz4Block(lz4, data.length, null), data), index,
                "four-way LZ4 through the reference decoder");
        check(Arrays.equals(M3SnappyRawDecoder.decode(snappy, data.length), data), index,
                "four-way Snappy through the donor decoder");
        check(Arrays.equals(M3CompressionSpecDecoders.snappyRaw(snappy, null), data), index,
                "four-way Snappy through the reference decoder");
    }

    private static String report(int inputs, long[] totals, MessageDigest lz4Digest, MessageDigest snappyDigest,
            long trace) {
        StringBuilder out = new StringBuilder();
        line(out, "INPUTS", inputs);
        line(out, "INPUT_BYTES", totals[0]);
        line(out, "TWIN_DIRECT_IDENTICAL", inputs);
        line(out, "NAMED_FOURWAY_EQUALS_RELOCATED_FOURWAY", inputs);
        line(out, "ROUNDTRIP_DONOR_DECODERS", inputs);
        line(out, "ROUNDTRIP_REFERENCE_DECODERS", inputs);
        line(out, "LZ4_BYTES_DIRECT", totals[1]);
        line(out, "LZ4_BYTES_FOURWAY", totals[2]);
        line(out, "LZ4_FOURWAY_OVER_DIRECT", String.format(Locale.ROOT, "%.4f", totals[2] / (double) totals[1]));
        line(out, "SNAPPY_BYTES_DIRECT", totals[3]);
        line(out, "SNAPPY_BYTES_FOURWAY", totals[4]);
        line(out, "SNAPPY_FOURWAY_OVER_DIRECT", String.format(Locale.ROOT, "%.4f", totals[4] / (double) totals[3]));
        line(out, "LZ4_SHA256", HexFormat.of().formatHex(lz4Digest.digest()));
        line(out, "SNAPPY_SHA256", HexFormat.of().formatHex(snappyDigest.digest()));
        line(out, "CANDIDATE_TRACE", HexFormat.of().toHexDigits(trace));
        line(out, "RESULT", "PASS");
        return out.toString();
    }

    // ---- the encoder twin (M3Lz4RawBlockEncoder.encode and M3SnappyRawEncoder.encode, index as a parameter) --------

    static byte[] lz4(byte[] input, Candidates candidates) {
        M3CompressionDecoderAgreement.Bytes out = new M3CompressionDecoderAgreement.Bytes();
        candidates.begin(input);
        try {
            int anchor = 0;
            int position = 0;
            while (position <= input.length - LZ4_LAST_MATCH_START_GUARD) {
                int match = candidates.findAndRecord(position, MAX_DISTANCE);
                if (match < 0) {
                    position++;
                    continue;
                }
                int matched = M3LzByteKernels.commonPrefix(input, match, position,
                        input.length - position - LZ4_LAST_LITERALS);
                if (matched < KEY_BYTES) {
                    position++;
                    continue;
                }
                int literals = position - anchor;
                int matchExtra = matched - KEY_BYTES;
                out.add((Math.min(literals, NIBBLE_MAX) << 4) | Math.min(matchExtra, NIBBLE_MAX));
                extension(out, literals);
                out.add(input, anchor, literals);
                out.add((position - match) & 0xff);
                out.add((position - match) >>> 8);
                extension(out, matchExtra);
                position += matched;
                anchor = position;
            }
            int remaining = input.length - anchor;
            out.add(Math.min(remaining, NIBBLE_MAX) << 4);
            extension(out, remaining);
            out.add(input, anchor, remaining);
            return out.toArray();
        } finally {
            candidates.clear();
        }
    }

    private static void extension(M3CompressionDecoderAgreement.Bytes out, int length) {
        if (length < NIBBLE_MAX) {
            return;
        }
        int remaining = length - NIBBLE_MAX;
        while (remaining >= 255) {
            out.add(255);
            remaining -= 255;
        }
        out.add(remaining);
    }

    static byte[] snappy(byte[] input, Candidates candidates) {
        M3CompressionDecoderAgreement.Bytes out = new M3CompressionDecoderAgreement.Bytes();
        candidates.begin(input);
        try {
            M3CompressionDecoderAgreement.SnappyStream.varint(out, input.length);
            int anchor = 0;
            int position = 0;
            while (position <= input.length - KEY_BYTES) {
                int match = candidates.findAndRecord(position, MAX_DISTANCE);
                if (match < 0) {
                    position++;
                    continue;
                }
                int matched = M3LzByteKernels.commonPrefix(input, match, position, input.length - position);
                if (matched < KEY_BYTES) {
                    position++;
                    continue;
                }
                literal(out, input, anchor, position - anchor);
                copy(out, position - match, matched);
                position += matched;
                anchor = position;
            }
            literal(out, input, anchor, input.length - anchor);
            return out.toArray();
        } finally {
            candidates.clear();
        }
    }

    private static void literal(M3CompressionDecoderAgreement.Bytes out, byte[] data, int offset, int length) {
        if (length == 0) {
            return;
        }
        int encoded = length - 1;
        if (length <= 60) {
            out.add(encoded << 2);
        } else {
            int width = encoded <= 0xff ? 1 : encoded <= 0xffff ? 2 : encoded <= 0xffffff ? 3 : 4;
            out.add((59 + width) << 2);
            for (int i = 0; i < width; i++) {
                out.add(encoded >>> (8 * i));
            }
        }
        out.add(data, offset, length);
    }

    private static void copy(M3CompressionDecoderAgreement.Bytes out, int distance, int length) {
        int remaining = length;
        while (remaining > 0) {
            int count = Math.min(SNAPPY_MAX_COPY_LENGTH, remaining);
            out.add(((count - 1) << 2) | 2);
            out.add(distance & 0xff);
            out.add(distance >>> 8);
            remaining -= count;
        }
    }

    // ---- candidate indexes ------------------------------------------------------------------------------------------

    private static Candidates direct(M3LzMatchCandidateIndex index) {
        return new Candidates() {
            @Override public void begin(byte[] input) { index.begin(input); }
            @Override public int findAndRecord(int position, int distance) { return index.findAndRecord(position, distance); }
            @Override public void clear() { index.clear(); }
        };
    }

    private static Candidates direct(M3LzFourWayCandidateIndex index) {
        return new Candidates() {
            @Override public void begin(byte[] input) { index.begin(input); }
            @Override public int findAndRecord(int position, int distance) { return index.findAndRecord(position, distance); }
            @Override public void clear() { index.clear(); }
        };
    }

    /** A package-private index class of any package (the donor's or this module's), reached through method handles. */
    private static Candidates byName(String className) throws ReflectiveOperationException {
        Class<?> type = Class.forName(className);
        MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(type, MethodHandles.lookup());
        MethodType anyObject = MethodType.methodType(Object.class, int.class);
        Object index;
        try {
            index = lookup.findConstructor(type, MethodType.methodType(void.class, int.class)).asType(anyObject)
                    .invokeExact(TABLE_BITS);
        } catch (Throwable failed) {
            throw new ReflectiveOperationException("cannot construct " + className, failed);
        }
        MethodHandle begin = lookup.findVirtual(type, "begin", MethodType.methodType(void.class, byte[].class))
                .bindTo(index);
        MethodHandle find = lookup.findVirtual(type, "findAndRecord", MethodType.methodType(int.class, int.class,
                int.class)).bindTo(index);
        MethodHandle clear = lookup.findVirtual(type, "clear", MethodType.methodType(void.class)).bindTo(index);
        return new Candidates() {
            @Override public void begin(byte[] input) { invoke(() -> { begin.invokeExact(input); }); }
            @Override public int findAndRecord(int position, int distance) {
                try {
                    return (int) find.invokeExact(position, distance);
                } catch (RuntimeException | Error passed) {
                    throw passed;
                } catch (Throwable other) {
                    throw new IllegalStateException(other);
                }
            }
            @Override public void clear() { invoke(() -> { clear.invokeExact(); }); }
        };
    }

    private static void invoke(Call call) {
        try {
            call.run();
        } catch (RuntimeException | Error passed) {
            throw passed;
        } catch (Throwable other) {
            throw new IllegalStateException(other);
        }
    }

    @FunctionalInterface
    private interface Call {
        void run() throws Throwable;
    }

    /** Folds every candidate the wrapped index returns into a 64-bit trace digest. */
    private static final class Tracing implements Candidates {
        private final Candidates delegate;
        private long trace;

        Tracing(Candidates delegate) {
            this.delegate = delegate;
        }

        @Override public void begin(byte[] input) { delegate.begin(input); }

        @Override public int findAndRecord(int position, int distance) {
            int candidate = delegate.findAndRecord(position, distance);
            trace = (trace + candidate + 1) * TRACE_MULTIPLIER;
            return candidate;
        }

        @Override public void clear() { delegate.clear(); }
    }

    private static void update(MessageDigest digest, byte[] wire) {
        int length = wire.length;
        digest.update(new byte[] {(byte) (length >>> 24), (byte) (length >>> 16), (byte) (length >>> 8),
                (byte) length});
        digest.update(wire);
    }

    private static void line(StringBuilder out, String key, Object value) {
        out.append(PREFIX).append(key).append('=').append(value).append('\n');
    }

    private static void check(boolean condition, int index, String message) {
        if (!condition) {
            throw new AssertionError(message + " at input " + index + " (" + M3CompressionFormatOracle.category(index)
                    + ")");
        }
    }
}
