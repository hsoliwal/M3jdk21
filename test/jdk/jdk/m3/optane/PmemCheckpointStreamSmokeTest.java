// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
/*
 * @test id=file
 * @summary Bounded checkpoint streams, legacy-format compatibility and failure ownership
 * @requires (os.family == "linux")
 * @modules jdk.m3.io/jdk.m3.optane
 * @run main/othervm -Xmx96m test.PmemCheckpointStreamSmokeTest
 */
/*
 * @test id=posix
 * @summary Checkpoint stream contracts through actual POSIX JNI and injected persistence failures
 * @requires (os.family == "linux")
 * @modules jdk.m3.io/jdk.m3.optane
 * @run main/othervm -Xcheck:jni -Xmx96m test.PmemCheckpointStreamSmokeTest --require-posix
 */
/*
 * @test id=bounded_heap
 * @summary Stream a 32 MiB checkpoint under a 16 MiB JVM heap with bounded transfer arrays
 * @requires (os.family == "linux")
 * @modules jdk.m3.io/jdk.m3.optane
 * @run main/othervm -Xmx16m test.PmemCheckpointStreamSmokeTest --heap-stream
 */
package test;

import jdk.m3.optane.PmemAccess;
import jdk.m3.optane.PmemBackend;
import jdk.m3.optane.PmemBlockStore;
import jdk.m3.optane.PmemCheckpoint;
import jdk.m3.optane.PmemGranularity;
import jdk.m3.optane.PmemRegion;
import jdk.m3.optane.PmemSdk;
import jdk.m3.optane.PmemTaskLog;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

/** Actual FILE/POSIX, old/new protocol and independent crash-model stream oracle. */
public final class PmemCheckpointStreamSmokeTest {
    private static final String SCHEMA = "a".repeat(64);
    private static final String CHECKPOINT = "b".repeat(64);
    private static final String OTHER = "c".repeat(64);
    private static final byte[] PAYLOAD = pattern(173, 7);
    private static int checks, persistenceBoundaries;

    private PmemCheckpointStreamSmokeTest() { }

    public static void main(String[] args) throws Exception {
        if (args.length == 1 && "--heap-stream".equals(args[0])) { heapStream(); return; }
        boolean posix = args.length == 1 && "--require-posix".equals(args[0]);
        if (args.length > 1 || args.length == 1 && !posix) throw new IllegalArgumentException("unknown stream test option");
        realBackend(PmemBackend.FILE);
        if (posix) {
            check(PmemSdk.capabilities().nativeAvailable(), "required JNI backend exists");
            realBackend(PmemBackend.POSIX);
        }
        preflight();
        writerFailures();
        readerFailures();
        lifetimesAndOwnership();
        corruption();
        crashBoundaries();
        System.out.println("PMEM_CHECKPOINT_STREAM_PASS file=true posix=" + posix
                + " checks=" + checks + " persistenceBoundaries=" + persistenceBoundaries);
    }

