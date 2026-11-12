// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
/*
 * @test id=file
 * @summary Durable fixed-slot blocks, exact prefix identities and complete checkpoint refusal contracts
 * @modules jdk.m3.io/jdk.m3.optane
 * @run main/othervm -Xmx96m test.PmemDurableBlocksSmokeTest
 */
/*
 * @test id=posix
 * @summary Durable-block contracts through the actual POSIX JNI owner and injected failure model
 * @requires (os.family == "linux")
 * @modules jdk.m3.io/jdk.m3.optane
 * @run main/othervm -Xcheck:jni -Xmx96m test.PmemDurableBlocksSmokeTest --require-posix
 */
package test;

import jdk.m3.optane.PmemAccess;
import jdk.m3.optane.PmemBackend;
import jdk.m3.optane.PmemBlockStore;
import jdk.m3.optane.PmemCheckpoint;
import jdk.m3.optane.PmemGranularity;
import jdk.m3.optane.PmemPrefixCache;
import jdk.m3.optane.PmemPrefixKey;
import jdk.m3.optane.PmemRegion;
import jdk.m3.optane.PmemSdk;
import jdk.m3.optane.PmemTaskLog;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;

/**
 * Bounded independent durability/order/refusal oracle. FILE always runs; pass
 * --require-posix to demand the original native POSIX backend as an additional
 * gate. The volatile/durable fault model is a simulation, not a power-loss test.
 */
public final class PmemDurableBlocksSmokeTest {
    private static final String SCHEMA = "a".repeat(64);
    private static final String KEY = "b".repeat(64);
    private static final String OTHER = "c".repeat(64);
    private static final String CHECKPOINT = "d".repeat(64);
    private static final byte[][] CHUNKS = { { 2, 3, 5 }, {}, { 7, 11, 13, 17, 19 } };
    private static int assertions;
    private static int simulatedFailures;

    private PmemDurableBlocksSmokeTest() { }

    public static void main(String[] args) throws Exception {
        boolean posix = args.length == 1 && "--require-posix".equals(args[0]);
        if (args.length > 1 || args.length == 1 && !posix) throw new IllegalArgumentException("unknown test option");
        realBackend(PmemBackend.FILE);
        if (posix) {
            check(PmemSdk.capabilities().nativeAvailable(), "POSIX JNI library must be available");
            realBackend(PmemBackend.POSIX);
        }
        prefixIdentities();
        blockFaults();
        configurationFaults();
        checkpointFaults();
        checkpointRefusals();
        receiverFailures();
        boundsAndOwnership();
        System.out.println("PMEM_DURABLE_BLOCKS_PASS file=true posix=" + posix
                + " simulatedFailures=" + simulatedFailures + " assertions=" + assertions);
    }

