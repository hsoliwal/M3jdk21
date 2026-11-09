// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Modified 2026-10-31: packed LCS trace and rolling-row distance, preserving script ties.
// editDistance adapts Apache Commons Text LevenshteinDistance (Apache-2.0).
// Copyright 2014-2026 The Apache Software Foundation; see this task's donor NOTICE.
package jdk.m3.file;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Delta generator using longest-common-subsequence (LCS) dynamic programming.
 *
 * <h2>LCS contract and cost</h2>
 * <ul>
 * <li><b>Optimal</b>: Produces minimal edit distance</li>
 * <li><b>Work</b>: O(n×m) hash comparisons; indexed list access has its own cost</li>
 * <li><b>Simple</b>: No heavy dependencies or complex state</li>
 * </ul>
 *
 * <h2>Problem Definition</h2>
 * <pre>
 * Input: Two hash sequences
 *   OLD: [A, B, C, D, E]
 *   NEW: [A, B, X, D, E, F]
 *
 * Output: Minimal edit script
 *   KEEP A, B
 *   DELETE C
 *   INSERT X
 *   KEEP D, E
 *   INSERT F
 * </pre>
 *
 * <h2>Why This Is Cheap</h2>
 * <pre>
 * Byte-level LCS:
 *   - Compare byte sequences
 *   - Quadratic cells in the byte counts
 *
 * Chunk-level diff:
 *   - Compare hundreds of hashes
 *   - Quadratic cells in the chunk counts
 *   - Hash equality is O(1)
 * </pre>
 *
 * <h2>Performance</h2>
 * <ul>
 * <li><b>Time</b>: O(n×m) with constant-time indexed list access; no measured throughput claim</li>
 * <li><b>Workspace</b>: n×m direction bits plus one int[m+1] row; output operations are separate</li>
 * <li><b>Optimal</b>: Produces shortest edit sequence</li>
 * </ul>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * M3ChunkDeltaGenerator generator = new M3ChunkDeltaGenerator();
 *
 * List<String> oldChunks = [...];
 * List<String> newChunks = [...];
 *
 * M3ChunkDelta delta = generator.generate(
 *     oldRoot, oldChunks,
 *     newRoot, newChunks
 * );
 *
 * log.info(delta.getStats().format());
 * }</pre>
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/c278513bd458f4c9a69e4f5fe261c9450368be3b/synexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-fileindex-cache-delta-20261031/DeltaGenerator.java.txt blob 5fb5a5eda7b3; recipe m3/file/recipe/delta-generator-20261106/relocate.py v3
public final class M3ChunkDeltaGenerator {
    /** Stateless; one instance serves any number of sequences. */
    public M3ChunkDeltaGenerator() { }

    /**
     * Generate delta between two chunk sequences.
     *
     * @param oldRoot Old root hash
     * @param oldChunks Old chunk sequence
     * @param newRoot New root hash
     * @param newChunks New chunk sequence
     * @return Delta
     */
    public M3ChunkDelta generate(
        String oldRoot,
        List<String> oldChunks,
        String newRoot,
        List<String> newChunks
    ) {
        // Fast path: identical
        if (oldRoot.equals(newRoot)) {
            List<M3ChunkDelta.Operation> ops = oldChunks.stream()
                .map(M3ChunkDelta.Keep::new)
                .map(M3ChunkDelta.Operation.class::cast)
                .toList();
            return new M3ChunkDelta(oldRoot, newRoot, ops);
        }

        // Fast path: completely different
        if (oldChunks.isEmpty()) {
            List<M3ChunkDelta.Operation> ops = newChunks.stream()
                .map(hash -> new M3ChunkDelta.Insert(hash, 0))  // Size unknown here
                .map(M3ChunkDelta.Operation.class::cast)
                .toList();
            return new M3ChunkDelta(oldRoot, newRoot, ops);
        }

        if (newChunks.isEmpty()) {
            List<M3ChunkDelta.Operation> ops = oldChunks.stream()
                .map(M3ChunkDelta.Delete::new)
                .map(M3ChunkDelta.Operation.class::cast)
                .toList();
            return new M3ChunkDelta(oldRoot, newRoot, ops);
        }

        // LCS edit script with the original deterministic tie policy
        List<M3ChunkDelta.Operation> ops = lcsDiff(oldChunks, newChunks);
        return new M3ChunkDelta(oldRoot, newRoot, ops);
    }

