// SPDX-License-Identifier: Apache-2.0
package jdk.internal.mindex;

import java.util.Arrays;
import java.util.Objects;

/**
 * Precomputed odd/even reachability for the {@link M3ReasoningRelationKind#ATTACK} lane.
 *
 * <p>This is one specific abstract-argumentation query implementation: odd path length is treated
 * as indirect attack and even path length as support; the zero-length path makes each active node
 * support itself. Cycles may make both predicates true. The source relation graph remains
 * authoritative.</p>
 *
 * <p>The retained image is two bounded primitive bit matrices over active canonical node IDs.</p>
 */
public final class M3AttackParityIndex {
    public enum Parity {
        EVEN,
        ODD
    }

    public record Budget(int maxActiveNodes, long maxRetainedBytes) {
        public static final Budget DEFAULT =
                new Budget(4_096, 64L * 1024L * 1024L);

        public Budget {
            if (maxActiveNodes < 0 || maxRetainedBytes < 0L) {
                throw new IllegalArgumentException("attack-parity budget must be nonnegative");
            }
        }
    }

    private final M3ReasoningGraph graph;
    private final int[] localIds;
    private final int[] rowByLocal;
    private final int wordsPerSource;
    private final long[] evenWords;
    private final long[] oddWords;

    private M3AttackParityIndex(
            M3ReasoningGraph graph,
            int[] localIds,
            int[] rowByLocal,
            int wordsPerSource,
            long[] evenWords,
            long[] oddWords) {
        this.graph = graph;
        this.localIds = localIds;
        this.rowByLocal = rowByLocal;
        this.wordsPerSource = wordsPerSource;
        this.evenWords = evenWords;
        this.oddWords = oddWords;
    }

    public static M3AttackParityIndex compile(M3ReasoningGraph graph) {
        return compile(graph, Budget.DEFAULT);
    }

    public static M3AttackParityIndex compile(M3ReasoningGraph source, Budget budget) {
        M3ReasoningGraph graph = Objects.requireNonNull(source, "graph");
        Budget checkedBudget = Objects.requireNonNull(budget, "budget");

        boolean[] active = new boolean[graph.size()];
        for (int sourceLocal = 0; sourceLocal < graph.size(); sourceLocal++) {
            int count =
                    graph.outgoingCountLocal(
                            sourceLocal, M3ReasoningRelationKind.ATTACK);
            if (count == 0) continue;
            active[sourceLocal] = true;
            for (int ordinal = 0; ordinal < count; ordinal++) {
                active[
                        graph.outgoingTargetLocalUnchecked(
                                sourceLocal,
                                M3ReasoningRelationKind.ATTACK,
                                ordinal)] = true;
            }
        }

        int activeCount = 0;
        for (boolean present : active) if (present) activeCount++;
        if (activeCount > checkedBudget.maxActiveNodes()) {
            throw new IllegalArgumentException("attack-parity active-node budget exceeded");
        }

        int[] localIds = new int[activeCount];
        int[] rowByLocal = new int[graph.size()];
        Arrays.fill(rowByLocal, -1);
        int cursor = 0;
        for (int local = 0; local < active.length; local++) {
            if (active[local]) {
                localIds[cursor] = local;
                rowByLocal[local] = cursor;
                cursor++;
            }
        }

        int wordsPerSource = (activeCount + 63) >>> 6;
        long matrixWords = Math.multiplyExact((long) activeCount, wordsPerSource);
        long retainedBytes =
                Math.addExact(
                        Math.multiplyExact(
                                Math.multiplyExact(matrixWords, 2L), Long.BYTES),
                        Math.multiplyExact(
                                (long) (localIds.length + rowByLocal.length), Integer.BYTES));
        if (matrixWords > Integer.MAX_VALUE
                || retainedBytes > checkedBudget.maxRetainedBytes()) {
            throw new IllegalArgumentException("attack-parity retained-byte budget exceeded");
        }

        long[] even = new long[(int) matrixWords];
        long[] odd = new long[(int) matrixWords];
        int[] queue = new int[Math.max(1, Math.multiplyExact(activeCount, 2))];

        for (int sourceRow = 0; sourceRow < activeCount; sourceRow++) {
            int read = 0;
            int write = 0;
            set(even, wordsPerSource, sourceRow, sourceRow);
            queue[write++] = sourceRow << 1;

            while (read < write) {
                int state = queue[read++];
                int row = state >>> 1;
                int parity = state & 1;
                int sourceLocal = localIds[row];
                int count =
                        graph.outgoingCountLocal(
                                sourceLocal, M3ReasoningRelationKind.ATTACK);
                for (int ordinal = 0; ordinal < count; ordinal++) {
                    int targetLocal =
                            graph.outgoingTargetLocalUnchecked(
                                    sourceLocal,
                                    M3ReasoningRelationKind.ATTACK,
                                    ordinal);
                    int targetRow = rowByLocal[targetLocal];
                    if (targetRow < 0) {
                        throw new IllegalStateException(
                                "active attack target missing from parity image");
                    }
                    int nextParity = parity ^ 1;
                    long[] plane = nextParity == 0 ? even : odd;
                    if (!get(plane, wordsPerSource, sourceRow, targetRow)) {
                        set(plane, wordsPerSource, sourceRow, targetRow);
                        queue[write++] = (targetRow << 1) | nextParity;
                    }
                }
            }
        }

        return new M3AttackParityIndex(
                graph, localIds, rowByLocal, wordsPerSource, even, odd);
    }