    private static void realBackend(PmemBackend backend) throws Exception {
        int capacity = 40, maximum = 1024;
        long origin = 17, fileOffset = 4099;
        long length = origin + PmemBlockStore.requiredBytes(capacity, maximum) + 13;
        Path file = Files.createTempFile("m3-checkpoint-stream-", ".bin");
        try {
            size(file, fileOffset + length);
            PmemCheckpoint.Info committed;
            try (PmemRegion region = PmemSdk.open(file, fileOffset, length, PmemAccess.READ_WRITE, backend)) {
                check(region.backend() == backend, "requested backend used");
                PmemBlockStore store = PmemBlockStore.create(region, origin, capacity, maximum, SCHEMA);
                byte[][] chunks = { {}, { 3, 5 }, {}, { 7, 11, 13 }, {} };
                PmemCheckpoint.commit(store, CHECKPOINT, 0, chunks.length, new byte[64], (index, transfer) -> {
                    System.arraycopy(chunks[index], 0, transfer, 0, chunks[index].length);
                    return chunks[index].length;
                });
                check(Arrays.equals(read(store), new byte[] { 3, 5, 7, 11, 13 }), "old chunks flatten in order, including empty chunks");
                committed = PmemCheckpoint.commitStream(store, CHECKPOINT, 1, 200, new byte[64], out -> {
                    out.write(PAYLOAD[0]);
                    out.write(PAYLOAD, 1, 72);
                    out.flush();
                    out.write(new byte[0]);
                    out.write(PAYLOAD, 73, PAYLOAD.length - 73);
                    out.close();
                    out.close();
                    check(PmemCheckpoint.inspect(store, CHECKPOINT).generation() == 0, "closing writer cannot publish before callback returns");
                });
                check(committed.chunks() == 3 && committed.totalBytes() == PAYLOAD.length, "partial final chunk and actual length committed");
                check(Arrays.equals(read(store), PAYLOAD), "stream payload exact");
                RecordingTarget target = new RecordingTarget();
                check(PmemCheckpoint.restore(store, CHECKPOINT, new byte[64], target).equals(committed), "old restore reads new manifest");
                check(target.commits == 1 && target.aborts == 0 && Arrays.equals(target.bytes.toByteArray(), PAYLOAD), "old receiver observes complete new stream");
                IOException primary = new IOException("encoder failed after close");
                IOException observed = expect(IOException.class, () -> PmemCheckpoint.commitStream(store, CHECKPOINT, 2, 200,
                        new byte[64], out -> { out.write(PAYLOAD); out.close(); throw primary; }));
                check(observed == primary && PmemCheckpoint.inspect(store, CHECKPOINT).equals(committed), "encoder failure after close retains prior checkpoint");
                int[] calls = { 0 };
                expect(IllegalArgumentException.class, () -> PmemCheckpoint.commitStream(store, CHECKPOINT, 1, 1,
                        new byte[64], out -> calls[0]++));
                check(calls[0] == 0, "stale generation refuses before encoder");
            }
            try (PmemRegion region = PmemSdk.open(file, fileOffset, length, PmemAccess.READ_WRITE, backend)) {
                PmemBlockStore store = PmemBlockStore.open(region, origin, capacity, maximum, SCHEMA);
                check(PmemCheckpoint.inspect(store, CHECKPOINT).equals(committed), "manifest survives real close/reopen");
                check(Arrays.equals(read(store), PAYLOAD), "stream survives real close/reopen");
                byte[] decoded = PmemCheckpoint.readStream(store, CHECKPOINT, 200, new byte[64], in -> {
                    check(in.available() == PAYLOAD.length, "available is finite remaining length");
                    check(in.skip(-1) == 0 && in.skip(0) == 0, "nonpositive skips preserve cursor");
                    byte[] result = new byte[PAYLOAD.length];
                    check(in.readNBytes(result, 0, 17) == 17, "readNBytes crosses bounded transfer interface");
                    check(in.readNBytes(result, 17, result.length - 17) == result.length - 17, "exact remaining read");
                    check(in.read() == -1 && in.read(new byte[0]) == 0 && in.available() == 0, "finite EOF including zero-length read");
                    in.close();
                    in.close();
                    return result;
                });
                check(Arrays.equals(decoded, PAYLOAD), "decoder result returned after complete input");
                PmemCheckpoint.Info empty = PmemCheckpoint.commitStream(store, CHECKPOINT, 2, 0, new byte[64], out -> {
                    out.write(new byte[0]); out.flush(); out.close();
                });
                check(empty.chunks() == 0 && empty.totalBytes() == 0, "empty stream uses existing zero-chunk encoding");
                check(PmemCheckpoint.readStream(store, CHECKPOINT, 0, new byte[1], in -> in.read()) == -1, "empty stream EOF");
            }
        } finally { Files.deleteIfExists(file); }
    }