    private static void realBackend(PmemBackend backend) throws Exception {
        int capacity = 40, maximum = 512;
        long origin = 17, fileOffset = 4099;
        long length = origin + PmemBlockStore.requiredBytes(capacity, maximum) + 13;
        Path file = Files.createTempFile("m3-durable-blocks-", ".bin");
        try {
            try (FileChannel channel = FileChannel.open(file, StandardOpenOption.WRITE)) {
                channel.position(fileOffset + length - 1);
                channel.write(ByteBuffer.wrap(new byte[] { 0 }));
            }
            PmemPrefixKey prefix = key(new int[] { 1, 2 });
            PmemPrefixKey otherPrefix = key(new int[] { 1, 3 });
            byte[] target = new byte[maximum];
            long checkpointOrdinal;
            try (PmemRegion region = PmemSdk.open(file, fileOffset, length, PmemAccess.READ_WRITE, backend)) {
                check(region.backend() == backend, "requested backend actually used");
                PmemBlockStore store = PmemBlockStore.create(region, origin, capacity, maximum, SCHEMA);
                check(store.append(KEY, new byte[] { 8, 1, 2, 3, 9 }, 1, 3) == 0, "first block ordinal");
                check(store.append(OTHER, new byte[0], 0, 0) == 1, "empty payload committed");
                check(store.read(1, new byte[0], 0) == 0, "empty payload read");
                check(store.append(KEY, new byte[] { 4, 5 }, 0, 2) == 2, "replacement is append-only");
                check(store.findLatest(KEY) == 2, "latest committed key");
                check(store.read(0, target, 1) == 3 && target[1] == 1 && target[3] == 3, "caller-owned slice read");
                check(store.metadata(0).payloadSha256().equals(hash(new byte[] { 1, 2, 3 })), "independent payload digest");
                expect(IOException.class, () -> PmemBlockStore.create(region, origin, capacity, maximum, SCHEMA));
                expect(IOException.class, () -> PmemBlockStore.open(region, origin, capacity, maximum, OTHER));
                PmemPrefixCache cache = new PmemPrefixCache(store, 1);
                check(cache.indexBytes() == 40, "bounded primitive index size");
                cache.put(prefix, new byte[] { 21 }, 0, 1);
                cache.put(otherPrefix, new byte[] { 22, 23 }, 0, 2);
                check(cache.get(prefix, target, 0) == 1 && target[0] == 21, "collision falls back to complete key");
                check(cache.get(otherPrefix, target, 0) == 2 && target[1] == 23, "colliding other key intact");
                store.append(prefix.sha256(), new byte[] { 24, 25, 26 }, 0, 3);
                check(cache.get(prefix, target, 0) == 3 && target[2] == 26, "direct store append invalidates hints");
                cache.releaseIndex();
                check(cache.indexBytes() == 0 && cache.payloadLength(otherPrefix) == 2, "pressure release preserves lookup");
                check(cache.get(key(new int[] { 9 }), target, 0) == -1, "prefix miss");
                PmemCheckpoint.Info info = checkpoint(store, 0);
                checkpointOrdinal = info.manifestOrdinal();
                check(info.chunks() == 3 && info.totalBytes() == 8, "complete manifest totals");
                RecordingTarget restored = new RecordingTarget();
                check(PmemCheckpoint.restore(store, CHECKPOINT, target, restored).equals(info), "restore complete identity");
                restored.assertPublished(CHUNKS);
                int[] sourceCalls = { 0 };
                expect(IllegalArgumentException.class, () -> PmemCheckpoint.commit(store, CHECKPOINT, 0, 1,
                        target, (index, buffer) -> { sourceCalls[0]++; return 0; }));
                check(sourceCalls[0] == 0, "stale generation refuses before source");
                expect(IOException.class, () -> PmemCheckpoint.commit(store, CHECKPOINT, 1, 2, target,
                        (index, buffer) -> { if (index == 1) throw new IOException("source unavailable"); buffer[0] = 99; return 1; }));
                check(PmemCheckpoint.inspect(store, CHECKPOINT).equals(info), "source failure preserves prior manifest");
            }
            try (PmemRegion region = PmemSdk.open(file, fileOffset, length, PmemAccess.READ_WRITE, backend)) {
                PmemBlockStore store = PmemBlockStore.open(region, origin, capacity, maximum, SCHEMA);
                check(!store.recoveredIncompleteTail(), "unreferenced committed chunk is not a torn tail");
                PmemPrefixCache cache = new PmemPrefixCache(store, 0);
                check(cache.get(prefix, target, 0) == 3 && target[2] == 26, "prefix survives real close and reopen");
                RecordingTarget restored = new RecordingTarget();
                check(PmemCheckpoint.restore(store, CHECKPOINT, target, restored).manifestOrdinal() == checkpointOrdinal,
                        "checkpoint survives close and reopen");
                restored.assertPublished(CHUNKS);
                checkpoint(store, 2);
                check(PmemCheckpoint.inspect(store, CHECKPOINT).generation() == 2, "newer complete generation selected");
                long payloadAt = origin + (capacity + 1L) * PmemTaskLog.RECORD_BYTES + PmemBlockStore.HEADER_BYTES;
                region.write(payloadAt, new byte[] { 42 }, 0, 1);
                region.persist(payloadAt, 1);
                Arrays.fill(target, (byte) 77);
                expect(IOException.class, () -> store.read(0, target, 0));
                check(target[0] == 77 && store.requiresReopen(), "known corruption refuses before output copy");
                expect(IOException.class, () -> PmemBlockStore.open(region, origin, capacity, maximum, SCHEMA));
            }
        } finally { Files.deleteIfExists(file); }
    }