    public M3ReasoningGraph graph() {
        return graph;
    }

    public int nodeCount() {
        return localIds.length;
    }

    public boolean contains(long nodeId) {
        int local = graph.localId(nodeId);
        return rowByLocal[local] >= 0;
    }

    public boolean supports(long sourceId, long targetId) {
        return reaches(sourceId, targetId, Parity.EVEN);
    }

    public boolean indirectlyAttacks(long sourceId, long targetId) {
        return reaches(sourceId, targetId, Parity.ODD);
    }

    public boolean reaches(long sourceId, long targetId, Parity parity) {
        int sourceLocal = graph.localId(sourceId);
        int targetLocal = graph.localId(targetId);
        int sourceRow = rowByLocal[sourceLocal];
        int targetRow = rowByLocal[targetLocal];
        if (sourceRow < 0 || targetRow < 0) return false;
        return get(
                Objects.requireNonNull(parity, "parity") == Parity.EVEN ? evenWords : oddWords,
                wordsPerSource,
                sourceRow,
                targetRow);
    }

    public long[] reachable(long sourceId, Parity parity) {
        int sourceLocal = graph.localId(sourceId);
        int sourceRow = rowByLocal[sourceLocal];
        if (sourceRow < 0) return new long[0];
        long[] plane = Objects.requireNonNull(parity, "parity") == Parity.EVEN
                ? evenWords
                : oddWords;
        long[] result = new long[localIds.length];
        int count = 0;
        int base = sourceRow * wordsPerSource;
        for (int word = 0; word < wordsPerSource; word++) {
            long bits = plane[base + word];
            while (bits != 0L) {
                int bit = Long.numberOfTrailingZeros(bits);
                int row = (word << 6) + bit;
                if (row < localIds.length) {
                    result[count++] = graph.nodeIdAt(localIds[row]);
                }
                bits &= bits - 1L;
            }
        }
        return Arrays.copyOf(result, count);
    }

    public long retainedPrimitiveBytes() {
        return (long) (localIds.length + rowByLocal.length) * Integer.BYTES
                + (long) (evenWords.length + oddWords.length) * Long.BYTES;
    }

    private static boolean get(
            long[] plane, int wordsPerSource, int sourceRow, int targetRow) {
        return (plane[sourceRow * wordsPerSource + (targetRow >>> 6)]
                        & (1L << (targetRow & 63)))
                != 0L;
    }

    private static void set(
            long[] plane, int wordsPerSource, int sourceRow, int targetRow) {
        plane[sourceRow * wordsPerSource + (targetRow >>> 6)]
                |= 1L << (targetRow & 63);
    }
}
