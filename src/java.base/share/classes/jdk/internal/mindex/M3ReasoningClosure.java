// SPDX-License-Identifier: Apache-2.0
package jdk.internal.mindex;

import java.util.Arrays;
import java.util.Objects;

/**
 * Bounded transitive-closure image for one relation lane of an {@link M3ReasoningGraph}.
 *
 * <p>This is one concrete reachability implementation: one bit row per graph node, with SCC
 * membership derived mechanically from mutual reachability. The source graph remains authoritative
 * and no graph object hierarchy is retained.</p>
 */
public final class M3ReasoningClosure {
    public record Budget(int maxNodes, long maxRetainedBytes) {
        public static final Budget DEFAULT =
                new Budget(8_192, 128L * 1024L * 1024L);

        public Budget {
            if (maxNodes < 0 || maxRetainedBytes < 0L) {
                throw new IllegalArgumentException("closure budget must be nonnegative");
            }
        }
    }

    private final M3ReasoningGraph graph;
    private final M3ReasoningRelationKind kind;
    private final int wordsPerRow;
    private final long[] reachableBits;
    private final int[] componentIds;
    private final int componentCount;

    private M3ReasoningClosure(
            M3ReasoningGraph graph,
            M3ReasoningRelationKind kind,
            int wordsPerRow,
            long[] reachableBits,
            int[] componentIds,
            int componentCount) {
        this.graph = graph;
        this.kind = kind;
        this.wordsPerRow = wordsPerRow;
        this.reachableBits = reachableBits;
        this.componentIds = componentIds;
        this.componentCount = componentCount;
    }

    public static M3ReasoningClosure compile(
            M3ReasoningGraph graph, M3ReasoningRelationKind kind) {
        return compile(graph, kind, Budget.DEFAULT);
    }

    public static M3ReasoningClosure compile(
            M3ReasoningGraph source,
            M3ReasoningRelationKind relationKind,
            Budget budget) {
        M3ReasoningGraph graph = Objects.requireNonNull(source, "graph");
        M3ReasoningRelationKind kind = Objects.requireNonNull(relationKind, "kind");
        Budget checkedBudget = Objects.requireNonNull(budget, "budget");

        int nodes = graph.size();
        if (nodes > checkedBudget.maxNodes()) {
            throw new IllegalArgumentException("closure node budget exceeded");
        }
        int wordsPerRow = (nodes + 63) >>> 6;
        long words = Math.multiplyExact((long) nodes, wordsPerRow);
        long retained =
                Math.addExact(
                        Math.multiplyExact(words, Long.BYTES),
                        Math.multiplyExact((long) nodes, Integer.BYTES));
        if (words > Integer.MAX_VALUE || retained > checkedBudget.maxRetainedBytes()) {
            throw new IllegalArgumentException("closure retained-byte budget exceeded");
        }

        long[] bits = new long[(int) words];
        int[] queue = new int[Math.max(1, nodes)];
        boolean[] seen = new boolean[nodes];

        for (int sourceLocal = 0; sourceLocal < nodes; sourceLocal++) {
            Arrays.fill(seen, false);
            int read = 0;
            int write = 0;
            seen[sourceLocal] = true;
            queue[write++] = sourceLocal;
            set(bits, wordsPerRow, sourceLocal, sourceLocal);

            while (read < write) {
                int current = queue[read++];
                int count = graph.outgoingCountLocal(current, kind);
                for (int ordinal = 0; ordinal < count; ordinal++) {
                    int target = graph.outgoingTargetLocalUnchecked(current, kind, ordinal);
                    set(bits, wordsPerRow, sourceLocal, target);
                    if (!seen[target]) {
                        seen[target] = true;
                        queue[write++] = target;
                    }
                }
            }
        }

        int[] components = new int[nodes];
        Arrays.fill(components, -1);
        int componentCount = 0;
        for (int left = 0; left < nodes; left++) {
            if (components[left] >= 0) continue;
            int component = componentCount++;
            components[left] = component;
            for (int right = left + 1; right < nodes; right++) {
                if (components[right] < 0
                        && get(bits, wordsPerRow, left, right)
                        && get(bits, wordsPerRow, right, left)) {
                    components[right] = component;
                }
            }
        }

        return new M3ReasoningClosure(
                graph, kind, wordsPerRow, bits, components, componentCount);
    }

    public M3ReasoningGraph graph() {
        return graph;
    }

    public M3ReasoningRelationKind kind() {
        return kind;
    }

    public int componentCount() {
        return componentCount;
    }

    public boolean reachable(long sourceId, long targetId) {
        int source = graph.localId(sourceId);
        int target = graph.localId(targetId);
        return get(reachableBits, wordsPerRow, source, target);
    }

    public int component(long nodeId) {
        return componentIds[graph.localId(nodeId)];
    }

    public boolean sameComponent(long leftId, long rightId) {
        return component(leftId) == component(rightId);
    }

    public long[] reachableFrom(long sourceId) {
        int source = graph.localId(sourceId);
        long[] result = new long[graph.size()];
        int count = 0;
        int base = source * wordsPerRow;
        for (int word = 0; word < wordsPerRow; word++) {
            long bits = reachableBits[base + word];
            while (bits != 0L) {
                int bit = Long.numberOfTrailingZeros(bits);
                int local = (word << 6) + bit;
                if (local < graph.size()) result[count++] = graph.nodeIdAt(local);
                bits &= bits - 1L;
            }
        }
        return Arrays.copyOf(result, count);
    }

    public long[] componentMembers(long nodeId) {
        int wanted = component(nodeId);
        long[] result = new long[graph.size()];
        int count = 0;
        for (int local = 0; local < componentIds.length; local++) {
            if (componentIds[local] == wanted) result[count++] = graph.nodeIdAt(local);
        }
        return Arrays.copyOf(result, count);
    }

    public long retainedPrimitiveBytes() {
        return (long) reachableBits.length * Long.BYTES
                + (long) componentIds.length * Integer.BYTES;
    }

    private static boolean get(
            long[] bits, int wordsPerRow, int source, int target) {
        return (bits[source * wordsPerRow + (target >>> 6)]
                        & (1L << (target & 63)))
                != 0L;
    }

    private static void set(
            long[] bits, int wordsPerRow, int source, int target) {
        bits[source * wordsPerRow + (target >>> 6)] |= 1L << (target & 63);
    }
}