    private static void preflight() throws Exception {
        Memory region = memory(4, 256);
        PmemBlockStore store = PmemBlockStore.create(region, 0, 4, 256, SCHEMA);
        int[] calls = { 0 };
        for (long bound : new long[] { -1, Long.MAX_VALUE, 65L * PmemCheckpoint.maxChunks(store) })
            expect(IllegalArgumentException.class, () -> PmemCheckpoint.commitStream(store, CHECKPOINT, 0, bound,
                    new byte[64], out -> calls[0]++));
        expect(IllegalArgumentException.class, () -> PmemCheckpoint.commitStream(store, CHECKPOINT, -1, 0,
                new byte[64], out -> calls[0]++));
        expect(IllegalArgumentException.class, () -> PmemCheckpoint.commitStream(store, CHECKPOINT, 0, 0,
                new byte[0], out -> calls[0]++));
        expect(IllegalArgumentException.class, () -> PmemCheckpoint.commitStream(store, CHECKPOINT, 0, 0,
                new byte[257], out -> calls[0]++));
        expect(IllegalArgumentException.class, () -> PmemCheckpoint.commitStream(store, "invalid", 0, 0,
                new byte[64], out -> calls[0]++));
        check(calls[0] == 0 && store.committedCount() == 0, "invalid bounds never invoke encoder or append");
        PmemBlockStore small = PmemBlockStore.create(memory(3, 256), 0, 3, 256, SCHEMA);
        expect(IOException.class, () -> PmemCheckpoint.commitStream(small, CHECKPOINT, 0, 192,
                new byte[64], out -> calls[0]++));
        check(calls[0] == 0 && small.committedCount() == 0, "maximum complete reservation precedes callback");
        PmemCheckpoint.commitStream(store, CHECKPOINT, 0, 65, new byte[64], out -> out.write(new byte[65]));
        expect(IllegalArgumentException.class, () -> PmemCheckpoint.readStream(store, CHECKPOINT, 64,
                new byte[64], in -> ++calls[0]));
        expect(IllegalArgumentException.class, () -> PmemCheckpoint.readStream(store, CHECKPOINT, Long.MAX_VALUE,
                new byte[63], in -> ++calls[0]));
        expect(IllegalArgumentException.class, () -> PmemCheckpoint.readStream(store, CHECKPOINT, -1,
                new byte[64], in -> ++calls[0]));
        expect(IOException.class, () -> PmemCheckpoint.readStream(store, OTHER, 100,
                new byte[64], in -> ++calls[0]));
        check(calls[0] == 0, "reader limits, buffer geometry and missing checkpoint fail before decoding");
        check(!store.requiresReopen(), "argument refusals do not poison store");
        region.close();
        expect(IllegalStateException.class, () -> PmemCheckpoint.readStream(store, CHECKPOINT, 100,
                new byte[64], in -> ++calls[0]));
    }

    private static void writerFailures() throws Exception {
        for (int mode = 0; mode < 12; mode++) {
            PmemBlockStore store = initialized(memory(40, 1024));
            PmemCheckpoint.Info before = PmemCheckpoint.inspect(store, CHECKPOINT);
            byte[] transfer = new byte[64];
            int scenario = mode;
            expect(IOException.class, () -> PmemCheckpoint.commitStream(store, CHECKPOINT, 1, 192, transfer, out -> {
                switch (scenario) {
                    case 0 -> caught(IOException.class, () -> out.write(new byte[193]));
                    case 1 -> caught(NullPointerException.class, () -> out.write((byte[]) null));
                    case 2 -> caught(IndexOutOfBoundsException.class, () -> out.write(new byte[1], -1, 1));
                    case 3 -> caught(IllegalArgumentException.class, () -> out.write(transfer));
                    case 4 -> { out.close(); caught(IOException.class, () -> out.write(new byte[0])); }
                    case 5 -> { out.close(); caught(IOException.class, () -> out.write(1)); }
                    case 6 -> { out.close(); caught(IOException.class, out::flush); }
                    case 7 -> {
                        check(foreign(() -> out.write(1)) instanceof IOException, "foreign writer fails before borrowed-store lock");
                    }
                    case 8 -> { store.append(OTHER, new byte[] { 4 }, 0, 1); }
                    case 9 -> {
                        out.write(new byte[192]);
                        caught(IOException.class, () -> out.write(1));
                    }
                    case 10 -> caught(IndexOutOfBoundsException.class, () -> out.write(new byte[1], 0, Integer.MAX_VALUE));
                    case 11 -> caught(NullPointerException.class, () -> out.write(null, 0, 0));
                    default -> throw new AssertionError("unknown writer scenario");
                }
            }));
            check(PmemCheckpoint.inspect(store, CHECKPOINT).equals(before), "caught writer failure cannot publish scenario " + mode);
        }
        for (boolean error : new boolean[] { false, true }) {
            PmemBlockStore store = initialized(memory(40, 1024));
            Throwable primary = error ? new AssertionError("encoder error") : new IllegalStateException("encoder exception");
            Class<? extends Throwable> expectedType = error ? AssertionError.class : IllegalStateException.class;
            Throwable seen = expect(expectedType,
                    () -> PmemCheckpoint.commitStream(store, CHECKPOINT, 1, 192, new byte[64], out -> {
                        out.write(PAYLOAD);
                        if (primary instanceof Error e) throw e;
                        throw (RuntimeException) primary;
                    }));
            check(seen == primary && PmemCheckpoint.inspect(store, CHECKPOINT).generation() == 0, "callback primary failure preserved");
        }
        Memory memory = memory(40, 1024);
        PmemBlockStore store = initialized(memory);
        memory.failPersist(1, 0);
        expect(IOException.class, () -> PmemCheckpoint.commitStream(store, CHECKPOINT, 1, 192, new byte[64], out -> {
            caught(IOException.class, () -> out.write(PAYLOAD));
        }));
        check(store.requiresReopen(), "caught persistence error still poisons backing handle");
        memory.crash();
        check(PmemCheckpoint.inspect(PmemBlockStore.open(memory, 0, 40, 1024, SCHEMA), CHECKPOINT).generation() == 0,
                "caught persistence error leaves prior durable checkpoint");
    }

