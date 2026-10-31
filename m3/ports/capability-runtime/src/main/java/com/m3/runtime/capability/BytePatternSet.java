// SPDX-License-Identifier: Apache-2.0
package com.m3.runtime.capability;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.CancellationException;

/**
 * Immutable sparse Aho-Corasick prefilter over exact source bytes, not Unicode or regex.
 * One result bit per input pattern (up to 64), even when bytes are repeated or overlapping.
 * The only retained payload is primitive automaton metadata; input needles are not retained.
 * A positive bit is an exact containment fact, not an ordered match/capture position.
 */
public final class BytePatternSet {
    public static final int MAX_PATTERNS = 64;
    public static final int MAX_PATTERN_BYTES = 4_096;
    public static final int MAX_TOTAL_PATTERN_BYTES = 16_384;
    public static final int MAX_METADATA_BYTES = 1_048_576;

    private final int patternCount;
    private final int[] starts;
    private final byte[] labels;
    private final int[] targets;
    private final int[] failure;
    private final long[] outputs;

    private static final class Node {
        final Map<Integer, Integer> edges = new TreeMap<>();
        int fail;
        long output;
    }

    private BytePatternSet(int patternCount, int[] starts, byte[] labels, int[] targets,
                           int[] failure, long[] outputs) {
        this.patternCount = patternCount;
        this.starts = starts;
        this.labels = labels;
        this.targets = targets;
        this.failure = failure;
        this.outputs = outputs;
    }

    /** Defensive input snapshot; one deterministic transient trie pass, no payload retained. */
    public static BytePatternSet compile(List<byte[]> supplied) {
        Objects.requireNonNull(supplied, "patterns");
        if (supplied.size() > MAX_PATTERNS) throw new IllegalArgumentException("pattern count budget");
        ArrayList<Node> nodes = new ArrayList<>();
        nodes.add(new Node());
        int total = 0;
        for (int i = 0; i < supplied.size(); i++) {
            checkpoint(i);
            byte[] pattern = Objects.requireNonNull(supplied.get(i), "pattern").clone();
            if (pattern.length == 0 || pattern.length > MAX_PATTERN_BYTES) {
                throw new IllegalArgumentException("nonempty byte pattern budget");
            }
            total = Math.addExact(total, pattern.length);
            if (total > MAX_TOTAL_PATTERN_BYTES) throw new IllegalArgumentException("total pattern byte budget");
            int state = 0;
            for (byte unit : pattern) {
                int unsigned = Byte.toUnsignedInt(unit);
                Integer next = nodes.get(state).edges.get(unsigned);
                if (next == null) {
                    next = nodes.size();
                    nodes.get(state).edges.put(unsigned, next);
                    nodes.add(new Node());
                }
                state = next;
            }
            nodes.get(state).output |= 1L << i;
        }
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        for (int child : nodes.get(0).edges.values()) queue.addLast(child);
        while (!queue.isEmpty()) {
            checkpoint(queue.size());
            int state = queue.removeFirst();
            Node parent = nodes.get(state);
            for (Map.Entry<Integer, Integer> edge : parent.edges.entrySet()) {
                int symbol = edge.getKey();
                int child = edge.getValue();
                int link = parent.fail;
                while (link != 0 && !nodes.get(link).edges.containsKey(symbol)) {
                    link = nodes.get(link).fail;
                }
                Integer back = nodes.get(link).edges.get(symbol);
                nodes.get(child).fail = back == null ? 0 : back;
                nodes.get(child).output |= nodes.get(nodes.get(child).fail).output;
                queue.addLast(child);
            }
        }
        int states = nodes.size(), edges = 0;
        for (Node node : nodes) edges = Math.addExact(edges, node.edges.size());
        long retained = 4L * (states + 1L) + 5L * edges + 12L * states;
        if (retained > MAX_METADATA_BYTES) throw new IllegalArgumentException("automaton metadata budget");
        int[] starts = new int[states + 1];
        byte[] labels = new byte[edges];
        int[] targets = new int[edges];
        int[] failure = new int[states];
        long[] outputs = new long[states];
        int cursor = 0;
        for (int state = 0; state < states; state++) {
            checkpoint(state);
            starts[state] = cursor;
            Node node = nodes.get(state);
            failure[state] = node.fail;
            outputs[state] = node.output;
            for (Map.Entry<Integer, Integer> edge : node.edges.entrySet()) {
                labels[cursor] = (byte) (int) edge.getKey();
                targets[cursor] = edge.getValue();
                cursor++;
            }
        }
        starts[states] = cursor;
        return new BytePatternSet(supplied.size(), starts, labels, targets, failure, outputs);
    }

    public int patternCount() { return patternCount; }
    public int stateCount() { return failure.length; }
    public long retainedPrimitiveBytes() {
        return 4L * (starts.length + targets.length + failure.length)
                + labels.length + 8L * outputs.length;
    }

    /** Source-relative exact containment bits; no matched offsets or text arrays retained. */
    public long scan(SourceArtifact.Span source) {
        SourceArtifact.Span checked = Objects.requireNonNull(source, "source");
        int state = 0;
        long mask = 0;
        for (int i = 0; i < checked.length(); i++) {
            checkpoint(i);
            int symbol = Byte.toUnsignedInt(checked.byteAt(i));
            int next = findEdge(state, symbol);
            while (next < 0 && state != 0) {
                state = failure[state];
                next = findEdge(state, symbol);
            }
            state = next < 0 ? 0 : next;
            mask |= outputs[state];
        }
        checkpoint(checked.length());
        return mask;
    }

    /** Explicit JNI candidate, using the already installed source-pinned LF native library. */
    public long scanNative(SourceArtifact.Span source) {
        SourceArtifact.Span checked = Objects.requireNonNull(source, "source");
        checkpoint(0);
        byte[] copy = checked.copy();
        ByteBuffer direct = ByteBuffer.allocateDirect(copy.length);
        direct.put(copy).flip();
        // Reuse the sole source-pinned JNI admission owner: a separately loaded
        // unverified library must never satisfy this new optional native call.
        if (LfCount.nativeCount(direct, 0, 0) != 0L) {
            throw new IllegalStateException("pinned JNI preflight returned nonzero");
        }
        try {
            long mask = scanSet0(direct, copy.length, starts, labels, targets, failure, outputs);
            checkpoint(checked.length());
            long allowed = patternCount == Long.SIZE ? -1L : (1L << patternCount) - 1L;
            if ((mask & ~allowed) != 0L) throw new IllegalStateException("invalid native pattern mask");
            return mask;
        } catch (UnsatisfiedLinkError absent) {
            throw new IllegalStateException("pinned native library lacks byte-pattern set extension", absent);
        }
    }

    private int findEdge(int state, int symbol) {
        int low = starts[state], high = starts[state + 1] - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            int found = Byte.toUnsignedInt(labels[mid]);
            if (found == symbol) return targets[mid];
            if (found < symbol) low = mid + 1;
            else high = mid - 1;
        }
        return -1;
    }

    private static void checkpoint(int position) {
        if ((position & 255) == 0 && Thread.currentThread().isInterrupted()) {
            throw new CancellationException("byte-pattern set interrupted");
        }
    }

    private static native long scanSet0(ByteBuffer bytes, int length,
            int[] offsets, byte[] edgeBytes, int[] edgeTargets, int[] failure, long[] masks);
}
