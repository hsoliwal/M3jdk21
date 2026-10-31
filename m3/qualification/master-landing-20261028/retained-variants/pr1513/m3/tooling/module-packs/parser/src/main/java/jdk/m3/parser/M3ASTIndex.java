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
 * Optional semantic projection retains only original binding keys and diagnostic text in
 * reference lanes plus primitive kinds/modifiers/flags/ranges. No foreign binding or AST
 * object survives publication. Nodes are occurrence identities within this source generation.
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
    /** Binding flags are independent of structural/recovery flags. */
    public static final int BINDING_RECOVERED = 1, BINDING_DEPRECATED = 2;
    public static final int PROBLEM_ERROR = 1, PROBLEM_WARNING = 2;
    private final String[] bindingKeys, typeKeys, problemMessages;
    private final int[] bindingKinds, bindingModifiers, bindingFlags;
    private final int[] problemIds, problemStarts, problemEnds, problemFlags;
    private final M3File source;
    private final Domain domain;
    private final String[] names;
    private final int[] kinds, parents, starts, ends, flags, first, next, childCounts;

    private M3ASTIndex(Builder builder) {
        bindingKeys = builder.bindingKeys == null ? null : Arrays.copyOf(builder.bindingKeys, builder.size);
        typeKeys = builder.typeKeys == null ? null : Arrays.copyOf(builder.typeKeys, builder.size);
        bindingKinds = builder.bindingKinds == null ? null : Arrays.copyOf(builder.bindingKinds, builder.size);
        bindingModifiers = builder.bindingModifiers == null ? null : Arrays.copyOf(builder.bindingModifiers, builder.size);
        bindingFlags = builder.bindingFlags == null ? null : Arrays.copyOf(builder.bindingFlags, builder.size);
        problemMessages = Arrays.copyOf(builder.problemMessages, builder.problemCount);
        problemIds = Arrays.copyOf(builder.problemIds, builder.problemCount);
        problemStarts = Arrays.copyOf(builder.problemStarts, builder.problemCount);
        problemEnds = Arrays.copyOf(builder.problemEnds, builder.problemCount);
        problemFlags = Arrays.copyOf(builder.problemFlags, builder.problemCount);
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
    /** True only for an explicitly requested semantic projection, including unresolved input. */
    public boolean hasSemanticProjection() { return bindingKeys != null; }
    /** Original JDT key, or null when unresolved; never a cross-environment cache key. */
    public String bindingKey(int node) { check(node); return bindingKeys == null ? null : bindingKeys[node]; }
    public String typeKey(int node) { check(node); return typeKeys == null ? null : typeKeys[node]; }
    public int bindingKind(int node) { check(node); return bindingKinds == null ? 0 : bindingKinds[node]; }
    public int bindingModifiers(int node) { check(node); return bindingModifiers == null ? 0 : bindingModifiers[node]; }
    public int bindingFlags(int node) { check(node); return bindingFlags == null ? 0 : bindingFlags[node]; }
    public int problemCount() { return problemIds.length; }
    public int problemId(int problem) { return problemIds[Objects.checkIndex(problem, problemIds.length)]; }
    public int problemStart(int problem) { return problemStarts[Objects.checkIndex(problem, problemIds.length)]; }
    public int problemEnd(int problem) { return problemEnds[Objects.checkIndex(problem, problemIds.length)]; }
    public int problemFlags(int problem) { return problemFlags[Objects.checkIndex(problem, problemIds.length)]; }
    public String problemMessage(int problem) { return problemMessages[Objects.checkIndex(problem, problemIds.length)]; }
    /** Structural primitive storage; semantic storage is reported separately for compatibility. */
    public long primitiveBytes() { return (long) kinds.length * 8L * Integer.BYTES; }
    public long semanticPrimitiveBytes() {
        return (bindingKinds == null ? 0L : (long) bindingKinds.length * 3L * Integer.BYTES)
                + (long) problemIds.length * 4L * Integer.BYTES;
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
        private String[] bindingKeys, typeKeys;
        private int[] bindingKinds, bindingModifiers, bindingFlags;
        private String[] problemMessages = new String[0];
        private int[] problemIds = new int[0], problemStarts = new int[0];
        private int[] problemEnds = new int[0], problemFlags = new int[0];
        private int problemCount;

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
            requireAncestorRange(parent, start, end);
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
            requireAncestorRange(parents[node], start, end);
            // Children may themselves have unknown ranges. Walk their descendants using
            // the existing parent/sibling lanes, without recursion or a scratch stack.
            for (int child = first[node]; child >= 0;) {
                if (starts[child] >= 0 && (starts[child] < start || ends[child] > end)) {
                    throw new IllegalArgumentException("completed AST range excludes an indexed descendant");
                }
                if (first[child] >= 0) { child = first[child]; continue; }
                while (child != node && next[child] < 0) child = parents[child];
                child = child == node ? -1 : next[child];
            }
            starts[node] = start;
            ends[node] = end;
            if (exact) flags[node] |= EXACT_RANGE;
        }

        private void requireAncestorRange(int parent, int start, int end) {
            if (start < 0) return;
            for (int ancestor = parent; ancestor >= 0; ancestor = parents[ancestor]) {
                if (starts[ancestor] >= 0) {
                    if (start < starts[ancestor] || end > ends[ancestor]) {
                        throw new IllegalArgumentException("AST child escapes known ancestor range");
                    }
                    return; // This known range was already checked against its own ancestors.
                }
            }
        }

        void enableSemantics() {
            if (sealed) throw new IllegalStateException("index already sealed");
            bindingKeys = new String[kinds.length]; typeKeys = new String[kinds.length];
            bindingKinds = new int[kinds.length]; bindingModifiers = new int[kinds.length];
            bindingFlags = new int[kinds.length];
        }

        void binding(int node, String key, String type, int kind, int modifiers, int state) {
            if (sealed || bindingKeys == null) throw new IllegalStateException("semantic index not open");
            Objects.checkIndex(node, size);
            bindingKeys[node] = key; typeKeys[node] = type;
            bindingKinds[node] = kind; bindingModifiers[node] = modifiers; bindingFlags[node] = state;
        }

        void problem(int id, int start, int end, int state, String message) {
            if (sealed || bindingKeys == null) throw new IllegalStateException("semantic index not open");
            if (problemCount == maxNodes) throw new IllegalArgumentException("AST diagnostic budget exceeded");
            if (!((start == -1 && end == -1) || (start >= 0 && end >= start && end <= source.length()))) {
                start = end = -1;
            }
            if (problemCount == problemIds.length) {
                int cap = Math.min(maxNodes, Math.max(8, problemCount + (problemCount >>> 1) + 1));
                problemIds = Arrays.copyOf(problemIds, cap); problemStarts = Arrays.copyOf(problemStarts, cap);
                problemEnds = Arrays.copyOf(problemEnds, cap); problemFlags = Arrays.copyOf(problemFlags, cap);
                problemMessages = Arrays.copyOf(problemMessages, cap);
            }
            problemIds[problemCount] = id; problemStarts[problemCount] = start;
            problemEnds[problemCount] = end; problemFlags[problemCount] = state;
            problemMessages[problemCount++] = Objects.requireNonNull(message, "problem message");
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
            if (bindingKeys != null) {
                bindingKeys = Arrays.copyOf(bindingKeys, cap); typeKeys = Arrays.copyOf(typeKeys, cap);
                bindingKinds = Arrays.copyOf(bindingKinds, cap);
                bindingModifiers = Arrays.copyOf(bindingModifiers, cap);
                bindingFlags = Arrays.copyOf(bindingFlags, cap);
            }
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
