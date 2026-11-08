// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.compression;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.TreeMap;

/**
 * Decoder agreement oracle of the decoder leaf: the donor decoder pair ({@link M3Lz4RawBlockDecoder},
 * {@link M3SnappyRawDecoder}, reached by class name so the same driver replays the donor classes) against the strict
 * reference decoders of {@link M3CompressionSpecDecoders}, which share no code with them.
 *
 * <ol>
 *   <li>VALID: the format oracle's {@value M3CompressionFormatOracle#DEFAULT_INPUTS} seeded inputs encoded by the
 *       relocated encoders (one LZ4 block and one Snappy stream each), plus {@value #GRAMMAR} hand-built streams per
 *       codec carrying what neither encoder emits (Snappy COPY_1 / COPY_4 tags and wide literal headers, LZ4 long
 *       extension runs and overlapping offsets 1..4): both decoders accept and return the input.</li>
 *   <li>MALFORMED: grammar streams with one injected defect of a known kind (truncated, offset 0, offset beyond the
 *       output, literal overrun, match overrun, 255-run length overflow, Snappy length varint overflow, trailing
 *       bytes, declared size too long): both decoders refuse with {@link IllegalArgumentException} and the class of
 *       the donor's refusal is one of the classes the reference decoder's refusal names (input end, offset, output
 *       overrun, end-of-block rule, length header).</li>
 *   <li>FUZZ: {@value #FUZZ} random or mutated streams through the donor decoders under bounded caps: no exception
 *       other than {@link IllegalArgumentException}, no output past the cap. Where the reference decoder runs under
 *       the same bound, a different decision is admitted only when the donor accepts an LZ4 block that the reference
 *       decoder refuses for an encoder-side end-of-block rule (the donor, like the reference C decoder, does not
 *       enforce those rules); accepted outputs must be equal.</li>
 * </ol>
 *
 * <p>Test scope only; prints {@code M3_DECODER_AGREEMENT_*} lines (no class name, no timing) and throws
 * {@link AssertionError} on any failure. usage: {@code M3CompressionDecoderAgreement [lz4DecoderClass
 * snappyDecoderClass [inputs]]}
 */
public final class CompressionDecoderAgreement {
    static final int GRAMMAR = 20_000;
    static final int MALFORMED_PER_KIND = 10_000;
    static final int FUZZ = 200_000;
    private static final long SEED = 0x4d33_4445_434f_4445L;
    private static final int TABLE_BITS = 12;
    private static final int FUZZ_CAP = 1 << 20;
    private static final String PREFIX = "M3_DECODER_AGREEMENT_";

    /** Refusal classes: input ended early, bad offset, output overrun, LZ4 end-of-block rule, length header, argument. */
    enum Refusal { INPUT_END, OFFSET, OVERRUN, END_OF_BLOCK, LENGTH_HEADER, ARGUMENT }

    enum Kind { TRUNCATED, OFFSET_ZERO, OFFSET_BEYOND_OUTPUT, LITERAL_OVERRUN, MATCH_OVERRUN, LENGTH_RUN_OVERFLOW,
        VARINT_OVERFLOW, TRAILING, DECLARED_TOO_LONG }

    private static final Map<String, EnumSet<Refusal>> CLASSES = new TreeMap<>();

    static {
        EnumSet<Refusal> structural = EnumSet.of(Refusal.INPUT_END, Refusal.OFFSET, Refusal.OVERRUN,
                Refusal.END_OF_BLOCK);
        refusal(EnumSet.of(Refusal.INPUT_END), "truncated LZ4 literal", "LZ4 decoded size mismatch",
                "LZ4 block lacks final literal sequence", "truncated LZ4 token", "truncated LZ4 block",
                "LZ4 token truncated", "LZ4 offset truncated", "LZ4 length extension truncated",
                "LZ4 decoded size differs from the declared size", "truncated Snappy block",
                "Snappy output size mismatch", "Snappy length varint truncated", "Snappy tag truncated",
                "Snappy literal length truncated", "Snappy copy-1 offset truncated", "Snappy copy offset truncated",
                "Snappy decoded size differs from the declared length");
        refusal(EnumSet.of(Refusal.OFFSET), "invalid LZ4 match distance", "LZ4 offset 0 or before the output start",
                "Snappy offset 0 or before the output start");
        refusal(EnumSet.of(Refusal.OVERRUN), "LZ4 match exceeds output", "LZ4 sequence exceeds output",
                "Snappy copy past the declared length");
        refusal(EnumSet.of(Refusal.INPUT_END, Refusal.OVERRUN), "LZ4 literal run past the block",
                "truncated Snappy literal", "Snappy literal past the stream");
        refusal(EnumSet.of(Refusal.INPUT_END, Refusal.END_OF_BLOCK), "LZ4 last sequence carries a match length");
        refusal(EnumSet.of(Refusal.END_OF_BLOCK, Refusal.OVERRUN), "LZ4 match starts within the last 12 bytes",
                "LZ4 match reaches the last 5 bytes");
        refusal(EnumSet.of(Refusal.OFFSET, Refusal.OVERRUN), "invalid Snappy back-reference");
        refusal(EnumSet.of(Refusal.LENGTH_HEADER), "invalid Snappy length varint", "Snappy output exceeds bound",
                "Snappy length varint above 32 bits", "Snappy length varint longer than 5 bytes",
                "Snappy declared length too large for one array");
        refusal(structural, "invalid empty LZ4 raw block");
        refusal(EnumSet.of(Refusal.ARGUMENT), "invalid LZ4 output bound", "negative output limit");
    }

