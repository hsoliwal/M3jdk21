/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
 */

/*
 * Not a jtreg test: the differential dump driver of recipe m3/qualification/hash-xxhash64-streaming-20261106.
 *
 *   java [--patch-module java.base=<classes>] --add-exports java.base/jdk.internal.m3.hash=ALL-UNNAMED
 *        [-cp <donor classes>] M3XxHash64StreamingDump m3|<donor one-shot class> [random-cases]
 *
 * One line per case: every length 0..1024 and <random-cases> log-uniform random lengths up to one MiB, each with the
 * seeds 0, 1, -1 and one drawn seed, each through the one-shot, the whole heap buffer (read-only, big-endian), the whole
 * direct buffer (sliced at offset 9), 1 / 31 / 32 / 33-byte updates (inputs up to 64 KiB), random splits, a 47-byte
 * prefix with an intermediate getValue, and reset-and-reuse; the no-seed one-shot forms (byte[], heap and direct
 * ByteBuffer) under the default seed; hashFile over files of the lengths 0, 1, 31, 32, 33, 4095, 4096, 4097 and
 * 1 MiB + 17 with the default seed and the four seeds. The dump over the donor surface (reflection on the donor's
 * XXHash64 and its nested StreamingHash64) and the dump over the relocated surface (M3XxHash64Streaming and
 * M3LongHashFunction.xx) must be byte-identical, and in every line every streaming value must equal the one-shot of the
 * same bytes (a MISMATCH line otherwise; exit code 1). Inputs, lengths, splits and the drawn seed come from one
 * SplittableRandom(M3JDK_DIFFERENTIAL_SEED), so both runs see the same corpus.
 */

import java.io.BufferedOutputStream;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.SplittableRandom;
import jdk.internal.m3.hash.LongHashFunction;
import jdk.internal.m3.hash.XxHash64Streaming;

public final class XxHash64StreamingDump {
    //FLLDCIM: M3 - LITERAL_CONSTANTS (tests): the corpus of the differential, one bound per name
    /** Seed of the SplittableRandom that draws every input byte, random length, split and the fourth seed. */
    static final long M3JDK_DIFFERENTIAL_SEED = 0x4d334a444b5f5858L;
    /** Every length 0..SMALL_MAX runs every pattern. */
    static final int SMALL_MAX = 1024;
    /** Default count of the random-length cases. */
    static final int RANDOM_CASES = 10_000;
    /** Random lengths are log-uniform: a bucket b in 0..RANDOM_BUCKETS, then uniform in [2^(b-1), 2^b); bucket 0 is the empty input; bucket 20 reaches one MiB - 1. */
    static final int RANDOM_BUCKETS = 20;
    /** The byte-wise and stripe-boundary split patterns run on inputs up to this length. */
    static final int SPLIT_MAX = 65_536;
    /** Upper bound of one random split. */
    static final int RANDOM_SPLIT_MAX = 65_536;
    /** The donor test's intermediate prefix. */
    static final int PREFIX = 47;
    /** Direct buffers carry the input at this offset, with this slack after it (the donor test's layout). */
    static final int DIRECT_OFFSET = 9;
    static final int DIRECT_SLACK = 17;
    /** The known xxHash64 of the empty input with seed 0. */
    static final long EMPTY_SEED0 = 0xEF46DB3751D8E999L;
    static final int[] CHUNKS = {1, 31, 32, 33};
    static final int[] FILE_LENGTHS = {0, 1, 31, 32, 33, 4_095, 4_096, 4_097, (1 << 20) + 17};
    static final String SKIP = "-";

    /** The donor's and the owner's surface, one method per API shape the dump exercises. */
    interface Surface {
        long oneShot(byte[] data, long seed) throws Throwable;
        long oneShotDefault(byte[] data) throws Throwable;
        long oneShotBuffer(ByteBuffer buffer) throws Throwable;
        Object streaming(long seed) throws Throwable;
        void update(Object streaming, ByteBuffer input) throws Throwable;
        long value(Object streaming) throws Throwable;
        void reset(Object streaming) throws Throwable;
        long hashFile(Path path) throws Throwable;
        long hashFile(Path path, long seed) throws Throwable;
    }