    private static void prefixIdentities() throws Exception {
        PmemPrefixKey.Context context = context("ns", SCHEMA, KEY, "q8:scale-v1", "kv:v1:d4:head2", OTHER);
        PmemPrefixKey original = PmemPrefixKey.of(context, new int[] { 1, 2 }, new long[] { 4, 5 });
        check(original.equals(PmemPrefixKey.contiguous(context, new int[] { 0, 1, 2, 0 }, 1, 2, 4)), "equivalent prefix encodings");
        check(original.hashCode() == PmemPrefixKey.of(context, new int[] { 1, 2 }, new long[] { 4, 5 }).hashCode(), "stable key hashCode");
        List<PmemPrefixKey.Context> changes = List.of(
                context("ns2", SCHEMA, KEY, "q8:scale-v1", "kv:v1:d4:head2", OTHER),
                context("ns", OTHER, KEY, "q8:scale-v1", "kv:v1:d4:head2", OTHER),
                context("ns", SCHEMA, OTHER, "q8:scale-v1", "kv:v1:d4:head2", OTHER),
                context("ns", SCHEMA, KEY, "q4:scale-v1", "kv:v1:d4:head2", OTHER),
                context("ns", SCHEMA, KEY, "q8:scale-v1", "kv:v2:d4:head2", OTHER),
                context("ns", SCHEMA, KEY, "q8:scale-v1", "kv:v1:d4:head2", SCHEMA));
        for (PmemPrefixKey.Context changed : changes)
            check(!original.equals(PmemPrefixKey.of(changed, new int[] { 1, 2 }, new long[] { 4, 5 })), "every context field binds identity");
        check(!original.equals(PmemPrefixKey.of(context, new int[] { 2, 1 }, new long[] { 4, 5 })), "token order binds identity");
        check(!original.equals(PmemPrefixKey.of(context, new int[] { 1, 2 }, new long[] { 4, 6 })), "exact positions bind identity");
        check(!original.equals(PmemPrefixKey.of(context, new int[] { 1 }, new long[] { 4 })), "token count binds identity");
        int[] borrowed = { 1, 2 };
        PmemPrefixKey detached = PmemPrefixKey.of(context, borrowed, new long[] { 4, 5 });
        borrowed[0] = 777;
        check(detached.equals(original), "key does not retain token arrays");
        expect(IllegalArgumentException.class, () -> context("\ud800", SCHEMA, KEY, "q8", "v1", OTHER));
        expect(IllegalArgumentException.class, () -> context("ns", "A".repeat(64), KEY, "q8", "v1", OTHER));
        expect(IllegalArgumentException.class, () -> context("ns", SCHEMA, KEY, "q8", "é".repeat(3000), OTHER));
        expect(IllegalArgumentException.class, () -> PmemPrefixKey.of(context, new int[] { -1 }, new long[] { 0 }));
        expect(IllegalArgumentException.class, () -> PmemPrefixKey.of(context, new int[] { 1 }, new long[] { -1 }));
        expect(IllegalArgumentException.class, () -> PmemPrefixKey.of(context, new int[] { 1 }, new long[0]));
        expect(IllegalArgumentException.class, () -> PmemPrefixKey.contiguous(context, new int[] { 1, 2 }, 0, 2, Long.MAX_VALUE));
        expect(IndexOutOfBoundsException.class, () -> PmemPrefixKey.contiguous(context, new int[1], 1, 1, 0));
    }

