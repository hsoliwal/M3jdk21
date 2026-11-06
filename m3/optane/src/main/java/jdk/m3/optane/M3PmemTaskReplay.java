// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.optane;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Read-only, fail-closed source/candidate reconciliation of a PMEM task log.
 *
 * <p>Call this on a freshly reopened {@link M3PmemTaskLog} after an interrupted
 * run. The reviewed task/source sequence comes from the canonical M3 catalogue
 * and Maven recipe owner; candidate bytes come from its immutable content
 * store. No model is reinvoked, no source is rewritten and no new JNI API is
 * introduced. Caller retains exclusive PMEM region ownership.</p>
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/5ecfe052562cdd1a7a6570068654c7f813fd0f47/m3-pmem-sdk/src/main/java/com/synexia/m3/pmem/PmemTaskReplay.java blob 6676c47aebe2; recipe m3/optane/recipe/pmem-task-log-replay-20261106/relocate.py v3
public final class M3PmemTaskReplay {
    public static final int MAX_CANDIDATE_BYTES = 64 * 1024;
    private static final byte[] DOMAIN =
            "m3.pmem.reviewed-task-sequence/1\n".getBytes(StandardCharsets.US_ASCII);
    private static final HexFormat HEX = HexFormat.of();

    /** Stable task identity already incorporating file/atom, contract and recipe. */
    public record Expected(long ordinal, String taskSha256, String sourceSha256) {
        public Expected {
            if (ordinal < 0) throw new IllegalArgumentException("REPLAY_ORDINAL");
            taskSha256 = sha(taskSha256);
            sourceSha256 = sha(sourceSha256);
        }
    }

    /** Existing content-addressed candidate output owner, not a model runner. */
    @FunctionalInterface
    public interface ContentReader {
        byte[] read(String resultSha256) throws IOException;
    }

    /** Adapter for the existing M3 ProgressMonitor/cancellation authority. */
    @FunctionalInterface
    public interface ProgressMonitor {
        void checkpoint(long verified, long total) throws IOException;
    }

    public record Replay(long committed, long nextOrdinal, long remaining,
            long candidates, long failures, long skipped, boolean incompleteTail,
            String reviewedSequenceSha256, boolean promotionAuthorized) {
        public Replay {
            if (committed < 0 || nextOrdinal != committed || remaining < 0
                    || candidates < 0 || failures < 0 || skipped < 0 || promotionAuthorized) {
                throw new IllegalArgumentException("REPLAY_RESULT_INVALID");
            }
            reviewedSequenceSha256 = sha(reviewedSequenceSha256);
        }
    }

    private M3PmemTaskReplay() { }

    /**
     * Verify the whole reviewed task sequence and every previously committed
     * receipt/output before returning the exact next ordinal. A different
     * contract, source preimage, order or content-store result refuses replay.
     */
    public static Replay reconcile(M3PmemTaskLog log, List<Expected> reviewed,
            ContentReader outputs, ProgressMonitor monitor) throws IOException {
        Objects.requireNonNull(log, "log");
        Objects.requireNonNull(reviewed, "reviewed");
        Objects.requireNonNull(outputs, "outputs");
        Objects.requireNonNull(monitor, "monitor");
        long count = reviewed.size();
        long committed = log.committedCount();
        if (count == 0 || count > log.capacity() || committed > count) {
            throw new IOException("REPLAY_REVIEWED_SEQUENCE_CAPACITY_OR_EXCESS_RECEIPTS");
        }

        MessageDigest sequence = digest();
        sequence.update(DOMAIN);
        Set<String> uniqueTasks = new HashSet<>(reviewed.size());
        for (int i = 0; i < reviewed.size(); i++) {
            Expected expected = Objects.requireNonNull(reviewed.get(i), "expected task");
            if (expected.ordinal() != i || !uniqueTasks.add(expected.taskSha256())) {
                throw new IOException("REPLAY_NONCONTIGUOUS_OR_DUPLICATE_TASK");
            }
            sequence.update(ByteBuffer.allocate(Long.BYTES).putLong(i).array());
            sequence.update(HEX.parseHex(expected.taskSha256()));
            sequence.update(HEX.parseHex(expected.sourceSha256()));
        }
        String reviewedSha = HEX.formatHex(sequence.digest());

        long candidates = 0;
        long failures = 0;
        long skipped = 0;
        for (int i = 0; i < committed; i++) {
            monitor.checkpoint(i, committed);
            Expected expected = reviewed.get(i);
            M3PmemTaskLog.Receipt receipt = log.read(i);
            if (receipt.ordinal() != i
                    || !receipt.taskSha256().equals(expected.taskSha256())
                    || !receipt.sourceSha256().equals(expected.sourceSha256())) {
                throw new IOException("REPLAY_SOURCE_CONTRACT_OR_ORDER_DRIFT");
            }
            byte[] content = outputs.read(receipt.resultSha256());
            if (content == null || content.length > MAX_CANDIDATE_BYTES
                    || !HEX.formatHex(digest().digest(content))
                            .equals(receipt.resultSha256())) {
                throw new IOException("REPLAY_MISSING_OR_CORRUPT_CANDIDATE_CONTENT");
            }
            switch (receipt.status()) {
                case CANDIDATE -> candidates++;
                case FAILED -> failures++;
                case SKIPPED -> skipped++;
            }
        }
        monitor.checkpoint(committed, committed);
        return new Replay(committed, committed, count - committed,
                candidates, failures, skipped, log.recoveredIncompleteTail(),
                reviewedSha, false);
    }

    private static String sha(String value) {
        Objects.requireNonNull(value, "sha256");
        if (!value.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("REPLAY_SHA256_PIN");
        }
        return value;
    }

    private static MessageDigest digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }
}
