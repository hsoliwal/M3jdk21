// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.m3.parser;

import java.util.Arrays;
import java.util.Objects;

/**
 * One source-owned, primitive-coordinate *index* of a parser's existing AST.
 *
 * <p>It is not a replacement parser, semantic tree, source owner or mutable AST.
 * The external parser remains authoritative. This index retains only kind IDs,
 * source geometry, occurrence-parent coordinates and sibling chains; no node
 * objects, Maps, entries, Strings of token text or compiler objects are retained.
 * Nodes are occurrence identities within this immutable source generation.
 */
public final class M3ASTIndex {
    public enum Domain { ECLIPSE_JDT, MAVEN_POM, GRADLE_GROOVY, GRADLE_KOTLIN, JACKSON_JSON }
    public static final int NO_RANGE = -1;
    /** The origin parser guarantees an exact half-open UTF-16 source range. */
    public static final int EXACT_RANGE = 1;
    /** Syntax exists, but semantic binding/effective build model is not certified. */
    public static final int SYNTAX_ONLY = 2;
    /** The external source owner could not be verified against the original AST. */
    public static final int SOURCE_UNVERIFIED = 4;
    public static final int RECOVERED = 8;
    private final M3File source;
    private final Domain domain;
    private final String[] names;
    private final int[] kinds, parents, starts, ends, flags, first, next, childCounts;

    private M3ASTIndex(Builder builder) {
        source = builder.source;
        domain = builder.domain;
        names = Arrays.copyOf(builder.names, builder.nameCount);
        kinds = Arrays.copyOf(builder.kinds, builder.size);
        parents = Arrays.copyOf(builder.parents, builder.size);
        starts = Arrays.copyOf(builder.starts, builder.size);
        ends = Arrays.copyOf(builder.ends, builder.size);
        flags = Arrays.copyOf(builder.flags, builder.size);
        first = Arrays.copyOf(builder.first, builder.size);
        next = Arrays.copyOf(builder.next, builder.size);
        childCounts = Arrays.copyOf(builder.childCounts, builder.size);
    }

    public static Builder builder(M3File source, Domain domain, int maxNodes) {
        return new Builder(source, domain, maxNodes);
    }
    public M3File source() { return source; }
    public Domain domain() { return domain; }
    public int nodeCount() { return kinds.length; }
    public String kind(int node) { return names[kinds[check(node)]]; }
    public int parent(int node) { return parents[check(node)]; }
    public int flags(int node) { return flags[check(node)]; }
    public int start(int node) { return starts[check(node)]; }
    public int end(int node) { return ends[check(node)]; }
    public int firstChild(int node) { return first[check(node)]; }
    public int nextSibling(int node) { return next[check(node)]; }
    public int childCount(int node) { return childCounts[check(node)]; }
    public boolean hasExactRange(int node) {
        check(node);
        return starts[node] >= 0 && ends[node] >= 0 && (flags[node] & EXACT_RANGE) != 0;
    }
    /** Materialization is explicit, never implicit during indexing or navigation. */
    public String sourceText(int node) {
        if (!hasExactRange(node)) throw new IllegalStateException("unverified/unknown source range");
        return source.subSequence(starts[node], ends[node]);
    }
    public long primitiveBytes() { return (long) kinds.length * 8L * Integer.BYTES; }

