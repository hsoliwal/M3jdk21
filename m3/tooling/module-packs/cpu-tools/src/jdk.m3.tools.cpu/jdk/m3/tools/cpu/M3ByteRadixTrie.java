// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.tools.cpu;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable unsigned-byte prefix trie stored entirely in primitive CSR-style lanes.
 * Each edge packs one unsigned byte in bits 24..31 and a child node in bits 0..23.
 * Repeated keys retain multiplicity. No caller arrays are retained.
 */
public final class M3ByteRadixTrie {
    public static final int MAX_KEYS = 1_000_000;
    public static final int MAX_SYMBOLS = 2_000_000;
    private static final int NODE_MASK = 0x00ff_ffff;

    private final int[] nodeStarts;
    private final int[] edges;
    private final int[] prefixes;
    private final int[] terminals;
    private final int keyCount;
    private final int distinctCount;

    private M3ByteRadixTrie(int[] nodeStarts, int[] edges, int[] prefixes,
                            int[] terminals, int keyCount, int distinctCount) {
        this.nodeStarts = nodeStarts;
        this.edges = edges;
        this.prefixes = prefixes;
        this.terminals = terminals;
        this.keyCount = keyCount;
        this.distinctCount = distinctCount;
    }

    /** Snapshot a packed sequence of possibly overlapping, possibly empty binary keys. */
    public static M3ByteRadixTrie copyOfPacked(byte[] packed, int[] offsets, int[] lengths) {
        byte[] bytes = Objects.requireNonNull(packed, "packed").clone();
        int[] starts = Objects.requireNonNull(offsets, "offsets").clone();
        int[] sizes = Objects.requireNonNull(lengths, "lengths").clone();
        int totalSymbols = validateSlices(bytes, starts, sizes);
        int capacity = totalSymbols + 1;
        int[] first = new int[capacity];
        Arrays.fill(first, -1);
        int[] next = new int[totalSymbols];
        int[] child = new int[totalSymbols];
        byte[] label = new byte[totalSymbols];
        int[] visits = new int[capacity];
        int[] completed = new int[capacity];
        int nodes = 1;
        int usedEdges = 0;
        int distinct = 0;

        for (int key = 0; key < starts.length; key++) {
            int node = 0;
            visits[0]++;
            int limit = starts[key] + sizes[key];
            for (int pos = starts[key]; pos < limit; pos++) {
                int symbol = bytes[pos] & 255;
                int found = -1;
                for (int edge = first[node]; edge != -1; edge = next[edge]) {
                    if ((label[edge] & 255) == symbol) {
                        found = child[edge];
                        break;
                    }
                }
                if (found == -1) {
                    if (nodes >= capacity || usedEdges >= totalSymbols)
                        throw new AssertionError("trie bound invariant");
                    found = nodes++;
                    child[usedEdges] = found;
                    label[usedEdges] = (byte) symbol;
                    next[usedEdges] = first[node];
                    first[node] = usedEdges++;
                }
                node = found;
                visits[node]++;
            }
            if (completed[node]++ == 0) distinct++;
        }

        int[] positions = new int[nodes + 1];
        for (int node = 0; node < nodes; node++) {
            int degree = 0;
            for (int edge = first[node]; edge != -1; edge = next[edge]) degree++;
            if (degree > 256) throw new AssertionError("unsigned-byte radix");
            positions[node + 1] = positions[node] + degree;
        }
        if (positions[nodes] != usedEdges) throw new AssertionError("edge ownership");
        int[] ordered = new int[usedEdges];
        int[] scratch = new int[256];
        int[] childByByte = new int[256];
        for (int node = 0; node < nodes; node++) {
            int degree = 0;
            for (int edge = first[node]; edge != -1; edge = next[edge]) {
                int symbol = label[edge] & 255;
                scratch[degree++] = symbol;
                childByByte[symbol] = child[edge];
            }
            Arrays.sort(scratch, 0, degree);
            for (int i = 0; i < degree; i++) {
                int symbol = scratch[i];
                ordered[positions[node] + i] = (symbol << 24) | childByByte[symbol];
            }
        }
        return new M3ByteRadixTrie(positions, ordered, Arrays.copyOf(visits, nodes),
                Arrays.copyOf(completed, nodes), starts.length, distinct);
    }