    private static void blockFaults() throws Exception {
        for (int stage = 1; stage <= 3; stage++) {
            for (boolean after : new boolean[] { false, true }) {
                SimulatedRegion memory = memory(4, 128);
                PmemBlockStore store = PmemBlockStore.create(memory, 0, 4, 128, SCHEMA);
                store.append(KEY, new byte[] { 1 }, 0, 1);
                memory.failPersist(stage, after ? Integer.MAX_VALUE : 0);
                expect(IOException.class, () -> store.append(KEY, new byte[] { 2, 3 }, 0, 2));
                simulatedFailures++;
                check(store.requiresReopen(), "uncertain persist poisons handle");
                expect(IllegalStateException.class, () -> store.append(KEY, new byte[] { 4 }, 0, 1));
                memory.crash();
                PmemBlockStore reopened = PmemBlockStore.open(memory, 0, 4, 128, SCHEMA);
                boolean committed = stage == 3 && after;
                check(reopened.committedCount() == (committed ? 2 : 1), "only durable marker promotes payload");
                byte[] bytes = new byte[4];
                reopened.read(reopened.findLatest(KEY), bytes, 0);
                check(bytes[0] == (committed ? 2 : 1), "last complete payload selected after crash");
                if (!committed) {
                    check(reopened.recoveredIncompleteTail() == (stage > 1 || after), "recoverable tail evidence");
                    check(reopened.append(KEY, new byte[] { 9 }, 0, 1) == 1, "incomplete slot overwritten");
                    check(reopened.read(1, bytes, 0) == 1 && bytes[0] == 9, "shorter replacement excludes stale tail bytes");
                }
            }
        }
        for (int stage : new int[] { 1, 2 }) {
            SimulatedRegion memory = memory(3, 128);
            PmemBlockStore store = PmemBlockStore.create(memory, 0, 3, 128, SCHEMA);
            memory.failPersist(stage, 7);
            expect(IOException.class, () -> store.append(KEY, new byte[] { 3 }, 0, 1));
            simulatedFailures++;
            memory.crash();
            PmemBlockStore reopened = PmemBlockStore.open(memory, 0, 3, 128, SCHEMA);
            check(reopened.committedCount() == 0 && reopened.recoveredIncompleteTail(), "torn uncommitted body recovered");
            reopened.append(KEY, new byte[] { 8 }, 0, 1);
        }
        SimulatedRegion torn = memory(3, 128);
        PmemBlockStore tornStore = PmemBlockStore.create(torn, 0, 3, 128, SCHEMA);
        torn.failPersist(3, 4);
        expect(IOException.class, () -> tornStore.append(KEY, new byte[] { 3 }, 0, 1));
        simulatedFailures++;
        torn.crash();
        expect(IOException.class, () -> PmemBlockStore.open(torn, 0, 3, 128, SCHEMA));
        for (int stage = 1; stage <= 4; stage++) {
            SimulatedRegion memory = memory(3, 128);
            PmemBlockStore store = PmemBlockStore.create(memory, 0, 3, 128, SCHEMA);
            memory.failWrite(stage);
            expect(IllegalStateException.class, () -> store.append(KEY, new byte[] { 3 }, 0, 1));
            simulatedFailures++;
            check(store.requiresReopen(), "uncertain write poisons handle");
            memory.crash();
            check(PmemBlockStore.open(memory, 0, 3, 128, SCHEMA).committedCount() == 0, "write failure cannot publish");
        }
        SimulatedRegion ordered = memory(3, 128);
        PmemBlockStore store = PmemBlockStore.create(ordered, 0, 3, 128, SCHEMA);
        ordered.persistTrace.clear();
        store.append(KEY, new byte[] { 2, 4 }, 0, 2);
        check(ordered.persistTrace.equals(List.of("576:130", "144:144", "280:8")), "payload/body/marker persist order");
        SimulatedRegion visibleOnly = memory(3, 128);
        PmemBlockStore uncertain = PmemBlockStore.create(visibleOnly, 0, 3, 128, SCHEMA);
        visibleOnly.failPersist(3, 0);
        expect(IOException.class, () -> uncertain.append(KEY, new byte[] { 6 }, 0, 1));
        simulatedFailures++;
        check(PmemBlockStore.open(visibleOnly, 0, 3, 128, SCHEMA).committedCount() == 1,
                "reopen can recover a visible valid marker without a prior crash");
        visibleOnly.crash();
        check(PmemBlockStore.open(visibleOnly, 0, 3, 128, SCHEMA).committedCount() == 1,
                "successful reopen re-persisted visible payload and marker before returning");
        SimulatedRegion recoveryFailure = memory(3, 128);
        PmemBlockStore beforeRecovery = PmemBlockStore.create(recoveryFailure, 0, 3, 128, SCHEMA);
        beforeRecovery.append(KEY, new byte[] { 6 }, 0, 1);
        recoveryFailure.failPersist(1, 0);
        expect(IOException.class, () -> PmemBlockStore.open(recoveryFailure, 0, 3, 128, SCHEMA));
        simulatedFailures++;
        check(PmemBlockStore.open(recoveryFailure, 0, 3, 128, SCHEMA).committedCount() == 1,
                "recovery barrier failure refuses open and can be retried");
    }