    private static void readerFailures() throws Exception {
        for (int mode = 0; mode < 18; mode++) {
            PmemBlockStore store = initialized(memory(40, 1024));
            byte[] transfer = new byte[64];
            int scenario = mode;
            expect(IOException.class, () -> PmemCheckpoint.readStream(store, CHECKPOINT, 192, transfer, in -> {
                switch (scenario) {
                    case 0 -> { in.read(); }
                    case 1 -> { in.close(); }
                    case 2 -> { in.skipNBytes(PAYLOAD.length); caught(NullPointerException.class, () -> in.read((byte[]) null)); }
                    case 3 -> { in.skipNBytes(PAYLOAD.length); caught(IndexOutOfBoundsException.class, () -> in.read(new byte[2], 1, 2)); }
                    case 4 -> { in.skipNBytes(PAYLOAD.length); caught(IllegalArgumentException.class, () -> in.read(transfer)); }
                    case 5 -> { in.skipNBytes(PAYLOAD.length); caught(IllegalArgumentException.class, () -> in.readNBytes(-1)); }
                    case 6 -> { in.skipNBytes(PAYLOAD.length); caught(NullPointerException.class, () -> in.readNBytes(null, 0, 0)); }
                    case 7 -> caught(EOFException.class, () -> in.skipNBytes(PAYLOAD.length + 1));
                    case 8 -> { in.skipNBytes(PAYLOAD.length); caught(IOException.class, in::reset); }
                    case 9 -> {
                        in.skipNBytes(PAYLOAD.length - 1);
                        caught(IOException.class, () -> in.transferTo(new OutputStream() {
                            @Override public void write(int value) throws IOException { throw new IOException("decoder sink failed"); }
                        }));
                    }
                    case 10 -> { in.skipNBytes(PAYLOAD.length); caught(NullPointerException.class, () -> in.transferTo(null)); }
                    case 11 -> { in.skipNBytes(PAYLOAD.length); in.close(); caught(IOException.class, () -> in.read(new byte[0])); }
                    case 12 -> {
                        in.skipNBytes(PAYLOAD.length);
                        check(foreign(in::read) instanceof IOException, "foreign reader fails before borrowed-store lock");
                    }
                    case 13 -> { in.skipNBytes(PAYLOAD.length); store.append(OTHER, new byte[] { 9 }, 0, 1); }
                    case 14 -> { in.skipNBytes(PAYLOAD.length); in.close(); caught(IOException.class, in::available); }
                    case 15 -> { in.skipNBytes(PAYLOAD.length); caught(IndexOutOfBoundsException.class, () -> in.readNBytes(new byte[1], -1, 0)); }
                    case 16 -> { in.skipNBytes(PAYLOAD.length); caught(IllegalArgumentException.class, () -> in.readNBytes(transfer, 0, 0)); }
                    case 17 -> { in.skipNBytes(PAYLOAD.length); in.close(); caught(IOException.class, () -> in.skipNBytes(0)); }
                    default -> throw new AssertionError("unknown decoder scenario");
                }
                return "must-not-escape";
            }));
            check(Arrays.equals(read(store), PAYLOAD), "failed decoding leaves stored checkpoint intact scenario " + mode);
        }
        PmemBlockStore store = initialized(memory(40, 1024));
        IOException primary = new IOException("decoder rejected payload");
        check(expect(IOException.class, () -> PmemCheckpoint.readStream(store, CHECKPOINT, 192, new byte[64], in -> {
            in.skipNBytes(PAYLOAD.length); throw primary;
        })) == primary, "decoder exception identity preserved");
    }