    /** The relocated surface: M3XxHash64Streaming and the owner's one-shot M3LongHashFunction.xx(seed). */
    static final class Relocated implements Surface {
        @Override public long oneShot(byte[] data, long seed) { return LongHashFunction.xx(seed).hashBytes(data); }
        @Override public long oneShotDefault(byte[] data) { return oneShot(data, XxHash64Streaming.DEFAULT_SEED); }
        @Override public long oneShotBuffer(ByteBuffer buffer) { return LongHashFunction.xx(XxHash64Streaming.DEFAULT_SEED).hashBytes(buffer); }
        @Override public Object streaming(long seed) { return new XxHash64Streaming(seed); }
        @Override public void update(Object streaming, ByteBuffer input) { ((XxHash64Streaming) streaming).update(input); }
        @Override public long value(Object streaming) { return ((XxHash64Streaming) streaming).getValue(); }
        @Override public void reset(Object streaming) { ((XxHash64Streaming) streaming).reset(); }
        @Override public long hashFile(Path path) throws Throwable { return XxHash64Streaming.hashFile(path); }
        @Override public long hashFile(Path path, long seed) throws Throwable { return XxHash64Streaming.hashFile(path, seed); }
    }

    /** The donor surface through public method handles: hash(byte[], long), hash(byte[]), hash(ByteBuffer), hashFile(Path[, long]) and the nested StreamingHash64. */
    static final class Donor implements Surface {
        private static final String NESTED = "$StreamingHash64";
        private final MethodHandle oneShot, oneShotDefault, oneShotBuffer, constructor, update, value, reset, file, fileSeeded;

        Donor(String className) throws ReflectiveOperationException {
            Class<?> owner = Class.forName(className);
            Class<?> nested = Class.forName(className + NESTED);
            MethodHandles.Lookup lookup = MethodHandles.publicLookup();
            oneShot = lookup.findStatic(owner, "hash", MethodType.methodType(long.class, byte[].class, long.class));
            oneShotDefault = lookup.findStatic(owner, "hash", MethodType.methodType(long.class, byte[].class));
            oneShotBuffer = lookup.findStatic(owner, "hash", MethodType.methodType(long.class, ByteBuffer.class));
            constructor = lookup.findConstructor(nested, MethodType.methodType(void.class, long.class));
            update = lookup.findVirtual(nested, "update", MethodType.methodType(void.class, ByteBuffer.class));
            value = lookup.findVirtual(nested, "getValue", MethodType.methodType(long.class));
            reset = lookup.findVirtual(nested, "reset", MethodType.methodType(void.class));
            file = lookup.findStatic(owner, "hashFile", MethodType.methodType(long.class, Path.class));
            fileSeeded = lookup.findStatic(owner, "hashFile", MethodType.methodType(long.class, Path.class, long.class));
        }

        @Override public long oneShot(byte[] data, long seed) throws Throwable { return (long) oneShot.invoke(data, seed); }
        @Override public long oneShotDefault(byte[] data) throws Throwable { return (long) oneShotDefault.invoke(data); }
        @Override public long oneShotBuffer(ByteBuffer buffer) throws Throwable { return (long) oneShotBuffer.invoke(buffer); }
        @Override public Object streaming(long seed) throws Throwable { return constructor.invoke(seed); }
        @Override public void update(Object streaming, ByteBuffer input) throws Throwable { update.invoke(streaming, input); }
        @Override public long value(Object streaming) throws Throwable { return (long) value.invoke(streaming); }
        @Override public void reset(Object streaming) throws Throwable { reset.invoke(streaming); }
        @Override public long hashFile(Path path) throws Throwable { return (long) file.invoke(path); }
        @Override public long hashFile(Path path, long seed) throws Throwable { return (long) fileSeeded.invoke(path, seed); }
    }