    private final MethodHandle lz4;
    private final MethodHandle snappy;
    private final MessageDigest digest;
    private final TreeMap<String, Long> counters = new TreeMap<>();

    private CompressionDecoderAgreement(String lz4Class, String snappyClass) throws Exception {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        lz4 = lookup.findStatic(Class.forName(lz4Class), "decode",
                MethodType.methodType(byte[].class, byte[].class, int.class, int.class));
        snappy = lookup.findStatic(Class.forName(snappyClass), "decode",
                MethodType.methodType(byte[].class, byte[].class, int.class));
        digest = MessageDigest.getInstance("SHA-256");
    }

    public static void main(String[] args) throws Throwable {
        String lz4 = args.length > 0 ? args[0] : Lz4RawBlockDecoder.class.getName();
        String snappy = args.length > 1 ? args[1] : SnappyRawDecoder.class.getName();
        int inputs = args.length > 2 ? Integer.parseInt(args[2]) : CompressionFormatOracle.DEFAULT_INPUTS;
        long started = System.nanoTime();
        System.out.print(run(lz4, snappy, inputs));
        System.err.printf("M3_DECODER_AGREEMENT_SECONDS=%.1f%n", (System.nanoTime() - started) / 1e9);
    }

    /** Runs the oracle over the relocated decoders with the default input count. */
    public static String runAll() throws Throwable {
        return run(Lz4RawBlockDecoder.class.getName(), SnappyRawDecoder.class.getName(),
                CompressionFormatOracle.DEFAULT_INPUTS);
    }

    static String run(String lz4Class, String snappyClass, int inputs) throws Throwable {
        CompressionDecoderAgreement oracle = new CompressionDecoderAgreement(lz4Class, snappyClass);
        oracle.encoderStreams(inputs);
        oracle.grammarStreams();
        oracle.malformedStreams();
        oracle.fuzz();
        return oracle.report();
    }

    // ---- 1. valid streams -------------------------------------------------------------------------------------------

    private void encoderStreams(int inputs) throws Throwable {
        Lz4RawBlockEncoder lz4Encoder = new Lz4RawBlockEncoder(TABLE_BITS);
        SnappyRawEncoder snappyEncoder = new SnappyRawEncoder(TABLE_BITS);
        for (int index = 0; index < inputs; index++) {
            byte[] data = CompressionFormatOracle.input(index);
            byte[] packedLz4 = lz4Encoder.encode(data, CompressionFormatOracle.lz4Bound(data.length));
            byte[] packedSnappy = snappyEncoder.encode(data, CompressionFormatOracle.snappyBound(data.length));
            validLz4("ENCODER", packedLz4, data);
            validSnappy("ENCODER", packedSnappy, data);
            count("ENCODER_INPUT_BYTES", data.length);
        }
        count("ENCODER_INPUTS", inputs);
    }

    private void grammarStreams() throws Throwable {
        SplittableRandom random = new SplittableRandom(SEED);
        for (int i = 0; i < GRAMMAR; i++) {
            Lz4Stream lz4Stream = Lz4Stream.grammar(random, i);
            validLz4("GRAMMAR", lz4Stream.wire(), lz4Stream.plain());
            SnappyStream snappyStream = SnappyStream.grammar(random, i, false);
            validSnappy("GRAMMAR", snappyStream.wire(), snappyStream.plain());
            count("GRAMMAR_LZ4_MATCHES", lz4Stream.offsets.size());
            count("GRAMMAR_SNAPPY_COPY_TAGS", snappyStream.copies());
        }
    }