    private static void configurationFaults() throws Exception {
        for (int stage = 1; stage <= 2; stage++) {
            for (boolean after : new boolean[] { false, true }) {
                SimulatedRegion memory = memory(3, 128);
                memory.failPersist(stage, after ? Integer.MAX_VALUE : 0);
                expect(IOException.class, () -> PmemBlockStore.create(memory, 0, 3, 128, SCHEMA));
                simulatedFailures++;
                memory.crash();
                if (stage == 2 && after) {
                    check(PmemBlockStore.open(memory, 0, 3, 128, SCHEMA).committedCount() == 0, "uncertain creation may have committed");
                } else {
                    expect(IOException.class, () -> PmemBlockStore.open(memory, 0, 3, 128, SCHEMA));
                    check(PmemBlockStore.create(memory, 0, 3, 128, SCHEMA).committedCount() == 0, "uncommitted configuration can be retried");
                }
            }
        }
    }

    private static void checkpointFaults() throws Exception {
        for (int stage = 1; stage <= 9; stage++) {
            for (boolean after : new boolean[] { false, true }) {
                SimulatedRegion memory = memory(20, 256);
                PmemBlockStore store = PmemBlockStore.create(memory, 0, 20, 256, SCHEMA);
                PmemCheckpoint.commit(store, CHECKPOINT, 1, 2, new byte[8], PmemDurableBlocksSmokeTest::twoChunks);
                memory.failPersist(stage, after ? Integer.MAX_VALUE : 0);
                expect(IOException.class, () -> PmemCheckpoint.commit(store, CHECKPOINT, 2, 2, new byte[8],
                        PmemDurableBlocksSmokeTest::twoChunks));
                simulatedFailures++;
                check(store.requiresReopen(), "checkpoint uncertain append requires reopen");
                memory.crash();
                PmemBlockStore reopened = PmemBlockStore.open(memory, 0, 20, 256, SCHEMA);
                PmemCheckpoint.Info info = PmemCheckpoint.inspect(reopened, CHECKPOINT);
                check(info.generation() == (stage == 9 && after ? 2 : 1), "checkpoint manifest is sole publication point");
                RecordingTarget receiver = new RecordingTarget();
                PmemCheckpoint.restore(reopened, CHECKPOINT, new byte[8], receiver);
                receiver.assertPublished(new byte[][] { { 1, 2 }, { 2, 3 } });
            }
        }
    }