    private final Surface surface;
    private final PrintStream out;
    private final SplittableRandom random = new SplittableRandom(M3JDK_DIFFERENTIAL_SEED);
    private long cases, lines, bytesHashed, mismatches;

    private XxHash64StreamingDump(Surface surface, PrintStream out) {
        this.surface = surface;
        this.out = out;
    }

    public static void main(String[] args) throws Throwable {
        Surface surface = args.length > 0 && !args[0].equals("m3") ? new Donor(args[0]) : new Relocated();
        int randomCases = args.length > 1 ? Integer.parseInt(args[1]) : RANDOM_CASES;
        PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream(FileDescriptor.out), 1 << 16), false);
        XxHash64StreamingDump dump = new XxHash64StreamingDump(surface, out);
        int exit = dump.run(randomCases);
        out.flush();
        System.exit(exit);
    }

    private int run(int randomCases) throws Throwable {
        long[] seeds = {0L, 1L, -1L, random.nextLong()};
        long vector = surface.oneShot(new byte[0], 0L);
        line(String.format("VECTOR empty seed0=%016x expect=%016x", vector, EMPTY_SEED0));
        check(vector == EMPTY_SEED0, "VECTOR", 0, 0L);
        for (int length = 0; length <= SMALL_MAX; length++) {
            dumpCase(bytes(length), seeds);
        }
        for (int at = 0; at < randomCases; at++) {
            dumpCase(bytes(randomLength()), seeds);
        }
        dumpFiles(seeds);
        line(String.format("TOTAL cases=%d lines=%d bytesHashed=%d mismatches=%d", cases, lines, bytesHashed, mismatches));
        return mismatches == 0 ? 0 : 1;
    }

    private int randomLength() {
        int bucket = random.nextInt(RANDOM_BUCKETS + 1);
        return bucket == 0 ? 0 : (1 << (bucket - 1)) + random.nextInt(1 << (bucket - 1));
    }

    private byte[] bytes(int length) {
        byte[] data = new byte[length];
        random.nextBytes(data);
        return data;
    }

    private void dumpCase(byte[] data, long[] seeds) throws Throwable {
        for (long seed : seeds) {
            long one = surface.oneShot(data, seed);
            StringBuilder row = new StringBuilder(256).append("L=").append(data.length).append(" S=").append(hex(seed))
                .append(" one=").append(hex(one));
            compare(row, "heap", stream(seed, heap(data)), one, data.length, seed);
            compare(row, "direct", stream(seed, direct(data)), one, data.length, seed);
            for (int chunk : CHUNKS) {
                row.append(" s").append(chunk).append('=');
                if (data.length <= SPLIT_MAX) {
                    long chunked = chunked(seed, data, chunk);
                    row.append(hex(chunked));
                    check(chunked == one, "s" + chunk, data.length, seed);
                } else {
                    row.append(SKIP);
                }
            }
            compare(row, "rnd", randomSplits(seed, data), one, data.length, seed);
            intermediate(row, seed, data, one);
            line(row.toString());
            cases++;
        }
        long expected = surface.oneShot(data, XxHash64Streaming.DEFAULT_SEED);
        StringBuilder row = new StringBuilder(128).append("L=").append(data.length).append(" D expect=").append(hex(expected));
        compare(row, "oneD", surface.oneShotDefault(data), expected, data.length, XxHash64Streaming.DEFAULT_SEED);
        compare(row, "bufD", surface.oneShotBuffer(heap(data)), expected, data.length, XxHash64Streaming.DEFAULT_SEED);
        compare(row, "dirD", surface.oneShotBuffer(direct(data)), expected, data.length, XxHash64Streaming.DEFAULT_SEED);
        bytesHashed += 4L * data.length;
        line(row.toString());
        cases++;
    }

    private void intermediate(StringBuilder row, long seed, byte[] data, long one) throws Throwable {
        int prefix = Math.min(PREFIX, data.length);
        Object streaming = surface.streaming(seed);
        surface.update(streaming, ByteBuffer.wrap(data, 0, prefix));
        long pre = surface.value(streaming);
        long prefixExpected = surface.oneShot(Arrays.copyOf(data, prefix), seed);
        row.append(" pre=").append(hex(pre));
        check(pre == prefixExpected, "pre", data.length, seed);
        surface.update(streaming, ByteBuffer.wrap(data, prefix, data.length - prefix));
        compare(row, "two", surface.value(streaming), one, data.length, seed);
        surface.reset(streaming);
        surface.update(streaming, ByteBuffer.wrap(data));
        compare(row, "reset", surface.value(streaming), one, data.length, seed);
        bytesHashed += 3L * data.length;
    }

    private long stream(long seed, ByteBuffer input) throws Throwable {
        Object streaming = surface.streaming(seed);
        surface.update(streaming, input);
        bytesHashed += input.limit();
        return surface.value(streaming);
    }

    private long chunked(long seed, byte[] data, int chunk) throws Throwable {
        Object streaming = surface.streaming(seed);
        for (int start = 0; start < data.length; start += chunk) {
            int end = Math.min(data.length, start + chunk);
            surface.update(streaming, ByteBuffer.wrap(data, start, end - start).order(ByteOrder.BIG_ENDIAN).asReadOnlyBuffer());
        }
        bytesHashed += data.length;
        return surface.value(streaming);
    }

    private long randomSplits(long seed, byte[] data) throws Throwable {
        Object streaming = surface.streaming(seed);
        int bound = Math.max(1, Math.min(data.length, RANDOM_SPLIT_MAX));
        for (int start = 0; start < data.length;) {
            int end = Math.min(data.length, start + random.nextInt(1, bound + 1));
            surface.update(streaming, ByteBuffer.wrap(data, start, end - start));
            start = end;
        }
        bytesHashed += data.length;
        return surface.value(streaming);
    }

    private void dumpFiles(long[] seeds) throws Throwable {
        Path directory = Files.createTempDirectory("m3-xxhash64-streaming-dump-");
        Path file = directory.resolve("input.bin");
        try {
            for (int length : FILE_LENGTHS) {
                byte[] data = bytes(length);
                Files.write(file, data);
                StringBuilder row = new StringBuilder(160).append("F=").append(length);
                compare(row, "D", surface.hashFile(file), surface.oneShotDefault(data), length, XxHash64Streaming.DEFAULT_SEED);
                for (long seed : seeds) {
                    compare(row, "S" + hex(seed), surface.hashFile(file, seed), surface.oneShot(data, seed), length, seed);
                }
                bytesHashed += 10L * length;
                line(row.toString());
                cases++;
            }
        } finally {
            Files.deleteIfExists(file);
            Files.deleteIfExists(directory);
        }
    }

    private static ByteBuffer heap(byte[] data) {
        return ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN).asReadOnlyBuffer();
    }

    private static ByteBuffer direct(byte[] data) {
        ByteBuffer direct = ByteBuffer.allocateDirect(data.length + DIRECT_OFFSET + DIRECT_SLACK);
        direct.position(DIRECT_OFFSET);
        direct.put(data);
        direct.flip();
        direct.position(DIRECT_OFFSET);
        return direct.slice().order(ByteOrder.BIG_ENDIAN);
    }

    private void compare(StringBuilder row, String name, long actual, long expected, int length, long seed) {
        row.append(' ').append(name).append('=').append(hex(actual));
        check(actual == expected, name, length, seed);
    }

    private void check(boolean equal, String name, int length, long seed) {
        if (!equal) {
            mismatches++;
            line("MISMATCH " + name + " L=" + length + " S=" + hex(seed));
        }
    }

    private void line(String text) {
        out.println(text);
        lines++;
    }

    private static String hex(long value) {
        return String.format("%016x", value);
    }
}
