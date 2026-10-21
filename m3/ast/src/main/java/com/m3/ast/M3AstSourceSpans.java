// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors.
package com.m3.ast;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable occurrence-indexed UTF-16 source geometry for a canonical {@link M3Ast}.
 *
 * <p>M3String owns the source text. This plane retains only its caller-supplied owner,
 * coordinate and generation, plus primitive source offsets. It never copies or retains source
 * characters, native pointers or parser nodes. Offsets refer to the <em>logical</em> composite
 * M3String in Java UTF-16 code units, including across tuple/segment boundaries.</p>
 *
 * <p>Geometry belongs to <em>occurrences</em>, not canonical atom rows: two equal subtree
 * occurrences may share the same atom ID but have different source locations. Source positions
 * must arrive in the same root-first preorder as {@link M3Ast#preOrder(int)}. Parser-specific
 * state masks also belong to the occurrence; they do not change atom identity.</p>
 */
public final class M3AstSourceSpans {
    private final M3Ast ast;
    private final long sourceOwnerId;
    private final long sourceCoordinate;
    private final long sourceGeneration;
    private final int sourceUtf16Length;
    private final int[] atomIds;
    private final int[] parents;
    private final int[] childOffsets;
    private final int[] childArena;
    private final int[] rootOccurrences;
    private final int[] starts;
    private final int[] ends;
    private final int[] stateMasks;
    private final long[] knownSpanWords;

    private M3AstSourceSpans(M3Ast ast, long sourceOwnerId, long sourceCoordinate,
            long sourceGeneration, int sourceUtf16Length, int[] atomIds, int[] parents,
            int[] childOffsets, int[] childArena, int[] rootOccurrences,
            int[] starts, int[] ends, int[] stateMasks, long[] knownSpanWords) {
        this.ast = ast;
        this.sourceOwnerId = sourceOwnerId;
        this.sourceCoordinate = sourceCoordinate;
        this.sourceGeneration = sourceGeneration;
        this.sourceUtf16Length = sourceUtf16Length;
        this.atomIds = atomIds;
        this.parents = parents;
        this.childOffsets = childOffsets;
        this.childArena = childArena;
        this.rootOccurrences = rootOccurrences;
        this.starts = starts;
        this.ends = ends;
        this.stateMasks = stateMasks;
        this.knownSpanWords = knownSpanWords;
    }

    /** Create a source plane with zero occurrence-specific state bits. */
    public static M3AstSourceSpans index(M3Ast ast, long ownerId, long coordinate,
            long generation, int utf16Length, int[] starts, int[] ends) {
        return index(ast, ownerId, coordinate, generation, utf16Length, starts, ends, null);
    }

    /**
     * Build a dense preorder projection. An unknown/recovered span is exactly (-1,-1).
     * Half-unknown, reversed, negative or out-of-source positions are rejected. Unlike canonical
     * atom identity, this plane permits parser-generated child spans outside a parent's range;
     * providers are authoritative about recovered/synthetic source geometry.
     *
     * @param stateMasks optional per-occurrence provider state; null means all zeros
     */
    public static M3AstSourceSpans index(M3Ast ast, long ownerId, long coordinate,
            long generation, int utf16Length, int[] starts, int[] ends, int[] stateMasks) {
        Objects.requireNonNull(ast, "ast");
        Objects.requireNonNull(starts, "starts");
        Objects.requireNonNull(ends, "ends");
        if (ownerId <= 0 || coordinate <= 0 || generation < 0 || utf16Length < 0) {
            throw new IllegalArgumentException("invalid M3String source owner/coordinate/generation/length");
        }
        long expected = 0;
        for (int i = 0; i < ast.rootCount(); i++) {
            expected = Math.addExact(expected, ast.expandedSize(ast.root(i)));
        }
        if (expected > Integer.MAX_VALUE - 8L) {
            throw new IllegalArgumentException("AST occurrence lane exceeds JVM limit");
        }
        int n = (int) expected;
        if (starts.length != n || ends.length != n
                || (stateMasks != null && stateMasks.length != n)) {
            throw new IllegalArgumentException("source spans must cover every expanded AST occurrence");
        }
        int[] startsCopy = starts.clone();
        int[] endsCopy = ends.clone();
        int[] statesCopy = stateMasks == null ? new int[n] : stateMasks.clone();
        long[] known = new long[(n >>> 6) + ((n & 63) == 0 ? 0 : 1)];
        for (int i = 0; i < n; i++) {
            int start = startsCopy[i];
            int end = endsCopy[i];
            if (start == -1 && end == -1) continue;
            if (start < 0 || end < start || end > utf16Length) {
                throw new IllegalArgumentException("invalid UTF-16 source span at occurrence " + i);
            }
            known[i >>> 6] |= 1L << (i & 63);
        }

        int[] atomIds = new int[n];
        int[] parents = new int[n];
        int[] childOffsets = new int[n];
        int[] childArena = new int[n - ast.rootCount()];
        int[] roots = new int[ast.rootCount()];
        int stackCapacity = Math.min(Math.max(8, ast.rootCount()), n);
        int[] pendingAtoms = new int[stackCapacity];
        int[] pendingParents = new int[stackCapacity];
        int[] pendingOrdinals = new int[stackCapacity];
        int pending = 0;
        for (int root = ast.rootCount() - 1; root >= 0; root--) {
            pendingAtoms[pending] = ast.root(root);
            pendingParents[pending] = -1;
            pendingOrdinals[pending++] = root;
        }
        int written = 0;
        int childSpace = 0;
        while (pending != 0) {
            int next = --pending;
            int atom = pendingAtoms[next];
            int parent = pendingParents[next];
            int ordinal = pendingOrdinals[next];
            int occurrence = written++;
            atomIds[occurrence] = atom;
            parents[occurrence] = parent;
            if (parent < 0) {
                roots[ordinal] = occurrence;
            } else {
                childArena[childOffsets[parent] + ordinal] = occurrence;
            }
            childOffsets[occurrence] = childSpace;
            int count = ast.childCount(atom);
            childSpace = Math.addExact(childSpace, count);
            int need = pending + count;
            if (need > pendingAtoms.length) {
                int grown = pendingAtoms.length;
                while (grown < need) grown = (int) Math.min((long) n,
                        Math.max((long) need, grown + (grown >>> 1) + 1L));
                pendingAtoms = Arrays.copyOf(pendingAtoms, grown);
                pendingParents = Arrays.copyOf(pendingParents, grown);
                pendingOrdinals = Arrays.copyOf(pendingOrdinals, grown);
            }
            for (int child = count - 1; child >= 0; child--) {
                pendingAtoms[pending] = ast.childAt(atom, child);
                pendingParents[pending] = occurrence;
                pendingOrdinals[pending++] = child;
            }
        }
        if (written != n || childSpace != childArena.length) {
            throw new IllegalStateException("AST expanded-size/child geometry drift");
        }
        return new M3AstSourceSpans(ast, ownerId, coordinate, generation, utf16Length,
                atomIds, parents, childOffsets, childArena, roots,
                startsCopy, endsCopy, statesCopy, known);
    }

    public M3Ast ast() { return ast; }
    public long sourceOwnerId() { return sourceOwnerId; }
    public long sourceCoordinate() { return sourceCoordinate; }
    public long sourceGeneration() { return sourceGeneration; }
    public int sourceUtf16Length() { return sourceUtf16Length; }
    public int occurrenceCount() { return atomIds.length; }
    public int rootCount() { return rootOccurrences.length; }
    public int rootOccurrence(int ordinal) {
        return rootOccurrences[Objects.checkIndex(ordinal, rootOccurrences.length)];
    }
    public int atomId(int occurrence) { return atomIds[checked(occurrence)]; }
    public int parentOccurrence(int occurrence) { return parents[checked(occurrence)]; }
    public int childCount(int occurrence) { return ast.childCount(atomId(occurrence)); }
    public int childOccurrence(int occurrence, int ordinal) {
        int row = checked(occurrence);
        return childArena[childOffsets[row] + Objects.checkIndex(ordinal, childCount(row))];
    }
    public boolean hasSpan(int occurrence) {
        int row = checked(occurrence);
        return (knownSpanWords[row >>> 6] & (1L << (row & 63))) != 0L;
    }
    /** Unknown ranges return -1 for both endpoints. */
    public int startUtf16(int occurrence) { return starts[checked(occurrence)]; }
    public int endUtf16(int occurrence) { return ends[checked(occurrence)]; }
    public int stateMask(int occurrence) { return stateMasks[checked(occurrence)]; }

    /** Generation checking is explicit; the plane does not extend M3String owner lifetime. */
    public void requireSource(long ownerId, long coordinate, long generation) {
        if (sourceOwnerId != ownerId || sourceCoordinate != coordinate
                || sourceGeneration != generation) {
            throw new IllegalStateException("stale or unrelated M3String source generation");
        }
    }

    /** Retained primitive-array payload, excluding headers, alignment and the shared AST. */
    public long primitiveBytes() {
        return 4L * (6L * atomIds.length + childArena.length + rootOccurrences.length)
                + 8L * knownSpanWords.length;
    }

    private int checked(int occurrence) {
        return Objects.checkIndex(occurrence, atomIds.length);
    }
}