    /** Validate the entire packed batch, including a global work-size bound. */
    static int validateSlices(byte[] data, int[] starts, int[] sizes) {
        Objects.requireNonNull(data, "data");
        Objects.requireNonNull(starts, "starts");
        Objects.requireNonNull(sizes, "sizes");
        if (data.length > MAX_SYMBOLS || starts.length > MAX_KEYS || starts.length != sizes.length)
            throw new IllegalArgumentException("packed trie batch too large or mismatched");
        int sum = 0;
        for (int i = 0; i < starts.length; i++) {
            int from = starts[i];
            int length = sizes[i];
            if (from < 0 || from > data.length || length < 0 || length > data.length - from)
                throw new IllegalArgumentException("invalid packed trie span");
            if (length > MAX_SYMBOLS - sum)
                throw new IllegalArgumentException("packed trie symbol budget exceeded");
            sum += length;
        }
        return sum;
    }

    public int size() { return keyCount; }
    public int distinctKeyCount() { return distinctCount; }
    public int nodeCount() { return prefixes.length; }
    public int edgeCount() { return edges.length; }

    /** Exact key multiplicity, including the empty key. */
    public int countExact(byte[] key) {
        byte[] request = Objects.requireNonNull(key, "key");
        int node = walk(request, 0, request.length);
        return node < 0 ? 0 : terminals[node];
    }

    /** Number of stored keys starting with prefix, including duplicates. */
    public int countPrefix(byte[] prefix) {
        byte[] request = Objects.requireNonNull(prefix, "prefix");
        int node = walk(request, 0, request.length);
        return node < 0 ? 0 : prefixes[node];
    }

    public boolean containsPrefix(byte[] prefix) { return countPrefix(prefix) != 0; }

    /** Longest *stored full key* that is a prefix of this query; -1 when absent. */
    public int longestStoredPrefixLength(byte[] query) {
        byte[] request = Objects.requireNonNull(query, "query");
        int node = 0;
        int best = terminals[0] != 0 ? 0 : -1;
        for (int i = 0; i < request.length; i++) {
            node = nextNode(node, request[i] & 255);
            if (node < 0) break;
            if (terminals[node] != 0) best = i + 1;
        }
        return best;
    }

    /** Detached counts in caller-supplied query order; never sorts the query batch. */
    public int[] prefixCounts(byte[] packed, int[] offsets, int[] lengths) {
        byte[] data = Objects.requireNonNull(packed, "packed").clone();
        int[] starts = Objects.requireNonNull(offsets, "offsets").clone();
        int[] sizes = Objects.requireNonNull(lengths, "lengths").clone();
        validateSlices(data, starts, sizes);
        int[] result = new int[starts.length];
        for (int i = 0; i < starts.length; i++) {
            int node = walk(data, starts[i], sizes[i]);
            result[i] = node < 0 ? 0 : prefixes[node];
        }
        return result;
    }

    private int walk(byte[] data, int offset, int length) {
        int node = 0;
        for (int i = 0; i < length; i++) {
            node = nextNode(node, data[offset + i] & 255);
            if (node < 0) return -1;
        }
        return node;
    }

    private int nextNode(int node, int byteValue) {
        int low = nodeStarts[node];
        int high = nodeStarts[node + 1];
        while (low < high) {
            int mid = low + ((high - low) >>> 1);
            int current = edges[mid] >>> 24;
            if (current < byteValue) low = mid + 1;
            else high = mid;
        }
        if (low == nodeStarts[node + 1] || (edges[low] >>> 24) != byteValue) return -1;
        return edges[low] & NODE_MASK;
    }

    // Internal JNI ingress: one read-only owner, no Java public lane exposure.
    int[] ownedNodeStarts() { return nodeStarts; }
    int[] ownedEdges() { return edges; }
    int[] ownedPrefixTotals() { return prefixes; }
}