    private static void lifetimesAndOwnership() throws Exception {
        PmemBlockStore store = initialized(memory(40, 1024));
        OutputStream[] leakedOutput = new OutputStream[1];
        PmemCheckpoint.Info complete = PmemCheckpoint.commitStream(store, CHECKPOINT, 1, 192, new byte[64], out -> {
            leakedOutput[0] = out; out.write(PAYLOAD);
        });
        expect(IOException.class, () -> leakedOutput[0].write(3));
        expect(IOException.class, () -> leakedOutput[0].write(new byte[0]));
        expect(IOException.class, leakedOutput[0]::close);
        check(PmemCheckpoint.inspect(store, CHECKPOINT).equals(complete), "expired writer has no later side effects");
        InputStream[] leakedInput = new InputStream[1];
        PmemCheckpoint.readStream(store, CHECKPOINT, 192, new byte[64], in -> {
            leakedInput[0] = in;
            in.skipNBytes(PAYLOAD.length);
            return null;
        });
        expect(IOException.class, leakedInput[0]::read);
        expect(IOException.class, () -> leakedInput[0].readNBytes(0));
        expect(IOException.class, () -> leakedInput[0].skipNBytes(0));
        expect(IOException.class, leakedInput[0]::available);
        expect(IOException.class, leakedInput[0]::close);
        check(Arrays.equals(read(store), PAYLOAD), "expired decoder cannot affect later successful decoding");
    }

    private static void corruption() throws Exception {
        Memory before = memory(40, 1024);
        PmemBlockStore corrupted = initialized(before);
        before.corrupt(slotPayload(40, 1024, 0));
        int[] callbacks = { 0 };
        expect(IOException.class, () -> PmemCheckpoint.readStream(corrupted, CHECKPOINT, 192, new byte[64], in -> ++callbacks[0]));
        check(callbacks[0] == 0 && corrupted.requiresReopen(), "corrupt committed payload refuses before decoder");
        Memory during = memory(40, 1024);
        PmemBlockStore store = initialized(during);
        expect(IOException.class, () -> PmemCheckpoint.readStream(store, CHECKPOINT, 192, new byte[64], in -> {
            check(in.readNBytes(64).length == 64, "first chunk decoded before injected later corruption");
            during.corrupt(slotPayload(40, 1024, 1));
            caught(IOException.class, in::read);
            return "invalid";
        }));
        check(store.requiresReopen(), "stream reread detects corruption after preflight");
        Memory closed = memory(40, 1024);
        PmemBlockStore closingStore = initialized(closed);
        expect(IllegalStateException.class, () -> PmemCheckpoint.readStream(closingStore, CHECKPOINT, 192, new byte[64], in -> {
            in.skipNBytes(PAYLOAD.length);
            closed.close();
            return "closed-store";
        }));
    }

    private static void crashBoundaries() throws Exception {
        byte[] next = pattern(10, 19);
        for (int stage = 1; stage <= 12; stage++) {
            for (boolean after : new boolean[] { false, true }) {
                Memory memory = memory(24, 256);
                PmemBlockStore store = PmemBlockStore.create(memory, 0, 24, 256, SCHEMA);
                PmemCheckpoint.commit(store, CHECKPOINT, 0, 1, new byte[4], (index, bytes) -> { bytes[0] = 7; return 1; });
                memory.failPersist(stage, after ? Integer.MAX_VALUE : 0);
                expect(IOException.class, () -> PmemCheckpoint.commitStream(store, CHECKPOINT, 1, 12, new byte[4], out -> out.write(next)));
                persistenceBoundaries++;
                check(store.requiresReopen(), "uncertain append requires reopen");
                memory.crash();
                PmemBlockStore reopened = PmemBlockStore.open(memory, 0, 24, 256, SCHEMA);
                boolean published = stage == 12 && after;
                check(PmemCheckpoint.inspect(reopened, CHECKPOINT).generation() == (published ? 1 : 0), "manifest is sole durable publication point");
                byte[] restored = PmemCheckpoint.readStream(reopened, CHECKPOINT, 12, new byte[4], InputStream::readAllBytes);
                check(Arrays.equals(restored, published ? next : new byte[] { 7 }), "recovered bytes match complete selected generation");
            }
        }
    }

