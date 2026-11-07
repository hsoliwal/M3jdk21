/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.m3.collections.text;

import java.util.Arrays;
import java.util.Objects;
import jdk.m3.collections.LongIntHashMap;

/**
 * Frozen Aho-Corasick multi-pattern automaton over int symbols.
 *
 * <p>Runtime state is compressed sparse-row transitions plus primitive failure
 * and output-link lanes. Trie nodes and edges are integer indexes, not objects.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/IntAhoCorasick.java}
 * blob {@code 398b496a0125061c20c1d3b8f5750539ecf55e88} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class IntAhoCorasick {
    /** Once a build node has this many edges, admit a transient primitive index. */
    private static final int HASHED_BUILD_MIN_FANOUT = 32;

    @FunctionalInterface
    public interface MatchConsumer {
        void accept(int patternId, int startInclusive, int endExclusive);
    }

    private final int[] nodeOffsets;
    private final int[] edgeSymbols;
    private final int[] edgeTargets;
    private final int[] failure;
    private final int[] outputLink;
    private final int[] outputHead;
    private final int[] outputPattern;
    private final int[] outputNext;
    private final int[] patternLengths;

    private IntAhoCorasick(
            int[] nodeOffsets,
            int[] edgeSymbols,
            int[] edgeTargets,
            int[] failure,
            int[] outputLink,
            int[] outputHead,
            int[] outputPattern,
            int[] outputNext,
            int[] patternLengths) {
        this.nodeOffsets = nodeOffsets;
        this.edgeSymbols = edgeSymbols;
        this.edgeTargets = edgeTargets;
        this.failure = failure;
        this.outputLink = outputLink;
        this.outputHead = outputHead;
        this.outputPattern = outputPattern;
        this.outputNext = outputNext;
        this.patternLengths = patternLengths;
    }

    public static IntAhoCorasick compile(int[][] patterns) {
        Objects.requireNonNull(patterns, "patterns");
        long totalSymbols = 0L;
        for (int[] pattern : patterns) {
            Objects.requireNonNull(pattern, "pattern");
            if (pattern.length == 0) throw new IllegalArgumentException("empty pattern");
            totalSymbols = Math.addExact(totalSymbols, pattern.length);
        }
        if (totalSymbols > Integer.MAX_VALUE - 1L) {
            throw new IllegalArgumentException("patterns too large");
        }

        int maxNodes = (int) totalSymbols + 1;
        int[] firstEdge = new int[maxNodes];
        Arrays.fill(firstEdge, -1);
        int[] edgeSymbolBuild = new int[(int) totalSymbols];
        int[] edgeTargetBuild = new int[(int) totalSymbols];
        int[] edgeNextBuild = new int[(int) totalSymbols];

        int[] outputHead = new int[maxNodes];
        Arrays.fill(outputHead, -1);
        int[] outputPattern = new int[patterns.length];
        int[] outputNext = new int[patterns.length];
        int[] patternLengths = new int[patterns.length];

        // Build-only lookup; allocation is conditional on actual observed fanout.
        // Frozen CSR transitions and output-link lanes remain the sole retained state.
        LongIntHashMap buildIndex = null;

        int nodeCount = 1;
        int edgeCount = 0;
        for (int patternId = 0; patternId < patterns.length; patternId++) {
            int node = 0;
            int[] pattern = patterns[patternId];
            patternLengths[patternId] = pattern.length;
            for (int symbol : pattern) {
                int target = findBuildTransition(node, symbol, firstEdge, edgeSymbolBuild,
                        edgeTargetBuild, edgeNextBuild, buildIndex);
                if (target < 0) {
                    if (buildIndex == null
                            && atLeastEdges(firstEdge[node], edgeNextBuild,
                                    HASHED_BUILD_MIN_FANOUT)) {
                        buildIndex = indexExistingEdges(nodeCount, edgeCount, firstEdge,
                                edgeSymbolBuild, edgeTargetBuild, edgeNextBuild);
                    }
                    target = nodeCount++;
                    edgeSymbolBuild[edgeCount] = symbol;
                    edgeTargetBuild[edgeCount] = target;
                    edgeNextBuild[edgeCount] = firstEdge[node];
                    firstEdge[node] = edgeCount++;
                    if (buildIndex != null) buildIndex.put(edgeKey(node, symbol), target);
                }
                node = target;
            }
            outputPattern[patternId] = patternId;
            outputNext[patternId] = outputHead[node];
            outputHead[node] = patternId;
        }

        int[] failure = new int[nodeCount];
        int[] outputLink = new int[nodeCount];
        Arrays.fill(outputLink, -1);
        int[] queue = new int[nodeCount];
        int head = 0;
        int tail = 0;

        for (int edge = firstEdge[0]; edge >= 0; edge = edgeNextBuild[edge]) {
            int child = edgeTargetBuild[edge];
            failure[child] = 0;
            outputLink[child] = -1;
            queue[tail++] = child;
        }

        while (head < tail) {
            int node = queue[head++];
            for (int edge = firstEdge[node]; edge >= 0; edge = edgeNextBuild[edge]) {
                int symbol = edgeSymbolBuild[edge];
                int child = edgeTargetBuild[edge];
                int fallback = failure[node];
                int target = findBuildTransition(
                        fallback, symbol, firstEdge, edgeSymbolBuild, edgeTargetBuild,
                        edgeNextBuild, buildIndex);
                while (fallback != 0 && target < 0) {
                    fallback = failure[fallback];
                    target = findBuildTransition(
                            fallback, symbol, firstEdge, edgeSymbolBuild, edgeTargetBuild,
                            edgeNextBuild, buildIndex);
                }
                if (target < 0) {
                    target = findBuildTransition(
                            0, symbol, firstEdge, edgeSymbolBuild, edgeTargetBuild,
                            edgeNextBuild, buildIndex);
                }
                if (target < 0) target = 0;

                failure[child] = target;
                outputLink[child] = outputHead[target] >= 0 ? target : outputLink[target];
                queue[tail++] = child;
            }
        }

        int[] nodeOffsets = new int[nodeCount + 1];
        for (int node = 0; node < nodeCount; node++) {
            int degree = 0;
            for (int edge = firstEdge[node]; edge >= 0; edge = edgeNextBuild[edge]) degree++;
            nodeOffsets[node + 1] = nodeOffsets[node] + degree;
        }

        long[] packed = new long[edgeCount];
        for (int node = 0; node < nodeCount; node++) {
            int cursor = nodeOffsets[node];
            for (int edge = firstEdge[node]; edge >= 0; edge = edgeNextBuild[edge]) {
                packed[cursor++] =
                        ((long) edgeSymbolBuild[edge] << 32)
                                | (edgeTargetBuild[edge] & 0xffff_ffffL);
            }
            Arrays.sort(packed, nodeOffsets[node], nodeOffsets[node + 1]);
        }

        int[] edgeSymbols = new int[edgeCount];
        int[] edgeTargets = new int[edgeCount];
        for (int i = 0; i < edgeCount; i++) {
            edgeSymbols[i] = (int) (packed[i] >> 32);
            edgeTargets[i] = (int) packed[i];
        }

        return new IntAhoCorasick(
                nodeOffsets,
                edgeSymbols,
                edgeTargets,
                failure,
                outputLink,
                Arrays.copyOf(outputHead, nodeCount),
                outputPattern,
                outputNext,
                patternLengths);
    }

    public int patternCount() { return patternLengths.length; }
    public int nodeCount() { return failure.length; }
    public int transitionCount() { return edgeSymbols.length; }
    public int patternLength(int patternId) { return patternLengths[patternId]; }
    public long payloadBytes() {
        return (long) (
                nodeOffsets.length + edgeSymbols.length + edgeTargets.length
                        + failure.length + outputLink.length + outputHead.length
                        + outputPattern.length + outputNext.length + patternLengths.length)
                * Integer.BYTES;
    }

    public void scan(int[] text, MatchConsumer consumer) {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(consumer, "consumer");
        int state = 0;
        for (int index = 0; index < text.length; index++) {
            int symbol = text[index];
            int next = transition(state, symbol);
            while (next < 0 && state != 0) {
                state = failure[state];
                next = transition(state, symbol);
            }
            state = next >= 0 ? next : 0;

            emit(state, index + 1, consumer);
            for (int output = outputLink[state]; output >= 0; output = outputLink[output]) {
                emit(output, index + 1, consumer);
            }
        }
    }

    private void emit(int node, int endExclusive, MatchConsumer consumer) {
        for (int output = outputHead[node]; output >= 0; output = outputNext[output]) {
            int patternId = outputPattern[output];
            consumer.accept(
                    patternId,
                    endExclusive - patternLengths[patternId],
                    endExclusive);
        }
    }

    private int transition(int node, int symbol) {
        int lo = nodeOffsets[node];
        int hi = nodeOffsets[node + 1] - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            int candidate = edgeSymbols[mid];
            if (candidate < symbol) lo = mid + 1;
            else if (candidate > symbol) hi = mid - 1;
            else return edgeTargets[mid];
        }
        return -1;
    }

    private static long edgeKey(int node, int symbol) {
        return ((long) node << Integer.SIZE) | (symbol & 0xffff_ffffL);
    }

    /** Bounded cold-path eligibility test; no per-node degree metadata retained. */
    private static boolean atLeastEdges(int head, int[] next, int threshold) {
        int cursor = head;
        for (int i = 0; i < threshold; i++) {
            if (cursor < 0) return false;
            cursor = next[cursor];
        }
        return true;
    }

    /** Rebuild existing linked edges exactly once when the first wide node appears. */
    private static LongIntHashMap indexExistingEdges(
            int nodes, int edges, int[] firstEdge, int[] edgeSymbols,
            int[] edgeTargets, int[] edgeNext) {
        LongIntHashMap index = new LongIntHashMap(Math.min(edges, 512));
        for (int node = 0; node < nodes; node++) {
            for (int edge = firstEdge[node]; edge >= 0; edge = edgeNext[edge]) {
                index.put(edgeKey(node, edgeSymbols[edge]), edgeTargets[edge]);
            }
        }
        return index;
    }

    private static int findBuildTransition(
            int node,
            int symbol,
            int[] firstEdge,
            int[] edgeSymbols,
            int[] edgeTargets,
            int[] edgeNext,
            LongIntHashMap index) {
        return index == null
                ? findBuildTransition(node, symbol, firstEdge, edgeSymbols, edgeTargets, edgeNext)
                : index.getOrDefault(edgeKey(node, symbol), -1);
    }

    private static int findBuildTransition(
            int node,
            int symbol,
            int[] firstEdge,
            int[] edgeSymbols,
            int[] edgeTargets,
            int[] edgeNext) {
        for (int edge = firstEdge[node]; edge >= 0; edge = edgeNext[edge]) {
            if (edgeSymbols[edge] == symbol) return edgeTargets[edge];
        }
        return -1;
    }
}