    private void validLz4(String set, byte[] wire, byte[] expected) throws Throwable {
        byte[] donor;
        byte[] spec;
        try {
            donor = (byte[]) lz4.invokeExact(wire, expected.length, expected.length);
            spec = CompressionSpecDecoders.lz4Block(wire, expected.length, null);
        } catch (IllegalArgumentException refused) {
            throw new AssertionError("LZ4 " + set + " round trip refused: " + refused.getMessage(), refused);
        }
        check(Arrays.equals(donor, expected) && Arrays.equals(spec, expected), "LZ4 " + set + " round trip");
        update(donor);
        count(set + "_LZ4_ACCEPTED_BOTH", 1);
    }

    private void validSnappy(String set, byte[] wire, byte[] expected) throws Throwable {
        byte[] donor;
        byte[] spec;
        try {
            donor = (byte[]) snappy.invokeExact(wire, expected.length);
            spec = CompressionSpecDecoders.snappyRaw(wire, null);
        } catch (IllegalArgumentException refused) {
            throw new AssertionError("Snappy " + set + " round trip refused: " + refused.getMessage(), refused);
        }
        check(Arrays.equals(donor, expected) && Arrays.equals(spec, expected), "Snappy " + set + " round trip");
        update(donor);
        count(set + "_SNAPPY_ACCEPTED_BOTH", 1);
    }

    // ---- 2. malformed streams ---------------------------------------------------------------------------------------

    private void malformedStreams() throws Throwable {
        SplittableRandom random = new SplittableRandom(SEED ^ 0x6d61_6c66L);
        for (Kind kind : Kind.values()) {
            for (int i = 0; i < MALFORMED_PER_KIND; i++) {
                if (kind != Kind.VARINT_OVERFLOW) {
                    malformedLz4(kind, random, i);
                }
                if (kind != Kind.LENGTH_RUN_OVERFLOW) {
                    malformedSnappy(kind, random, i);
                }
            }
        }
    }

    private void malformedLz4(Kind kind, SplittableRandom random, int i) throws Throwable {
        Lz4Stream stream = Lz4Stream.grammar(random, i);
        while (stream.offsets.isEmpty() && (kind == Kind.OFFSET_ZERO || kind == Kind.OFFSET_BEYOND_OUTPUT
                || kind == Kind.MATCH_OVERRUN)) {
            stream = Lz4Stream.grammar(random, i);
        }
        byte[] wire = stream.wire();
        int size = stream.size();
        switch (kind) {
            case TRUNCATED -> wire = Arrays.copyOf(wire, random.nextInt(wire.length));
            case OFFSET_ZERO -> wire = stream.withOffset(random.nextInt(stream.offsets.size()), 0);
            case OFFSET_BEYOND_OUTPUT -> wire = beyondLz4(stream, random);
            case LITERAL_OVERRUN -> size -= 1 + random.nextInt(stream.finalLiterals);
            case MATCH_OVERRUN -> {
                wire = Arrays.copyOf(wire, stream.lastMatchWireEnd);
                size = stream.lastMatchEnd - 1 - random.nextInt(stream.lastMatchLength);
            }
            case LENGTH_RUN_OVERFLOW -> {
                size = 1 + random.nextInt(2000);
                wire = Lz4Stream.lengthRun(random, size, i % 2 == 0);
            }
            case TRAILING -> wire = trailing(wire, random);
            case DECLARED_TOO_LONG -> size += 1 + random.nextInt(100);
            default -> throw new AssertionError(kind);
        }
        byte[] packed = wire;
        int declared = size;
        refusedBoth("LZ4", kind, () -> CompressionSpecDecoders.lz4Block(wireCopy(packed), declared, null),
                () -> (byte[]) lz4.invokeExact(packed, declared, declared));
    }

    private static byte[] beyondLz4(Lz4Stream stream, SplittableRandom random) {
        for (int attempt = 0; attempt < stream.offsets.size(); attempt++) {
            int j = random.nextInt(stream.offsets.size());
            int at = stream.matchOps.get(j);
            if (at < 0xffff) {
                return stream.withOffset(j, at + 1 + random.nextInt(Math.min(0xffff - at, 1000)));
            }
        }
        return stream.withOffset(0, 0); // every match sits past 64 KiB: offset 0 stands in (rare, counted alike)
    }

