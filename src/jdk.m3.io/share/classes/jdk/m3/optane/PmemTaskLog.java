// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.optane;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Single-writer, append-only PMem receipts for candidate source-analysis tasks.
 *
 * <p>The caller owns the {@link PmemRegion}, its reservation and its lifetime.
 * Each 144-byte record stores three SHA-256 identities, a status, an integrity
 * digest and a separately persisted commit marker. Source and model output live
 * in their existing owners: this log stores neither payload nor execution
 * authority. The mapping must be pre-sized and exclusively reserved for this
 * log; another writer or an external file replacement violates that contract.
 * A successful persist call is durability evidence for the selected backend,
 * not transactional atomicity or a physical power-loss qualification.</p>
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/5ecfe052562cdd1a7a6570068654c7f813fd0f47/m3-pmem-sdk/src/main/java/com/synexia/m3/pmem/PmemTaskLog.java blob 6111bfd4566f; recipe m3/optane/recipe/pmem-task-log-replay-20261106/relocate.py v3
public final class PmemTaskLog {
    public static final int RECORD_BYTES = 144;
    private static final int HASHED_BYTES = 104;
    private static final int MARKER_OFFSET = 136;
    private static final byte[] MAGIC = "M3TL".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] COMMIT = "M3COMMIT".getBytes(StandardCharsets.US_ASCII);
    private static final HexFormat HEX = HexFormat.of();

    /** A model's report is never a source-rewrite or test-promotion decision. */
    public enum Status { CANDIDATE, FAILED, SKIPPED }

    /** On-demand projection of a validated committed record. */
    public record Receipt(long ordinal, Status status, String taskSha256,
                          String sourceSha256, String resultSha256) { }

    /** Adapter point for the existing M3 ProgressMonitor/cancellation owner. */
    @FunctionalInterface
    public interface ProgressMonitor {
        void checkpoint(long inspected, long capacity);
    }

    private final PmemRegion region;
    private final long origin;
    private final long capacity;
    private final ProgressMonitor monitor;
    private final MessageDigest digest;
    private long committed;
    private boolean recoveredTail;
    private boolean reopenRequired;

    private PmemTaskLog(PmemRegion region, long origin, long capacity,
                        ProgressMonitor monitor) {
        this.region = region;
        this.origin = origin;
        this.capacity = capacity;
        this.monitor = monitor;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    /**
     * Reopen a dedicated record range and verify every previously committed
     * receipt. A zero commit marker on the next nonempty slot indicates an
     * interrupted append, which may be replaced by the next append.
     */
    public static PmemTaskLog open(PmemRegion region, long origin, long capacity,
                                   ProgressMonitor monitor) throws IOException {
        Objects.requireNonNull(region, "region");
        if (region.access() != PmemAccess.READ_WRITE) {
            throw new IllegalArgumentException("task log requires writable mapping");
        }
        if (origin < 0 || capacity <= 0 || origin > region.length()
                || capacity > (region.length() - origin) / RECORD_BYTES) {
            throw new IndexOutOfBoundsException("task log range exceeds mapping");
        }
        PmemTaskLog log = new PmemTaskLog(region, origin, capacity,
                monitor == null ? (done, total) -> { } : monitor);
        byte[] frame = new byte[RECORD_BYTES];
        boolean tailReached = false;
        for (long slot = 0; slot < capacity; slot++) {
            log.monitor.checkpoint(slot, capacity);
            region.read(log.offset(slot), frame, 0, frame.length);
            if (tailReached) {
                if (!allZero(frame)) {
                    throw new IOException("nonempty record beyond PMem task log tail");
                }
                continue;
            }
            if (allZero(frame)) {
                tailReached = true;
                continue;
            }
            if (zeroMarker(frame)) {
                log.recoveredTail = true;
                tailReached = true;
                continue;
            }
            log.verify(frame);
            log.committed++;
        }
        return log;
    }

    public synchronized long committedCount() { return committed; }
    public synchronized long capacity() { return capacity; }
    public synchronized boolean recoveredIncompleteTail() { return recoveredTail; }

    /**
     * Persist payload and a zero marker first, then persist the commit marker.
     * A failed append has an unknown durability outcome until reopened. The
     * caller must retain that failure and must not assume an unsuccessful
     * persist means none of its bytes reached persistent storage.
     */
    public synchronized Receipt append(Status status, String taskSha256,
                                       String sourceSha256, String resultSha256)
            throws IOException {
        Objects.requireNonNull(status, "status");
        byte[] task = parseHash(taskSha256);
        byte[] source = parseHash(sourceSha256);
        byte[] result = parseHash(resultSha256);
        if (reopenRequired) {
            throw new IllegalStateException("PMem task append outcome uncertain: reopen required");
        }
        if (committed >= capacity) throw new IOException("task log capacity exhausted");
        monitor.checkpoint(committed, capacity);
        byte[] frame = new byte[RECORD_BYTES];
        System.arraycopy(MAGIC, 0, frame, 0, MAGIC.length);
        frame[4] = 1;
        frame[5] = statusCode(status);
        System.arraycopy(task, 0, frame, 8, task.length);
        System.arraycopy(source, 0, frame, 40, source.length);
        System.arraycopy(result, 0, frame, 72, result.length);
        digest.reset();
        digest.update(frame, 0, HASHED_BYTES);
        System.arraycopy(digest.digest(), 0, frame, HASHED_BYTES, 32);
        long at = offset(committed);
        try {
            region.write(at, frame, 0, frame.length);
            region.persist(at, frame.length);
            region.write(at + MARKER_OFFSET, COMMIT, 0, COMMIT.length);
            region.persist(at + MARKER_OFFSET, COMMIT.length);
        } catch (IOException | RuntimeException | Error failed) {
            reopenRequired = true;
            throw failed;
        }
        Receipt receipt = new Receipt(committed, status, taskSha256, sourceSha256, resultSha256);
        committed++;
        recoveredTail = false;
        return receipt;
    }

    /** Read and validate one receipt; no per-entry objects are retained in the log. */
    public synchronized Receipt read(long ordinal) throws IOException {
        if (ordinal < 0 || ordinal >= committed) {
            throw new IndexOutOfBoundsException("uncommitted task receipt");
        }
        monitor.checkpoint(ordinal, capacity);
        byte[] frame = new byte[RECORD_BYTES];
        region.read(offset(ordinal), frame, 0, frame.length);
        Status status = verify(frame);
        return new Receipt(ordinal, status,
                HEX.formatHex(frame, 8, 40),
                HEX.formatHex(frame, 40, 72),
                HEX.formatHex(frame, 72, 104));
    }

    private Status verify(byte[] frame) throws IOException {
        if (!matches(frame, 0, MAGIC)
                || frame[4] != 1 || frame[6] != 0 || frame[7] != 0
                || !matches(frame, MARKER_OFFSET, COMMIT)) {
            throw new IOException("corrupt committed PMem task record header");
        }
        int code = frame[5] & 255;
        Status status = decodeStatus(code);
        digest.reset();
        digest.update(frame, 0, HASHED_BYTES);
        byte[] expected = digest.digest();
        if (!matches(frame, HASHED_BYTES, expected)) {
            throw new IOException("corrupt committed PMem task receipt checksum");
        }
        return status;
    }

    private long offset(long ordinal) { return origin + ordinal * RECORD_BYTES; }

    private static byte[] parseHash(String hash) {
        Objects.requireNonNull(hash, "sha256");
        if (hash.length() != 64) {
            throw new IllegalArgumentException("expected lowercase SHA-256");
        }
        for (int i = 0; i < hash.length(); i++) {
            char c = hash.charAt(i);
            if (!(c >= '0' && c <= '9' || c >= 'a' && c <= 'f')) {
                throw new IllegalArgumentException("expected lowercase SHA-256");
            }
        }
        return HEX.parseHex(hash);
    }

    private static byte statusCode(Status status) {
        return switch (status) {
            case CANDIDATE -> 1;
            case FAILED -> 2;
            case SKIPPED -> 3;
        };
    }

    private static Status decodeStatus(int code) throws IOException {
        return switch (code) {
            case 1 -> Status.CANDIDATE;
            case 2 -> Status.FAILED;
            case 3 -> Status.SKIPPED;
            default -> throw new IOException("unknown PMem task status");
        };
    }

    private static boolean matches(byte[] data, int start, byte[] expected) {
        for (int i = 0; i < expected.length; i++) {
            if (data[start + i] != expected[i]) return false;
        }
        return true;
    }

    private static boolean zeroMarker(byte[] frame) {
        for (int i = MARKER_OFFSET; i < RECORD_BYTES; i++) {
            if (frame[i] != 0) return false;
        }
        return true;
    }

    private static boolean allZero(byte[] bytes) {
        for (byte value : bytes) if (value != 0) return false;
        return true;
    }
}
