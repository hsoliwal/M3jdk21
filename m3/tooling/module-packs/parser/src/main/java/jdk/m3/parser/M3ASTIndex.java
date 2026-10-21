// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.m3.parser;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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
    public enum Domain { ECLIPSE_JDT, MAVEN_POM, GRADLE_GROOVY, GRADLE_KOTLIN, JACKSON_JSON, JAVAC }
    public static final int NO_RANGE = -1;
    /** The origin parser guarantees an exact half-open UTF-16 source range. */
    public static final int EXACT_RANGE = 1;
    /** Syntax exists, but semantic binding/effective build model is not certified. */
    public static final int SYNTAX_ONLY = 2;
    /** The external source owner could not be verified against the original AST. */
    public static final int SOURCE_UNVERIFIED = 4;
    public static final int RECOVERED = 8;
    /** Resolved from original javac declaration modifiers only; never inferred from spelling. */
    public static final int STATIC = 16, FINAL = 32, INTERFACE = 64;
    private final M3File source;
    private final Domain domain;
    private final String[] names;
    private final int[] kinds, parents, starts, ends, flags, first, next, childCounts;
    /** Optional signed 1-based name/literal IDs. Zero means no metadata for this row. */
    private final int[] semantic;
    private final String[] semanticValues;
    /** Built only for consumers requesting source lines; no per-node line objects. */
    private volatile int[] lineStarts;

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
        semantic = builder.semantic == null ? new int[0] : Arrays.copyOf(builder.semantic, builder.size);
        semanticValues = builder.semanticValues == null ? new String[0]
                : Arrays.copyOf(builder.semanticValues, builder.semanticCount);
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
    public long primitiveBytes() {
        int[] lines = lineStarts;
        return (long) kinds.length * 8L * Integer.BYTES
                + (long) semantic.length * Integer.BYTES
                + (lines == null ? 0L : (long) lines.length * Integer.BYTES);
    }

    /** Optional original compiler identifier, interned in this source's dictionary. */
    public String simpleName(int node) {
        int row = check(node);
        int id = semantic.length == 0 ? 0 : semantic[row];
        return id > 0 ? semanticValues[id - 1] : null;
    }

    /** Optional original javac literal value, not a reconstructed source spelling. */
    public String literalValue(int node) {
        int row = check(node);
        int id = semantic.length == 0 ? 0 : semantic[row];
        return id < 0 ? semanticValues[-id - 1] : null;
    }

    /** Resolve the nearest enclosing node of an original kind without another AST walk. */
    public int firstEnclosing(int node, String kind) {
        Objects.requireNonNull(kind, "kind");
        for (int parent = parents[check(node)]; parent >= 0; parent = parents[parent]) {
            if (kind.equals(kind(parent))) return parent;
        }
        return -1;
    }

    /** One-based Java source line of an exact UTF-16 start offset, or -1 if unknown. */
    public int line(int node) {
        int row = check(node);
        if (!hasExactRange(row)) return -1;
        int[] lines = lines();
        int found = Arrays.binarySearch(lines, starts[row]);
        return found >= 0 ? found + 1 : -found - 1;
    }

    /** Pack one-based inclusive start/end line IDs into a single long, without allocating. */
    public long spanLines(int node) {
        int row = check(node);
        if (!hasExactRange(row)) return -1L;
        int startLine = line(row);
        int endOffset = Math.max(starts[row], ends[row] - 1);
        int[] lines = lines();
        int found = Arrays.binarySearch(lines, endOffset);
        int endLine = found >= 0 ? found + 1 : -found - 1;
        return ((long) startLine << Integer.SIZE) | Integer.toUnsignedLong(endLine);
    }

    private int[] lines() {
        int[] lines = lineStarts;
        if (lines != null) return lines;
        synchronized (this) {
            lines = lineStarts;
            if (lines != null) return lines;
            int[] starts = new int[16];
            starts[0] = 0;
            int size = 1;
            for (int i = 0; i < source.length(); i++) {
                char value = source.charAt(i);
                if (value == '\r' && i + 1 < source.length() && source.charAt(i + 1) == '\n') i++;
                else if (value != '\r' && value != '\n') continue;
                if (size == starts.length) starts = Arrays.copyOf(starts, starts.length * 2);
                starts[size++] = i + 1;
            }
            lineStarts = lines = Arrays.copyOf(starts, size);
            return lines;
        }
    }

    /** Exact source UTF-16BE SHA-256. Computed only on request; no substring materialized. */
    public String exactSha256(int node) {
        int row = check(node);
        if (!hasExactRange(row)) throw new IllegalStateException("unverified AST source range");
        MessageDigest digest = sha256();
        byte[] scratch = new byte[4096];
        int used = 0;
        for (int i = starts[row]; i < ends[row]; i++) {
            char value = source.charAt(i);
            if (used == scratch.length) {
                digest.update(scratch);
                used = 0;
            }
            scratch[used++] = (byte) (value >>> 8);
            scratch[used++] = (byte) value;
        }
        digest.update(scratch, 0, used);
        return HexFormat.of().formatHex(digest.digest());
    }

    /**
     * Order-sensitive SHA-256 of kind, modifier flags and child arity only.
     * Names, literal spelling and source coordinates are deliberately masked.
     * This is a discovery fingerprint, not a proof of semantic equivalence.
     */
    public String structuralSha256(int node) {
        int root = check(node);
        MessageDigest digest = sha256();
        updateText(digest, "M3AST-STRUCTURE-1");
        int[] pending = new int[32];
        int size = 1;
        pending[0] = root;
        while (size > 0) {
            int current = pending[--size];
            updateText(digest, kind(current));
            updateWord(digest, flags[current] & (STATIC | FINAL | INTERFACE));
            updateWord(digest, childCounts[current]);
            int firstPending = size;
            for (int child = first[current]; child >= 0; child = next[child]) {
                if (size == pending.length) pending = Arrays.copyOf(pending, pending.length * 2);
                pending[size++] = child;
            }
            for (int a = firstPending, b = size - 1; a < b; a++, b--) {
                int swap = pending[a]; pending[a] = pending[b]; pending[b] = swap;
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static MessageDigest sha256() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException failure) { throw new IllegalStateException(failure); }
    }

    private static void updateWord(MessageDigest digest, int value) {
        digest.update((byte) (value >>> 24));
        digest.update((byte) (value >>> 16));
        digest.update((byte) (value >>> 8));
        digest.update((byte) value);
    }

    private static void updateText(MessageDigest digest, String value) {
        updateWord(digest, value.length());
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
        private int[] semantic;
        private String[] semanticValues;
        private Map<String, Integer> semanticDictionary;
        private int semanticCount;

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
            if ((flags & ~(EXACT_RANGE | SYNTAX_ONLY | SOURCE_UNVERIFIED | RECOVERED
                    | STATIC | FINAL | INTERFACE)) != 0) {
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

        /** Attach original javac simple-name or literal metadata without retaining compiler nodes. */
        public void annotate(int node, String name, String literal) {
            if (sealed) throw new IllegalStateException("index already sealed");
            Objects.checkIndex(node, size);
            if ((name == null) == (literal == null)) {
                throw new IllegalArgumentException("exactly one identifier or literal required");
            }
            if (semantic == null) semantic = new int[kinds.length];
            if (semantic[node] != 0) throw new IllegalStateException("node already annotated");
            String value = name == null ? literal : name;
            if (value.length() > 1_048_576) throw new IllegalArgumentException("semantic literal too large");
            if (semanticDictionary == null) {
                semanticDictionary = new HashMap<>();
                semanticValues = new String[16];
            }
            Integer id = semanticDictionary.get(value);
            if (id == null) {
                if (semanticCount == 1_000_000) throw new IllegalArgumentException("semantic dictionary budget");
                id = semanticCount++;
                if (id == semanticValues.length)
                    semanticValues = Arrays.copyOf(semanticValues, semanticValues.length * 2);
                semanticValues[id] = value;
                semanticDictionary.put(value, id);
            }
            semantic[node] = name == null ? -(id + 1) : id + 1;
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
            if (semantic != null) semantic = Arrays.copyOf(semantic, cap);
        }
    }
}
