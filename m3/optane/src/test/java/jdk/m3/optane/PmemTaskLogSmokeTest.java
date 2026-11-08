// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.optane;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

/** Independent deterministic oracle; no installed JNI library is required. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/5ecfe052562cdd1a7a6570068654c7f813fd0f47/m3-pmem-sdk/src/test/java/com/synexia/m3/pmem/PmemTaskLogSmokeTest.java blob fdf1af53ab43; recipe m3/optane/recipe/pmem-task-log-replay-20261106/relocate.py v3
public final class PmemTaskLogSmokeTest {
    private static final String ZERO = "0".repeat(64);
    private static final String ONE = "1".repeat(64);
    private static final String TWO = "2".repeat(64);

    private PmemTaskLogSmokeTest() { }

    public static void main(String[] args) throws Exception {
        twentyThousandReceiptsAndReopen();
        interruptedAppendAndTailRecovery();
        committedCorruptionRefuses();
        recordBeyondGapRefuses();
        boundsAndCancellation();
        System.out.println("PMEM_TASK_LOG_SMOKE_PASS records=20000");
    }

    private static void twentyThousandReceiptsAndReopen() throws Exception {
        int capacity = 20_000;
        MemoryRegion memory = new MemoryRegion(0L, capacity * PmemTaskLog.RECORD_BYTES);
        PmemTaskLog log = PmemTaskLog.open(memory, 0L, capacity, null);
        for (int i = 0; i < capacity; i++) {
            String task = String.format("%064x", i);
            PmemTaskLog.Receipt receipt = log.append(PmemTaskLog.Status.CANDIDATE,
                    task, ONE, TWO);
            check(receipt.ordinal() == i, "ordered append");
        }
        check(log.committedCount() == capacity, "complete 20,000-file journal");
        check(memory.persistCalls == 2 * capacity, "body and marker persisted separately");
        PmemTaskLog again = PmemTaskLog.open(memory, 0L, capacity, null);
        check(again.committedCount() == capacity, "committed count after reopen");
        check(!again.recoveredIncompleteTail(), "no incomplete tail");
        check(again.read(0).taskSha256().equals(ZERO), "first identity retained");
        check(again.read(capacity - 1).taskSha256()
                .equals(String.format("%064x", capacity - 1)), "last identity retained");
        expect(IOException.class, () -> again.append(PmemTaskLog.Status.SKIPPED, ZERO, ONE, TWO));
    }

    private static void interruptedAppendAndTailRecovery() throws Exception {
        long above2Gib = 3L * 1024 * 1024 * 1024;
        MemoryRegion memory = new MemoryRegion(above2Gib, 3 * PmemTaskLog.RECORD_BYTES);
        PmemTaskLog log = PmemTaskLog.open(memory, above2Gib, 3, null);
        log.append(PmemTaskLog.Status.CANDIDATE, ZERO, ONE, TWO);
        memory.failCommitWrite = true;
        expect(IllegalStateException.class,
                () -> log.append(PmemTaskLog.Status.FAILED, ONE, TWO, ZERO));
        expect(IllegalStateException.class,
                () -> log.append(PmemTaskLog.Status.SKIPPED, TWO, ZERO, ONE));
        PmemTaskLog recovered = PmemTaskLog.open(memory, above2Gib, 3, null);
        check(recovered.committedCount() == 1, "uncommitted tail not promoted");
        check(recovered.recoveredIncompleteTail(), "incomplete append signaled");
        recovered.append(PmemTaskLog.Status.FAILED, ONE, TWO, ZERO);
        check(recovered.committedCount() == 2, "one slot reused");
        check(recovered.read(1).status() == PmemTaskLog.Status.FAILED, "status retained");
        check(recovered.read(0).resultSha256().equals(TWO), "earlier durable result retained");
    }

    private static void committedCorruptionRefuses() throws Exception {
        MemoryRegion memory = new MemoryRegion(0L, 2 * PmemTaskLog.RECORD_BYTES);
        PmemTaskLog log = PmemTaskLog.open(memory, 0L, 2, null);
        log.append(PmemTaskLog.Status.CANDIDATE, ZERO, ONE, TWO);
        memory.data[9] ^= 1;
        expect(IOException.class, () -> PmemTaskLog.open(memory, 0L, 2, null));
        expect(IOException.class, () -> log.read(0));
    }

    private static void recordBeyondGapRefuses() throws Exception {
        MemoryRegion memory = new MemoryRegion(0L, 3 * PmemTaskLog.RECORD_BYTES);
        PmemTaskLog log = PmemTaskLog.open(memory, 0L, 3, null);
        log.append(PmemTaskLog.Status.CANDIDATE, ZERO, ONE, TWO);
        memory.data[2 * PmemTaskLog.RECORD_BYTES + 8] = 1;
        expect(IOException.class, () -> PmemTaskLog.open(memory, 0L, 3, null));
    }

    private static void boundsAndCancellation() throws Exception {
        MemoryRegion memory = new MemoryRegion(0L, PmemTaskLog.RECORD_BYTES);
        expect(IndexOutOfBoundsException.class,
                () -> PmemTaskLog.open(memory, 1, 1, null));
        expect(IllegalArgumentException.class,
                () -> PmemTaskLog.open(new MemoryRegion(0L, PmemTaskLog.RECORD_BYTES)
                        .readOnly(), 0, 1, null));
        AtomicBoolean stop = new AtomicBoolean();
        PmemTaskLog cancelled = PmemTaskLog.open(memory, 0, 1, (done, total) -> {
            if (stop.get()) throw new IllegalStateException("cancelled");
        });
        stop.set(true);
        expect(IllegalStateException.class,
                () -> cancelled.append(PmemTaskLog.Status.CANDIDATE, ZERO, ONE, TWO));
        PmemTaskLog log = PmemTaskLog.open(memory, 0, 1, null);
        expect(IllegalArgumentException.class,
                () -> log.append(PmemTaskLog.Status.CANDIDATE, "not-sha256", ONE, TWO));
        check(log.committedCount() == 0, "invalid task cannot advance journal");
        expect(IndexOutOfBoundsException.class, () -> log.read(0));
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
        private boolean readonly;
        private boolean closed;
        private boolean failCommitWrite;
        private int persistCalls;

        MemoryRegion(long base, int size) {
            this.base = base;
            this.data = new byte[size];
        }

        MemoryRegion readOnly() { readonly = true; return this; }
        @Override public long length() { return base + data.length; }
        @Override public long fileOffset() { return 0; }
        @Override public Path path() { return Path.of("pmem-task-log-test"); }
        @Override public PmemAccess access() {
            return readonly ? PmemAccess.READ_ONLY : PmemAccess.READ_WRITE;
        }
        @Override public PmemBackend backend() { return PmemBackend.POSIX; }
        @Override public PmemGranularity persistenceGranularity() {
            return PmemGranularity.PAGE;
        }
        @Override public boolean isOpen() { return !closed; }

        @Override public void read(long offset, byte[] target, int arrayOffset, int count) {
            requireOpen();
            System.arraycopy(data, index(offset, count), target, arrayOffset, count);
        }
        @Override public void write(long offset, byte[] source, int arrayOffset, int count) {
            requireOpen();
            if (readonly) throw new IllegalStateException("read-only");
            if (failCommitWrite && count == 8) {
                failCommitWrite = false;
                throw new IllegalStateException("simulated crash before commit marker");
            }
            System.arraycopy(source, arrayOffset, data, index(offset, count), count);
        }
        @Override public void persist(long offset, long count) {
            requireOpen();
            index(offset, Math.toIntExact(count));
            persistCalls++;
        }
        @Override public void close() { closed = true; }

        private int index(long offset, int count) {
            if (offset < base || count < 0 || offset - base > data.length - count) {
                throw new IndexOutOfBoundsException("fake PMem range");
            }
            return Math.toIntExact(offset - base);
        }
        private void requireOpen() {
            if (closed) throw new IllegalStateException("closed");
        }
    }
}
