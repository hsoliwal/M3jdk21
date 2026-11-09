// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.m3.parser;

import java.util.Arrays;
import java.util.HashMap;
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
    public enum Domain { ECLIPSE_JDT, MAVEN_POM, GRADLE_GROOVY, GRADLE_KOTLIN, JACKSON_JSON, JAVAC }
    public static final int NO_RANGE = -1;
    /** The origin parser guarantees an exact half-open UTF-16 source range. */
    public static final int EXACT_RANGE = 1;
    /** Syntax exists, but semantic binding/effective build model is not certified. */
    public static final int SYNTAX_ONLY = 2;
    /** The external source owner could not be verified against the original AST. */
    public static final int SOURCE_UNVERIFIED = 4;
    public static final int RECOVERED = 8;
    /** Original javac modifiers; never synthesize semantic binding from syntax. */
    public static final int STATIC = 16, FINAL = 32, INTERFACE = 64;
    private static final long HASH_SEED = 0xcbf29ce484222325L;
    private static final long HASH_PRIME = 0x100000001b3L;
    private final M3File source;
    private final Domain domain;
    private final String[] names, symbols;
    private final int[] kinds, parents, starts, ends, flags, first, next, childCounts;
    /** Optional javac-only lanes; absent for Maven/JDT/Jackson/Gradle indexes. */
    private final int[] lines, endLines, symbolIds, literalIds;
    private volatile long[] structureHashes;

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
        lines = trim(builder.lines, builder.size);
        endLines = trim(builder.endLines, builder.size);
        symbolIds = trim(builder.symbolIds, builder.size);
        literalIds = trim(builder.literalIds, builder.size);
        symbols = builder.symbols == null ? new String[0] : Arrays.copyOf(builder.symbols, builder.symbolCount);
    }
    private static int[] trim(int[] values, int length) {
        return values == null ? null : Arrays.copyOf(values, length);
    }
    private static long hashKind(String value, int flags) {
        long result = HASH_SEED;
        for (int i = 0; i < value.length(); i++) result = (result ^ value.charAt(i)) * HASH_PRIME;
        return (result ^ flags) * HASH_PRIME;
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
    /** Original javac LineMap positions; -1 for other domains/unavailable geometry. */
    public int line(int node) {
        int row = check(node);
        return lines == null ? -1 : lines[row];
    }
    public record LineSpan(int first, int last) { }
    /** Inclusive line span, or (-1,-1) when unavailable. */
    public LineSpan spanLines(int node) {
        int row = check(node);
        return lines == null ? new LineSpan(-1, -1) : new LineSpan(lines[row], endLines[row]);
    }
    /** Simple name/value dictionaries are indexed by primitive node rows, not entry objects. */
    public String simpleName(int node) {
        int row = check(node);
        return symbolIds == null || symbolIds[row] < 0 ? null : symbols[symbolIds[row]];
    }
    public String literal(int node) {
        int row = check(node);
        return literalIds == null || literalIds[row] < 0 ? null : symbols[literalIds[row]];
    }
    /** First strict ancestor of the named kind, -1 when none. */
    public int firstEnclosing(int node, String syntaxKind) {
        Objects.requireNonNull(syntaxKind, "syntaxKind");
        for (int row = parents[check(node)]; row >= 0; row = parents[row]) {
            if (names[kinds[row]].equals(syntaxKind)) return row;
        }
        return -1;
    }
    /** Lazy, immutable-on-publication structural hint: never a semantic proof. */
    public long structuralHash64(int node) {
        int row = check(node);
        long[] hashes = structureHashes;
        if (hashes == null) {
            synchronized (this) {
                hashes = structureHashes;
                if (hashes == null) {
                    hashes = new long[nodeCount()];
                    for (int current = nodeCount() - 1; current >= 0; current--) {
                        long hash = hashKind(names[kinds[current]], flags[current] & (STATIC | FINAL | INTERFACE));
                        for (int child = first[current]; child != -1; child = next[child]) {
                            hash = (hash ^ hashes[child]) * HASH_PRIME;
                        }
                        hashes[current] = (hash ^ childCounts[current]) * HASH_PRIME;
                    }
                    structureHashes = hashes;
                }
            }
        }
        return hashes[row];
    }
    /** Exact source-range fingerprint; collision-prone discovery signal, not an exact proof. */
    public long exactHash64(int node) {
        if (!hasExactRange(node)) throw new IllegalStateException("exact source hash requires verified range");
        long hash = HASH_SEED;
        for (int i = starts[node]; i < ends[node]; i++) hash = (hash ^ source.charAt(i)) * HASH_PRIME;
        return hash;
    }
    public long primitiveBytes() {
        return (long) kinds.length * 8L * Integer.BYTES
                + (lines == null ? 0L : (long) kinds.length * 4L * Integer.BYTES)
                + (structureHashes == null ? 0L : (long) kinds.length * Long.BYTES);
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
        private int[] lines, endLines, symbolIds, literalIds;
        private String[] symbols;
        private final HashMap<String, Integer> symbolDictionary = new HashMap<>();
        private int symbolCount;
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
            int permitted = EXACT_RANGE | SYNTAX_ONLY | SOURCE_UNVERIFIED | RECOVERED;
            if (domain == Domain.JAVAC) permitted |= STATIC | FINAL | INTERFACE;
            if ((flags & ~permitted) != 0) {
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
            if (symbolIds != null) {
                symbolIds[row] = -1; literalIds[row] = -1;
                lines[row] = -1; endLines[row] = -1;
            }
            if (parent >= 0) {
                if (first[parent] == -1) first[parent] = row;
                else next[last[parent]] = row;
                last[parent] = row;
                childCounts[parent]++;
            }
            return row;
        }

        /**
         * Attach authoritative javac-only facts to the just-added node.
         * The transient hash map interns repeated spellings and does not survive seal().
         */
        public void annotateJavac(int row, int firstLine, int finalLine,
                String simpleName, String literalValue) {
            if (sealed || domain != Domain.JAVAC || row != size - 1) {
                throw new IllegalStateException("Javac facts require current unsealed Javac row");
            }
            if ((firstLine == -1) != (finalLine == -1)
                    || (firstLine != -1 && (firstLine < 1 || finalLine < firstLine))) {
                throw new IllegalArgumentException("invalid javac line span");
            }
            if (lines == null) {
                lines = new int[kinds.length]; endLines = new int[kinds.length];
                symbolIds = new int[kinds.length]; literalIds = new int[kinds.length];
                Arrays.fill(lines, -1); Arrays.fill(endLines, -1);
                Arrays.fill(symbolIds, -1); Arrays.fill(literalIds, -1);
            }
            lines[row] = firstLine; endLines[row] = finalLine;
            symbolIds[row] = atom(simpleName);
            literalIds[row] = atom(literalValue);
        }
        private int atom(String text) {
            if (text == null) return -1;
            if (text.length() > 65_535) {
                throw new IllegalArgumentException("javac value dictionary string budget exceeded");
            }
            Integer existing = symbolDictionary.get(text);
            if (existing != null) return existing;
            if (symbolCount == maxNodes) {
                throw new IllegalArgumentException("javac value dictionary budget exceeded");
            }
            if (symbols == null) symbols = new String[16];
            if (symbolCount == symbols.length) {
                symbols = Arrays.copyOf(symbols,
                        Math.min(maxNodes, Math.max(symbolCount + 1, symbols.length * 2)));
            }
            int slot = symbolCount++;
            symbols[slot] = text;
            symbolDictionary.put(text, slot);
            return slot;
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
            if (lines != null) {
                lines = Arrays.copyOf(lines, cap); endLines = Arrays.copyOf(endLines, cap);
                symbolIds = Arrays.copyOf(symbolIds, cap); literalIds = Arrays.copyOf(literalIds, cap);
            }
        }
    }
}