    private static void checkpointRefusals() throws Exception {
        for (int mutation = 0; mutation < 11; mutation++) {
            SimulatedRegion memory = memory(20, 256);
            PmemBlockStore store = PmemBlockStore.create(memory, 0, 20, 256, SCHEMA);
            PmemCheckpoint.Info info = PmemCheckpoint.commit(store, CHECKPOINT, 1, 2, new byte[8],
                    PmemDurableBlocksSmokeTest::twoChunks);
            PmemBlockStore.Metadata metadata = store.metadata(info.manifestOrdinal());
            byte[] manifest = new byte[metadata.length()];
            store.read(info.manifestOrdinal(), manifest, 0);
            ByteBuffer fields = ByteBuffer.wrap(manifest);
            int length = manifest.length;
            switch (mutation) {
                case 0 -> fields.putLong(64 + 48, fields.getLong(64));
                case 1 -> fields.putInt(64 + 8, 99);
                case 2 -> manifest[64 + 16] ^= 1;
                case 3 -> length--;
                case 4 -> manifest[32] ^= 1;
                case 5 -> fields.putInt(8, 2);
                case 6 -> fields.putInt(64 + 12, 1);
                case 7 -> fields.putLong(24, 999);
                case 8 -> fields.putLong(64, info.manifestOrdinal() + 1);
                case 9 -> fields.putInt(12, Integer.MAX_VALUE);
                case 10 -> fields.putLong(16, 2);
                default -> throw new AssertionError("unknown mutation");
            }
            store.append(metadata.keySha256(), manifest, 0, length);
            RecordingTarget receiver = new RecordingTarget();
            expect(IOException.class, () -> PmemCheckpoint.restore(store, CHECKPOINT, new byte[256], receiver));
            check(receiver.beginCalls == 0 && receiver.commitCalls == 0, "malformed manifest never begins restoration");
        }
        SimulatedRegion memory = memory(20, 256);
        PmemBlockStore store = PmemBlockStore.create(memory, 0, 20, 256, SCHEMA);
        PmemCheckpoint.commit(store, CHECKPOINT, 1, 2, new byte[8], PmemDurableBlocksSmokeTest::twoChunks);
        RecordingTarget tooSmall = new RecordingTarget();
        expect(IllegalArgumentException.class, () -> PmemCheckpoint.restore(store, CHECKPOINT, new byte[1], tooSmall));
        check(tooSmall.beginCalls == 0, "small buffer refuses before receiver");
        long firstPayload = 21L * PmemTaskLog.RECORD_BYTES + PmemBlockStore.HEADER_BYTES;
        memory.corrupt(firstPayload);
        RecordingTarget receiver = new RecordingTarget();
        expect(IOException.class, () -> PmemCheckpoint.restore(store, CHECKPOINT, new byte[8], receiver));
        check(receiver.beginCalls == 0, "corrupt committed chunk never begins restore");
    }

    private static void receiverFailures() throws Exception {
        for (int failureAt = 0; failureAt < 4; failureAt++) {
            SimulatedRegion memory = memory(20, 256);
            PmemBlockStore store = PmemBlockStore.create(memory, 0, 20, 256, SCHEMA);
            PmemCheckpoint.commit(store, CHECKPOINT, 1, 2, new byte[8], PmemDurableBlocksSmokeTest::twoChunks);
            RecordingTarget receiver = new RecordingTarget();
            receiver.failureAt = failureAt;
            expect(IOException.class, () -> PmemCheckpoint.restore(store, CHECKPOINT, new byte[8], receiver));
            check(receiver.abortCalls == 1 && receiver.published == null && receiver.staged.isEmpty(), "receiver failure aborts staged restore");
        }
        SimulatedRegion memory = memory(20, 256);
        PmemBlockStore store = PmemBlockStore.create(memory, 0, 20, 256, SCHEMA);
        PmemCheckpoint.commit(store, CHECKPOINT, 1, 2, new byte[8], PmemDurableBlocksSmokeTest::twoChunks);
        RecordingTarget receiver = new RecordingTarget();
        receiver.onBegin = () -> memory.failNextRead = true;
        expect(IllegalStateException.class, () -> PmemCheckpoint.restore(store, CHECKPOINT, new byte[8], receiver));
        check(receiver.abortCalls == 1 && receiver.commitCalls == 0, "read failure after preflight aborts restore");
        RecordingTarget abortFailure = new RecordingTarget();
        abortFailure.failureAt = 1;
        abortFailure.failAbort = true;
        IOException primary = expect(IOException.class, () -> PmemCheckpoint.restore(store, CHECKPOINT, new byte[8], abortFailure));
        check(primary.getSuppressed().length == 1, "abort failure preserves original failure");
    }