    private void malformedSnappy(Kind kind, SplittableRandom random, int i) throws Throwable {
        boolean needsCopy = kind == Kind.OFFSET_ZERO || kind == Kind.OFFSET_BEYOND_OUTPUT || kind == Kind.MATCH_OVERRUN;
        SnappyStream stream = SnappyStream.grammar(random, i, kind == Kind.LITERAL_OVERRUN);
        while (needsCopy && (stream.copies() == 0 || (kind == Kind.MATCH_OVERRUN && !stream.endsWithCopy()))) {
            stream = SnappyStream.grammar(random, i, false);
        }
        byte[] wire;
        int size = stream.size();
        switch (kind) {
            case TRUNCATED -> wire = Arrays.copyOf(stream.wire(), random.nextInt(stream.wire().length));
            case OFFSET_ZERO -> wire = stream.withCopyOffset(random, false);
            case OFFSET_BEYOND_OUTPUT -> wire = stream.withCopyOffset(random, true);
            case LITERAL_OVERRUN -> wire = stream.withDeclared(size - 1 - random.nextInt(stream.lastLength()));
            case MATCH_OVERRUN -> wire = stream.withDeclared(size - 1 - random.nextInt(stream.lastLength()));
            case VARINT_OVERFLOW -> wire = varintOverflow(stream, random);
            case TRAILING -> wire = trailing(stream.wire(), random);
            case DECLARED_TOO_LONG -> wire = stream.withDeclared(size + 1 + random.nextInt(100));
            default -> throw new AssertionError(kind);
        }
        byte[] packed = wire;
        int cap = Math.max(size, 0) + 128;
        refusedBoth("SNAPPY", kind, () -> CompressionSpecDecoders.snappyRaw(wireCopy(packed), null),
                () -> (byte[]) snappy.invokeExact(packed, cap));
    }

    /** Length headers past 5 bytes or past 32 bits, or at least 2^31 - 8 (refused before any allocation). */
    private static byte[] varintOverflow(SnappyStream stream, SplittableRandom random) {
        Bytes header = new Bytes();
        switch (random.nextInt(3)) {
            case 0 -> {
                int bytes = 5 + random.nextInt(5);
                for (int b = 0; b < bytes; b++) {
                    header.add(0x80 | random.nextInt(128));
                }
                header.add(random.nextInt(128));
            }
            case 1 -> {
                for (int b = 0; b < 4; b++) {
                    header.add(0x80 | random.nextInt(128));
                }
                header.add(0x10 + random.nextInt(0x70));
            }
            default -> SnappyStream.varint(header, (1L << 31) - 8 + random.nextLong((1L << 32) - (1L << 31) + 8));
        }
        byte[] body = stream.body();
        header.add(body, 0, body.length);
        return header.toArray();
    }

    private static byte[] trailing(byte[] wire, SplittableRandom random) {
        byte[] out = Arrays.copyOf(wire, wire.length + 1 + random.nextInt(8));
        for (int at = wire.length; at < out.length; at++) {
            out[at] = (byte) random.nextInt(256);
        }
        return out;
    }

    private void refusedBoth(String codec, Kind kind, Decode spec, Decode donor) {
        String specMessage = refusal(spec, codec + " " + kind + " reference");
        String donorMessage = refusal(donor, codec + " " + kind + " donor");
        EnumSet<Refusal> specClasses = classes(specMessage);
        EnumSet<Refusal> donorClasses = classes(donorMessage);
        EnumSet<Refusal> shared = EnumSet.copyOf(donorClasses);
        shared.retainAll(specClasses);
        check(!shared.isEmpty(), codec + " " + kind + " refusal classes differ: donor '" + donorMessage
                + "' reference '" + specMessage + "'");
        count("MALFORMED_" + codec + "_" + kind + "_REFUSED_BOTH", 1);
        count("MALFORMED_" + codec + "_REFUSED_BOTH", 1);
        count("MALFORMED_" + codec + "_CLASS_" + (donorClasses.size() == 1 && specClasses.size() == 1
                ? "EXACT" : "SHARED"), 1);
        count("PAIR " + codec + " | " + kind + " | " + donorMessage + " | " + specMessage, 1);
    }

    private static String refusal(Decode decode, String what) {
        try {
            decode.run();
        } catch (IllegalArgumentException refused) {
            return refused.getMessage();
        } catch (Throwable other) {
            throw new AssertionError(what + " threw " + other, other);
        }
        throw new AssertionError(what + " accepted a malformed stream");
    }

    private static EnumSet<Refusal> classes(String message) {
        EnumSet<Refusal> known = CLASSES.get(message);
        if (known == null) {
            throw new AssertionError("refusal message without a class: " + message);
        }
        return known;
    }

    // ---- 3. fuzz ----------------------------------------------------------------------------------------------------