    /**
     * Preserve the original LCS script, including insertion-first ties while backtracking.
     * One bit per cell retains the choice; one primitive row retains the LCS lengths.
     * Equal cells need no direction bit because equality is checked during reconstruction.
     */
    private List<M3ChunkDelta.Operation> lcsDiff(List<String> oldSeq, List<String> newSeq) {
        int n = oldSeq.size();
        int m = newSeq.size();
        // Preserve the original matrix's integer-dimension overflow exception.
        if (n == Integer.MAX_VALUE || m == Integer.MAX_VALUE) {
            throw new NegativeArraySizeException("LCS dimension overflow");
        }
        long words = (((long) n * m) + 63L) >>> 6;
        if (words > Integer.MAX_VALUE) {
            throw new OutOfMemoryError("LCS direction array exceeds Java array limit");
        }
        long[] insertDirections = new long[(int) words];
        int[] lengths = new int[m + 1];
        for (int i = 1; i <= n; i++) {
            int diagonal = 0;
            long cell = (long) (i - 1) * m;
            for (int j = 1; j <= m; j++, cell++) {
                int above = lengths[j];
                if (oldSeq.get(i - 1).equals(newSeq.get(j - 1))) {
                    lengths[j] = diagonal + 1;
                } else if (lengths[j - 1] >= above) {
                    lengths[j] = lengths[j - 1];
                    insertDirections[(int) (cell >>> 6)] |= 1L << (cell & 63L);
                }
                diagonal = above;
            }
        }

        List<M3ChunkDelta.Operation> ops = new ArrayList<>();
        int i = n, j = m;
        while (i > 0 || j > 0) {
            if (i > 0 && j > 0 && oldSeq.get(i - 1).equals(newSeq.get(j - 1))) {
                ops.add(new M3ChunkDelta.Keep(oldSeq.get(i - 1)));
                i--;
                j--;
            } else if (j > 0 && (i == 0 || insertionAt(insertDirections, (long) (i - 1) * m + j - 1))) {
                ops.add(new M3ChunkDelta.Insert(newSeq.get(j - 1), 0));
                j--;
            } else {
                ops.add(new M3ChunkDelta.Delete(oldSeq.get(i - 1)));
                i--;
            }
        }
        Collections.reverse(ops);
        return ops;
    }

    private static boolean insertionAt(long[] directions, long cell) {
        return (directions[(int) (cell >>> 6)] & (1L << (cell & 63L))) != 0;
    }

    /**
     * Generate delta with chunk size information.
     *
     * @param oldRoot Old root hash
     * @param oldChunks Old chunk sequence
     * @param newRoot New root hash
     * @param newChunks New chunk sequence
     * @param chunkStore Chunk store to lookup sizes
     * @return Delta with accurate chunk sizes
     */
    public M3ChunkDelta generateWithSizes(
        String oldRoot,
        List<String> oldChunks,
        String newRoot,
        List<String> newChunks,
        Map<String, byte[]> chunkStore
    ) {
        M3ChunkDelta delta = generate(oldRoot, oldChunks, newRoot, newChunks);

        // Enhance INSERT operations with actual sizes
        List<M3ChunkDelta.Operation> enhancedOps = delta.operations().stream()
            .map(op -> {
                if (op instanceof M3ChunkDelta.Insert ins) {
                    byte[] data = chunkStore.get(ins.hash());
                    int size = data != null ? data.length : 0;
                    return new M3ChunkDelta.Insert(ins.hash(), size);
                }
                return op;
            })
            .map(M3ChunkDelta.Operation.class::cast)
            .toList();

        return new M3ChunkDelta(oldRoot, newRoot, enhancedOps);
    }

    /**
     * Check if two chunk sequences are identical.
     *
     * @param seq1 First sequence
     * @param seq2 Second sequence
     * @return true if identical
     */
    public static boolean areIdentical(List<String> seq1, List<String> seq2) {
        if (seq1.size() != seq2.size()) return false;

        for (int i = 0; i < seq1.size(); i++) {
            if (!seq1.get(i).equals(seq2.get(i))) {
                return false;
            }
        }

        return true;
    }

    /**
     * Compute substitution-aware Levenshtein distance using O(min(n,m)) primitive workspace.
     *
     * @param oldSeq Old sequence
     * @param newSeq New sequence
     * @return Edit distance
     */
    public static int editDistance(List<String> oldSeq, List<String> newSeq) {
        int n = oldSeq.size();
        int m = newSeq.size();
        if (n == Integer.MAX_VALUE || m == Integer.MAX_VALUE) {
            throw new NegativeArraySizeException("edit-distance dimension overflow");
        }
        if (n == 0) return m;
        if (m == 0) return n;

        // Rolling-row recurrence adapted from Apache Commons Text LevenshteinDistance.
        // Keep old.equals(new) even when transposing, so a null new hash remains a mismatch.
        boolean columnsAreOld = n < m;
        int columns = Math.min(n, m);
        int rows = Math.max(n, m);
        int[] costs = new int[columns + 1];
        for (int column = 0; column <= columns; column++) costs[column] = column;
        for (int row = 1; row <= rows; row++) {
            int diagonal = costs[0];
            costs[0] = row;
            for (int column = 1; column <= columns; column++) {
                int above = costs[column];
                String oldHash = oldSeq.get(columnsAreOld ? column - 1 : row - 1);
                String newHash = newSeq.get(columnsAreOld ? row - 1 : column - 1);
                costs[column] = oldHash.equals(newHash) ? diagonal
                        : 1 + Math.min(diagonal, Math.min(above, costs[column - 1]));
                diagonal = above;
            }
        }
        return costs[columns];
    }
}