    private static void boundsAndOwnership() throws Exception {
        expect(IllegalArgumentException.class, () -> PmemBlockStore.requiredBytes(Long.MAX_VALUE, 1));
        expect(IllegalArgumentException.class, () -> PmemBlockStore.requiredBytes(1, 0));
        expect(IllegalArgumentException.class, () -> PmemBlockStore.requiredBytes(0, 1));
        SimulatedRegion memory = memory(1, 256);
        expect(IndexOutOfBoundsException.class, () -> PmemBlockStore.create(memory, 1, 1, 256, SCHEMA));
        memory.readOnly = true;
        expect(IllegalArgumentException.class, () -> PmemBlockStore.create(memory, 0, 1, 256, SCHEMA));
        memory.readOnly = false;
        PmemBlockStore store = PmemBlockStore.create(memory, 0, 1, 256, SCHEMA);
        expect(IllegalArgumentException.class, () -> store.append("bad", new byte[1], 0, 1));
        expect(IndexOutOfBoundsException.class, () -> store.append(KEY, new byte[1], 1, 1));
        expect(IllegalArgumentException.class, () -> store.append(KEY, new byte[257], 0, 257));
        expect(IndexOutOfBoundsException.class, () -> store.metadata(0));
        expect(IllegalArgumentException.class, () -> new PmemPrefixCache(store, PmemPrefixCache.MAX_INDEX_ENTRIES + 1));
        check(!store.requiresReopen() && store.committedCount() == 0, "invalid arguments have no append effect");
        int[] reads = { 0 };
        expect(IOException.class, () -> PmemCheckpoint.commit(store, CHECKPOINT, 1, 1, new byte[8],
                (index, bytes) -> { reads[0]++; return 1; }));
        check(reads[0] == 0, "whole checkpoint capacity checked before source");
        store.append(KEY, new byte[] { 9 }, 0, 1);
        expect(IOException.class, () -> store.append(KEY, new byte[] { 8 }, 0, 1));
        check(!store.requiresReopen(), "capacity refusal does not poison valid store");
        expect(IndexOutOfBoundsException.class, () -> store.read(0, new byte[0], 0));
        memory.close();
        expect(IllegalStateException.class, () -> store.read(0, new byte[1], 0));
        expect(IllegalStateException.class, () -> PmemBlockStore.open(memory, 0, 1, 256, SCHEMA));
        long high = 3L * 1024 * 1024 * 1024;
        SimulatedRegion largeOffset = new SimulatedRegion(high, Math.toIntExact(PmemBlockStore.requiredBytes(3, 128)));
        PmemBlockStore highStore = PmemBlockStore.create(largeOffset, high, 3, 128, SCHEMA);
        highStore.append(KEY, new byte[] { 7 }, 0, 1);
        largeOffset.crash();
        check(PmemBlockStore.open(largeOffset, high, 3, 128, SCHEMA).findLatest(KEY) == 0, "long-offset reservation above 2GiB");
        expect(IOException.class, () -> PmemBlockStore.open(largeOffset, high, 3, 127, SCHEMA));
    }

    private static PmemCheckpoint.Info checkpoint(PmemBlockStore store, long generation) throws IOException {
        return PmemCheckpoint.commit(store, CHECKPOINT, generation, CHUNKS.length, new byte[32], (index, buffer) -> {
            System.arraycopy(CHUNKS[index], 0, buffer, 0, CHUNKS[index].length);
            return CHUNKS[index].length;
        });
    }

    private static int twoChunks(int index, byte[] buffer) {
        buffer[0] = (byte) (index + 1);
        buffer[1] = (byte) (index + 2);
        return 2;
    }

    private static PmemPrefixKey key(int[] tokens) {
        return PmemPrefixKey.contiguous(context("test", SCHEMA, KEY, "q8:v1", "kv:v1:d4", OTHER), tokens, 0, tokens.length, 0);
    }

    private static PmemPrefixKey.Context context(String namespace, String model, String tokenizer,
                                                 String quantization, String layout, String adapters) {
        return new PmemPrefixKey.Context(namespace, model, tokenizer, quantization, layout, adapters);
    }

    private static String hash(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private static SimulatedRegion memory(int capacity, int maximum) {
        return new SimulatedRegion(0, Math.toIntExact(PmemBlockStore.requiredBytes(capacity, maximum)));
    }

    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }

    @FunctionalInterface
    private interface Action { void run() throws Exception; }

    private static <T extends Throwable> T expect(Class<T> type, Action action) throws Exception {
        assertions++;
        try { action.run(); }
        catch (Throwable failure) {
            if (type.isInstance(failure)) return type.cast(failure);
            throw new AssertionError("expected " + type.getName() + ", got " + failure, failure);
        }
        throw new AssertionError("expected " + type.getName());
    }