    private void fuzz() throws Throwable {
        SplittableRandom random = new SplittableRandom(SEED ^ 0x6675_7a7aL);
        Lz4RawBlockEncoder lz4Encoder = new Lz4RawBlockEncoder(10);
        SnappyRawEncoder snappyEncoder = new SnappyRawEncoder(10);
        for (int i = 0; i < FUZZ; i++) {
            byte[] lz4Wire;
            byte[] snappyWire;
            int expected;
            switch (i % 4) {
                case 0, 1 -> {
                    lz4Wire = randomBytes(random, i % 4 == 0 ? 300 : 40);
                    snappyWire = randomBytes(random, i % 4 == 0 ? 300 : 40);
                    expected = random.nextInt(i % 4 == 0 ? 1025 : 65);
                }
                case 2 -> {
                    Lz4Stream l = Lz4Stream.grammar(random, i);
                    lz4Wire = mutate(l.wire(), random);
                    snappyWire = mutate(SnappyStream.grammar(random, i, false).wire(), random);
                    expected = l.size();
                }
                default -> {
                    byte[] data = new byte[random.nextInt(2049)];
                    for (int at = 0; at < data.length; at++) {
                        data[at] = (byte) random.nextInt(i % 8 == 3 ? 4 : 256);
                    }
                    lz4Wire = mutate(lz4Encoder.encode(data, data.length * 2 + 64), random);
                    snappyWire = mutate(snappyEncoder.encode(data, data.length * 2 + 64), random);
                    expected = data.length;
                }
            }
            fuzzLz4(lz4Wire, expected);
            fuzzSnappy(snappyWire, i % 2 == 0 ? FUZZ_CAP : 1 + random.nextInt(4096));
        }
        count("FUZZ_STREAMS", FUZZ);
    }

    private void fuzzLz4(byte[] wire, int expected) throws Throwable {
        byte[] donor = donorOrNull(() -> (byte[]) lz4.invokeExact(wire, expected, expected), "LZ4");
        check(donor == null || donor.length == expected, "LZ4 fuzz output length");
        String specRefusal = null;
        byte[] spec = null;
        try {
            spec = CompressionSpecDecoders.lz4Block(wireCopy(wire), expected, null);
        } catch (IllegalArgumentException refused) {
            specRefusal = refused.getMessage();
        }
        decision("LZ4", donor, spec, specRefusal);
    }

    private void fuzzSnappy(byte[] wire, int cap) throws Throwable {
        byte[] donor = donorOrNull(() -> (byte[]) snappy.invokeExact(wire, cap), "SNAPPY");
        check(donor == null || donor.length <= cap, "Snappy fuzz output past the cap");
        long declared = declaredLength(wire);
        if (declared < 0 || declared > cap) {
            check(donor == null, "Snappy fuzz accepted a declared length past the cap");
            count("FUZZ_SNAPPY_REFERENCE_NOT_RUN_DECLARED_PAST_CAP", 1);
            return;
        }
        String specRefusal = null;
        byte[] spec = null;
        try {
            spec = CompressionSpecDecoders.snappyRaw(wireCopy(wire), null);
        } catch (IllegalArgumentException refused) {
            specRefusal = refused.getMessage();
        }
        decision("SNAPPY", donor, spec, specRefusal);
    }

    private byte[] donorOrNull(Decode decode, String codec) {
        try {
            byte[] out = (byte[]) decode.run();
            count("FUZZ_" + codec + "_DONOR_ACCEPTED", 1);
            return out;
        } catch (IllegalArgumentException refused) {
            classes(refused.getMessage());
            count("FUZZ_" + codec + "_DONOR_REFUSED", 1);
            return null;
        } catch (Throwable other) {
            throw new AssertionError(codec + " fuzz threw " + other + " (only IllegalArgumentException is documented)",
                    other);
        }
    }

    private void decision(String codec, byte[] donor, byte[] spec, String specRefusal) {
        if (donor != null && spec != null) {
            check(Arrays.equals(donor, spec), codec + " fuzz outputs differ");
            count("FUZZ_" + codec + "_ACCEPTED_BOTH", 1);
        } else if (donor == null && spec == null) {
            count("FUZZ_" + codec + "_REFUSED_BOTH", 1);
        } else if (donor != null) {
            check(classes(specRefusal).contains(Refusal.END_OF_BLOCK) && codec.equals("LZ4"),
                    codec + " fuzz: donor accepted, reference refused '" + specRefusal + "'");
            count("FUZZ_" + codec + "_DONOR_ACCEPTED_REFERENCE_END_OF_BLOCK_RULE", 1);
        } else {
            throw new AssertionError(codec + " fuzz: reference accepted, donor refused");
        }
    }

    /** The Snappy length varint as the reference decoder reads it (at most 5 bytes), or -1 when it is not complete. */
    private static long declaredLength(byte[] wire) {
        long value = 0;
        for (int shift = 0, at = 0; shift <= 28 && at < wire.length; shift += 7, at++) {
            value |= (long) (wire[at] & 0x7f) << shift;
            if ((wire[at] & 0x80) == 0) {
                return value;
            }
        }
        return -1;
    }

    private static byte[] randomBytes(SplittableRandom random, int maximum) {
        byte[] out = new byte[random.nextInt(maximum + 1)];
        random.nextBytes(out);
        return out;
    }

