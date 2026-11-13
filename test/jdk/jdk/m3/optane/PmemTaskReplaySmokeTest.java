// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.optane;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicInteger;

/** Independent no-native 20,000-record proof of source/content-bound replay. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/5ecfe052562cdd1a7a6570068654c7f813fd0f47/m3-pmem-sdk/src/test/java/com/synexia/m3/pmem/PmemTaskReplaySmokeTest.java blob ead08b77f59d; recipe make/m3/optane/recipe/pmem-task-log-replay-20261106/relocate.py v3
public final class PmemTaskReplaySmokeTest {
    private static final String SOURCE = "a".repeat(64);
    private static final byte[] OUTPUT = "candidate-review-only".getBytes(StandardCharsets.UTF_8);
    private static final String OUTPUT_SHA = hash(OUTPUT);

    private PmemTaskReplaySmokeTest() { }

    public static void main(String[] args) throws Exception {
        twentyThousandVerifiedReplayedTasks();
        interruptedWriteReconcilesWithoutReInference();
        System.out.println("PMEM_TASK_REPLAY_SMOKE_PASS records=20000");
    }

    private static void twentyThousandVerifiedReplayedTasks() throws Exception {
        int capacity = 20_000;
        MemoryRegion region = new MemoryRegion(0L, capacity * PmemTaskLog.RECORD_BYTES);
        PmemTaskLog writer = PmemTaskLog.open(region, 0, capacity, null);
        List<PmemTaskReplay.Expected> expected = new ArrayList<>(capacity);
        for (int i = 0; i < capacity; i++) {
            String task = String.format("%064x", i);
            expected.add(new PmemTaskReplay.Expected(i, task, SOURCE));
            PmemTaskLog.Status status = i % 1000 == 1 ? PmemTaskLog.Status.FAILED
                    : i % 1000 == 2 ? PmemTaskLog.Status.SKIPPED
                    : PmemTaskLog.Status.CANDIDATE;
            writer.append(status, task, SOURCE, OUTPUT_SHA);
        }
        check(region.persistCalls == 40_000, "two persistence fences per receipt");
        PmemTaskLog reopened = PmemTaskLog.open(region, 0, capacity, null);
        AtomicInteger checkpoints = new AtomicInteger();
        var state = PmemTaskReplay.reconcile(reopened, expected,
                hash -> OUTPUT,
                (done, total) -> checkpoints.incrementAndGet());
        check(state.committed() == 20_000 && state.nextOrdinal() == 20_000,
                "exact first uncommitted task ordinal");
        check(state.remaining() == 0, "all tasks present");
        check(state.candidates() == 19_960 && state.failures() == 20 && state.skipped() == 20,
                "status counts verified without model call");
        check(checkpoints.get() == 20_001, "progress counted all receipts");
        check(!state.incompleteTail() && !state.promotionAuthorized(),
                "no false crash or source-change authority");
        check(state.reviewedSequenceSha256().equals(PmemTaskReplay.reconcile(
                reopened, expected, hash -> OUTPUT, (done, total) -> { })
                .reviewedSequenceSha256()), "stable reviewed sequence root");

        expect(IOException.class, () -> PmemTaskReplay.reconcile(reopened,
                expected.subList(0, 19_999), hash -> OUTPUT, (done, total) -> { }));
        expect(IOException.class, () -> PmemTaskReplay.reconcile(reopened,
                expected, hash -> null, (done, total) -> { }));
        expect(IOException.class, () -> PmemTaskReplay.reconcile(reopened,
                expected, hash -> "tampered".getBytes(StandardCharsets.UTF_8),
                (done, total) -> { }));
        expect(IOException.class, () -> PmemTaskReplay.reconcile(reopened,
                expected, hash -> new byte[PmemTaskReplay.MAX_CANDIDATE_BYTES + 1],
                (done, total) -> { }));
        expect(IOException.class, () -> PmemTaskReplay.reconcile(reopened,
                expected, hash -> OUTPUT, (done, total) -> {
                    if (done == 400) throw new IOException("cancelled");
                }));

        List<PmemTaskReplay.Expected> drift = new ArrayList<>(expected);
        drift.set(10, new PmemTaskReplay.Expected(10, "f".repeat(64), SOURCE));
        expect(IOException.class, () -> PmemTaskReplay.reconcile(reopened,
                drift, hash -> OUTPUT, (done, total) -> { }));
        drift.set(10, new PmemTaskReplay.Expected(10, expected.get(9).taskSha256(), SOURCE));
        expect(IOException.class, () -> PmemTaskReplay.reconcile(reopened,
                drift, hash -> OUTPUT, (done, total) -> { }));
        drift.set(10, new PmemTaskReplay.Expected(11, expected.get(10).taskSha256(), SOURCE));
        expect(IOException.class, () -> PmemTaskReplay.reconcile(reopened,
                drift, hash -> OUTPUT, (done, total) -> { }));
        drift.set(10, new PmemTaskReplay.Expected(10, expected.get(10).taskSha256(),
                "b".repeat(64)));
        expect(IOException.class, () -> PmemTaskReplay.reconcile(reopened,
                drift, hash -> OUTPUT, (done, total) -> { }));
        expect(IllegalArgumentException.class,
                () -> new PmemTaskReplay.Expected(-1, "0".repeat(64), SOURCE));
    }

    private static void interruptedWriteReconcilesWithoutReInference() throws Exception {
        long base = 3L * 1024 * 1024 * 1024;
        MemoryRegion region = new MemoryRegion(base, 3 * PmemTaskLog.RECORD_BYTES);
        PmemTaskLog log = PmemTaskLog.open(region, base, 3, null);
        String first = "1".repeat(64);
        String second = "2".repeat(64);
        String third = "3".repeat(64);
        List<PmemTaskReplay.Expected> expected = List.of(
                new PmemTaskReplay.Expected(0, first, SOURCE),
                new PmemTaskReplay.Expected(1, second, SOURCE),
                new PmemTaskReplay.Expected(2, third, SOURCE));
        log.append(PmemTaskLog.Status.CANDIDATE, first, SOURCE, OUTPUT_SHA);
        region.failCommitWrite = true;
        expect(IllegalStateException.class, () ->
                log.append(PmemTaskLog.Status.CANDIDATE, second, SOURCE, OUTPUT_SHA));
        PmemTaskLog reopened = PmemTaskLog.open(region, base, 3, null);
        var replay = PmemTaskReplay.reconcile(reopened, expected,
                hash -> OUTPUT, (done, total) -> { });
        check(replay.committed() == 1 && replay.nextOrdinal() == 1
                && replay.remaining() == 2 && replay.incompleteTail(),
                "crash tail skipped without claiming candidate completion");
        reopened.append(PmemTaskLog.Status.CANDIDATE, second, SOURCE, OUTPUT_SHA);
        reopened.append(PmemTaskLog.Status.SKIPPED, third, SOURCE, OUTPUT_SHA);
        var complete = PmemTaskReplay.reconcile(PmemTaskLog.open(region, base, 3, null),
                expected, hash -> OUTPUT, (done, total) -> { });
        check(complete.committed() == 3 && complete.candidates() == 2
                && complete.skipped() == 1, "verified resumed prefix");
    }

    private static String hash(byte[] raw) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw));
        } catch (Exception impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private interface ThrowingRunnable { void run() throws Exception; }

    private static void expect(Class<? extends Throwable> type, ThrowingRunnable action)
            throws Exception {
        try { action.run(); }
        catch (Throwable failure) {
            if (type.isInstance(failure)) return;
            throw new AssertionError("expected " + type.getName(), failure);
        }
        throw new AssertionError("expected " + type.getName());
    }

    private static final class MemoryRegion implements PmemRegion {
        private final long base;
        private final byte[] data;
        private boolean closed;
        private boolean failCommitWrite;
        private int persistCalls;

        MemoryRegion(long base, int size) {
            this.base = base;
            data = new byte[size];
        }
        @Override public long length() { return base + data.length; }
        @Override public long fileOffset() { return 0; }
        @Override public Path path() { return Path.of("pmem-task-replay-test"); }
        @Override public PmemAccess access() { return PmemAccess.READ_WRITE; }
        @Override public PmemBackend backend() { return PmemBackend.POSIX; }
        @Override public PmemGranularity persistenceGranularity() { return PmemGranularity.PAGE; }
        @Override public boolean isOpen() { return !closed; }
        @Override public void read(long offset, byte[] dest, int start, int count) {
            requireOpen();
            System.arraycopy(data, index(offset, count), dest, start, count);
        }
        @Override public void write(long offset, byte[] source, int start, int count) {
            requireOpen();
            if (failCommitWrite && count == 8) {
                failCommitWrite = false;
                throw new IllegalStateException("simulated lost commit marker");
            }
            System.arraycopy(source, start, data, index(offset, count), count);
        }
        @Override public void persist(long offset, long count) {
            requireOpen();
            index(offset, Math.toIntExact(count));
            persistCalls++;
        }
        @Override public void close() { closed = true; }
        private int index(long offset, int length) {
            if (offset < base || length < 0 || offset - base > data.length - length) {
                throw new IndexOutOfBoundsException("fake PMem range");
            }
            return Math.toIntExact(offset - base);
        }
        private void requireOpen() {
            if (closed) throw new IllegalStateException("closed mapping");
        }
    }
}