    private static final class RecordingTarget implements PmemCheckpoint.RestoreTarget {
        private int beginCalls, commitCalls, abortCalls;
        private int failureAt = -1;
        private boolean failAbort;
        private Runnable onBegin;
        private final List<byte[]> staged = new ArrayList<>();
        private byte[][] published;

        @Override public void begin(long generation, int chunks, long totalBytes) throws IOException {
            beginCalls++;
            if (failureAt == 0) throw new IOException("injected receiver begin failure");
            if (onBegin != null) onBegin.run();
        }
        @Override public void writeChunk(int index, byte[] bytes, int length) throws IOException {
            if (failureAt == index + 1) throw new IOException("injected receiver chunk failure");
            check(index == staged.size(), "receiver observes ordered chunk stream");
            staged.add(Arrays.copyOf(bytes, length));
        }
        @Override public void commit() throws IOException {
            commitCalls++;
            if (failureAt == 3) throw new IOException("injected receiver commit failure");
            published = staged.toArray(byte[][]::new);
            staged.clear();
        }
        @Override public void abort() throws IOException {
            abortCalls++;
            staged.clear();
            if (failAbort) throw new IOException("injected receiver abort failure");
        }
        void assertPublished(byte[][] expected) {
            check(beginCalls == 1 && commitCalls == 1 && abortCalls == 0, "single complete publication");
            check(Arrays.deepEquals(expected, published), "exact restored chunk boundaries and bytes");
        }
    }

    private static final class SimulatedRegion implements PmemRegion {
        private final long base;
        private final byte[] data, durable;
        private final List<String> persistTrace = new ArrayList<>();
        private boolean closed, readOnly, failNextRead;
        private int persists, writes, failPersistAt = -1, failWriteAt = -1, copyBeforeFailure;

        SimulatedRegion(long base, int size) { this.base = base; data = new byte[size]; durable = new byte[size]; }
        void failPersist(int relativeCall, int bytes) { failPersistAt = persists + relativeCall; copyBeforeFailure = bytes; }
        void failWrite(int relativeCall) { failWriteAt = writes + relativeCall; }
        void crash() { System.arraycopy(durable, 0, data, 0, data.length); failPersistAt = -1; failWriteAt = -1; }
        void corrupt(long offset) { int at = index(offset, 1); data[at] ^= 1; durable[at] ^= 1; }
        @Override public long length() { return base + data.length; }
        @Override public long fileOffset() { return 0; }
        @Override public Path path() { return Path.of("simulated-pmem-durable-blocks"); }
        @Override public PmemAccess access() { return readOnly ? PmemAccess.READ_ONLY : PmemAccess.READ_WRITE; }
        @Override public PmemBackend backend() { return PmemBackend.FILE; }
        @Override public PmemGranularity persistenceGranularity() { return PmemGranularity.PAGE; }
        @Override public boolean isOpen() { return !closed; }
        @Override public void close() { closed = true; }
        @Override public void read(long offset, byte[] target, int arrayOffset, int count) {
            if (failNextRead) { failNextRead = false; throw new IllegalStateException("injected read failure"); }
            System.arraycopy(data, index(offset, count), target, arrayOffset, count);
        }
        @Override public void write(long offset, byte[] source, int arrayOffset, int count) {
            if (readOnly) throw new IllegalStateException("read-only simulation");
            if (++writes == failWriteAt) throw new IllegalStateException("injected write failure");
            System.arraycopy(source, arrayOffset, data, index(offset, count), count);
        }
        @Override public void persist(long offset, long count) throws IOException {
            int length = Math.toIntExact(count), at = index(offset, length);
            persistTrace.add(offset + ":" + count);
            if (++persists == failPersistAt) {
                System.arraycopy(data, at, durable, at, Math.min(copyBeforeFailure, length));
                throw new IOException("injected persist failure");
            }
            System.arraycopy(data, at, durable, at, length);
        }
        private int index(long offset, int length) {
            if (closed) throw new IllegalStateException("simulation closed");
            if (offset < base || length < 0 || offset - base > data.length - length)
                throw new IndexOutOfBoundsException("simulation range");
            return Math.toIntExact(offset - base);
        }
    }
}