    /** One to four mutations: bit flip, byte replacement, byte insertion, byte deletion. */
    private static byte[] mutate(byte[] wire, SplittableRandom random) {
        byte[] out = wire.clone();
        for (int m = 1 + random.nextInt(4); m > 0; m--) {
            int at = out.length == 0 ? 0 : random.nextInt(out.length);
            switch (random.nextInt(4)) {
                case 0 -> { if (out.length > 0) out[at] ^= (byte) (1 << random.nextInt(8)); }
                case 1 -> { if (out.length > 0) out[at] = (byte) random.nextInt(256); }
                case 2 -> {
                    byte[] grown = new byte[out.length + 1];
                    System.arraycopy(out, 0, grown, 0, at);
                    grown[at] = (byte) random.nextInt(256);
                    System.arraycopy(out, at, grown, at + 1, out.length - at);
                    out = grown;
                }
                default -> {
                    if (out.length > 0) {
                        byte[] shrunk = new byte[out.length - 1];
                        System.arraycopy(out, 0, shrunk, 0, at);
                        System.arraycopy(out, at + 1, shrunk, at, out.length - at - 1);
                        out = shrunk;
                    }
                }
            }
        }
        return out;
    }

    // ---- report -----------------------------------------------------------------------------------------------------

    private String report() {
        StringBuilder out = new StringBuilder();
        counters.forEach((key, value) -> {
            if (key.startsWith("PAIR ")) {
                out.append(PREFIX).append("PAIR=").append(key.substring(5)).append(" | ").append(value).append('\n');
            } else {
                out.append(PREFIX).append(key).append('=').append(value).append('\n');
            }
        });
        out.append(PREFIX).append("VALID_OUTPUT_SHA256=").append(HexFormat.of().formatHex(digest.digest()))
                .append('\n');
        out.append(PREFIX).append("RESULT=PASS\n");
        return out.toString();
    }

    private void count(String key, long amount) {
        counters.merge(key, amount, Long::sum);
    }

    private void update(byte[] decoded) {
        int length = decoded.length;
        digest.update(new byte[] {(byte) (length >>> 24), (byte) (length >>> 16), (byte) (length >>> 8),
                (byte) length});
        digest.update(decoded);
    }