    private static void heapStream() throws Exception {
        final int payloadBytes = 32 * 1024 * 1024, chunkBytes = 256 * 1024, capacity = 129;
        check(Runtime.getRuntime().maxMemory() < payloadBytes, "heap probe requires heap below whole payload");
        Path file = Files.createTempFile("m3-checkpoint-bounded-heap-", ".bin");
        byte[] source = new byte[chunkBytes], transfer = new byte[chunkBytes];
        MessageDigest expected = MessageDigest.getInstance("SHA-256");
        long size = PmemBlockStore.requiredBytes(capacity, chunkBytes);
        try {
            size(file, size);
            try (PmemRegion region = PmemSdk.open(file, 0, size, PmemAccess.READ_WRITE, PmemBackend.FILE)) {
                PmemBlockStore store = PmemBlockStore.create(region, 0, capacity, chunkBytes, SCHEMA);
                PmemCheckpoint.Info info = PmemCheckpoint.commitStream(store, CHECKPOINT, 0, payloadBytes, transfer, out -> {
                    for (int chunk = 0; chunk < payloadBytes / chunkBytes; chunk++) {
                        for (int i = 0; i < source.length; i++) source[i] = (byte) (i * 31 + chunk * 17);
                        expected.update(source);
                        out.write(source);
                    }
                });
                check(info.totalBytes() == payloadBytes && info.chunks() == 128, "large stream has exact bounded geometry");
            }
            byte[] actual;
            try (PmemRegion region = PmemSdk.open(file, 0, size, PmemAccess.READ_WRITE, PmemBackend.FILE)) {
                PmemBlockStore store = PmemBlockStore.open(region, 0, capacity, chunkBytes, SCHEMA);
                actual = PmemCheckpoint.readStream(store, CHECKPOINT, payloadBytes, transfer, in -> {
                    MessageDigest digest;
                    try { digest = MessageDigest.getInstance("SHA-256"); }
                    catch (java.security.NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
                    byte[] bytes = new byte[16384];
                    long total = 0;
                    for (int count; (count = in.read(bytes)) != -1;) { digest.update(bytes, 0, count); total += count; }
                    check(total == payloadBytes, "large decoder consumes exact bytes");
                    return digest.digest();
                });
            }
            check(MessageDigest.isEqual(expected.digest(), actual), "large reopened stream has independent incremental digest");
            System.out.println("PMEM_CHECKPOINT_HEAP_STREAM_PASS payloadBytes=" + payloadBytes
                    + " maxHeapBytes=" + Runtime.getRuntime().maxMemory() + " transferBytes=" + chunkBytes
                    + " sha256=" + HexFormat.of().formatHex(actual));
        } finally { Files.deleteIfExists(file); }
    }

    private static PmemBlockStore initialized(Memory memory) throws IOException {
        PmemBlockStore store = PmemBlockStore.create(memory, 0, 40, 1024, SCHEMA);
        PmemCheckpoint.commit(store, CHECKPOINT, 0, 3, new byte[64], (index, transfer) -> {
            int length = Math.min(64, PAYLOAD.length - index * 64);
            System.arraycopy(PAYLOAD, index * 64, transfer, 0, length);
            return length;
        });
        return store;
    }

    private static byte[] read(PmemBlockStore store) throws IOException {
        return PmemCheckpoint.readStream(store, CHECKPOINT, 4096, new byte[64], InputStream::readAllBytes);
    }

    private static long slotPayload(int capacity, int maximum, int ordinal) {
        return (capacity + 1L) * PmemTaskLog.RECORD_BYTES + ordinal * (PmemBlockStore.HEADER_BYTES + (long) maximum)
                + PmemBlockStore.HEADER_BYTES;
    }

    private static Memory memory(int capacity, int maximum) {
        return new Memory(Math.toIntExact(PmemBlockStore.requiredBytes(capacity, maximum)));
    }

    private static byte[] pattern(int size, int salt) {
        byte[] bytes = new byte[size];
        for (int i = 0; i < size; i++) bytes[i] = (byte) (i * 37 + salt);
        return bytes;
    }

    private static void size(Path file, long bytes) throws IOException {
        try (FileChannel channel = FileChannel.open(file, StandardOpenOption.WRITE)) {
            channel.position(bytes - 1);
            channel.write(ByteBuffer.wrap(new byte[] { 0 }));
        }
    }

    @FunctionalInterface private interface Action { void run() throws Exception; }

    private static Throwable foreign(Action action) throws IOException {
        FutureTask<Throwable> work = new FutureTask<>(() -> {
            try { action.run(); return null; }
            catch (Throwable failure) { return failure; }
        });
        Thread thread = new Thread(work, "checkpoint-stream-foreign-owner");
        thread.setDaemon(true);
        thread.start();
        try { return work.get(2, TimeUnit.SECONDS); }
        catch (Exception failure) { throw new IOException("foreign stream operation blocked or failed unexpectedly", failure); }
    }

    private static void caught(Class<? extends Throwable> type, Action action) throws IOException {
        try { expect(type, action); }
        catch (IOException | RuntimeException | Error failure) { throw failure; }
        catch (Exception failure) { throw new IOException(failure); }
    }

    private static <T extends Throwable> T expect(Class<T> type, Action action) throws Exception {
        checks++;
        try { action.run(); }
        catch (Throwable failure) {
            if (type.isInstance(failure)) return type.cast(failure);
            throw new AssertionError("expected " + type.getName() + ", got " + failure, failure);
        }
        throw new AssertionError("expected " + type.getName());
    }

    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }

