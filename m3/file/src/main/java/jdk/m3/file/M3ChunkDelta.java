// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.file;


import java.util.ArrayList;
import java.util.List;

/**
 * Delta between two chunk sequences.
 *
 * <h2>Core Principle</h2>
 * <blockquote>
 * <b>A delta is not "text diff". A delta is "chunk sequence diff".</b>
 * </blockquote>
 *
 * <h2>Delta Operations</h2>
 * <pre>
 * KEEP:   Chunk unchanged (reuse hash)
 * INSERT: New chunk added
 * DELETE: Chunk removed
 * </pre>
 *
 * <h2>Example</h2>
 * <pre>
 * OLD: [A, B, C, D, E]
 * NEW: [A, B, X, D, E, F]
 *
 * DELTA:
 * - KEEP A, B     (unchanged)
 * - DELETE C      (removed)
 * - INSERT X      (added)
 * - KEEP D, E     (unchanged)
 * - INSERT F      (added)
 * </pre>
 *
 * <h2>Why This Beats Byte-Level Diffs</h2>
 * <pre>
 * Byte diff (Git-style):
 *   - Format-dependent
 *   - Fragile on binaries
 *   - O(n×m) complexity
 *
 * Chunk diff:
 *   - Format-agnostic
 *   - Works on any content
 *   - O(n+m) with hash lookup
 *   - Aligned with storage blocks
 * </pre>
 *
 * <h2>Delta Size</h2>
 * <pre>
 * Each operation: ~40 bytes (op type + hash + metadata)
 *
 * For typical change (1% of file):
 *   File: 1MB → 128 chunks (8KB avg)
 *   Changed: 1-2 chunks
 *   Delta: ~80 bytes (not 10KB+ for byte diff)
 * </pre>
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/c278513bd458f4c9a69e4f5fe261c9450368be3b/synexia-fileindex/src/main/java/com/synexia/fileindex/content/chunk/ChunkDelta.java blob ed7af3f28af5; recipe m3/file/recipe/delta-generator-20261106/relocate.py v3
public final class M3ChunkDelta {
    private final String oldRoot;
    private final String newRoot;
    private final List<Operation> operations;

    public M3ChunkDelta(String oldRoot, String newRoot, List<Operation> operations) {
        this.oldRoot = oldRoot;
        this.newRoot = newRoot;
        this.operations = List.copyOf(operations);
    }

    /** Delta operation. */
    public sealed interface Operation permits Keep, Insert, Delete {
        /** Operation type. */
        enum Type { KEEP, INSERT, DELETE }

        /** Get operation type. */
        Type type();

        /** Get affected chunk hash. */
        String hash();
    }

    /** KEEP operation (chunk unchanged). */
    public record Keep(String hash) implements Operation {
        @Override
        public Type type() { return Type.KEEP; }

        @Override
        public String toString() {
            return "KEEP " + hash.substring(0, 12);
        }
    }

    /** INSERT operation (new chunk added). */
    public record Insert(String hash, int size) implements Operation {
        @Override
        public Type type() { return Type.INSERT; }

        @Override
        public String toString() {
            return String.format("INSERT %s (%d bytes)", hash.substring(0, 12), size);
        }
    }

    /** DELETE operation (chunk removed). */
    public record Delete(String hash) implements Operation {
        @Override
        public Type type() { return Type.DELETE; }

        @Override
        public String toString() {
            return "DELETE " + hash.substring(0, 12);
        }
    }

    /** Get old root hash. */
    public String oldRoot() {
        return oldRoot;
    }

    /** Get new root hash. */
    public String newRoot() {
        return newRoot;
    }

    /** Get delta operations. */
    public List<Operation> operations() {
        return operations;
    }

    /** Check if delta is empty (no changes). */
    public boolean isEmpty() {
        return operations.stream().allMatch(op -> op.type() == Operation.Type.KEEP);
    }

    /** Get delta statistics. */
    public Stats getStats() {
        int kept = 0, inserted = 0, deleted = 0;
        long insertedBytes = 0;

        for (Operation op : operations) {
            switch (op.type()) {
                case KEEP -> kept++;
                case INSERT -> {
                    inserted++;
                    if (op instanceof Insert ins) {
                        insertedBytes += ins.size();
                    }
                }
                case DELETE -> deleted++;
            }
        }

        return new Stats(kept, inserted, deleted, insertedBytes);
    }

    /**
     * Apply delta to old chunk list to produce new chunk list.
     *
     * @param oldChunks Old chunk sequence
     * @return New chunk sequence
     */
    public List<String> apply(List<String> oldChunks) {
        List<String> result = new ArrayList<>();
        int oldIndex = 0;

        for (Operation op : operations) {
            switch (op.type()) {
                case KEEP -> {
                    if (oldIndex >= oldChunks.size()) {
                        throw new IllegalStateException("KEEP beyond old chunk list");
                    }
                    result.add(oldChunks.get(oldIndex++));
                }
                case INSERT -> result.add(op.hash());
                case DELETE -> {
                    if (oldIndex >= oldChunks.size()) {
                        throw new IllegalStateException("DELETE beyond old chunk list");
                    }
                    oldIndex++;
                }
            }
        }

        return result;
    }

    /** Delta statistics. */
    public record Stats(int kept, int inserted, int deleted, long insertedBytes) {
        /** Total operations. */
        public int total() {
            return kept + inserted + deleted;
        }

        /** Change ratio (0.0 to 1.0). */
        public double changeRatio() {
            int total = total();
            return total > 0 ? (double) (inserted + deleted) / total : 0.0;
        }

        @Override
        public String toString() {
            return String.format(
                "Delta[kept=%d, inserted=%d, deleted=%d, %.1f%% changed]",
                kept, inserted, deleted, changeRatio() * 100
            );
        }

        /** Format as human-readable string. */
        public String format() {
            return String.format(
                "Kept: %d chunks" +
                "Inserted: %d chunks (%,d bytes)" +
                "Deleted: %d chunks" +
                "Total operations: %d" +
                "Change ratio: %.1f%%",
                kept,
                inserted, insertedBytes,
                deleted,
                total(),
                changeRatio() * 100
            );
        }
    }

    @Override
    public String toString() {
        Stats stats = getStats();
        return String.format(
            "M3ChunkDelta[%s → %s, %s]",
            oldRoot.substring(0, 12),
            newRoot.substring(0, 12),
            stats
        );
    }
}