    private static byte[] wireCopy(byte[] wire) {
        return wire.clone();
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void refusal(EnumSet<Refusal> classes, String... messages) {
        for (String message : messages) {
            CLASSES.put(message, classes);
        }
    }

    @FunctionalInterface
    private interface Decode {
        Object run() throws Throwable;
    }

    /** Growable byte buffer of the stream builders. */
    static final class Bytes {
        private byte[] bytes = new byte[64];
        private int size;

        void add(int value) {
            ensure(1);
            bytes[size++] = (byte) value;
        }

        void add(byte[] values, int offset, int length) {
            ensure(length);
            System.arraycopy(values, offset, bytes, size, length);
            size += length;
        }

        void copy(int distance, int length) {
            ensure(length);
            for (int i = 0; i < length; i++, size++) {
                bytes[size] = bytes[size - distance];
            }
        }

        int size() {
            return size;
        }

        byte[] toArray() {
            return Arrays.copyOf(bytes, size);
        }

        private void ensure(int more) {
            if (size + more > bytes.length) {
                bytes = Arrays.copyOf(bytes, Math.max(size + more, bytes.length * 2));
            }
        }
    }

    /** One LZ4 block built from sequences (literals, offset, match length) and a final literal run. */
    static final class Lz4Stream {
        private final List<byte[]> literals = new ArrayList<>();
        final List<Integer> offsets = new ArrayList<>();
        private final List<Integer> lengths = new ArrayList<>();
        final List<Integer> matchOps = new ArrayList<>();
        private byte[] tail = new byte[0];
        private byte[] plain;
        int finalLiterals;
        int lastMatchEnd;
        int lastMatchLength;
        int lastMatchWireEnd;

        static Lz4Stream grammar(SplittableRandom random, int salt) {
            Lz4Stream stream = new Lz4Stream();
            Bytes out = new Bytes();
            int lastStart = -1;
            for (int s = random.nextInt(6); s > 0; s--) {
                byte[] literal = content(random, (out.size() == 0 ? 1 : 0) + length(random, salt, 20, 300, 1200));
                out.add(literal, 0, literal.length);
                int op = out.size();
                int offset = random.nextInt(4) == 0 ? 1 + random.nextInt(Math.min(op, 4))
                        : 1 + random.nextInt(Math.min(op, 0xffff));
                int length = 4 + length(random, salt + 1, 30, 300, 2000);
                out.copy(offset, length);
                stream.literals.add(literal);
                stream.offsets.add(offset);
                stream.lengths.add(length);
                stream.matchOps.add(op);
                lastStart = op;
                stream.lastMatchLength = length;
            }
            int needed = lastStart < 0 ? 5 : Math.max(5, lastStart + 12 - out.size());
            stream.tail = content(random, needed + random.nextInt(salt % 7 == 0 ? 400 : 40));
            stream.lastMatchEnd = out.size();
            out.add(stream.tail, 0, stream.tail.length);
            stream.finalLiterals = stream.tail.length;
            stream.plain = out.toArray();
            stream.lastMatchWireEnd = stream.sequencesWireSize();
            return stream;
        }

        byte[] plain() {
            return plain;
        }

        int size() {
            return plain.length;
        }

        byte[] wire() {
            return withOffset(-1, 0);
        }

        byte[] withOffset(int index, int offset) {
            Bytes out = new Bytes();
            for (int s = 0; s < offsets.size(); s++) {
                sequence(out, literals.get(s), s == index ? offset : offsets.get(s), lengths.get(s));
            }
            out.add(Math.min(tail.length, 15) << 4);
            extension(out, tail.length);
            out.add(tail, 0, tail.length);
            return out.toArray();
        }

        /** Wire size up to the end of the last match (sequences only), for the match-overrun defect. */
        private int sequencesWireSize() {
            Bytes out = new Bytes();
            for (int s = 0; s < offsets.size(); s++) {
                sequence(out, literals.get(s), offsets.get(s), lengths.get(s));
            }
            return out.size();
        }

        private static void sequence(Bytes out, byte[] literal, int offset, int length) {
            int extra = length - 4;
            out.add((Math.min(literal.length, 15) << 4) | Math.min(extra, 15));
            extension(out, literal.length);
            out.add(literal, 0, literal.length);
            out.add(offset & 0xff);
            out.add(offset >>> 8);
            extension(out, extra);
        }

        private static void extension(Bytes out, int length) {
            if (length < 15) {
                return;
            }
            int remaining = length - 15;
            for (; remaining >= 255; remaining -= 255) {
                out.add(255);
            }
            out.add(remaining);
        }

        /** A literal or match length whose 255-run extension carries it past {@code size} output bytes. */
        static byte[] lengthRun(SplittableRandom random, int size, boolean literalRun) {
            Bytes out = new Bytes();
            int runs = (size - 15) / 255 + 1 + random.nextInt(3);
            if (literalRun) {
                out.add(0xf0);
            } else {
                out.add(0x1f);
                out.add('a');
                out.add(1);
                out.add(0);
            }
            for (int r = 0; r < runs; r++) {
                out.add(255);
            }
            out.add(random.nextInt(255));
            byte[] extra = content(random, 1 + random.nextInt(32));
            out.add(extra, 0, extra.length);
            return out.toArray();
        }
    }

    /** One Snappy raw stream built from elements (literals with a chosen header width, COPY_1/2/4 tags). */
    static final class SnappyStream {
        private final List<int[]> elements = new ArrayList<>(); // {kind 0..3, length, offset or header width, op}
        private final List<byte[]> payloads = new ArrayList<>();
        private byte[] plain;

        static SnappyStream grammar(SplittableRandom random, int salt, boolean endWithLiteral) {
            SnappyStream stream = new SnappyStream();
            Bytes out = new Bytes();
            int count = 1 + random.nextInt(12);
            for (int e = 0; e < count; e++) {
                boolean last = e == count - 1;
                if (out.size() == 0 || (last && endWithLiteral) || random.nextInt(3) == 0) {
                    byte[] literal = content(random, 1 + length(random, salt, 59, 300, 70_000));
                    int minimal = literal.length <= 60 ? 0 : widthOf(literal.length - 1);
                    int width = minimal < 4 && random.nextInt(8) == 0 ? minimal + 1 : minimal;
                    stream.elements.add(new int[] {0, literal.length, width, out.size()});
                    stream.payloads.add(literal);
                    out.add(literal, 0, literal.length);
                } else {
                    int op = out.size();
                    int kind = 1 + random.nextInt(3);
                    int length = kind == 1 ? 4 + random.nextInt(8) : 1 + random.nextInt(64);
                    int limit = kind == 1 ? Math.min(op, 2047) : kind == 2 ? Math.min(op, 0xffff) : op;
                    int offset = random.nextInt(4) == 0 ? 1 + random.nextInt(Math.min(limit, 4))
                            : 1 + random.nextInt(limit);
                    stream.elements.add(new int[] {kind, length, offset, op});
                    stream.payloads.add(null);
                    out.copy(offset, length);
                }
            }
            stream.plain = out.toArray();
            return stream;
        }

        byte[] plain() {
            return plain;
        }

        int size() {
            return plain.length;
        }

        int copies() {
            return (int) elements.stream().filter(element -> element[0] != 0).count();
        }

        boolean endsWithCopy() {
            return elements.get(elements.size() - 1)[0] != 0;
        }

        int lastLength() {
            return elements.get(elements.size() - 1)[1];
        }

        byte[] wire() {
            return withDeclared(plain.length);
        }

        byte[] body() {
            return serialize(-1, 0);
        }

        byte[] withDeclared(long declared) {
            Bytes out = new Bytes();
            varint(out, declared);
            byte[] body = body();
            out.add(body, 0, body.length);
            return out.toArray();
        }

        /** A copy whose offset is 0, or beyond its output position (within the width of its tag). */
        byte[] withCopyOffset(SplittableRandom random, boolean beyond) {
            List<Integer> candidates = new ArrayList<>();
            for (int e = 0; e < elements.size(); e++) {
                int[] element = elements.get(e);
                int limit = element[0] == 1 ? 2047 : element[0] == 2 ? 0xffff : Integer.MAX_VALUE;
                if (element[0] != 0 && (!beyond || element[3] < limit)) {
                    candidates.add(e);
                }
            }
            if (candidates.isEmpty()) {
                return withFirstCopyOffsetZero();
            }
            int e = candidates.get(random.nextInt(candidates.size()));
            int[] element = elements.get(e);
            int limit = element[0] == 1 ? 2047 : element[0] == 2 ? 0xffff : Integer.MAX_VALUE;
            int offset = beyond ? element[3] + 1 + random.nextInt(Math.min(limit - element[3], 1000)) : 0;
            Bytes out = new Bytes();
            varint(out, plain.length);
            byte[] body = serialize(e, offset);
            out.add(body, 0, body.length);
            return out.toArray();
        }

        /** Every copy sits past the reach of its tag: offset 0 on the first copy stands in (rare, counted alike). */
        private byte[] withFirstCopyOffsetZero() {
            int e = 0;
            while (elements.get(e)[0] == 0) {
                e++;
            }
            Bytes out = new Bytes();
            varint(out, plain.length);
            byte[] body = serialize(e, 0);
            out.add(body, 0, body.length);
            return out.toArray();
        }

        private byte[] serialize(int index, int offset) {
            Bytes out = new Bytes();
            for (int e = 0; e < elements.size(); e++) {
                int[] element = elements.get(e);
                if (element[0] == 0) {
                    literal(out, payloads.get(e), element[2]);
                } else {
                    copy(out, element[0], element[1], e == index ? offset : element[2]);
                }
            }
            return out.toArray();
        }

        private static void literal(Bytes out, byte[] literal, int width) {
            int encoded = literal.length - 1;
            if (width == 0) {
                out.add(encoded << 2);
            } else {
                out.add((59 + width) << 2);
                for (int b = 0; b < width; b++) {
                    out.add(encoded >>> (8 * b));
                }
            }
            out.add(literal, 0, literal.length);
        }

        private static void copy(Bytes out, int kind, int length, int offset) {
            if (kind == 1) {
                out.add(((offset >>> 8) << 5) | ((length - 4) << 2) | 1);
                out.add(offset & 0xff);
            } else {
                out.add(((length - 1) << 2) | kind);
                for (int b = 0; b < (kind == 2 ? 2 : 4); b++) {
                    out.add(offset >>> (8 * b));
                }
            }
        }

        static void varint(Bytes out, long value) {
            long remaining = value;
            while ((remaining & ~0x7fL) != 0) {
                out.add((int) (remaining & 0x7f) | 0x80);
                remaining >>>= 7;
            }
            out.add((int) remaining);
        }

        private static int widthOf(int encoded) {
            return encoded <= 0xff ? 1 : encoded <= 0xffff ? 2 : encoded <= 0xffffff ? 3 : 4;
        }
    }

    /** Short (mostly up to {@code small}), sometimes up to {@code medium}, rarely up to {@code large}. */
    private static int length(SplittableRandom random, int salt, int small, int medium, int large) {
        int pick = random.nextInt(32);
        return pick == 0 && salt % 3 == 0 ? random.nextInt(large + 1)
                : pick < 5 ? random.nextInt(medium + 1) : random.nextInt(small + 1);
    }

    /** Random, low-alphabet or single-byte content (so later matches find real repeats). */
    private static byte[] content(SplittableRandom random, int length) {
        byte[] out = new byte[length];
        switch (random.nextInt(3)) {
            case 0 -> random.nextBytes(out);
            case 1 -> {
                for (int i = 0; i < length; i++) {
                    out[i] = (byte) ('a' + random.nextInt(4));
                }
            }
            default -> Arrays.fill(out, (byte) random.nextInt(256));
        }
        return out;
    }
}