    private static final class RecordingTarget implements PmemCheckpoint.RestoreTarget {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        int commits, aborts;
        @Override public void begin(long generation, int chunks, long totalBytes) { }
        @Override public void writeChunk(int index, byte[] source, int length) { bytes.write(source, 0, length); }
        @Override public void commit() { commits++; }
        @Override public void abort() { aborts++; bytes.reset(); }
    }

    /** Volatile versus durable bytes; this is a persistence model, not physical power-loss evidence. */
    private static final class Memory implements PmemRegion {
        private final byte[] visible, durable;
        private boolean closed;
        private int persists, failAt = -1, copyBeforeFailure;
        Memory(int length) { visible = new byte[length]; durable = new byte[length]; }
        void failPersist(int relative, int copied) { failAt = persists + relative; copyBeforeFailure = copied; }
        void crash() { System.arraycopy(durable, 0, visible, 0, visible.length); failAt = -1; }
        void corrupt(long offset) { visible[index(offset, 1)] ^= 1; durable[index(offset, 1)] ^= 1; }
        @Override public long length() { return visible.length; }
        @Override public long fileOffset() { return 0; }
        @Override public Path path() { return Path.of("simulated-checkpoint-stream"); }
        @Override public PmemAccess access() { return PmemAccess.READ_WRITE; }
        @Override public PmemBackend backend() { return PmemBackend.FILE; }
        @Override public PmemGranularity persistenceGranularity() { return PmemGranularity.PAGE; }
        @Override public boolean isOpen() { return !closed; }
        @Override public void close() { closed = true; }
        @Override public void read(long offset, byte[] target, int start, int length) {
            System.arraycopy(visible, index(offset, length), target, start, length);
        }
        @Override public void write(long offset, byte[] source, int start, int length) {
            System.arraycopy(source, start, visible, index(offset, length), length);
        }
        @Override public void persist(long offset, long length) throws IOException {
            int count = Math.toIntExact(length), start = index(offset, count);
            if (++persists == failAt) {
                System.arraycopy(visible, start, durable, start, Math.min(count, copyBeforeFailure));
                throw new IOException("injected persistence boundary " + persists);
            }
            System.arraycopy(visible, start, durable, start, count);
        }
        private int index(long offset, int length) {
            if (closed) throw new IllegalStateException("simulation closed");
            if (offset < 0 || length < 0 || offset > visible.length - length) throw new IndexOutOfBoundsException();
            return Math.toIntExact(offset);
        }
    }
}