    /**
     * SHA-256 of the versioned domain, node kinds and ordered tree topology.
     * Source characters, source ranges and flags are excluded. Equal structure
     * hashes never certify source, binding, ABI or build-context equivalence.
     * Encoding uses length-prefixed UTF-16 code units and big-endian integers.
     * This is an explicit O(nodes + kind text) observation with bounded scratch.
     */
    public String structureSha256() {
        java.security.MessageDigest digest;
        try { digest = java.security.MessageDigest.getInstance("SHA-256"); }
        catch (java.security.NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
        hashText(digest, "M3AST_STRUCTURE_V1");
        hashText(digest, domain.name());
        hashInt(digest, kinds.length);
        for (int i = 0; i < kinds.length; i++) {
            hashText(digest, names[kinds[i]]);
            hashInt(digest, parents[i]);
            hashInt(digest, first[i]);
            hashInt(digest, next[i]);
            hashInt(digest, childCounts[i]);
        }
        return java.util.HexFormat.of().formatHex(digest.digest());
    }

    /**
     * Exact comparison of two immutable indices for the same URI and domain.
     * Uses actual lanes and kind strings, never a digest as equality proof.
     * Occurrence rows are generation-local: an insertion can shift later rows;
     * this is a bounded row delta, not a minimal edit script or a stable node ID.
     * Source changes are reported independently even when indexed rows match.
     * Parser/options/binding context is still the original provider's authority.
     */
    public Delta deltaFrom(M3ASTIndex previous) {
        Objects.requireNonNull(previous, "previous");
        if (domain != previous.domain || !source.uri().equals(previous.source.uri())) {
            throw new IllegalArgumentException("different AST source identity or domain");
        }
        byte[] states = new byte[Math.max(kinds.length, previous.kinds.length)];
        for (int i = 0; i < states.length; i++) {
            if (i >= previous.kinds.length) { states[i] = Delta.ADDED; continue; }
            if (i >= kinds.length) { states[i] = Delta.REMOVED; continue; }
            int state = 0;
            if (!names[kinds[i]].equals(previous.names[previous.kinds[i]])
                    || parents[i] != previous.parents[i] || first[i] != previous.first[i]
                    || next[i] != previous.next[i] || childCounts[i] != previous.childCounts[i]) {
                state |= Delta.STRUCTURE;
            }
            if (starts[i] != previous.starts[i] || ends[i] != previous.ends[i]) state |= Delta.RANGE;
            if (flags[i] != previous.flags[i]) state |= Delta.FLAGS;
            states[i] = (byte) state;
        }
        return new Delta(previous.kinds.length, kinds.length,
                !source.text().equals(previous.source.text()), states);
    }

    /** Immutable downstream change mask; retains no AST, provider or source payload. */
    public static final class Delta {
        public static final byte ADDED = 1;
        public static final byte REMOVED = 2;
        public static final byte STRUCTURE = 4;
        public static final byte RANGE = 8;
        public static final byte FLAGS = 16;
        private final int previousCount, currentCount;
        private final boolean sourceChanged;
        private final byte[] states;

        private Delta(int previousCount, int currentCount, boolean sourceChanged, byte[] states) {
            this.previousCount = previousCount;
            this.currentCount = currentCount;
            this.sourceChanged = sourceChanged;
            this.states = states;
        }
        public int previousNodeCount() { return previousCount; }
        public int currentNodeCount() { return currentCount; }
        public int rows() { return states.length; }
        public boolean sourceChanged() { return sourceChanged; }
        public int stateAt(int row) { return states[Objects.checkIndex(row, states.length)]; }
        /** Fresh caller-owned, ascending row indices. */
        public int[] changedRows() {
            int count = 0;
            for (byte state : states) if (state != 0) count++;
            int[] rows = new int[count];
            for (int i = 0, at = 0; i < states.length; i++) if (states[i] != 0) rows[at++] = i;
            return rows;
        }
    }

    private static void hashInt(java.security.MessageDigest digest, int value) {
        digest.update((byte) (value >>> 24));
        digest.update((byte) (value >>> 16));
        digest.update((byte) (value >>> 8));
        digest.update((byte) value);
    }

    private static void hashText(java.security.MessageDigest digest, String value) {
        hashInt(digest, value.length());
        for (int i = 0; i < value.length(); i++) {
            char unit = value.charAt(i);
            digest.update((byte) (unit >>> 8));
            digest.update((byte) unit);
        }
    }

    private int check(int node) { return Objects.checkIndex(node, kinds.length); }

    /** Bounded, single-owner creation; discarded immediately after immutable publication. */
    public static final class Builder {
        private final M3File source;
        private final Domain domain;
        private final int maxNodes;
        private String[] names = new String[16];
        private int nameCount;
        private int[] kinds = new int[32], parents = new int[32], starts = new int[32];
        private int[] ends = new int[32], flags = new int[32], first = new int[32];
        private int[] last = new int[32], next = new int[32], childCounts = new int[32];
        private int size;
        private boolean sealed;

        private Builder(M3File source, Domain domain, int maxNodes) {
            this.source = Objects.requireNonNull(source, "source");
            this.domain = Objects.requireNonNull(domain, "domain");
            if (maxNodes < 1 || maxNodes > 20_000_000) {
                throw new IllegalArgumentException("AST node budget outside 1..20m");
            }
            this.maxNodes = maxNodes;
        }

        /**
         * Appends one occurrence with an already appended parent; first node is root.
         * Unknown source coordinates must use exactly (-1,-1), never guessed positions.
         */
        public int add(int parent, String kind, int start, int end, int flags) {
            if (sealed) throw new IllegalStateException("index already sealed");
            if (size == maxNodes) throw new IllegalArgumentException("AST index node budget exceeded");
            if (size == 0 ? parent != -1 : parent < 0 || parent >= size) {
                throw new IllegalArgumentException("bad AST parent or root coordinate");
            }
            if (!((start == NO_RANGE && end == NO_RANGE)
                    || (start >= 0 && end >= start && end <= source.length()))) {
                throw new IllegalArgumentException("invalid UTF-16 half-open AST range");
            }
            if ((flags & ~(EXACT_RANGE | SYNTAX_ONLY | SOURCE_UNVERIFIED | RECOVERED)) != 0) {
                throw new IllegalArgumentException("unknown AST index flags");
            }
            if ((flags & EXACT_RANGE) != 0 && start == NO_RANGE) {
                throw new IllegalArgumentException("exact range without source coordinates");
            }
            if (parent >= 0 && starts[parent] >= 0 && ends[parent] >= 0 && start >= 0
                    && (start < starts[parent] || end > ends[parent])) {
                throw new IllegalArgumentException("AST child escapes known parent range");
            }
            int name = nameId(Objects.requireNonNull(kind, "kind"));
            if (size == kinds.length) grow();
            int row = size++;
            kinds[row] = name;
            parents[row] = parent;
            starts[row] = start;
            ends[row] = end;
            this.flags[row] = flags;
            first[row] = last[row] = next[row] = -1;
            if (parent >= 0) {
                if (first[parent] == -1) first[parent] = row;
                else next[last[parent]] = row;
                last[parent] = row;
                childCounts[parent]++;
            }
            return row;
        }

        /** Complete a previously unknown block/source range, before publication. */
        public void completeRange(int node, int start, int end, boolean exact) {
            if (sealed) throw new IllegalStateException("index already sealed");
            Objects.checkIndex(node, size);
            if (starts[node] != NO_RANGE || ends[node] != NO_RANGE) {
                throw new IllegalArgumentException("node already has a source range");
            }
            if (start < 0 || end < start || end > source.length()) {
                throw new IllegalArgumentException("invalid completed source range");
            }
            int parent = parents[node];
            if (parent >= 0 && starts[parent] >= 0 && ends[parent] >= 0
                    && (start < starts[parent] || end > ends[parent])) {
                throw new IllegalArgumentException("completed range escapes parent");
            }
            for (int child = first[node]; child >= 0; child = next[child]) {
                if (starts[child] >= 0 && (starts[child] < start || ends[child] > end)) {
                    throw new IllegalArgumentException("completed AST range excludes an indexed child");
                }
            }
            starts[node] = start;
            ends[node] = end;
            if (exact) flags[node] |= EXACT_RANGE;
        }

        public M3ASTIndex seal() {
            if (sealed || size < 1) throw new IllegalStateException("AST index not open or empty");
            sealed = true;
            return new M3ASTIndex(this);
        }

        private int nameId(String value) {
            if (value.isEmpty() || value.length() > 1024) {
                throw new IllegalArgumentException("invalid bounded AST kind");
            }
            for (int i = 0; i < nameCount; i++) {
                if (names[i].equals(value)) return i;
            }
            if (nameCount == 4096) throw new IllegalArgumentException("AST kind dictionary exceeded");
            if (nameCount == names.length) names = Arrays.copyOf(names, names.length * 2);
            names[nameCount] = value;
            return nameCount++;
        }

        private void grow() {
            int cap = (int) Math.min(maxNodes, Math.max((long) size + 1L, size + (long) (size >>> 1) + 1));
            kinds = Arrays.copyOf(kinds, cap);
            parents = Arrays.copyOf(parents, cap);
            starts = Arrays.copyOf(starts, cap);
            ends = Arrays.copyOf(ends, cap);
            flags = Arrays.copyOf(flags, cap);
            first = Arrays.copyOf(first, cap);
            last = Arrays.copyOf(last, cap);
            next = Arrays.copyOf(next, cap);
            childCounts = Arrays.copyOf(childCounts, cap);
        }
    }
}
